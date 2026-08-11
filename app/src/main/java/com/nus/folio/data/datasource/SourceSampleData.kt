package com.nus.folio.data.datasource

import com.nus.folio.domain.model.CreateSourceRequest
import com.nus.folio.domain.model.Source
import com.nus.folio.domain.model.SourceContentFormat
import com.nus.folio.domain.model.SourceDetail
import com.nus.folio.domain.model.SourceLibrary
import com.nus.folio.domain.model.SourceSheetTab
import com.nus.folio.domain.model.SourceStatus
import com.nus.folio.domain.model.SourceType
import java.util.concurrent.ConcurrentHashMap

/**
 * Shared sample catalog and detail builders used by debug/release [SourceDataSource].
 */
internal object SourceSampleData {

    private val createdDetails = ConcurrentHashMap<String, DetailParts>()

    fun mutableDefaultSources(): MutableList<Source> = samples.toMutableList()

    fun libraryFrom(sources: List<Source>): SourceLibrary =
        SourceLibrary(
            sources = sources,
            allCount = sources.size,
            papersCount = sources.count { it.type == SourceType.FILE },
            booksCount = sources.count { it.type == SourceType.BOOK },
            webCount = sources.count { it.type == SourceType.WEB },
            textCount = sources.count { it.type == SourceType.TEXT },
        )

    /**
     * Persists detail payload for a release-created source so [buildSourceDetail]
     * can render from the creation request instead of the fixed-ID sample catalog.
     */
    fun rememberCreatedDetail(sourceId: String, request: CreateSourceRequest) {
        createdDetails[sourceId] = detailPartsFromRequest(request)
    }

    fun rememberUpdatedManualContent(source: Source, content: String) {
        val existing = createdDetails[source.id] ?: sampleDetailParts(source.id)
        val extension = existing.extension.ifBlank { "txt" }
        createdDetails[source.id] = existing.copy(
            extension = extension,
            format = SourceContentFormat.DOCUMENT,
            htmlContent = manualContentHtml(
                title = source.title,
                author = source.author,
                content = content,
            ),
            plainContent = content,
            originalFileName = existing.originalFileName
                ?: buildOriginalFileName(source.title, extension),
        )
    }

    fun forgetCreatedDetail(sourceId: String) {
        createdDetails.remove(sourceId)
    }

    fun buildSourceDetail(source: Source): SourceDetail {
        val parts = createdDetails[source.id] ?: sampleDetailParts(source.id)
        val resolvedExtension = source.fileExtension.ifBlank { parts.extension }

        return SourceDetail(
            id = source.id,
            title = source.title,
            author = source.author,
            addedLabel = source.addedLabel,
            type = source.type,
            status = source.status,
            spaceId = source.spaceId,
            fileExtension = resolvedExtension,
            contentFormat = parts.format,
            originalFileName = parts.originalFileName
                ?: buildOriginalFileName(source.title, resolvedExtension),
            htmlContent = parts.htmlContent,
            sheets = parts.sheets,
            plainContent = parts.plainContent,
        )
    }

    private fun sampleDetailParts(sourceId: String): DetailParts =
        when (sourceId) {
            "1" -> DetailParts("pdf", SourceContentFormat.DOCUMENT, SourceSampleHtml.turingPaperHtml, emptyList())
            "2" -> DetailParts("pdf", SourceContentFormat.DOCUMENT, SourceSampleHtml.arendtBookHtml, emptyList())
            "3" -> DetailParts(
                "epub",
                SourceContentFormat.DOCUMENT,
                SourceSampleHtml.weaponsOfMathDestructionHtml,
                emptyList(),
            )
            "4" -> DetailParts(
                "pdf",
                SourceContentFormat.DOCUMENT,
                SourceSampleHtml.surveillanceCapitalismHtml,
                emptyList(),
            )
            "5" -> DetailParts(
                "pdf",
                SourceContentFormat.DOCUMENT,
                SourceSampleHtml.attentionPaperHtml,
                emptyList(),
            )
            "6" -> DetailParts(
                "txt",
                SourceContentFormat.DOCUMENT,
                SourceSampleHtml.interviewNotesHtml,
                emptyList(),
            )
            "7" -> DetailParts(
                "pptx",
                SourceContentFormat.SLIDES,
                SourceSampleHtml.lectureSlidesHtml,
                emptyList(),
            )
            "8" -> DetailParts(
                "txt",
                SourceContentFormat.DOCUMENT,
                SourceSampleHtml.syllabusDraftHtml,
                emptyList(),
            )
            "9" -> DetailParts(
                "md",
                SourceContentFormat.DOCUMENT,
                SourceSampleHtml.neuralNetworksArticleHtml,
                emptyList(),
            )
            "10" -> DetailParts(
                "xlsx",
                SourceContentFormat.SHEET,
                null,
                SourceSampleHtml.researchSpreadsheetSheets,
                originalFileName = "research-metrics-dashboard.xlsx",
            )
            else -> DetailParts(
                "pdf",
                SourceContentFormat.DOCUMENT,
                sampleDocumentHtml("Untitled source", "", "Sample document preview."),
                emptyList(),
            )
        }

    private fun sampleDocumentHtml(title: String, author: String, preview: String): String =
        buildString {
            append("<h1>")
            append(escapeHtml(title))
            append("</h1>")
            if (author.isNotBlank()) {
                append("""<p class="byline">""")
                append(escapeHtml(author))
                append("</p>")
            }
            append("<p>")
            append(escapeHtml(preview))
            append("</p>")
        }

    private fun detailPartsFromRequest(request: CreateSourceRequest): DetailParts =
        when (request) {
            is CreateSourceRequest.Manual -> {
                val title = request.title.trim()
                DetailParts(
                    extension = "txt",
                    format = SourceContentFormat.DOCUMENT,
                    htmlContent = manualContentHtml(
                        title = title,
                        author = request.author.trim(),
                        content = request.content.trim(),
                    ),
                    sheets = emptyList(),
                    originalFileName = buildOriginalFileName(title, "txt"),
                    plainContent = request.content.trim(),
                )
            }
            is CreateSourceRequest.Web -> {
                val title = request.title.trim().ifBlank { request.sourceUrl.trim() }
                DetailParts(
                    extension = "md",
                    format = SourceContentFormat.DOCUMENT,
                    htmlContent = webContentHtml(
                        title = title,
                        author = request.author.trim(),
                        sourceUrl = request.sourceUrl.trim(),
                    ),
                    sheets = emptyList(),
                    originalFileName = buildOriginalFileName(title, "md"),
                )
            }
            is CreateSourceRequest.File -> {
                val fileName = request.fileName.trim().ifBlank { "source.pdf" }
                val extension = fileExtensionFrom(fileName).ifBlank { "pdf" }
                val format = contentFormatFrom(extension)
                val title = request.title.trim().ifBlank { fileName }
                when (format) {
                    SourceContentFormat.SHEET -> DetailParts(
                        extension = extension,
                        format = format,
                        htmlContent = null,
                        sheets = SourceSampleHtml.researchSpreadsheetSheets,
                        originalFileName = fileName,
                    )
                    SourceContentFormat.SLIDES -> DetailParts(
                        extension = extension,
                        format = format,
                        htmlContent = SourceSampleHtml.lectureSlidesHtml,
                        sheets = emptyList(),
                        originalFileName = fileName,
                    )
                    SourceContentFormat.DOCUMENT -> DetailParts(
                        extension = extension,
                        format = format,
                        htmlContent = fileDocumentHtml(
                            title = title,
                            author = request.author.trim(),
                            fileName = fileName,
                        ),
                        sheets = emptyList(),
                        originalFileName = fileName,
                    )
                }
            }
        }

    private fun fileExtensionFrom(fileName: String): String =
        fileName.substringAfterLast('.', missingDelimiterValue = "")
            .trim()
            .lowercase()
            .takeIf { it.isNotBlank() && it.length <= 8 && !it.contains(' ') }
            .orEmpty()

    private fun contentFormatFrom(extension: String): SourceContentFormat =
        when (extension.lowercase()) {
            "xlsx", "xls", "csv" -> SourceContentFormat.SHEET
            "pptx", "ppt" -> SourceContentFormat.SLIDES
            else -> SourceContentFormat.DOCUMENT
        }

    private fun manualContentHtml(title: String, author: String, content: String): String =
        buildString {
            append("<h1>")
            append(escapeHtml(title))
            append("</h1>")
            if (author.isNotBlank()) {
                append("""<p class="byline">""")
                append(escapeHtml(author))
                append("</p>")
            }
            append(plainTextToHtmlParagraphs(content))
        }

    private fun webContentHtml(title: String, author: String, sourceUrl: String): String =
        buildString {
            append("<h1>")
            append(escapeHtml(title))
            append("</h1>")
            if (author.isNotBlank()) {
                append("""<p class="byline">""")
                append(escapeHtml(author))
                append("</p>")
            }
            append("<p>Saved from ")
            append(escapeHtml(sourceUrl))
            append(".</p>")
            append("<p>Local release preview for a web source.</p>")
        }

    private fun fileDocumentHtml(title: String, author: String, fileName: String): String =
        buildString {
            append("<h1>")
            append(escapeHtml(title))
            append("</h1>")
            if (author.isNotBlank()) {
                append("""<p class="byline">""")
                append(escapeHtml(author))
                append("</p>")
            }
            append("<p>Local release preview for ")
            append(escapeHtml(fileName))
            append(".</p>")
        }

    private fun plainTextToHtmlParagraphs(content: String): String {
        if (content.isBlank()) return ""
        return content
            .split(Regex("\\r?\\n\\r?\\n"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(separator = "") { paragraph ->
                val lines = escapeHtml(paragraph).replace("\r\n", "\n").replace("\n", "<br/>")
                "<p>$lines</p>"
            }
    }

    private fun escapeHtml(value: String): String =
        value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")

    private fun buildOriginalFileName(title: String, extension: String): String {
        val slug = title
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifBlank { "source" }
        return "$slug.$extension"
    }

    private data class DetailParts(
        val extension: String,
        val format: SourceContentFormat,
        val htmlContent: String?,
        val sheets: List<SourceSheetTab>,
        val originalFileName: String? = null,
        val plainContent: String? = null,
    )

    private val samples = listOf(
        Source(
            id = "1",
            title = "Alan Turing: Computing Machinery",
            type = SourceType.FILE,
            author = "Alan Turing",
            addedLabel = "Added 2d ago",
            status = SourceStatus.READY,
            spaceId = "1",
            fileExtension = "pdf",
        ),
        Source(
            id = "2",
            title = "The Origins of Totalitarianism",
            type = SourceType.FILE,
            author = "Hannah Arendt",
            addedLabel = "Added 2d ago",
            status = SourceStatus.READY,
            spaceId = "2",
            fileExtension = "pdf",
        ),
        Source(
            id = "3",
            title = "Weapons of Math Destruction",
            type = SourceType.BOOK,
            author = "Cathy O'Neil",
            addedLabel = "Added 2d ago",
            status = SourceStatus.PROCESSING,
            spaceId = "2",
            fileExtension = "epub",
        ),
        Source(
            id = "4",
            title = "The Age of Surveillance Capitalism",
            type = SourceType.FILE,
            author = "Shoshana Zuboff",
            addedLabel = "Added 2d ago",
            status = SourceStatus.FAILED,
            spaceId = "1",
            fileExtension = "pdf",
        ),
        Source(
            id = "5",
            title = "Attention Is All You Need",
            type = SourceType.FILE,
            author = "Vaswani et al.",
            addedLabel = "Added 2d ago",
            status = SourceStatus.READY,
            spaceId = "1",
            fileExtension = "pdf",
        ),
        Source(
            id = "6",
            title = "Interview notes: archival methods",
            type = SourceType.TEXT,
            author = "Field notes",
            addedLabel = "Added 2d ago",
            status = SourceStatus.READY,
            spaceId = "3",
            fileExtension = "txt",
        ),
        Source(
            id = "7",
            title = "Lecture slides: Week 7",
            type = SourceType.FILE,
            author = "Teaching staff",
            addedLabel = "Added 3d ago",
            status = SourceStatus.READY,
            spaceId = "4",
            fileExtension = "pptx",
        ),
        Source(
            id = "8",
            title = "Course syllabus draft",
            type = SourceType.TEXT,
            author = "Teaching staff",
            addedLabel = "Added 3d ago",
            status = SourceStatus.READY,
            spaceId = "4",
            fileExtension = "txt",
        ),
        Source(
            id = "9",
            title = "Wikipedia: Neural Networks",
            type = SourceType.WEB,
            author = "Wikipedia",
            addedLabel = "Added 2d ago",
            status = SourceStatus.READY,
            spaceId = "1",
            fileExtension = "md",
        ),
        Source(
            id = "10",
            title = "Research metrics dashboard",
            type = SourceType.FILE,
            author = "Research team",
            addedLabel = "Added 1d ago",
            status = SourceStatus.READY,
            spaceId = "1",
            fileExtension = "xlsx",
        ),
    )
}
