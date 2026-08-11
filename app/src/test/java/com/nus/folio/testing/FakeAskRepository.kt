package com.nus.folio.testing

import com.nus.folio.domain.model.AskCitation
import com.nus.folio.domain.model.AskStreamEvent
import com.nus.folio.domain.model.SourceType
import com.nus.folio.domain.repository.AskRepository
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FakeAskRepository : AskRepository {

    var lastSpaceId: String? = null

    var getSuggestedQuestionsResult: Result<List<String>>? = null
    var getSuggestedQuestionsCallCount = 0
    var lastSuggestedSourceId: String? = null

    var streamAnswerCallCount = 0
    var lastStreamQuestion: String? = null
    var lastStreamSourceId: String? = null
    var streamEvents: List<AskStreamEvent>? = null
    /** When true, the stream suspends after emitting [streamEvents] until cancelled. */
    var hangAfterStreamEvents: Boolean = false

    override suspend fun getSuggestedQuestions(sourceId: String): Result<List<String>> {
        getSuggestedQuestionsCallCount++
        lastSuggestedSourceId = sourceId
        getSuggestedQuestionsResult?.let { return it }
        return Result.success(sampleSuggestions[sourceId].orEmpty())
    }

    override fun streamAnswer(
        spaceId: String,
        question: String,
        sourceId: String?,
    ): Flow<AskStreamEvent> {
        streamAnswerCallCount++
        lastSpaceId = spaceId
        lastStreamQuestion = question
        lastStreamSourceId = sourceId
        val events = streamEvents ?: listOf(
            AskStreamEvent.Delta("Grounded answer for "),
            AskStreamEvent.Delta("\"$question\" [1]."),
            AskStreamEvent.Completed(
                citations = listOf(
                    AskCitation(
                        index = 1,
                        sourceId = sourceId ?: "1",
                        sourceTitle = "Sample source",
                        sourceType = SourceType.FILE,
                        fileExtension = "pdf",
                        locationLabel = "Page 1",
                        evidenceText = "Grounded answer",
                    ),
                ),
                limitation = null,
            ),
        )
        return flow {
            for (event in events) {
                emit(event)
            }
            if (hangAfterStreamEvents) {
                awaitCancellation()
            }
        }
    }

    companion object {
        val sampleSuggestions = mapOf(
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
        )
    }
}
