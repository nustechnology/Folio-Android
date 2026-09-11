package com.nus.folio.data.network

import com.nus.folio.domain.model.AskCitation
import com.nus.folio.domain.model.AskConversation
import com.nus.folio.domain.model.AskConversationDetail
import com.nus.folio.domain.model.AskConversationLibrary
import com.nus.folio.domain.model.AskConversationMessage
import com.nus.folio.domain.model.AskConversationPaging
import com.nus.folio.domain.model.AskConversationRole
import com.nus.folio.domain.model.AskFeedbackRating
import com.nus.folio.domain.model.AskStreamEvent
import com.nus.folio.domain.model.AskSuggestions
import com.nus.folio.domain.util.NoteUpdatedLabelFormatter
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
import java.time.Instant
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

    suspend fun listConversations(
        accessToken: String,
        spaceId: String,
        search: String? = null,
        page: Int = AskConversationPaging.DEFAULT_PAGE,
        limit: Int = AskConversationPaging.DEFAULT_LIMIT,
    ): AskConversationLibrary

    suspend fun getConversation(
        accessToken: String,
        spaceId: String,
        conversationId: String,
    ): AskConversationDetail

    suspend fun updateConversation(
        accessToken: String,
        spaceId: String,
        conversationId: String,
        title: String,
    ): AskConversation

    suspend fun deleteConversation(
        accessToken: String,
        spaceId: String,
        conversationId: String,
    )
}

/**
 * Thin HTTP client for Ask endpoints.
 * POST /ask streams SSE; GET /ask/suggestions returns three question chips.
 * Aborting the collecting coroutine disconnects the request so the server can persist
 * a partial answer with `stopped: true`.
 */
class AskApiClient(
    private val baseUrl: String = FolioApiPaths.BASE_URL,
    private val nowInstant: () -> Instant = { Instant.now() },
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

    override suspend fun listConversations(
        accessToken: String,
        spaceId: String,
        search: String?,
        page: Int,
        limit: Int,
    ): AskConversationLibrary = withContext(Dispatchers.IO) {
        val safePage = page.coerceAtLeast(AskConversationPaging.DEFAULT_PAGE)
        val safeLimit = limit.coerceAtLeast(1)
        FolioHttp.get(
            url = FolioApiPaths.spaceConversations(
                spaceId,
                baseUrl,
                buildConversationsQuery(search, safePage, safeLimit),
            ),
            accessToken = accessToken,
            failureLabel = "Get conversations",
            parse = { response ->
                parseConversationsPage(
                    payload = response.body,
                    page = safePage,
                    limit = safeLimit,
                    nowInstant = nowInstant(),
                )
            },
        )
    }

    override suspend fun getConversation(
        accessToken: String,
        spaceId: String,
        conversationId: String,
    ): AskConversationDetail = withContext(Dispatchers.IO) {
        FolioHttp.get(
            url = FolioApiPaths.spaceConversation(spaceId, conversationId, baseUrl),
            accessToken = accessToken,
            failureLabel = "Get conversation",
            parse = { response ->
                parseConversationDetail(response.body, nowInstant())
            },
        )
    }

    override suspend fun updateConversation(
        accessToken: String,
        spaceId: String,
        conversationId: String,
        title: String,
    ): AskConversation = withContext(Dispatchers.IO) {
        val trimmedTitle = title.trim()
        FolioHttp.patchJson(
            url = FolioApiPaths.spaceConversation(spaceId, conversationId, baseUrl),
            jsonBody = buildUpdateConversationJson(trimmedTitle),
            accessToken = accessToken,
            failureLabel = "Update conversation",
            parse = { response ->
                parseUpdatedConversation(
                    payload = response.body,
                    fallback = AskConversation(
                        id = conversationId,
                        title = trimmedTitle,
                        dateLabel = "",
                        spaceId = spaceId,
                    ),
                    nowInstant = nowInstant(),
                )
            },
        )
    }

    override suspend fun deleteConversation(
        accessToken: String,
        spaceId: String,
        conversationId: String,
    ) = withContext(Dispatchers.IO) {
        FolioHttp.delete(
            url = FolioApiPaths.spaceConversation(spaceId, conversationId, baseUrl),
            accessToken = accessToken,
            failureLabel = "Delete conversation",
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

        internal fun buildConversationsQuery(
            search: String?,
            page: Int,
            limit: Int,
        ): String = buildString {
            append("page=")
            append(page)
            append("&limit=")
            append(limit)
            val trimmedSearch = search?.trim().orEmpty()
            if (trimmedSearch.isNotEmpty()) {
                append("&search=")
                append(URLEncoder.encode(trimmedSearch, Charsets.UTF_8.name()))
            }
        }

        internal fun parseConversations(
            payload: String,
            nowInstant: Instant = Instant.now(),
        ): List<AskConversation> {
            if (payload.isBlank()) return emptyList()
            val items = extractJsonArrayObjects(payload, "conversations")
                .ifEmpty { extractJsonArrayObjects(payload, "items") }
            return items.mapNotNull { item ->
                runCatching { parseConversationSummary(item, nowInstant) }.getOrNull()
            }
        }

        internal fun parseConversationsPage(
            payload: String,
            page: Int = AskConversationPaging.DEFAULT_PAGE,
            limit: Int = AskConversationPaging.DEFAULT_LIMIT,
            nowInstant: Instant = Instant.now(),
        ): AskConversationLibrary {
            val conversations = parseConversations(payload, nowInstant)
            if (payload.isBlank()) {
                return AskConversationLibrary(
                    conversations = emptyList(),
                    page = page,
                    limit = limit,
                    totalCount = 0,
                    hasMore = false,
                )
            }
            val pagination = extractJsonObject(payload, "pagination")
            val responsePage = pagination?.let { matchJsonInt(it, "page") } ?: page
            val responseLimit = pagination?.let { matchJsonInt(it, "limit") } ?: limit
            val totalCount = pagination?.let { matchJsonInt(it, "totalCount") }
            val totalPages = pagination?.let { matchJsonInt(it, "totalPages") }
            val hasMore = when {
                totalPages != null -> responsePage < totalPages
                totalCount != null -> responsePage * responseLimit < totalCount
                else -> conversations.size >= responseLimit
            }
            return AskConversationLibrary(
                conversations = conversations,
                page = responsePage,
                limit = responseLimit,
                totalCount = (totalCount ?: conversations.size).coerceAtLeast(0),
                hasMore = hasMore,
            )
        }

        internal fun parseConversationDetail(
            payload: String,
            nowInstant: Instant = Instant.now(),
        ): AskConversationDetail {
            if (payload.isBlank()) {
                throw IOException("Get conversation failed: empty response")
            }
            val conversationJson = extractJsonObject(payload, "conversation") ?: payload
            val conversation = parseConversationSummary(conversationJson, nowInstant)
            val messages = extractJsonArrayObjects(conversationJson, "messages")
                .ifEmpty { extractJsonArrayObjects(payload, "messages") }
                .map { parseConversationMessage(it) }
            return AskConversationDetail(
                conversation = conversation,
                messages = messages,
            )
        }

        internal fun parseConversationSummary(
            json: String,
            nowInstant: Instant = Instant.now(),
        ): AskConversation {
            val id = firstQuoted(json, "id", "conversationId")
                ?: throw IOException("Conversation payload missing id")
            val spaceId = firstQuoted(json, "researchSpaceId", "spaceId").orEmpty()
            val title = firstQuoted(json, "title", "name", "preview")
                ?: "Untitled conversation"
            val updatedAt = firstQuoted(json, "updatedAt", "createdAt").orEmpty()
            return AskConversation(
                id = id,
                title = title,
                dateLabel = formatConversationDateLabel(updatedAt, nowInstant),
                spaceId = spaceId,
                sourceId = parseConversationScopeSourceId(json),
            )
        }

        /**
         * Reads `scope: { type, sourceId }` from the conversation object only.
         * Nested citation `sourceId` values must not be treated as the thread scope.
         */
        internal fun parseConversationScopeSourceId(json: String): String? {
            val scopeJson = extractJsonObject(json, "scope")
            val type = (scopeJson?.let { firstQuoted(it, "type") } ?: firstQuoted(json, "scope"))
                ?.trim()
                ?.lowercase()
            val sourceId = scopeJson
                ?.let { firstQuoted(it, "sourceId", "source_id") }
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
            return when (type) {
                SCOPE_SPACE, "entire_space", "entirespace" -> null
                else -> sourceId
            }
        }

        internal fun parseConversationMessage(json: String): AskConversationMessage {
            val id = firstQuoted(json, "id", "messageId").orEmpty()
            val content = firstQuoted(json, "content", "text", "question").orEmpty()
            return AskConversationMessage(
                id = id,
                role = mapConversationRole(firstQuoted(json, "role")),
                content = content,
                citations = parseCitations(json),
                limitation = firstQuoted(json, "limitation"),
                stopped = matchJsonBoolean(json, "stopped") ?: false,
                feedback = mapConversationFeedback(
                    firstQuoted(json, "feedback", "rating"),
                ),
                savedNoteId = firstQuoted(json, "savedNoteId"),
            )
        }

        internal fun mapConversationRole(raw: String?): AskConversationRole =
            when (raw?.trim()?.lowercase()) {
                "user", "human" -> AskConversationRole.USER
                else -> AskConversationRole.ASSISTANT
            }

        internal fun mapConversationFeedback(raw: String?): AskFeedbackRating? =
            when (raw?.trim()?.lowercase()) {
                "useful" -> AskFeedbackRating.USEFUL
                "not_useful", "notuseful", "not-useful" -> AskFeedbackRating.NOT_USEFUL
                else -> null
            }

        internal fun formatConversationDateLabel(
            isoInstant: String,
            nowInstant: Instant,
        ): String {
            val instant = parseIsoInstant(isoInstant) ?: return NoteUpdatedLabelFormatter.format(
                instant = nowInstant,
            )
            return NoteUpdatedLabelFormatter.format(instant = instant)
        }

        private fun parseIsoInstant(isoInstant: String): Instant? {
            val value = isoInstant.trim()
            if (value.isEmpty()) return null
            return runCatching { Instant.parse(value) }.getOrNull()
                ?: runCatching { Instant.parse(value.replace(" ", "T")) }.getOrNull()
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

        internal fun buildUpdateConversationJson(title: String): String = buildString {
            append('{')
            appendJsonField("title", title)
            append('}')
        }

        internal fun parseUpdatedConversation(
            payload: String,
            fallback: AskConversation,
            nowInstant: Instant = Instant.now(),
        ): AskConversation {
            if (payload.isBlank()) return fallback
            val json = extractJsonObject(payload, "conversation") ?: payload
            return runCatching { parseConversationSummary(json, nowInstant) }
                .getOrElse { fallback }
                .let { parsed ->
                    parsed.copy(
                        title = parsed.title.takeIf { it.isNotBlank() && it != "Untitled conversation" }
                            ?: fallback.title,
                        dateLabel = parsed.dateLabel.ifBlank { fallback.dateLabel },
                        spaceId = parsed.spaceId.ifBlank { fallback.spaceId },
                        sourceId = parsed.sourceId ?: fallback.sourceId,
                    )
                }
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
            ) ?: page?.let { "Page $it" }
                ?: pageReferenceLabel(firstQuoted(json, "pageReference"))
                    .orEmpty()
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

        private fun pageReferenceLabel(raw: String?): String? {
            val trimmed = raw?.trim().orEmpty()
            if (trimmed.isEmpty()) return null
            if (trimmed.startsWith("Page", ignoreCase = true)) return trimmed
            if (trimmed.all { it.isDigit() }) return "Page $trimmed"
            return trimmed
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

        internal fun extractJsonObject(payload: String, key: String): String? {
            val header = Regex("\"$key\"\\s*:\\s*\\{").find(payload) ?: return null
            val openIndex = header.range.last
            val end = matchingBrace(payload, openIndex) ?: return null
            return payload.substring(openIndex, end + 1)
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
