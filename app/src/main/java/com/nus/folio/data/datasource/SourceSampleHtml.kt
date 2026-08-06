package com.nus.folio.data.datasource

import com.nus.folio.domain.model.SourceSheetTab

/**
 * Embedded HTML / sheet preview blobs for [SourceSampleData].
 */
internal object SourceSampleHtml {

    val turingPaperHtml = """
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

    val arendtBookHtml = """
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

    val weaponsOfMathDestructionHtml = """
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

    val surveillanceCapitalismHtml = """
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

    val attentionPaperHtml = """
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

    val syllabusDraftHtml = """
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

    val lectureSlidesHtml = """
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

    val interviewNotesHtml = """
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

    val neuralNetworksArticleHtml = """
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

    val researchSpreadsheetSheets = listOf(
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
