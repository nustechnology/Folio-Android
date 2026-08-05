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

    fun libraryFor(sources: List<Source>, spaceId: String): SourceLibrary {
        val scoped = sources.filter { it.spaceId == spaceId }
        return libraryFrom(scoped)
    }

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

    fun forgetCreatedDetail(sourceId: String) {
        createdDetails.remove(sourceId)
    }

    /**
     * Persists a Manual/TEXT content patch for later [buildSourceDetail] reads.
     * Call only when the update payload includes content (non-null); null means omit.
     * Empty string is a valid patch and clears stored body text.
     */
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
            "1" -> DetailParts("pdf", SourceContentFormat.DOCUMENT, turingPaperHtml, emptyList())
            "2" -> DetailParts("pdf", SourceContentFormat.DOCUMENT, arendtBookHtml, emptyList())
            "3" -> DetailParts("epub", SourceContentFormat.DOCUMENT, weaponsOfMathDestructionHtml, emptyList())
            "4" -> DetailParts("pdf", SourceContentFormat.DOCUMENT, surveillanceCapitalismHtml, emptyList())
            "5" -> DetailParts("pdf", SourceContentFormat.DOCUMENT, attentionPaperHtml, emptyList())
            "6" -> DetailParts(
                "txt",
                SourceContentFormat.DOCUMENT,
                interviewNotesHtml,
                emptyList(),
                plainContent = "Interview notes on archival methods and digitization workflows.",
            )
            "7" -> DetailParts("pptx", SourceContentFormat.SLIDES, lectureSlidesHtml, emptyList())
            "8" -> DetailParts(
                "txt",
                SourceContentFormat.DOCUMENT,
                syllabusDraftHtml,
                emptyList(),
                plainContent = "Course syllabus draft covering weekly topics and readings.",
            )
            "9" -> DetailParts("md", SourceContentFormat.DOCUMENT, neuralNetworksArticleHtml, emptyList())
            "10" -> DetailParts(
                "xlsx",
                SourceContentFormat.SHEET,
                null,
                researchSpreadsheetSheets,
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
                val content = request.content.trim()
                DetailParts(
                    extension = "txt",
                    format = SourceContentFormat.DOCUMENT,
                    htmlContent = manualContentHtml(
                        title = title,
                        author = request.author.trim(),
                        content = content,
                    ),
                    sheets = emptyList(),
                    originalFileName = buildOriginalFileName(title, "txt"),
                    plainContent = content,
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
                        sheets = researchSpreadsheetSheets,
                        originalFileName = fileName,
                    )
                    SourceContentFormat.SLIDES -> DetailParts(
                        extension = extension,
                        format = format,
                        htmlContent = lectureSlidesHtml,
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

    private val turingPaperHtml = """
        <h1>Computing Machinery and Intelligence</h1>
        <p class="byline">Alan Turing · Mind, 1950</p>
        <h2>The Imitation Game</h2>
        <p>
            I propose to consider the question, "Can machines think?" This should begin with
            definitions of the meaning of the terms "machine" and "think." The definitions might
            be framed so as to reflect so far as possible the normal use of the words, but this
            attitude is dangerous.
        </p>
        <p>
            The new form of the problem can be described in terms of a game which we call the
            "imitation game." It is played with three people: a man (A), a woman (B), and an
            interrogator (C) who may be of either sex.
        </p>
        <h3>Digital Computers</h3>
        <p>The idea behind digital computers may be explained by saying that these machines are
            intended to carry out any operations which could be done by a human computer.</p>
        <ul>
            <li>Store unlimited information</li>
            <li>Execute logical operations at high speed</li>
            <li>Learn from experience through programming</li>
        </ul>
        <blockquote>
            We may hope that machines will eventually compete with men in all purely intellectual
            fields, but which are the best ones to start with?
        </blockquote>
        <h2>Learning Machines</h2>
        <p>
            Instead of trying to produce a programme to simulate the adult mind, why not rather
            try to produce one which simulates the child's? If this were then subjected to an
            appropriate course of education one would obtain the adult brain.
        </p>
    """.trimIndent()

    private val arendtBookHtml = """
        <h1>The Origins of Totalitarianism</h1>
        <p class="byline">Hannah Arendt · 1951</p>
        <h2>Imperialism and the nation-state</h2>
        <p>
            Arendt traces how imperial expansion and bureaucratic rule eroded the political
            foundations of the nation-state, creating conditions in which mass movements could
            mobilize populations against pluralism.
        </p>
        <h3>Key themes</h3>
        <ul>
            <li>Antisemitism as a political force in modern Europe</li>
            <li>Imperialism and the rise of race-thinking</li>
            <li>Totalitarian movements and the destruction of the public realm</li>
        </ul>
    """.trimIndent()

    private val weaponsOfMathDestructionHtml = """
        <h1>Weapons of Math Destruction</h1>
        <p class="byline">Cathy O'Neil · Crown, 2016</p>
        <h2>Opaque models and hidden harm</h2>
        <p>
            O'Neil argues that many scoring systems used in hiring, lending, and policing are
            opaque, scalable, and destructive — amplifying inequality while claiming mathematical
            neutrality.
        </p>
        <h3>What makes a WMD?</h3>
        <ul>
            <li>Opacity that shields the model from accountability</li>
            <li>Scale that affects large populations quickly</li>
            <li>Damage concentrated on vulnerable groups</li>
        </ul>
    """.trimIndent()

    private val surveillanceCapitalismHtml = """
        <h1>The Age of Surveillance Capitalism</h1>
        <p class="byline">Shoshana Zuboff · PublicAffairs, 2019</p>
        <h2>Behavioral surplus</h2>
        <p>
            Zuboff describes how digital platforms extract behavioral data to predict and shape
            future action, transforming personal experience into raw material for commercial
            forecasting markets.
        </p>
        <h3>Core claims</h3>
        <ul>
            <li>Surveillance capitalism operates beyond traditional market contracts</li>
            <li>Instrumentarian power aims to modify behavior at scale</li>
            <li>Democratic institutions lag behind extraction infrastructures</li>
        </ul>
    """.trimIndent()

    private val attentionPaperHtml = """
        <h1>Attention Is All You Need</h1>
        <p class="byline">Vaswani et al. · NeurIPS, 2017</p>
        <h2>The Transformer architecture</h2>
        <p>
            The authors propose the Transformer, relying entirely on self-attention mechanisms
            and dispensing with recurrence and convolutions for sequence transduction tasks such
            as machine translation.
        </p>
        <h3>Contributions</h3>
        <ul>
            <li>Multi-head attention captures long-range dependencies efficiently</li>
            <li>Parallelizable training improves throughput on modern hardware</li>
            <li>State-of-the-art results on WMT translation benchmarks</li>
        </ul>
    """.trimIndent()

    private val syllabusDraftHtml = """
        <h1>Course syllabus draft</h1>
        <p class="byline">Teaching staff · Draft for review</p>
        <h2>Learning outcomes</h2>
        <p>
            Students will evaluate primary sources across physical and digital archives, compare
            metadata practices, and produce annotated bibliographies with defensible citations.
        </p>
        <h3>Weekly outline</h3>
        <ul>
            <li>Week 1: Evidence, provenance, and the research question</li>
            <li>Week 4: OCR, transcription, and searchable text</li>
            <li>Week 7: Born-digital collections and web archives</li>
        </ul>
    """.trimIndent()

    private val lectureSlidesHtml = """
        <h2 class="slide-heading">Slide 1: Course Overview</h2>
        <p>Week 7 — History of Science in the Digital Age</p>
        <ul>
            <li>From card catalogues to computational archives</li>
            <li>How digitization changes evidence and interpretation</li>
            <li>Case study: Alan Turing's legacy</li>
        </ul>
        <hr class="slide-divider" />
        <h2 class="slide-heading">Slide 2: Archival Methods</h2>
        <p>Researchers now work across physical collections, born-digital records, and web archives.</p>
        <blockquote>Every archive is a theory of what counts as evidence.</blockquote>
        <hr class="slide-divider" />
        <h2 class="slide-heading">Slide 3: Discussion Prompts</h2>
        <ol>
            <li>What changes when a primary source becomes searchable text?</li>
            <li>Where do metadata and provenance matter most?</li>
            <li>How should students cite born-digital materials?</li>
        </ol>
    """.trimIndent()

    private val interviewNotesHtml = """
        <h1>Interview notes: archival methods</h1>
        <p class="byline">Field notes · March 2026</p>
        <h2>Session summary</h2>
        <p>
            Archivists described a shift from item-level description to collection-level discovery.
            Students still need close reading skills, but they also need to understand how
            digitization changes what survives in the record.
        </p>
        <h3>Key themes</h3>
        <ul>
            <li>Provenance is now part of every search result</li>
            <li>OCR quality shapes which questions feel answerable</li>
            <li>Teaching should include both the artifact and the pipeline</li>
        </ul>
        <blockquote>
            "We are not just preserving documents. We are preserving the conditions under which
            future readers can trust them."
        </blockquote>
    """.trimIndent()

    private val neuralNetworksArticleHtml = """
        <h1>Artificial neural network</h1>
        <p class="byline">Wikipedia · Saved article</p>
        <h2>Overview</h2>
        <p>
            An artificial neural network is a computational model inspired by biological neural
            networks. It consists of layers of interconnected nodes that transform input signals
            into useful representations.
        </p>
        <h3>Common architectures</h3>
        <ul>
            <li>Feedforward networks</li>
            <li>Convolutional networks for vision</li>
            <li>Recurrent and transformer models for sequences</li>
        </ul>
        <blockquote>
            Modern language models rely on attention mechanisms to weigh relationships between
            tokens across long contexts.
        </blockquote>
    """.trimIndent()

    private val researchSpreadsheetSheets = listOf(
        SourceSheetTab(
            id = "summary",
            name = "Summary",
            htmlTable = """
                <table>
                    <thead>
                        <tr>
                            <th>Metric</th>
                            <th>Q1</th>
                            <th>Q2</th>
                            <th>Q3</th>
                            <th>Q4</th>
                            <th>YoY</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td>Sources indexed</td>
                            <td>128</td>
                            <td>142</td>
                            <td>156</td>
                            <td>171</td>
                            <td>+18%</td>
                        </tr>
                        <tr>
                            <td>Notes created</td>
                            <td>32</td>
                            <td>41</td>
                            <td>38</td>
                            <td>47</td>
                            <td>+22%</td>
                        </tr>
                        <tr>
                            <td>Ask sessions</td>
                            <td>18</td>
                            <td>24</td>
                            <td>29</td>
                            <td>35</td>
                            <td>+31%</td>
                        </tr>
                    </tbody>
                </table>
            """.trimIndent(),
        ),
        SourceSheetTab(
            id = "raw",
            name = "Raw Data",
            htmlTable = """
                <table>
                    <thead>
                        <tr>
                            <th>Source ID</th>
                            <th>Title</th>
                            <th>Type</th>
                            <th>Status</th>
                            <th>Last accessed</th>
                            <th>Citations</th>
                            <th>Notes linked</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td>1</td>
                            <td>Alan Turing: Computing Machinery</td>
                            <td>PDF</td>
                            <td>Ready</td>
                            <td>2026-07-28</td>
                            <td>14</td>
                            <td>3</td>
                        </tr>
                        <tr>
                            <td>5</td>
                            <td>Attention Is All You Need</td>
                            <td>PDF</td>
                            <td>Ready</td>
                            <td>2026-07-27</td>
                            <td>22</td>
                            <td>5</td>
                        </tr>
                        <tr>
                            <td>9</td>
                            <td>Wikipedia: Neural Networks</td>
                            <td>Web</td>
                            <td>Ready</td>
                            <td>2026-07-26</td>
                            <td>6</td>
                            <td>1</td>
                        </tr>
                        <tr>
                            <td>10</td>
                            <td>Research metrics dashboard</td>
                            <td>Spreadsheet</td>
                            <td>Ready</td>
                            <td>2026-07-29</td>
                            <td>0</td>
                            <td>2</td>
                        </tr>
                    </tbody>
                </table>
            """.trimIndent(),
        ),
    )
}
