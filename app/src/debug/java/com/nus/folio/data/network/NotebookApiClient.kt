package com.nus.folio.data.network

import com.nus.folio.domain.model.Notebook
import com.nus.folio.domain.util.NotebookHtml
import com.nus.folio.domain.util.NotebookInputRules
import java.io.IOException
import java.net.HttpURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface NotebookApi {
    suspend fun getNotebook(
        accessToken: String,
        spaceId: String,
    ): Notebook

    suspend fun putNotebook(
        accessToken: String,
        spaceId: String,
        markdown: String,
    )
}

/**
 * Thin HTTP client for GET/PUT /api/v1/spaces/{spaceId}/notebook (debug builds).
 * The wire format is sanitized HTML; the editor continues to use markdown.
 */
class NotebookApiClient(
    private val baseUrl: String = FolioApiPaths.BASE_URL,
) : NotebookApi {

    override suspend fun getNotebook(
        accessToken: String,
        spaceId: String,
    ): Notebook = withContext(Dispatchers.IO) {
        FolioHttp.get(
            url = FolioApiPaths.spaceNotebook(spaceId, baseUrl),
            accessToken = accessToken,
            failureLabel = "Get notebook",
            requireSuccess = false,
            parse = { response -> notebookFromHttpResponse(response.code, response.body, spaceId) },
        )
    }

    override suspend fun putNotebook(
        accessToken: String,
        spaceId: String,
        markdown: String,
    ): Unit = withContext(Dispatchers.IO) {
        val html = NotebookHtml.markdownToHtml(markdown)
        if (html.length > NotebookInputRules.MAX_HTML_LENGTH) {
            throw IOException("Notebook HTML exceeds ${NotebookInputRules.MAX_HTML_LENGTH} characters")
        }
        val jsonBody = buildNotebookRequestJson(html)
        val bodyBytes = jsonBody.toByteArray(Charsets.UTF_8)
        if (bodyBytes.size > NotebookInputRules.MAX_JSON_BODY_BYTES) {
            throw IOException("Notebook payload exceeds 2 MB")
        }
        FolioHttp.putJson(
            url = FolioApiPaths.spaceNotebook(spaceId, baseUrl),
            jsonBody = jsonBody,
            accessToken = accessToken,
            failureLabel = "Save notebook",
            parse = { },
        )
    }

    companion object {
        internal fun notebookFromHttpResponse(
            code: Int,
            body: String,
            spaceId: String,
        ): Notebook {
            if (code == HttpURLConnection.HTTP_NOT_FOUND) {
                throw NotebookNotFoundException("Get notebook failed (HTTP 404)")
            }
            if (code !in 200..299) {
                throw FolioHttp.unauthorizedOrIo(body, code, "Get notebook")
            }
            return parseNotebookPayload(body, spaceId)
        }

        internal fun parseNotebookPayload(responseBody: String, fallbackSpaceId: String): Notebook {
            val notebookJson = extractJsonObject(responseBody, "notebook") ?: responseBody
            val html = matchQuotedString(notebookJson, "content")
                ?: throw IOException("Get notebook failed: missing or invalid content")
            val spaceId = matchQuotedString(notebookJson, "researchSpaceId")
                ?.takeIf { it.isNotBlank() }
                ?: fallbackSpaceId
            val conversion = NotebookHtml.htmlToMarkdownConversion(html)
            return Notebook(
                spaceId = spaceId,
                content = conversion.markdown,
                isReadOnly = conversion.isLossy,
            )
        }

        internal fun buildNotebookRequestJson(html: String): String =
            """{"content":${jsonQuoted(html)}}"""

        private fun jsonQuoted(value: String): String = buildString {
            append('"')
            value.forEach { ch ->
                when (ch) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\b' -> append("\\b")
                    '\u000C' -> append("\\f")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> if (ch.code < 0x20) {
                        append("\\u")
                        append(ch.code.toString(16).padStart(4, '0'))
                    } else {
                        append(ch)
                    }
                }
            }
            append('"')
        }

        private fun matchQuotedString(payload: String, key: String): String? {
            val header = Regex("\"$key\"\\s*:").find(payload) ?: return null
            var index = header.range.last + 1
            while (index < payload.length && payload[index].isWhitespace()) {
                index++
            }
            if (index >= payload.length || payload[index] != '"') return null
            return readQuotedString(payload, index)?.first
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

        private fun extractJsonObject(payload: String, key: String): String? {
            val header = Regex("\"$key\"\\s*:\\s*\\{").find(payload) ?: return null
            val start = header.range.last
            val end = matchingBrace(payload, start) ?: return null
            return payload.substring(start, end + 1)
        }

        private fun matchingBrace(payload: String, openIndex: Int): Int? {
            var depth = 0
            var inString = false
            var escape = false
            var index = openIndex
            while (index < payload.length) {
                val ch = payload[index]
                when {
                    escape -> escape = false
                    inString && ch == '\\' -> escape = true
                    ch == '"' -> inString = !inString
                    !inString && ch == '{' -> depth++
                    !inString && ch == '}' -> {
                        depth--
                        if (depth == 0) return index
                    }
                }
                index++
            }
            return null
        }
    }
}
