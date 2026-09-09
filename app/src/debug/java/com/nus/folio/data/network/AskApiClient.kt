package com.nus.folio.data.network

import com.nus.folio.domain.model.AskCitation
import com.nus.folio.domain.model.AskStreamEvent
import com.nus.folio.domain.model.AskSuggestions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference

interface AskApi {
    suspend fun getSuggestions(
        accessToken: String,
        spaceId: String,
        sourceId: String?,
    ): AskSuggestions

    fun streamAnswer(
        accessToken: String,
        spaceId: String,
        question: String,
        sourceId: String?,
        conversationId: String?,
    ): Flow<AskStreamEvent>

    suspend fun submitFeedback(
        accessToken: String,
        spaceId: String,
        conversationId: String,
        messageId: String,
        rating: String,
    )
}

/**
 * Thin HTTP client for Ask endpoints (debug builds against the ngrok/dev backend).
 * POST /ask streams SSE; GET /ask/suggestions returns three question chips.
 * Aborting the collecting coroutine disconnects the request so the server can persist
 * a partial answer with `stopped: true`.
 */
class AskApiClient(
    private val baseUrl: String = FolioApiPaths.BASE_URL,
) : AskApi {

    override suspend fun getSuggestions(
        accessToken: String,
        spaceId: String,
        sourceId: String?,
    ): AskSuggestions = withContext(Dispatchers.IO) {
        val trimmedSourceId = sourceId?.trim().orEmpty()
        val query = buildString {
            append("scope=")
            if (trimmedSourceId.isEmpty()) {
                append(SCOPE_SPACE)
            } else {
                append(SCOPE_SOURCE)
                append("&sourceId=")
                append(URLEncoder.encode(trimmedSourceId, Charsets.UTF_8.name()))
            }
        }
        FolioHttp.get(
            url = FolioApiPaths.spaceAskSuggestions(spaceId, baseUrl, query),
            accessToken = accessToken,
            failureLabel = "Get ask suggestions",
            parse = { response -> parseAskSuggestions(response.body) },
        )
    }

    override fun streamAnswer(
        accessToken: String,
        spaceId: String,
        question: String,
        sourceId: String?,
        conversationId: String?,
    ): Flow<AskStreamEvent> = callbackFlow {
        val url = FolioApiPaths.spaceAsk(spaceId, baseUrl)
        val jsonBody = buildAskRequestJson(
            question = question,
            sourceId = sourceId,
            conversationId = conversationId,
        )
        val activeConnection = AtomicReference<HttpURLConnection?>(null)

        val readerJob = launch(Dispatchers.IO) {
            var responseLogged = false
            val connection = try {
                FolioHttp.open(
                    method = "POST",
                    url = url,
                    headers = FolioHttp.jsonContentHeaders(accessToken) + mapOf(
                        "Accept" to "text/event-stream",
                        "Cache-Control" to "no-cache",
                    ),
                    connectTimeoutMs = FolioHttp.LONG_TIMEOUT_MS,
                    readTimeoutMs = 0,
                    doOutput = true,
                )
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                HttpDebugLogger.logError(method = "POST", url = url, error = error)
                close(error)
                return@launch
            }
            activeConnection.set(connection)
            try {
                HttpDebugLogger.logRequest(
                    method = "POST",
                    url = url,
                    body = jsonBody,
                    contentType = "application/json",
                )
                val bodyBytes = jsonBody.toByteArray(Charsets.UTF_8)
                connection.setFixedLengthStreamingMode(bodyBytes.size)
                connection.outputStream.use { output -> output.write(bodyBytes) }

                val code = connection.responseCode
                if (code !in 200..299) {
                    val responseBody = FolioHttp.readBody(connection.errorStream)
                    HttpDebugLogger.logResponse(
                        method = "POST",
                        url = url,
                        code = code,
                        body = responseBody,
                    )
                    responseLogged = true
                    close(FolioHttp.unauthorizedOrIo(responseBody, code, "Ask"))
                    return@launch
                }
                HttpDebugLogger.logResponse(
                    method = "POST",
                    url = url,
                    code = code,
                    body = "SSE stream opened",
                )
                responseLogged = true

                BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8)).use { reader ->
                    var eventName = ""
                    val dataLines = mutableListOf<String>()
                    while (isActive) {
                        val line = reader.readLine() ?: break
                        when {
                            line.startsWith("event:") -> {
                                eventName = line.removePrefix("event:").trim()
                            }
                            line.startsWith("data:") -> {
                                dataLines += line.removePrefix("data:").trimStart()
                            }
                            line.startsWith(":") ||
                                line.startsWith("id:") ||
                                line.startsWith("retry:") -> {
                                // ignore SSE comments / metadata
                            }
                            line.isEmpty() -> {
                                val payload = dataLines.joinToString("\n").trim()
                                val currentEvent = eventName
                                dataLines.clear()
                                eventName = ""
                                when (val parsed = parseAskSseEvent(currentEvent, payload)) {
                                    is AskSseParseResult.Event -> {
                                        HttpDebugLogger.logEvent(
                                            "SSE ask: ${parsed.event.javaClass.simpleName}",
                                        )
                                        send(parsed.event)
                                        if (parsed.event is AskStreamEvent.Completed) {
                                            close()
                                            return@launch
                                        }
                                    }
                                    is AskSseParseResult.Failure -> {
                                        close(parsed.error)
                                        return@launch
                                    }
                                    AskSseParseResult.Ignored -> Unit
                                }
                            }
                        }
                    }
                    if (dataLines.isNotEmpty()) {
                        when (val parsed = parseAskSseEvent(eventName, dataLines.joinToString("\n").trim())) {
                            is AskSseParseResult.Event -> {
                                send(parsed.event)
                                if (parsed.event is AskStreamEvent.Completed) {
                                    close()
                                    return@launch
                                }
                            }
                            is AskSseParseResult.Failure -> {
                                close(parsed.error)
                                return@launch
                            }
                            AskSseParseResult.Ignored -> Unit
                        }
                    }
                }
                if (isActive) {
                    close(IOException("Ask stream ended unexpectedly"))
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                if (!responseLogged) {
                    HttpDebugLogger.logError(method = "POST", url = url, error = error)
                }
                close(error)
            } finally {
                connection.disconnect()
                activeConnection.compareAndSet(connection, null)
            }
        }

        awaitClose {
            readerJob.cancel()
            activeConnection.getAndSet(null)?.disconnect()
        }
    }

    override suspend fun submitFeedback(
        accessToken: String,
        spaceId: String,
        conversationId: String,
        messageId: String,
        rating: String,
    ) = withContext(Dispatchers.IO) {
        FolioHttp.postJson(
            url = FolioApiPaths.spaceAskMessageFeedback(
                spaceId = spaceId,
                conversationId = conversationId,
                messageId = messageId,
                baseUrl = baseUrl,
            ),
            jsonBody = buildAskFeedbackJson(rating),
            accessToken = accessToken,
            failureLabel = "Submit ask feedback",
            parse = { },
        )
    }

    companion object {
        const val SCOPE_SPACE = "space"
        const val SCOPE_SOURCE = "source"
        private const val MAX_SUGGESTIONS = 3

        internal fun parseAskSuggestions(payload: String): AskSuggestions {
            if (payload.isBlank()) {
                return AskSuggestions(questions = emptyList(), isDynamic = false)
            }
            val questions = extractJsonStringArray(payload, "suggestions")
                .ifEmpty { extractJsonStringArray(payload, "questions") }
                .take(MAX_SUGGESTIONS)
            val isDynamic = matchJsonBoolean(payload, "isDynamic") ?: false
            return AskSuggestions(questions = questions, isDynamic = isDynamic)
        }

        internal fun buildAskRequestJson(
            question: String,
            sourceId: String?,
            conversationId: String?,
        ): String = buildString {
            append('{')
            appendJsonField("question", question)
            val trimmedSourceId = sourceId?.trim().orEmpty()
            append(',')
            if (trimmedSourceId.isEmpty()) {
                appendJsonField("scope", SCOPE_SPACE)
            } else {
                appendJsonField("scope", SCOPE_SOURCE)
                append(',')
                appendJsonField("sourceId", trimmedSourceId)
            }
            conversationId?.trim()?.takeIf { it.isNotEmpty() }?.let { id ->
                append(',')
                appendJsonField("conversationId", id)
            }
            append('}')
        }

        internal fun buildAskFeedbackJson(rating: String): String = buildString {
            append('{')
            appendJsonField("rating", rating)
            append('}')
        }

        internal fun parseAskSseEvent(event: String, data: String): AskSseParseResult {
            if (data.isBlank() || data == "[DONE]") return AskSseParseResult.Ignored
            val type = resolveEventType(event, data)
            return when (type) {
                "start" -> {
                    val conversationId = firstQuoted(
                        data,
                        "conversationId",
                        "conversation_id",
                    )
                    val messageId = firstQuoted(data, "messageId", "message_id").orEmpty()
                    if (conversationId.isNullOrBlank()) {
                        AskSseParseResult.Ignored
                    } else {
                        AskSseParseResult.Event(
                            AskStreamEvent.Started(
                                conversationId = conversationId,
                                messageId = messageId,
                            ),
                        )
                    }
                }
                "token" -> {
                    val text = firstQuotedRaw(data, "text", "content", "delta", "token").orEmpty()
                    if (text.isEmpty()) {
                        AskSseParseResult.Ignored
                    } else {
                        AskSseParseResult.Event(AskStreamEvent.Delta(text))
                    }
                }
                "citations" -> {
                    val citations = parseCitations(data)
                    if (citations.isEmpty()) {
                        AskSseParseResult.Ignored
                    } else {
                        AskSseParseResult.Event(AskStreamEvent.Citations(citations))
                    }
                }
                "done" -> {
                    AskSseParseResult.Event(
                        AskStreamEvent.Completed(
                            messageId = firstQuoted(data, "messageId", "message_id").orEmpty(),
                            content = matchQuotedString(data, "content"),
                            citations = parseCitations(data),
                            limitation = firstQuoted(data, "limitation"),
                            stopped = matchJsonBoolean(data, "stopped") ?: false,
                        ),
                    )
                }
                "error" -> {
                    val message = firstQuoted(data, "message", "detail", "error")
                        ?: "Ask generation failed"
                    val code = firstQuoted(data, "code")
                    val detail = if (code.isNullOrBlank()) message else "$message ($code)"
                    AskSseParseResult.Failure(IOException(detail))
                }
                else -> AskSseParseResult.Ignored
            }
        }

        internal fun parseCitations(payload: String): List<AskCitation> {
            val objects = extractJsonArrayObjects(payload, "citations")
                .ifEmpty { extractJsonArrayObjects(payload, "data") }
            return objects.mapIndexed { index, item ->
                parseCitation(item, fallbackIndex = index + 1)
            }
        }

        private fun parseCitation(json: String, fallbackIndex: Int): AskCitation {
            val index = matchJsonInt(json, "index")?.takeIf { it > 0 }
                ?: matchJsonInt(json, "n")?.takeIf { it > 0 }
                ?: fallbackIndex
            val page = matchJsonInt(json, "page")?.takeIf { it > 0 }
                ?: matchJsonInt(json, "pageNumber")?.takeIf { it > 0 }
            val locationLabel = firstQuoted(
                json,
                "locationLabel",
                "location",
                "locator",
                "pageLabel",
            ) ?: page?.let { "Page $it" }.orEmpty()
            return AskCitation(
                index = index,
                sourceId = firstQuoted(json, "sourceId", "source_id").orEmpty(),
                sourceTitle = firstQuoted(
                    json,
                    "sourceTitle",
                    "title",
                    "sourceName",
                    "name",
                ).orEmpty(),
                sourceType = SourcesJsonParsers.mapSourceType(
                    firstQuoted(json, "sourceType", "type").orEmpty(),
                ),
                fileExtension = firstQuoted(
                    json,
                    "fileExtension",
                    "fileType",
                    "extension",
                ).orEmpty(),
                locationLabel = locationLabel,
                evidenceText = firstQuoted(
                    json,
                    "evidenceText",
                    "quote",
                    "snippet",
                    "passage",
                    "excerpt",
                    "evidence",
                ).orEmpty(),
            )
        }

        private fun resolveEventType(event: String, data: String): String {
            val named = event.trim().lowercase(Locale.US)
            if (named.isNotEmpty()) return named
            return firstQuoted(data, "type", "event", "frame")
                ?.lowercase(Locale.US)
                .orEmpty()
        }

        private fun StringBuilder.appendJsonField(key: String, value: String) {
            append('"')
            append(key)
            append("\":")
            append(jsonQuoted(value))
        }

        internal fun jsonQuoted(value: String): String = buildString {
            append('"')
            value.forEach { ch ->
                when (ch) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(ch)
                }
            }
            append('"')
        }

        private fun firstQuoted(payload: String, vararg keys: String): String? {
            for (key in keys) {
                val value = matchQuotedString(payload, key)?.trim()
                if (!value.isNullOrEmpty() && !value.equals("null", ignoreCase = true)) {
                    return value
                }
            }
            return null
        }

        private fun firstQuotedRaw(payload: String, vararg keys: String): String? {
            for (key in keys) {
                val value = matchQuotedString(payload, key)
                if (!value.isNullOrEmpty() && !value.equals("null", ignoreCase = true)) {
                    return value
                }
            }
            return null
        }

        /** Quoted JSON string value; empty string is kept, missing/null returns null. */
        internal fun matchQuotedString(payload: String, key: String): String? {
            val header = Regex("\"$key\"\\s*:").find(payload) ?: return null
            var index = header.range.last + 1
            while (index < payload.length && payload[index].isWhitespace()) {
                index++
            }
            if (index >= payload.length || payload[index] != '"') return null
            return readQuotedString(payload, index)?.first
        }

        internal fun matchJsonInt(payload: String, key: String): Int? {
            val regex = Regex("\"$key\"\\s*:\\s*(-?\\d+)")
            return regex.find(payload)?.groupValues?.getOrNull(1)?.toIntOrNull()
        }

        internal fun matchJsonBoolean(payload: String, key: String): Boolean? {
            val regex = Regex("\"$key\"\\s*:\\s*(true|false)")
            return regex.find(payload)?.groupValues?.getOrNull(1)?.toBooleanStrictOrNull()
        }

        internal fun extractJsonArrayObjects(payload: String, key: String): List<String> {
            val header = Regex("\"$key\"\\s*:\\s*\\[").find(payload) ?: return emptyList()
            val start = header.range.last + 1
            val objects = mutableListOf<String>()
            var index = start
            while (index < payload.length) {
                when (payload[index]) {
                    ']' -> break
                    '{' -> {
                        val end = matchingBrace(payload, index) ?: break
                        objects += payload.substring(index, end + 1)
                        index = end + 1
                    }
                    else -> index++
                }
            }
            return objects
        }

        internal fun extractJsonStringArray(payload: String, key: String): List<String> {
            val header = Regex("\"$key\"\\s*:\\s*\\[").find(payload) ?: return emptyList()
            val start = header.range.last + 1
            val values = mutableListOf<String>()
            var index = start
            while (index < payload.length) {
                when (payload[index]) {
                    ']' -> break
                    '"' -> {
                        val (value, nextIndex) = readQuotedString(payload, index) ?: break
                        values += value
                        index = nextIndex
                    }
                    '{' -> {
                        val end = matchingBrace(payload, index) ?: break
                        firstQuoted(
                            payload.substring(index, end + 1),
                            "question",
                            "text",
                            "content",
                        )?.let { values += it }
                        index = end + 1
                    }
                    else -> index++
                }
            }
            return values.map { it.trim() }.filter { it.isNotEmpty() }
        }

        private fun readQuotedString(payload: String, openIndex: Int): Pair<String, Int>? {
            if (openIndex >= payload.length || payload[openIndex] != '"') return null
            val builder = StringBuilder()
            var index = openIndex + 1
            while (index < payload.length) {
                when (val ch = payload[index]) {
                    '"' -> return builder.toString() to (index + 1)
                    '\\' -> {
                        val escapeIndex = index + 1
                        if (escapeIndex >= payload.length) return null
                        when (val escaped = payload[escapeIndex]) {
                            'u' -> {
                                val hexStart = escapeIndex + 1
                                val hexEnd = hexStart + 4
                                if (hexEnd > payload.length) return null
                                val code = payload.substring(hexStart, hexEnd).toIntOrNull(16)
                                    ?: return null
                                builder.append(code.toChar())
                                index = hexEnd
                            }
                            'b' -> {
                                builder.append('\b')
                                index = escapeIndex + 1
                            }
                            'f' -> {
                                builder.append('\u000C')
                                index = escapeIndex + 1
                            }
                            'n' -> {
                                builder.append('\n')
                                index = escapeIndex + 1
                            }
                            'r' -> {
                                builder.append('\r')
                                index = escapeIndex + 1
                            }
                            't' -> {
                                builder.append('\t')
                                index = escapeIndex + 1
                            }
                            '"', '\\', '/' -> {
                                builder.append(escaped)
                                index = escapeIndex + 1
                            }
                            else -> return null
                        }
                    }
                    else -> {
                        builder.append(ch)
                        index++
                    }
                }
            }
            return null
        }

        private fun matchingBrace(payload: String, openIndex: Int): Int? {
            var depth = 0
            var inString = false
            var escape = false
            for (index in openIndex until payload.length) {
                val ch = payload[index]
                if (inString) {
                    when {
                        escape -> escape = false
                        ch == '\\' -> escape = true
                        ch == '"' -> inString = false
                    }
                    continue
                }
                when (ch) {
                    '"' -> inString = true
                    '{' -> depth++
                    '}' -> {
                        depth--
                        if (depth == 0) return index
                    }
                }
            }
            return null
        }
    }
}

internal sealed interface AskSseParseResult {
    data class Event(val event: AskStreamEvent) : AskSseParseResult
    data class Failure(val error: IOException) : AskSseParseResult
    data object Ignored : AskSseParseResult
}
