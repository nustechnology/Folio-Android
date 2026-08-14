package com.nus.folio.data.network

import com.nus.folio.domain.model.Source
import com.nus.folio.domain.model.SourceContentFormat
import com.nus.folio.domain.model.SourceDetail
import com.nus.folio.domain.model.SourceLibrary
import com.nus.folio.domain.model.SourcePaging
import com.nus.folio.domain.model.SourceSheetTab
import com.nus.folio.domain.model.SourceStatus
import com.nus.folio.domain.model.SourceType
import com.nus.folio.domain.model.StructuredContent
import com.nus.folio.domain.model.StructuredContentHtml
import com.nus.folio.domain.model.StructuredSheet
import com.nus.folio.domain.model.StructuredSlide
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

    fun parseSourcesPage(
        responseBody: String,
        nowMillis: Long,
        page: Int = SourcePaging.DEFAULT_PAGE,
        limit: Int = SourcePaging.DEFAULT_LIMIT,
    ): SourceLibrary {
        if (responseBody.isBlank()) {
            return SourceLibrary(
                sources = emptyList(),
                allCount = 0,
                papersCount = 0,
                booksCount = 0,
                webCount = 0,
                textCount = 0,
                page = page,
                limit = limit,
                hasMore = false,
            )
        }
        val root = JSONObject(responseBody)
        val data = root.optJSONObject("data")
        val sourcesArray = data?.optJSONArray("sources")
            ?: root.optJSONArray("sources")
            ?: JSONArray()
        val sources = buildList {
            for (index in 0 until sourcesArray.length()) {
                val item = sourcesArray.optJSONObject(index) ?: continue
                add(parseSource(item, nowMillis))
            }
        }

        val pagination = data?.optJSONObject("pagination")
            ?: root.optJSONObject("pagination")
        val aggregateCounts = listOfNotNull(
            data?.optJSONObject("counts"),
            root.optJSONObject("counts"),
            data,
            root,
            pagination,
        )
        val allCount = firstAvailableCount(
            containers = aggregateCounts,
            keys = listOf("allCount", "totalCount", "sourceCount"),
        ) ?: sources.size
        val papersCount = firstAvailableCount(
            containers = aggregateCounts,
            keys = listOf("papersCount", "fileCount", "totalFileCount"),
        ) ?: sources.count { it.type == SourceType.FILE }
        val booksCount = firstAvailableCount(
            containers = aggregateCounts,
            keys = listOf("booksCount", "totalBookCount"),
        ) ?: sources.count { it.type == SourceType.BOOK }
        val webCount = firstAvailableCount(
            containers = aggregateCounts,
            keys = listOf("webCount", "totalWebCount"),
        ) ?: sources.count { it.type == SourceType.WEB }
        val textCount = firstAvailableCount(
            containers = aggregateCounts,
            keys = listOf("textCount", "manualCount", "totalManualCount"),
        ) ?: sources.count { it.type == SourceType.TEXT }

        val totalPages = pagination?.takeIf { it.has("totalPages") }?.optInt("totalPages")
        val responsePage = pagination?.optInt("page", page) ?: page
        val responseLimit = pagination?.optInt("limit", limit) ?: limit
        val hasMore = when {
            totalPages != null -> responsePage < totalPages
            pagination != null && pagination.has("totalCount") ->
                responsePage * responseLimit < (allCount)
            else -> sources.size >= responseLimit
        }

        return SourceLibrary(
            sources = sources,
            allCount = allCount.coerceAtLeast(0),
            papersCount = papersCount.coerceAtLeast(0),
            booksCount = booksCount.coerceAtLeast(0),
            webCount = webCount.coerceAtLeast(0),
            textCount = textCount.coerceAtLeast(0),
            page = responsePage,
            limit = responseLimit,
            hasMore = hasMore,
        )
    }

    /** @deprecated Prefer [parseSourcesPage] for paged list responses. */
    fun parseSourcesList(responseBody: String, nowMillis: Long): List<Source> =
        parseSourcesPage(responseBody, nowMillis).sources

    private fun firstAvailableCount(
        containers: List<JSONObject>,
        keys: List<String>,
    ): Int? {
        for (container in containers) {
            for (key in keys) {
                if (container.has(key) && !container.isNull(key)) {
                    val value = container.optInt(key, Int.MIN_VALUE)
                    if (value != Int.MIN_VALUE) return value
                }
            }
        }
        return null
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
        val rawContent = json.optString("content").orEmpty()
        val structuredContent = parseStructuredContent(json)
        val contentFormat = structuredContent?.let(StructuredContentHtml::contentFormat)
            ?: contentFormatFrom(extension)
        val sheets = when (structuredContent) {
            is StructuredContent.Sheets ->
                StructuredContentHtml.toSheetTabs(structuredContent.sheets)
            else -> if (contentFormat == SourceContentFormat.SHEET) {
                parseSheets(json, rawContent)
            } else {
                emptyList()
            }
        }
        val htmlContent = when (structuredContent) {
            is StructuredContent.Document -> structuredContent.html
            is StructuredContent.Slides ->
                StructuredContentHtml.slidesToHtml(structuredContent.slides)
                    .takeIf { it.isNotBlank() }
            is StructuredContent.Sheets -> null
            null -> when {
                contentFormat == SourceContentFormat.SHEET -> null
                else -> contentToHtml(rawContent).takeIf { it.isNotBlank() }
            }
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
            structuredContent = structuredContent,
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

    fun parseStructuredContent(json: JSONObject): StructuredContent? {
        val asObject = json.optJSONObject("structuredContent")
            ?: json.optJSONObject("structured_content")
        if (asObject != null) {
            return parseStructuredContentObject(asObject)
        }
        val raw = sequenceOf("structuredContent", "structured_content")
            .map { json.optString(it) }
            .firstOrNull { it.isNotBlank() }
        return parseStructuredContent(raw)
    }

    fun parseStructuredContent(raw: String?): StructuredContent? {
        val value = raw?.trim().orEmpty()
        if (value.isBlank()) return null
        // Legacy: backend occasionally sent a raw HTML fragment instead of JSON.
        if (value.startsWith("<")) {
            return SourcesApiClient.sanitizeHtmlFragment(value)
                .takeIf { it.isNotBlank() }
                ?.let(StructuredContent::Document)
        }
        return runCatching { parseStructuredContentObject(JSONObject(value)) }.getOrNull()
    }

    private fun parseStructuredContentObject(obj: JSONObject): StructuredContent? {
        when (obj.optString("type").trim().lowercase(Locale.US)) {
            "document" -> {
                val html = SourcesApiClient.sanitizeHtmlFragment(obj.optString("html"))
                    .takeIf { it.isNotBlank() }
                    ?: return null
                return StructuredContent.Document(html)
            }
            "sheets" -> {
                val sheetsArray = obj.optJSONArray("sheets") ?: return null
                val sheets = buildList {
                    for (index in 0 until sheetsArray.length()) {
                        val item = sheetsArray.optJSONObject(index) ?: continue
                        add(
                            StructuredSheet(
                                name = item.optString("name").ifBlank { "Sheet ${index + 1}" },
                                headers = stringListFrom(item.optJSONArray("headers")),
                                rows = stringRowsFrom(item.optJSONArray("rows")),
                            ),
                        )
                    }
                }
                return sheets.takeIf { it.isNotEmpty() }?.let(StructuredContent::Sheets)
            }
            "slides" -> {
                val slidesArray = obj.optJSONArray("slides") ?: return null
                val slides = buildList {
                    for (index in 0 until slidesArray.length()) {
                        val item = slidesArray.optJSONObject(index) ?: continue
                        add(
                            StructuredSlide(
                                slideNumber = item.optInt("slideNumber", index + 1),
                                title = item.optString("title"),
                                bullets = stringListFrom(item.optJSONArray("bullets")),
                            ),
                        )
                    }
                }
                return slides.takeIf { it.isNotEmpty() }?.let(StructuredContent::Slides)
            }
            else -> {
                val html = SourcesApiClient.sanitizeHtmlFragment(obj.optString("html"))
                    .takeIf { it.isNotBlank() }
                    ?: return null
                return StructuredContent.Document(html)
            }
        }
    }

    private fun stringListFrom(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                add(array.optString(index))
            }
        }
    }

    private fun stringRowsFrom(array: JSONArray?): List<List<String>> {
        if (array == null) return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                add(stringListFrom(array.optJSONArray(index)))
            }
        }
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
