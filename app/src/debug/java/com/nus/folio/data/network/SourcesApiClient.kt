package com.nus.folio.data.network

import com.nus.folio.domain.model.Source
import com.nus.folio.domain.model.SourceContentFormat
import com.nus.folio.domain.model.SourceDetail
import com.nus.folio.domain.model.SourceProcessingEvent
import com.nus.folio.domain.model.SourceProcessingState
import com.nus.folio.domain.model.SourceSheetTab
import com.nus.folio.domain.model.SourceSort
import com.nus.folio.domain.model.SourceStatus
import com.nus.folio.domain.model.SourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.DataOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

interface SourcesApi {
    suspend fun listSources(
        accessToken: String,
        spaceId: String,
        sourceType: String? = null,
        search: String? = null,
        sort: String = SourceSort.DEFAULT.apiValue,
    ): List<Source>

    suspend fun getSource(
        accessToken: String,
        sourceId: String,
    ): SourceDetail

    suspend fun createWebSource(
        accessToken: String,
        spaceId: String,
        sourceUrl: String,
        title: String,
        author: String,
    ): Source

    suspend fun createManualSource(
        accessToken: String,
        spaceId: String,
        title: String,
        author: String,
        content: String,
    ): Source

    suspend fun createFileSource(
        accessToken: String,
        spaceId: String,
        title: String,
        author: String,
        fileName: String,
        mimeType: String,
        fileBytes: ByteArray,
    ): Source

    suspend fun retrySource(
        accessToken: String,
        sourceId: String,
    ): SourceDetail?

    suspend fun deleteSource(
        accessToken: String,
        sourceId: String,
    )

    /**
     * Updates title/author. [content] is included only for Manual/TEXT sources;
     * pass null for Web/File so the field is omitted from the body.
     */
    suspend fun updateSource(
        accessToken: String,
        sourceId: String,
        title: String,
        author: String,
        content: String? = null,
    ): Source

    /**
     * Returns a public/anonymous MinIO preview URL for the source, or null when unavailable.
     */
    suspend fun getSourcePreview(
        accessToken: String,
        sourceId: String,
    ): String?

    fun observeSourceStatus(accessToken: String): Flow<SourceProcessingEvent>
}

/**
 * Thin HTTP client for Folio sources create endpoints (debug builds against the ngrok/dev backend).
 */
class SourcesApiClient(
    private val baseUrl: String = FolioApiPaths.BASE_URL,
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
    private val multipartBoundaryFactory: () -> String = ::randomMultipartBoundary,
    private val delayMillis: suspend (Long) -> Unit = { delay(it) },
    private val refreshAccessToken: suspend () -> String? = { null },
) : SourcesApi {

    override suspend fun listSources(
        accessToken: String,
        spaceId: String,
        sourceType: String?,
        search: String?,
        sort: String,
    ): List<Source> = withContext(Dispatchers.IO) {
        val query = buildString {
            append("spaceId=")
            append(URLEncoder.encode(spaceId, Charsets.UTF_8.name()))
            append("&sort=")
            append(URLEncoder.encode(sort, Charsets.UTF_8.name()))
            val trimmedType = sourceType?.trim().orEmpty()
            if (trimmedType.isNotEmpty()) {
                append("&sourceType=")
                append(URLEncoder.encode(trimmedType, Charsets.UTF_8.name()))
            }
            val trimmedSearch = search?.trim().orEmpty()
            if (trimmedSearch.isNotEmpty()) {
                append("&search=")
                append(URLEncoder.encode(trimmedSearch, Charsets.UTF_8.name()))
            }
        }
        FolioHttp.get(
            url = FolioApiPaths.sources(baseUrl, query),
            accessToken = accessToken,
            connectTimeoutMs = TIMEOUT_MS,
            readTimeoutMs = TIMEOUT_MS,
            failureLabel = "Get sources",
            parse = { response -> SourcesJsonParsers.parseSourcesList(response.body, nowMillis()) },
        )
    }

    override suspend fun getSource(
        accessToken: String,
        sourceId: String,
    ): SourceDetail = withContext(Dispatchers.IO) {
        FolioHttp.get(
            url = FolioApiPaths.source(sourceId, baseUrl),
            accessToken = accessToken,
            connectTimeoutMs = TIMEOUT_MS,
            readTimeoutMs = TIMEOUT_MS,
            failureLabel = "Get source",
            parse = { response ->
                SourcesJsonParsers.parseSourceDetailResponse(response.body, nowMillis())
            },
        )
    }

    override suspend fun createWebSource(
        accessToken: String,
        spaceId: String,
        sourceUrl: String,
        title: String,
        author: String,
    ): Source = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("spaceId", spaceId)
            .put("sourceType", SOURCE_TYPE_WEB)
            .put("sourceUrl", sourceUrl)
            .put("title", title)
            .put("author", author)
        createSource(accessToken, body)
    }

    override suspend fun createManualSource(
        accessToken: String,
        spaceId: String,
        title: String,
        author: String,
        content: String,
    ): Source = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("spaceId", spaceId)
            .put("sourceType", SOURCE_TYPE_MANUAL)
            .put("title", title)
            .put("author", author)
            .put("content", content)
        createSource(accessToken, body)
    }

    override suspend fun createFileSource(
        accessToken: String,
        spaceId: String,
        title: String,
        author: String,
        fileName: String,
        mimeType: String,
        fileBytes: ByteArray,
    ): Source = withContext(Dispatchers.IO) {
        val boundary = chooseMultipartBoundary(fileBytes, multipartBoundaryFactory)
        val url = FolioApiPaths.sources(baseUrl)
        val connection = FolioHttp.open(
            method = "POST",
            url = url,
            headers = FolioHttp.jsonAcceptHeaders(accessToken) +
                ("Content-Type" to "multipart/form-data; boundary=$boundary"),
            connectTimeoutMs = TIMEOUT_MS,
            readTimeoutMs = TIMEOUT_MS,
            doOutput = true,
        )

        var responseLogged = false
        try {
            HttpDebugLogger.logRequest(
                method = "POST",
                url = url,
                body = "multipart: spaceId=$spaceId, sourceType=$SOURCE_TYPE_FILE, " +
                    "title=$title, author=$author, fileName=$fileName, " +
                    "mimeType=$mimeType, fileBytes=${fileBytes.size}",
                contentType = "multipart/form-data",
            )
            val resolvedMimeType = mimeType.ifBlank { "application/octet-stream" }
            connection.setFixedLengthStreamingMode(
                multipartBodyContentLength(
                    boundary = boundary,
                    spaceId = spaceId,
                    title = title,
                    author = author,
                    fileName = fileName,
                    mimeType = resolvedMimeType,
                    fileBytesSize = fileBytes.size,
                ),
            )
            DataOutputStream(connection.outputStream).use { output ->
                writeFormField(output, boundary, "spaceId", spaceId)
                writeFormField(output, boundary, "sourceType", SOURCE_TYPE_FILE)
                writeFormField(output, boundary, "title", title)
                writeFormField(output, boundary, "author", author)
                writeFileField(
                    output = output,
                    boundary = boundary,
                    fieldName = "file",
                    fileName = fileName,
                    mimeType = resolvedMimeType,
                    fileBytes = fileBytes,
                )
                output.writeUtf8("--$boundary--\r\n")
                output.flush()
            }

            val code = connection.responseCode
            val responseBody = FolioHttp.readBody(
                if (code in 200..299) connection.inputStream else connection.errorStream,
            )
            HttpDebugLogger.logResponse(method = "POST", url = url, code = code, body = responseBody)
            responseLogged = true
            if (code !in 200..299) {
                throw FolioHttp.unauthorizedOrIo(responseBody, code, "Create source")
            }
            SourcesJsonParsers.parseCreatedSource(responseBody, nowMillis())
        } catch (error: Throwable) {
            if (!responseLogged) {
                HttpDebugLogger.logError(method = "POST", url = url, error = error)
            }
            throw error
        } finally {
            connection.disconnect()
        }
    }

    override suspend fun retrySource(
        accessToken: String,
        sourceId: String,
    ): SourceDetail? = withContext(Dispatchers.IO) {
        FolioHttp.postEmpty(
            url = FolioApiPaths.sourceRetry(sourceId, baseUrl),
            accessToken = accessToken,
            connectTimeoutMs = TIMEOUT_MS,
            readTimeoutMs = TIMEOUT_MS,
            failureLabel = "Retry source",
            parse = { response ->
                if (response.body.isBlank()) {
                    null
                } else {
                    runCatching {
                        SourcesJsonParsers.parseSourceDetailResponse(response.body, nowMillis())
                    }.getOrNull()
                }
            },
        )
    }

    override suspend fun deleteSource(
        accessToken: String,
        sourceId: String,
    ): Unit = withContext(Dispatchers.IO) {
        FolioHttp.delete(
            url = FolioApiPaths.source(sourceId, baseUrl),
            accessToken = accessToken,
            connectTimeoutMs = TIMEOUT_MS,
            readTimeoutMs = TIMEOUT_MS,
            failureLabel = "Delete source",
        )
    }

    override suspend fun updateSource(
        accessToken: String,
        sourceId: String,
        title: String,
        author: String,
        content: String?,
    ): Source = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("title", title)
            .put("author", author)
        if (content != null) {
            body.put("content", content)
        }
        FolioHttp.patchJson(
            url = FolioApiPaths.source(sourceId, baseUrl),
            jsonBody = body.toString(),
            accessToken = accessToken,
            connectTimeoutMs = TIMEOUT_MS,
            readTimeoutMs = TIMEOUT_MS,
            failureLabel = "Update source",
            parse = { response ->
                SourcesJsonParsers.parseUpdatedSource(response.body, nowMillis())
            },
        )
    }

    override suspend fun getSourcePreview(
        accessToken: String,
        sourceId: String,
    ): String? = withContext(Dispatchers.IO) {
        FolioHttp.get(
            url = FolioApiPaths.sourcePreview(sourceId, baseUrl),
            accessToken = accessToken,
            connectTimeoutMs = TIMEOUT_MS,
            readTimeoutMs = TIMEOUT_MS,
            failureLabel = "Get source preview",
            requireSuccess = false,
            parse = { response ->
                when {
                    response.code == HttpURLConnection.HTTP_NOT_FOUND -> null
                    response.code !in 200..299 -> throw FolioHttp.unauthorizedOrIo(
                        response.body,
                        response.code,
                        "Get source preview",
                    )
                    else -> SourcesJsonParsers.parsePreviewUrl(response.body)
                }
            },
        )
    }

    override fun observeSourceStatus(accessToken: String): Flow<SourceProcessingEvent> =
        SourcesSseClient.observeSourceStatus(
            accessToken = accessToken,
            baseUrl = baseUrl,
            refreshAccessToken = refreshAccessToken,
            delayMillis = delayMillis,
        )

    private fun createSource(accessToken: String, body: JSONObject): Source =
        FolioHttp.postJson(
            url = FolioApiPaths.sources(baseUrl),
            jsonBody = body.toString(),
            accessToken = accessToken,
            connectTimeoutMs = TIMEOUT_MS,
            readTimeoutMs = TIMEOUT_MS,
            failureLabel = "Create source",
            parse = { response -> SourcesJsonParsers.parseCreatedSource(response.body, nowMillis()) },
        )

    private fun writeFormField(
        output: DataOutputStream,
        boundary: String,
        name: String,
        value: String,
    ) {
        output.writeUtf8("--$boundary\r\n")
        output.writeUtf8("Content-Disposition: form-data; name=\"$name\"\r\n\r\n")
        output.write(value.toByteArray(Charsets.UTF_8))
        output.writeUtf8("\r\n")
    }

    private fun writeFileField(
        output: DataOutputStream,
        boundary: String,
        fieldName: String,
        fileName: String,
        mimeType: String,
        fileBytes: ByteArray,
    ) {
        // RFC 7578: use filename (not filename*) with percent-encoding for non-ASCII.
        val safeName = encodeMultipartFileName(fileName)
        val safeMimeType = sanitizeMultipartMimeType(mimeType)
        output.writeUtf8("--$boundary\r\n")
        output.writeUtf8(
            "Content-Disposition: form-data; name=\"$fieldName\"; filename=\"$safeName\"\r\n",
        )
        output.writeUtf8("Content-Type: $safeMimeType\r\n\r\n")
        output.write(fileBytes)
        output.writeUtf8("\r\n")
    }

    /** Writes [text] as UTF-8 bytes; avoids [DataOutputStream.writeBytes] truncation. */
    private fun DataOutputStream.writeUtf8(text: String) {
        write(text.toByteArray(Charsets.UTF_8))
    }

    companion object {
        private const val TIMEOUT_MS = FolioHttp.LONG_TIMEOUT_MS
        private const val MAX_MULTIPART_FILENAME_LENGTH = 255
        private const val MAX_MULTIPART_MIME_TYPE_LENGTH = 127
        private const val DEFAULT_MULTIPART_MIME_TYPE = "application/octet-stream"
        /**
         * RFC 2045 token for type/subtype only (no parameters). Rejects CR/LF and
         * other header-breaking characters so ContentResolver-supplied MIME types
         * cannot inject multipart part headers (CWE-93).
         */
        private val MULTIPART_MIME_TYPE_PATTERN =
            Regex("^[A-Za-z0-9][A-Za-z0-9!#\$&+\\-.^_]*/[A-Za-z0-9][A-Za-z0-9!#\$&+\\-.^_]*$")
        internal const val MAX_SSE_RECONNECT_ATTEMPTS = 5
        /**
         * Minimum time an accepted SSE connection must stay open before the reconnect
         * budget resets without having received an event (e.g. keepalive-only streams).
         */
        internal const val MIN_SSE_STABLE_OPEN_MS = 10_000L
        private const val SSE_BACKOFF_BASE_MS = 1_000L
        private const val SSE_BACKOFF_MAX_MS = 16_000L
        const val SOURCE_TYPE_WEB = "Web"
        const val SOURCE_TYPE_MANUAL = "Manual"
        const val SOURCE_TYPE_FILE = "File"

        /**
         * Next 1-based reconnect attempt after a stream that had reached HTTP success.
         * Resets the budget when the connection stayed open for [MIN_SSE_STABLE_OPEN_MS];
         * otherwise continues counting consecutive flaps. When [openedAtMs] is null
         * (never opened), always increments.
         */
        internal fun nextSseReconnectAttempt(
            currentAttempt: Int,
            openedAtMs: Long?,
            nowMs: Long,
        ): Int {
            val base = when {
                openedAtMs == null -> currentAttempt
                nowMs - openedAtMs >= MIN_SSE_STABLE_OPEN_MS -> 0
                else -> currentAttempt
            }
            return base + 1
        }

        /** Exponential backoff for SSE reconnect attempts (1-based). Caps at 16s. */
        internal fun sseBackoffMillis(attempt: Int): Long {
            require(attempt >= 1) { "attempt must be >= 1" }
            val shift = (attempt - 1).coerceAtMost(4)
            val delay = SSE_BACKOFF_BASE_MS shl shift
            return delay.coerceAtMost(SSE_BACKOFF_MAX_MS)
        }

        /**
         * Exact UTF-8 byte length of the multipart body written by [createFileSource].
         * Used for [HttpURLConnection.setFixedLengthStreamingMode] so the request is
         * not fully buffered in memory before send.
         */
        internal fun multipartBodyContentLength(
            boundary: String,
            spaceId: String,
            title: String,
            author: String,
            fileName: String,
            mimeType: String,
            fileBytesSize: Int,
        ): Long {
            var length = 0L
            length += formFieldByteCount(boundary, "spaceId", spaceId)
            length += formFieldByteCount(boundary, "sourceType", SOURCE_TYPE_FILE)
            length += formFieldByteCount(boundary, "title", title)
            length += formFieldByteCount(boundary, "author", author)
            length += fileFieldByteCount(
                boundary = boundary,
                fieldName = "file",
                fileName = fileName,
                mimeType = mimeType,
                fileBytesSize = fileBytesSize,
            )
            length += utf8ByteCount("--$boundary--\r\n")
            return length
        }

        private fun formFieldByteCount(boundary: String, name: String, value: String): Long =
            utf8ByteCount("--$boundary\r\n") +
                utf8ByteCount("Content-Disposition: form-data; name=\"$name\"\r\n\r\n") +
                utf8ByteCount(value) +
                utf8ByteCount("\r\n")

        private fun fileFieldByteCount(
            boundary: String,
            fieldName: String,
            fileName: String,
            mimeType: String,
            fileBytesSize: Int,
        ): Long {
            val safeName = encodeMultipartFileName(fileName)
            val safeMimeType = sanitizeMultipartMimeType(mimeType)
            return utf8ByteCount("--$boundary\r\n") +
                utf8ByteCount(
                    "Content-Disposition: form-data; name=\"$fieldName\"; filename=\"$safeName\"\r\n",
                ) +
                utf8ByteCount("Content-Type: $safeMimeType\r\n\r\n") +
                fileBytesSize.toLong() +
                utf8ByteCount("\r\n")
        }

        private fun utf8ByteCount(text: String): Long =
            text.toByteArray(Charsets.UTF_8).size.toLong()

        /**
         * Strips header-breaking characters from a multipart filename before it is
         * interpolated into Content-Disposition. Keeps this in the uploader so the
         * same guard moves with the client when it graduates from debug to main.
         */
        internal fun sanitizeMultipartFileName(fileName: String): String {
            val sanitized = fileName
                .filter { ch -> ch != '\r' && ch != '\n' && ch != '"' && ch != '\\' }
                .take(MAX_MULTIPART_FILENAME_LENGTH)
            return sanitized.ifBlank { "file" }
        }

        /**
         * Restricts [mimeType] to a single `type/subtype` token for the multipart
         * Content-Type part header. Drops parameters and rejects CR/LF or other
         * non-token characters (CWE-93 header injection via ContentResolver).
         */
        internal fun sanitizeMultipartMimeType(mimeType: String): String {
            val typeSubtype = mimeType.trim().substringBefore(';').trim()
            if (typeSubtype.isEmpty() ||
                typeSubtype.length > MAX_MULTIPART_MIME_TYPE_LENGTH ||
                '\r' in typeSubtype ||
                '\n' in typeSubtype ||
                !MULTIPART_MIME_TYPE_PATTERN.matches(typeSubtype)
            ) {
                return DEFAULT_MULTIPART_MIME_TYPE
            }
            return typeSubtype
        }

        /**
         * Sanitizes then percent-encodes non-ASCII / non-printable octets per RFC 7578 §2
         * for the `filename` parameter. Does not emit `filename*` (forbidden by RFC 7578).
         */
        internal fun encodeMultipartFileName(fileName: String): String =
            percentEncodeMultipartFileName(sanitizeMultipartFileName(fileName))

        /**
         * Percent-encodes characters outside printable US-ASCII so MIME headers stay
         * ASCII-compatible while preserving the UTF-8 filename for the receiver.
         */
        internal fun percentEncodeMultipartFileName(fileName: String): String = buildString {
            for (ch in fileName) {
                if (ch.code in 0x20..0x7E) {
                    append(ch)
                } else {
                    for (byte in ch.toString().toByteArray(Charsets.UTF_8)) {
                        append('%')
                        append(
                            ((byte.toInt() and 0xFF)).toString(16)
                                .uppercase(Locale.US)
                                .padStart(2, '0'),
                        )
                    }
                }
            }
        }

        /** Random multipart boundary; injectable via [SourcesApiClient] for deterministic tests. */
        internal fun randomMultipartBoundary(): String =
            "Boundary-${UUID.randomUUID().toString().replace("-", "")}"

        /**
         * Picks a boundary that does not occur in [fileBytes], retrying with
         * [boundaryFactory] so file content cannot truncate the multipart body.
         */
        internal fun chooseMultipartBoundary(
            fileBytes: ByteArray,
            boundaryFactory: () -> String = ::randomMultipartBoundary,
            maxAttempts: Int = MAX_BOUNDARY_ATTEMPTS,
        ): String {
            repeat(maxAttempts) {
                val candidate = boundaryFactory()
                if (!fileBytes.containsAscii(candidate)) {
                    return candidate
                }
            }
            throw IOException("Could not generate a unique multipart boundary")
        }

        private fun ByteArray.containsAscii(value: String): Boolean {
            if (value.isEmpty() || size < value.length) return false
            val needle = value.toByteArray(Charsets.US_ASCII)
            outer@ for (start in 0..(size - needle.size)) {
                for (offset in needle.indices) {
                    if (this[start + offset] != needle[offset]) continue@outer
                }
                return true
            }
            return false
        }

        private const val MAX_BOUNDARY_ATTEMPTS = 8

        internal fun parseSseDataPayload(payload: String): SourceProcessingEvent? {
            if (payload.isBlank()) return null
            val sourceId = matchJsonString(payload, "sourceId") ?: return null
            val stateRaw = matchJsonString(payload, "state").orEmpty()
            val state = SourceProcessingEvent.parseState(stateRaw)
            val progress = matchJsonInt(payload, "progress") ?: defaultProgressFor(state)
            return SourceProcessingEvent(
                sourceId = sourceId,
                state = state,
                progress = progress.coerceIn(0, 100),
            )
        }

        /**
         * Processes an SSE `id:` field per the EventSource spec.
         *
         * @return the new last-event-id buffer value, or `null` if the field must be ignored
         * (value contains U+0000 NULL).
         */
        internal fun parseSseIdFieldValue(line: String): String? {
            require(line.startsWith("id:")) { "Expected id: field, got: $line" }
            val value = line.removePrefix("id:").trimStart()
            if ('\u0000' in value) return null
            return value
        }

        private fun matchJsonString(payload: String, key: String): String? {
            val regex = Regex("\"$key\"\\s*:\\s*\"([^\"]*)\"")
            return regex.find(payload)?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }
        }

        private fun matchJsonInt(payload: String, key: String): Int? {
            val regex = Regex("\"$key\"\\s*:\\s*(-?\\d+)")
            return regex.find(payload)?.groupValues?.getOrNull(1)?.toIntOrNull()
        }

        internal fun defaultProgressFor(state: SourceProcessingState): Int = when (state) {
            SourceProcessingState.ADDED -> 0
            SourceProcessingState.EXTRACTING_TEXT -> 25
            SourceProcessingState.INDEXING_EVIDENCE -> 50
            SourceProcessingState.READY,
            SourceProcessingState.FAILED,
            -> 100
        }

        internal fun mapSourceType(raw: String): SourceType = when (raw.trim().lowercase(Locale.US)) {
            "web" -> SourceType.WEB
            "manual", "text" -> SourceType.TEXT
            "file", "pdf" -> SourceType.FILE
            "book" -> SourceType.BOOK
            else -> SourceType.FILE
        }

        internal fun mapProcessingState(raw: String): SourceStatus =
            when (raw.trim().lowercase(Locale.US)) {
                "ready", "completed", "done", "processed" -> SourceStatus.READY
                "failed", "error" -> SourceStatus.FAILED
                else -> SourceStatus.PROCESSING
            }

        internal fun fileExtensionFrom(fileName: String, fileType: String): String {
            val fromName = fileName.substringAfterLast('.', missingDelimiterValue = "")
                .trim()
                .lowercase(Locale.US)
                .takeIf { it.isNotBlank() && it.length <= 8 && !it.contains(' ') && it != "null" }
            if (fromName != null) return fromName
            val type = fileType.trim().lowercase(Locale.US)
            if (type.isBlank() || type == "null") return ""
            return when {
                type.contains("pdf") -> "pdf"
                type.contains("html") -> "html"
                type.contains("markdown") || type == "md" -> "md"
                type.contains("spreadsheet") || type.contains("excel") || type == "xlsx" -> "xlsx"
                type.contains("presentation") || type.contains("powerpoint") || type == "pptx" -> "pptx"
                type.contains("plain") || type == "txt" || type.contains("text") -> "txt"
                else -> type.substringAfterLast('/').takeIf { it.isNotBlank() && it != "null" }.orEmpty()
            }
        }

        internal fun contentFormatFrom(extension: String): SourceContentFormat =
            when (extension.trim().lowercase(Locale.US)) {
                "xlsx", "xls", "csv" -> SourceContentFormat.SHEET
                "pptx", "ppt" -> SourceContentFormat.SLIDES
                else -> SourceContentFormat.DOCUMENT
            }

        internal fun contentToHtml(content: String): String {
            val trimmed = content.trim()
            if (trimmed.isEmpty()) return ""
            if (looksLikeHtml(trimmed)) {
                return sanitizeHtmlFragment(trimmed)
            }
            val escaped = escapeHtml(trimmed)
            return escaped
                .split(Regex("\\r?\\n\\r?\\n"))
                .joinToString(separator = "") { paragraph ->
                    val lines = paragraph.trim().replace("\r\n", "\n").replace("\n", "<br/>")
                    "<p>$lines</p>"
                }
        }

        internal fun sheetContentToHtml(content: String): String {
            val trimmed = content.trim()
            if (trimmed.isEmpty()) return ""
            if (trimmed.contains("<table", ignoreCase = true)) {
                return sanitizeHtmlFragment(trimmed)
            }

            val lines = trimmed.lines()
                .map { it.trimEnd() }
                .filter { it.isNotBlank() }
                .filterNot { line ->
                    // Skip markdown table separator rows: |---|---|
                    line.replace(" ", "").matches(Regex("^\\|?[-:|]+\\|?$"))
                }
            if (lines.isEmpty()) return contentToHtml(content)

            val delimiter = when {
                lines.any { it.contains('\t') } -> "\t"
                lines.count { it.count { ch -> ch == '|' } >= 2 } >= (lines.size / 2).coerceAtLeast(1) -> "|"
                lines.count { it.contains(',') } >= (lines.size / 2).coerceAtLeast(1) -> ","
                else -> null
            }
            if (delimiter == null) return contentToHtml(content)

            val rows = lines.map { line ->
                when (delimiter) {
                    "|" -> line.trim().trim('|').split('|').map { cell -> escapeHtml(cell.trim()) }
                    else -> line.split(delimiter).map { cell -> escapeHtml(cell.trim()) }
                }
            }.filter { row -> row.any { cell -> cell.isNotBlank() } }
            if (rows.isEmpty()) return contentToHtml(content)

            val columnCount = rows.maxOf { it.size }
            val normalized = rows.map { row ->
                if (row.size >= columnCount) row
                else row + List(columnCount - row.size) { "" }
            }
            val header = normalized.first()
            val body = normalized.drop(1)
            val headerHtml = header.joinToString("") { "<th>$it</th>" }
            val bodyHtml = body.joinToString("") { row ->
                "<tr>${row.joinToString("") { cell -> "<td>$cell</td>" }}</tr>"
            }
            return buildString {
                append("<table><thead><tr>")
                append(headerHtml)
                append("</tr></thead>")
                if (bodyHtml.isNotEmpty()) {
                    append("<tbody>")
                    append(bodyHtml)
                    append("</tbody>")
                }
                append("</table>")
            }
        }

        private fun looksLikeHtml(content: String): Boolean =
            content.contains("<table", ignoreCase = true) ||
                content.contains("<h1", ignoreCase = true) ||
                content.contains("<p", ignoreCase = true)

        /**
         * Sanitizes untrusted HTML fragments before they are assigned to
         * [com.nus.folio.domain.model.SourceDetail.htmlContent] / sheet tables.
         *
         * Backend content for Web/File sources is attacker-influenced. The detail
         * screen renders it in a WebView; even with JavaScript disabled, markup
         * must not pass through verbatim (CWE-79).
         *
         * Attribute stripping runs only inside tags — never across text nodes —
         * so prose like "hello world review" is not mistaken for attributes.
         */
        internal fun sanitizeHtmlFragment(html: String): String {
            var out = html
            for (tag in DANGEROUS_HTML_TAGS) {
                out = Regex(
                    "<$tag\\b[^>]*>.*?</$tag\\s*>",
                    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
                ).replace(out, "")
                out = Regex("<$tag\\b[^>]*/?>", RegexOption.IGNORE_CASE).replace(out, "")
            }
            out = Regex("<!--.*?-->", setOf(RegexOption.DOT_MATCHES_ALL)).replace(out, "")
            // Attribute / URI sanitization must stay inside tags only.
            out = HTML_TAG_REGEX.replace(out) { match ->
                val closer = match.groupValues[1]
                val name = match.groupValues[2]
                val rawAttrs = match.groupValues[3]
                if (closer == "/") {
                    "</$name>"
                } else {
                    "<$name${sanitizeAttributesInsideTag(rawAttrs)}>"
                }
            }
            // Drop tags that are not on the allowlist; keep text content.
            out = Regex(
                "</?(?!($ALLOWED_HTML_TAGS_ALTERNATION)\\b)([a-zA-Z][a-zA-Z0-9]*)\\b[^>]*>",
                RegexOption.IGNORE_CASE,
            ).replace(out, "")
            return out
        }

        /**
         * Sanitizes the attribute region of a single open/void tag (group after the name).
         * Preserves a trailing `/` for self-closing forms like `<br/>`.
         */
        private fun sanitizeAttributesInsideTag(rawAttrs: String): String {
            if (rawAttrs.isEmpty()) return ""
            val trimmedEnd = rawAttrs.trimEnd()
            val selfClosing = trimmedEnd.endsWith('/')
            var attrs = if (selfClosing) {
                trimmedEnd.dropLast(1).trimEnd()
            } else {
                rawAttrs
            }
            if (attrs.isNotEmpty()) {
                // Event-handler attributes: onclick, onerror, ...
                attrs = EVENT_HANDLER_ATTR_REGEX.replace(attrs, "")
                // Drop attributes outside the allowlist (keeps href/colspan/rowspan/class).
                attrs = DISALLOWED_ATTR_REGEX.replace(attrs, "")
                // Neutralize dangerous URI schemes in remaining URL attributes.
                attrs = HREF_ATTR_REGEX.replace(attrs) { match ->
                    val raw = match.groupValues[1]
                    val value = when {
                        raw.length >= 2 &&
                            ((raw.first() == '"' && raw.last() == '"') ||
                                (raw.first() == '\'' && raw.last() == '\'')) ->
                            raw.substring(1, raw.lastIndex)
                        else -> raw
                    }.trim().lowercase(Locale.US)
                    if (value.startsWith("javascript:") ||
                        value.startsWith("vbscript:") ||
                        value.startsWith("data:")
                    ) {
                        "href=\"#\""
                    } else {
                        match.value
                    }
                }
            }
            return when {
                selfClosing && attrs.isEmpty() -> " /"
                selfClosing -> "$attrs /"
                else -> attrs
            }
        }

        private fun escapeHtml(value: String): String =
            value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")

        /**
         * Tags that must never appear in source preview HTML (removed with body when present).
         */
        private val DANGEROUS_HTML_TAGS = listOf(
            "script", "style", "iframe", "object", "embed", "form", "input", "button",
            "textarea", "select", "link", "meta", "base", "svg", "math", "noscript",
            "template", "applet", "frame", "frameset", "video", "audio", "source",
        )

        /**
         * Safe structural/inline tags used by Folio source previews and [SourceHtmlTemplate].
         * Longer names first so the alternation matches correctly (e.g. `blockquote` before `b`).
         */
        private val ALLOWED_HTML_TAGS = listOf(
            "blockquote", "thead", "tbody", "strong", "table", "code", "span", "div",
            "h1", "h2", "h3", "h4", "pre", "br", "hr", "ul", "ol", "li", "td", "th",
            "tr", "em", "p", "a", "b", "i", "u",
        )

        private val ALLOWED_HTML_TAGS_ALTERNATION =
            ALLOWED_HTML_TAGS.joinToString("|")

        private val HTML_TAG_REGEX = Regex(
            "<(/?)([a-zA-Z][a-zA-Z0-9]*)([^>]*)>",
            RegexOption.IGNORE_CASE,
        )

        private val EVENT_HANDLER_ATTR_REGEX = Regex(
            "\\s+on[a-zA-Z]+\\s*=\\s*(?:\"[^\"]*\"|'[^']*'|[^\\s>]+)",
            RegexOption.IGNORE_CASE,
        )

        private val DISALLOWED_ATTR_REGEX = Regex(
            "\\s+(?!(?:href|colspan|rowspan|class)\\b)[a-zA-Z_:][-a-zA-Z0-9_:.]*" +
                "(?:\\s*=\\s*(?:\"[^\"]*\"|'[^']*'|[^\\s>]+))?",
            RegexOption.IGNORE_CASE,
        )

        private val HREF_ATTR_REGEX = Regex(
            """(?i)\bhref\s*=\s*("[^"]*"|'[^']*'|[^\s>]+)""",
        )

        internal fun parseSheets(json: JSONObject, fallbackContent: String): List<SourceSheetTab> {
            val sheetsArray = json.optJSONArray("sheets")
            if (sheetsArray != null && sheetsArray.length() > 0) {
                return buildList {
                    for (index in 0 until sheetsArray.length()) {
                        val item = sheetsArray.optJSONObject(index) ?: continue
                        val name = item.optString("name")
                            .ifBlank { item.optString("title") }
                            .ifBlank { "Sheet ${index + 1}" }
                        val tableHtml = sequenceOf("htmlTable", "content", "html")
                            .map { key -> item.optString(key) }
                            .firstOrNull { it.isNotBlank() }
                            .orEmpty()
                            .let(::sheetContentToHtml)
                        if (tableHtml.isBlank()) continue
                        add(
                            SourceSheetTab(
                                id = item.optString("id").ifBlank { "sheet-$index" },
                                name = name,
                                htmlTable = tableHtml,
                            ),
                        )
                    }
                }
            }
            val tableHtml = sheetContentToHtml(fallbackContent)
            if (tableHtml.isBlank()) return emptyList()
            return listOf(
                SourceSheetTab(
                    id = "sheet-1",
                    name = "Sheet 1",
                    htmlTable = tableHtml,
                ),
            )
        }

        internal fun parsePreviewUrl(responseBody: String): String? {
            if (responseBody.isBlank()) return null
            return matchJsonString(responseBody, "previewUrl")?.trim()?.takeIf { it.isNotBlank() }
        }

        internal fun parseSourceDetailResponse(
            responseBody: String,
            nowMillis: Long = System.currentTimeMillis(),
        ): SourceDetail {
            if (responseBody.isBlank()) {
                throw IOException("Get source failed: empty response")
            }
            val root = JSONObject(responseBody)
            val sourceJson = root.optJSONObject("data")?.optJSONObject("source")
                ?: root.optJSONObject("source")
                ?: throw IOException("Get source failed: missing source payload")
            return parseSourceDetail(sourceJson, nowMillis)
        }

        internal fun parseSource(json: JSONObject, nowMillis: Long): Source {
            val id = json.optString("id").takeIf { it.isNotBlank() }
                ?: throw IOException("Source payload missing id")
            val spaceId = sequenceOf("researchSpaceId", "spaceId")
                .map { json.optString(it) }
                .firstOrNull { it.isNotBlank() }
                .orEmpty()
            val title = json.optString("title").takeIf { it.isNotBlank() } ?: "Untitled source"
            val author = json.optString("author").orEmpty()
            val createdAt = json.optString("createdAt").orEmpty()
            val type = mapSourceType(json.optString("sourceType"))
            val fileName = json.optString("fileName").orEmpty()
            val fileType = json.optString("fileType").orEmpty()
            val fileExtension = when (type) {
                SourceType.TEXT, SourceType.WEB -> ""
                else -> fileExtensionFrom(fileName = fileName, fileType = fileType)
            }

            return Source(
                id = id,
                title = title,
                type = type,
                author = author,
                addedLabel = formatAddedLabel(createdAt, nowMillis),
                status = mapProcessingState(json.optString("processingState")),
                spaceId = spaceId,
                fileExtension = fileExtension,
            )
        }

        private fun parseSourceDetail(json: JSONObject, nowMillis: Long): SourceDetail {
            val listSource = parseSource(json, nowMillis)
            val fileName = json.optString("fileName").orEmpty()
            val extension = listSource.fileExtension
            val contentFormat = contentFormatFrom(extension)
            val rawContent = json.optString("content").orEmpty()
            val sheets = if (contentFormat == SourceContentFormat.SHEET) {
                parseSheets(json, rawContent)
            } else {
                emptyList()
            }
            val htmlContent = when (contentFormat) {
                SourceContentFormat.SHEET -> null
                else -> contentToHtml(rawContent).takeIf { it.isNotBlank() }
            }
            val plainContent = rawContent.takeIf {
                listSource.type == SourceType.TEXT && it.isNotBlank()
            }
            return SourceDetail(
                id = listSource.id,
                title = listSource.title,
                author = listSource.author,
                addedLabel = listSource.addedLabel,
                type = listSource.type,
                status = listSource.status,
                spaceId = listSource.spaceId,
                fileExtension = extension,
                contentFormat = contentFormat,
                originalFileName = fileName.ifBlank {
                    if (extension.isNotBlank()) "${listSource.title}.$extension" else listSource.title
                },
                htmlContent = htmlContent,
                sheets = sheets,
                plainContent = plainContent,
            )
        }

        internal fun formatAddedLabel(isoInstant: String, nowMillis: Long): String {
            val millis = parseIsoToMillis(isoInstant) ?: return "Added just now"
            val delta = (nowMillis - millis).coerceAtLeast(0L)
            val minutes = delta / 60_000L
            val hours = delta / 3_600_000L
            val days = delta / 86_400_000L
            return when {
                minutes < 1L -> "Added just now"
                minutes < 60L -> "Added ${minutes}m ago"
                hours < 24L -> "Added ${hours}h ago"
                days < 7L -> "Added ${days}d ago"
                else -> "Added ${days / 7L}w ago"
            }
        }

        private fun parseIsoToMillis(isoInstant: String): Long? {
            val value = normalizeIsoTimestamp(isoInstant.trim())
            if (value.isEmpty()) return null
            val patterns = listOf(
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
            )
            for (pattern in patterns) {
                val parsed = runCatching {
                    SimpleDateFormat(pattern, Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                        isLenient = false
                    }.parse(value)?.time
                }.getOrNull()
                if (parsed != null) return parsed
            }
            return null
        }

        internal fun normalizeIsoTimestamp(isoInstant: String): String {
            val value = isoInstant.trim()
            val dotIndex = value.indexOf('.')
            if (dotIndex < 0) return value

            var end = dotIndex + 1
            while (end < value.length && value[end].isDigit()) {
                end++
            }
            if (end == dotIndex + 1) return value

            val millis = value.substring(dotIndex + 1, end).padEnd(3, '0').take(3)
            return value.substring(0, dotIndex + 1) + millis + value.substring(end)
        }
    }
}
