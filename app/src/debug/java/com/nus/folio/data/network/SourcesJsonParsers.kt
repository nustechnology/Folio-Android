package com.nus.folio.data.network

import com.nus.folio.domain.model.Source
import com.nus.folio.domain.model.SourceContentFormat
import com.nus.folio.domain.model.SourceDetail
import com.nus.folio.domain.model.SourceSheetTab
import com.nus.folio.domain.model.SourceStatus
import com.nus.folio.domain.model.SourceType
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * JSON / mapping helpers for [SourcesApiClient] list, detail, and preview payloads.
 */
internal object SourcesJsonParsers {

    fun parseCreatedSource(responseBody: String, nowMillis: Long): Source {
        if (responseBody.isBlank()) {
            throw IOException("Create source failed: empty response")
        }
        val root = JSONObject(responseBody)
        val sourceJson = root.optJSONObject("data")?.optJSONObject("source")
            ?: root.optJSONObject("source")
            ?: throw IOException("Create source failed: missing source payload")
        return parseSource(sourceJson, nowMillis)
    }

    fun parseUpdatedSource(responseBody: String, nowMillis: Long): Source {
        if (responseBody.isBlank()) {
            throw IOException("Update source failed: empty response")
        }
        val root = JSONObject(responseBody)
        val sourceJson = root.optJSONObject("data")?.optJSONObject("source")
            ?: root.optJSONObject("source")
            ?: throw IOException("Update source failed: missing source payload")
        return parseSource(sourceJson, nowMillis)
    }

    fun parseSourcesList(responseBody: String, nowMillis: Long): List<Source> {
        if (responseBody.isBlank()) return emptyList()
        val root = JSONObject(responseBody)
        val sourcesArray = root.optJSONObject("data")?.optJSONArray("sources")
            ?: root.optJSONArray("sources")
            ?: JSONArray()
        return buildList {
            for (index in 0 until sourcesArray.length()) {
                val item = sourcesArray.optJSONObject(index) ?: continue
                add(parseSource(item, nowMillis))
            }
        }
    }

    fun parsePreviewUrl(responseBody: String): String? {
        if (responseBody.isBlank()) return null
        return matchJsonString(responseBody, "previewUrl")?.trim()?.takeIf { it.isNotBlank() }
    }

    fun parseSourceDetailResponse(
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

    fun parseSource(json: JSONObject, nowMillis: Long): Source {
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
        )
    }

    fun mapSourceType(raw: String): SourceType = when (raw.trim().lowercase(Locale.US)) {
        "web" -> SourceType.WEB
        "manual", "text" -> SourceType.TEXT
        "file", "pdf" -> SourceType.FILE
        "book" -> SourceType.BOOK
        else -> SourceType.FILE
    }

    fun mapProcessingState(raw: String): SourceStatus =
        when (raw.trim().lowercase(Locale.US)) {
            "ready", "completed", "done", "processed" -> SourceStatus.READY
            "failed", "error" -> SourceStatus.FAILED
            else -> SourceStatus.PROCESSING
        }

    fun fileExtensionFrom(fileName: String, fileType: String): String {
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

    fun contentFormatFrom(extension: String): SourceContentFormat =
        when (extension.trim().lowercase(Locale.US)) {
            "xlsx", "xls", "csv" -> SourceContentFormat.SHEET
            "pptx", "ppt" -> SourceContentFormat.SLIDES
            else -> SourceContentFormat.DOCUMENT
        }

    fun contentToHtml(content: String): String {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return ""
        if (trimmed.contains("<table", ignoreCase = true) ||
            trimmed.contains("<h1", ignoreCase = true) ||
            trimmed.contains("<p", ignoreCase = true)
        ) {
            return SourcesApiClient.sanitizeHtmlFragment(trimmed)
        }
        val escaped = trimmed
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
        return escaped
            .split(Regex("\\r?\\n\\r?\\n"))
            .joinToString(separator = "") { paragraph ->
                val lines = paragraph.trim().replace("\r\n", "\n").replace("\n", "<br/>")
                "<p>$lines</p>"
            }
    }

    fun sheetContentToHtml(content: String): String {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return ""
        if (trimmed.contains("<table", ignoreCase = true)) {
            return SourcesApiClient.sanitizeHtmlFragment(trimmed)
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

    private fun escapeHtml(value: String): String =
        value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")

    fun parseSheets(json: JSONObject, fallbackContent: String): List<SourceSheetTab> {
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

    fun formatAddedLabel(isoInstant: String, nowMillis: Long): String {
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

    fun normalizeIsoTimestamp(isoInstant: String): String {
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

    fun matchJsonString(payload: String, key: String): String? {
        val regex = Regex("\"$key\"\\s*:\\s*\"([^\"]*)\"")
        return regex.find(payload)?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }
    }

    fun matchJsonInt(payload: String, key: String): Int? {
        val regex = Regex("\"$key\"\\s*:\\s*(-?\\d+)")
        return regex.find(payload)?.groupValues?.getOrNull(1)?.toIntOrNull()
    }
}
