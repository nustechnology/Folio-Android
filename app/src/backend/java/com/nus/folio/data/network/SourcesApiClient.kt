package com.nus.folio.data.network

import com.nus.folio.domain.model.Source
import com.nus.folio.domain.model.SourceDetail
import com.nus.folio.domain.model.SourceLibrary
import com.nus.folio.domain.model.SourcePaging
import com.nus.folio.domain.model.SourceProcessingEvent
import com.nus.folio.domain.model.SourceSort
import com.nus.folio.domain.util.SourceImageUrlRules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.DataOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.util.Locale
import java.util.UUID

interface SourcesApi {
    suspend fun listSources(
        accessToken: String,
        spaceId: String,
        sourceType: String? = null,
        search: String? = null,
        sort: String = SourceSort.DEFAULT.apiValue,
        page: Int = SourcePaging.DEFAULT_PAGE,
        limit: Int = SourcePaging.DEFAULT_LIMIT,
    ): SourceLibrary

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
 * Thin HTTP client for Folio sources create endpoints.
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
        page: Int,
        limit: Int,
    ): SourceLibrary = withContext(Dispatchers.IO) {
        val safePage = page.coerceAtLeast(1)
        val safeLimit = limit.coerceAtLeast(1)
        val query = buildString {
            append("spaceId=")
            append(URLEncoder.encode(spaceId, Charsets.UTF_8.name()))
            append("&sort=")
            append(URLEncoder.encode(sort, Charsets.UTF_8.name()))
            append("&page=")
            append(safePage)
            append("&limit=")
            append(safeLimit)
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
            parse = { response ->
                SourcesJsonParsers.parseSourcesPage(
                    responseBody = response.body,
                    nowMillis = nowMillis(),
                    page = safePage,
                    limit = safeLimit,
                )
            },
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
                    mimeType = mimeType.ifBlank { "application/octet-stream" },
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
        const val SOURCE_TYPE_WEB = "Web"
        const val SOURCE_TYPE_MANUAL = "Manual"
        const val SOURCE_TYPE_FILE = "File"

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
                // Drop attributes outside the allowlist (keeps href/src/alt/…).
                attrs = DISALLOWED_ATTR_REGEX.replace(attrs, "")
                // Neutralize dangerous URI schemes in remaining URL attributes.
                attrs = HREF_ATTR_REGEX.replace(attrs) { match ->
                    if (isDangerousUri(match.groupValues[1])) "href=\"#\"" else match.value
                }
                attrs = SRC_ATTR_REGEX.replace(attrs) { match ->
                    if (isDangerousImageSrc(match.groupValues[1])) "src=\"\"" else match.value
                }
            }
            return when {
                selfClosing && attrs.isEmpty() -> " /"
                selfClosing -> "$attrs /"
                else -> attrs
            }
        }

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
            "blockquote", "figcaption", "section", "article", "caption", "thead", "tbody",
            "tfoot", "strong", "figure", "table", "code", "span", "div",
            "h1", "h2", "h3", "h4", "h5", "h6", "pre", "br", "hr", "ul", "ol", "li", "td", "th",
            "tr", "em", "p", "a", "b", "i", "u", "img", "sub", "sup",
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
            "\\s+(?!(?:href|src|alt|width|height|colspan|rowspan|class)\\b)[a-zA-Z_:][-a-zA-Z0-9_:.]*" +
                "(?:\\s*=\\s*(?:\"[^\"]*\"|'[^']*'|[^\\s>]+))?",
            RegexOption.IGNORE_CASE,
        )

        private val HREF_ATTR_REGEX = Regex(
            """(?i)\bhref\s*=\s*("[^"]*"|'[^']*'|[^\s>]+)""",
        )

        private val SRC_ATTR_REGEX = Regex(
            """(?i)\bsrc\s*=\s*("[^"]*"|'[^']*'|[^\s>]+)""",
        )

        private fun quotedAttrValue(raw: String): String {
            val value = when {
                raw.length >= 2 &&
                    ((raw.first() == '"' && raw.last() == '"') ||
                        (raw.first() == '\'' && raw.last() == '\'')) ->
                    raw.substring(1, raw.lastIndex)
                else -> raw
            }
            return value.trim().lowercase(Locale.US)
        }

        private fun isDangerousUri(raw: String): Boolean {
            val value = quotedAttrValue(raw)
            return value.startsWith("javascript:") ||
                value.startsWith("vbscript:") ||
                value.startsWith("data:")
        }

        /** Allows relative paths, data:image, and public http(s); blocks local/private hosts. */
        private fun isDangerousImageSrc(raw: String): Boolean {
            val value = quotedAttrValue(raw)
            return !SourceImageUrlRules.isAllowed(value, FolioApiPaths.BASE_URL)
        }

    }
}
