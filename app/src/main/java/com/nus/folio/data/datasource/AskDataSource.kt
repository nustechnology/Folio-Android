package com.nus.folio.data.datasource

import com.nus.folio.domain.model.AskCitation
import com.nus.folio.domain.model.AskStreamEvent
import com.nus.folio.domain.model.SourceType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class AskDataSource {

    /**
     * Returns up to 3 context-aware questions for a source that has summary/metadata.
     * Empty when the source has no suggestion metadata (UI falls back to static chips).
     */
    suspend fun fetchSuggestedQuestions(sourceId: String): List<String> {
        delay(100)
        return sourceSuggestions[sourceId].orEmpty().take(MAX_SUGGESTIONS)
    }

    fun streamAnswer(
        spaceId: String,
        question: String,
        sourceId: String?,
    ): Flow<AskStreamEvent> = flow {
        delay(THINKING_DELAY_MS)
        val answer = buildAnswer(spaceId = spaceId, question = question, sourceId = sourceId)
        val chunks = chunkText(answer.body)
        for (chunk in chunks) {
            emit(AskStreamEvent.Delta(chunk))
            delay(CHUNK_DELAY_MS)
        }
        emit(
            AskStreamEvent.Completed(
                citations = answer.citations,
                limitation = answer.limitation,
            ),
        )
    }

    private fun buildAnswer(
        spaceId: String,
        question: String,
        sourceId: String?,
    ): MockAnswer {
        val scoped = sourceId?.let { singleSourceAnswers[it] }
        if (scoped != null) return scoped

        val spaceAnswer = spaceAnswers[spaceId]
        if (spaceAnswer != null) return spaceAnswer

        return MockAnswer(
            body = "Based on the available evidence for \"$question\", the sources support a " +
                "measured reading of the claim [1]. Additional context appears across related " +
                "materials [2].",
            citations = listOf(
                AskCitation(
                    index = 1,
                    sourceId = "unknown",
                    sourceTitle = "Primary source",
                    sourceType = SourceType.FILE,
                    fileExtension = "pdf",
                    locationLabel = "Page 1",
                    evidenceText = "Primary evidence excerpt.",
                ),
                AskCitation(
                    index = 2,
                    sourceId = "unknown",
                    sourceTitle = "Supporting source",
                    sourceType = SourceType.WEB,
                    locationLabel = "Section 2",
                    evidenceText = "Supporting evidence excerpt.",
                ),
            ),
            limitation = "Limitation: Evidence coverage is limited for this space.",
        )
    }

    private fun chunkText(text: String): List<String> {
        if (text.isEmpty()) return emptyList()
        val chunks = mutableListOf<String>()
        var index = 0
        while (index < text.length) {
            val end = (index + CHUNK_SIZE).coerceAtMost(text.length)
            chunks += text.substring(index, end)
            index = end
        }
        return chunks
    }

    private data class MockAnswer(
        val body: String,
        val citations: List<AskCitation>,
        val limitation: String?,
    )

    companion object {
        private const val MAX_SUGGESTIONS = 3
        private const val THINKING_DELAY_MS = 400L
        private const val CHUNK_DELAY_MS = 45L
        private const val CHUNK_SIZE = 28

        private val sourceSuggestions = mapOf(
            "1" to listOf(
                "What is Turing's main claim about machine intelligence?",
                "How does the imitation game define thinking?",
                "Which objections does Turing anticipate?",
            ),
            "5" to listOf(
                "What problem does the Transformer address?",
                "How does self-attention work in this paper?",
                "What results support the architecture's claims?",
            ),
            "6" to listOf(
                "What archival methods are described?",
                "Where do provenance concerns appear?",
                "Summarize the interview takeaways.",
            ),
            "7" to listOf(
                "What are the learning objectives for Week 7?",
                "Which concepts need the most prep time?",
                "Summarize the lecture outline.",
            ),
            "9" to listOf(
                "How are neural networks defined here?",
                "What key architectures are covered?",
                "Where might this overview be incomplete?",
            ),
        )

        private val singleSourceAnswers = mapOf(
            "1" to MockAnswer(
                body = "Turing reframes the question of machine intelligence as an observable " +
                    "imitation game rather than an inner essence [1]. He anticipates objections " +
                    "about consciousness and creativity while defending behavioral criteria [1][3].",
                citations = listOf(
                    AskCitation(
                        index = 1,
                        sourceId = "1",
                        sourceTitle = "Alan Turing: Computing Machinery",
                        sourceType = SourceType.FILE,
                        fileExtension = "pdf",
                        locationLabel = "Page 14",
                        evidenceText = "The new form of the problem can be described in terms of a game which we call the \"imitation game.\"",
                    ),
                    AskCitation(
                        index = 3,
                        sourceId = "1",
                        sourceTitle = "Alan Turing: Computing Machinery",
                        sourceType = SourceType.FILE,
                        fileExtension = "pdf",
                        locationLabel = "Page 16",
                        evidenceText = "I propose to consider the question, \"Can machines think?\" This should begin with definitions of the meaning of the terms \"machine\" and \"think.\"",
                    ),
                ),
                limitation = "Limitation: Only one source contains the primary argument text.",
            ),
            "5" to MockAnswer(
                body = "The Transformer replaces recurrence with self-attention, enabling " +
                    "parallel sequence modeling [1]. Reported results show strong translation " +
                    "quality with less training cost than prior architectures [1][4].",
                citations = listOf(
                    AskCitation(
                        index = 1,
                        sourceId = "5",
                        sourceTitle = "Attention Is All You Need",
                        sourceType = SourceType.FILE,
                        fileExtension = "pdf",
                        locationLabel = "Page 2",
                        evidenceText = "The Transformer replaces recurrence with self-attention",
                    ),
                    AskCitation(
                        index = 4,
                        sourceId = "5",
                        sourceTitle = "Attention Is All You Need",
                        sourceType = SourceType.FILE,
                        fileExtension = "pdf",
                        locationLabel = "Page 8",
                        evidenceText = "Reported results show strong translation quality",
                    ),
                ),
                limitation = null,
            ),
            "6" to MockAnswer(
                body = "Field notes emphasize provenance before transcription and treat archival " +
                    "silence as analytic evidence [1]. Interview excerpts highlight repeated " +
                    "gaps in institutional records [1].",
                citations = listOf(
                    AskCitation(
                        index = 1,
                        sourceId = "6",
                        sourceTitle = "Interview notes: archival methods",
                        sourceType = SourceType.TEXT,
                        fileExtension = "txt",
                        locationLabel = "Paragraph 3",
                        evidenceText = "Prioritize provenance notes before transcription decisions",
                    ),
                ),
                limitation = "Limitation: Only one source contains a primary provider interview.",
            ),
        )

        private val spaceAnswers = mapOf(
            "1" to MockAnswer(
                body = "Across the space, sources converge on measurable behavior as a proxy for " +
                    "intelligence [1], while later work stresses attention mechanisms and " +
                    "evaluation metrics [5][9]. Disagreement appears mainly in what counts as " +
                    "sufficient evidence of understanding [1][5].",
                citations = listOf(
                    AskCitation(
                        index = 1,
                        sourceId = "1",
                        sourceTitle = "Alan Turing: Computing Machinery",
                        sourceType = SourceType.FILE,
                        fileExtension = "pdf",
                        locationLabel = "Page 14",
                        evidenceText = "The new form of the problem can be described in terms of a game which we call the \"imitation game.\"",
                    ),
                    AskCitation(
                        index = 5,
                        sourceId = "5",
                        sourceTitle = "Attention Is All You Need",
                        sourceType = SourceType.FILE,
                        fileExtension = "pdf",
                        locationLabel = "Page 2",
                        evidenceText = "The Transformer replaces recurrence with self-attention",
                    ),
                    AskCitation(
                        index = 9,
                        sourceId = "9",
                        sourceTitle = "Wikipedia: Neural Networks",
                        sourceType = SourceType.WEB,
                        locationLabel = "Overview",
                        evidenceText = "neural networks",
                    ),
                ),
                limitation = null,
            ),
            "4" to MockAnswer(
                body = "Week 7 materials outline seminar prompts on archival silence and source " +
                    "criticism [7]. The syllabus draft reinforces sequencing of evidence " +
                    "exercises before debate [8].",
                citations = listOf(
                    AskCitation(
                        index = 7,
                        sourceId = "7",
                        sourceTitle = "Lecture slides: Week 7",
                        sourceType = SourceType.FILE,
                        fileExtension = "pptx",
                        locationLabel = "Slide 4",
                        evidenceText = "archival silence and source criticism",
                    ),
                    AskCitation(
                        index = 8,
                        sourceId = "8",
                        sourceTitle = "Course syllabus draft",
                        sourceType = SourceType.TEXT,
                        fileExtension = "txt",
                        locationLabel = "Week 7",
                        evidenceText = "evidence exercises before debate",
                    ),
                ),
                limitation = null,
            ),
        )
    }
}
