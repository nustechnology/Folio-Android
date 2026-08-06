package com.nus.folio.testing

import com.nus.folio.domain.model.AskCitation
import com.nus.folio.domain.model.AskStreamEvent
import com.nus.folio.domain.model.AskTopic
import com.nus.folio.domain.model.SourceType
import com.nus.folio.domain.repository.AskRepository
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FakeAskRepository : AskRepository {

    var getAskTopicsResult: Result<List<AskTopic>>? = null
    var getAskTopicsCallCount = 0
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

    override suspend fun getAskTopics(spaceId: String): Result<List<AskTopic>> {
        getAskTopicsCallCount++
        lastSpaceId = spaceId
        getAskTopicsResult?.let { return it }
        return Result.success(sampleTopics.filter { it.spaceId == spaceId })
    }

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
        val sampleTopics = listOf(
            AskTopic("1a", "Core dissertation arguments", 4, 2, "1"),
            AskTopic("1b", "Turing and modern AI", 3, 1, "1"),
            AskTopic("2a", "Policy brief themes", 2, 2, "2"),
            AskTopic("3a", "Scientific manuscripts timeline", 1, 1, "3"),
            AskTopic("4a", "Week 7 lecture prep", 2, 1, "4"),
        )

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
