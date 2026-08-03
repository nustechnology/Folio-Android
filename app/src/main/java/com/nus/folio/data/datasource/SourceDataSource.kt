package com.nus.folio.data.datasource

import com.nus.folio.domain.model.Source
import com.nus.folio.domain.model.SourceContentFormat
import com.nus.folio.domain.model.SourceDetail
import com.nus.folio.domain.model.SourceLibrary
import com.nus.folio.domain.model.SourceSheetTab
import com.nus.folio.domain.model.SourceStatus
import com.nus.folio.domain.model.SourceType
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SourceDataSource {

    private val mutex = Mutex()
    private val sources: MutableList<Source> = defaultSources.toMutableList()

    suspend fun fetchSources(spaceId: String): SourceLibrary {
        delay(200)
        return libraryFor(spaceId)
    }

    suspend fun updateSource(source: Source): Source {
        delay(200)
        return mutex.withLock {
            val index = sources.indexOfFirst { it.id == source.id }
            if (index < 0) {
                throw NoSuchElementException("Source not found: ${source.id}")
            }
            sources[index] = source
            source
        }
    }

    suspend fun deleteSource(sourceId: String) {
        delay(200)
        mutex.withLock {
            val removed = sources.removeAll { it.id == sourceId }
            if (!removed) {
                throw NoSuchElementException("Source not found: $sourceId")
            }
        }
    }

    suspend fun fetchSourceDetail(spaceId: String, sourceId: String): SourceDetail {
        delay(200)
        return mutex.withLock {
            val source = sources.find { it.id == sourceId && it.spaceId == spaceId }
                ?: throw NoSuchElementException("Source not found")
            buildSourceDetail(source)
        }
    }

    private suspend fun libraryFor(spaceId: String): SourceLibrary =
        mutex.withLock {
            val scoped = sources.filter { it.spaceId == spaceId }
            SourceLibrary(
                sources = scoped,
                allCount = scoped.size,
                papersCount = scoped.count { it.type == SourceType.PDF },
                booksCount = scoped.count { it.type == SourceType.BOOK },
                webCount = scoped.count { it.type == SourceType.WEB },
                textCount = scoped.count { it.type == SourceType.TEXT },
            )
        }

    private fun buildSourceDetail(source: Source): SourceDetail {
        val (extension, format, htmlContent, sheets) = when (source.id) {
            "3" -> Quadruple("epub", SourceContentFormat.DOCUMENT, turingPaperHtml, emptyList())
            "7" -> Quadruple("pptx", SourceContentFormat.SLIDES, lectureSlidesHtml, emptyList())
            "10" -> Quadruple("xlsx", SourceContentFormat.SHEET, null, researchSpreadsheetSheets)
            "6", "8" -> Quadruple("txt", SourceContentFormat.DOCUMENT, interviewNotesHtml, emptyList())
            "9" -> Quadruple("md", SourceContentFormat.DOCUMENT, neuralNetworksArticleHtml, emptyList())
            else -> Quadruple("pdf", SourceContentFormat.DOCUMENT, turingPaperHtml, emptyList())
        }

        return SourceDetail(
            id = source.id,
            title = source.title,
            author = source.author,
            addedLabel = source.addedLabel,
            type = source.type,
            status = source.status,
            spaceId = source.spaceId,
            fileExtension = extension,
            contentFormat = format,
            originalFileName = buildOriginalFileName(source.title, extension),
            htmlContent = htmlContent,
            sheets = sheets,
        )
    }

    private fun buildOriginalFileName(title: String, extension: String): String {
        val slug = title
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifBlank { "source" }
        return "$slug.$extension"
    }

    private data class Quadruple<A, B, C, D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D,
    )

    companion object {
        private val defaultSources = listOf(
            Source(
                id = "1",
                title = "Alan Turing: Computing Machinery",
                type = SourceType.PDF,
                author = "Alan Turing",
                addedLabel = "Added 2d ago",
                status = SourceStatus.READY,
                spaceId = "1",
            ),
            Source(
                id = "2",
                title = "The Origins of Totalitarianism",
                type = SourceType.PDF,
                author = "Hannah Arendt",
                addedLabel = "Added 2d ago",
                status = SourceStatus.READY,
                spaceId = "2",
            ),
            Source(
                id = "3",
                title = "Weapons of Math Destruction",
                type = SourceType.BOOK,
                author = "Cathy O'Neil",
                addedLabel = "Added 2d ago",
                status = SourceStatus.PROCESSING,
                spaceId = "2",
            ),
            Source(
                id = "4",
                title = "The Age of Surveillance Capitalism",
                type = SourceType.PDF,
                author = "Shoshana Zuboff",
                addedLabel = "Added 2d ago",
                status = SourceStatus.FAILED,
                spaceId = "1",
            ),
            Source(
                id = "5",
                title = "Attention Is All You Need",
                type = SourceType.PDF,
                author = "Vaswani et al.",
                addedLabel = "Added 2d ago",
                status = SourceStatus.READY,
                spaceId = "1",
            ),
            Source(
                id = "6",
                title = "Interview notes: archival methods",
                type = SourceType.TEXT,
                author = "Field notes",
                addedLabel = "Added 2d ago",
                status = SourceStatus.READY,
                spaceId = "3",
            ),
            Source(
                id = "7",
                title = "Lecture slides: Week 7",
                type = SourceType.PDF,
                author = "Teaching staff",
                addedLabel = "Added 3d ago",
                status = SourceStatus.READY,
                spaceId = "4",
            ),
            Source(
                id = "8",
                title = "Course syllabus draft",
                type = SourceType.TEXT,
                author = "Teaching staff",
                addedLabel = "Added 3d ago",
                status = SourceStatus.READY,
                spaceId = "4",
            ),
            Source(
                id = "9",
                title = "Wikipedia: Neural Networks",
                type = SourceType.WEB,
                author = "Wikipedia",
                addedLabel = "Added 2d ago",
                status = SourceStatus.READY,
                spaceId = "1",
            ),
            Source(
                id = "10",
                title = "Research metrics dashboard",
                type = SourceType.PDF,
                author = "Research team",
                addedLabel = "Added 1d ago",
                status = SourceStatus.READY,
                spaceId = "1",
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
                                <td>PDF</td>
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
}
