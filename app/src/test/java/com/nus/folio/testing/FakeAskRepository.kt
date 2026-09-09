package com.nus.folio.testing

import com.nus.folio.domain.model.AskCitation
import com.nus.folio.domain.model.AskFeedbackRating
import com.nus.folio.domain.model.AskStreamEvent
import com.nus.folio.domain.model.AskSuggestions
import com.nus.folio.domain.model.SourceType
import com.nus.folio.domain.repository.AskRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FakeAskRepository : AskRepository {

    var lastSpaceId: String? = null

    var getSuggestedQuestionsResult: Result<AskSuggestions>? = null
    var getSuggestedQuestionsCallCount = 0
    var lastSuggestedSpaceId: String? = null
    var lastSuggestedSourceId: String? = null

    var streamAnswerCallCount = 0
    var lastStreamQuestion: String? = null
    var lastStreamSourceId: String? = null
    var lastStreamConversationId: String? = null
    var streamEvents: List<AskStreamEvent>? = null
    /** When true, the stream suspends after emitting [streamEvents] until cancelled. */
    var hangAfterStreamEvents: Boolean = false
    /** Thrown after [streamEvents] to simulate an SSE `error` / stream failure. */
    var streamThrow: Throwable? = null

    var submitFeedbackResult: Result<Unit> = Result.success(Unit)
    var submitFeedbackCallCount = 0
    /** When set, [submitFeedback] suspends until this deferred completes. */
    var submitFeedbackGate: CompletableDeferred<Unit>? = null
    var lastFeedbackSpaceId: String? = null
    var lastFeedbackConversationId: String? = null
    var lastFeedbackMessageId: String? = null
    var lastFeedbackRating: AskFeedbackRating? = null

    override suspend fun getSuggestedQuestions(
        spaceId: String,
        sourceId: String?,
    ): Result<AskSuggestions> {
        getSuggestedQuestionsCallCount++
        lastSuggestedSpaceId = spaceId
        lastSuggestedSourceId = sourceId
        getSuggestedQuestionsResult?.let { return it }
        val questions = sourceId?.let { sampleSuggestions[it] }.orEmpty()
        return Result.success(
            AskSuggestions(
                questions = questions,
                isDynamic = questions.isNotEmpty(),
            ),
        )
    }

    override fun streamAnswer(
        spaceId: String,
        question: String,
        sourceId: String?,
        conversationId: String?,
    ): Flow<AskStreamEvent> {
        streamAnswerCallCount++
        lastSpaceId = spaceId
        lastStreamQuestion = question
        lastStreamSourceId = sourceId
        lastStreamConversationId = conversationId
        val events = streamEvents ?: listOf(
            AskStreamEvent.Started(
                conversationId = "conv-1",
                messageId = "msg-1",
            ),
            AskStreamEvent.Delta("Grounded answer for "),
            AskStreamEvent.Delta("\"$question\" [1]."),
            AskStreamEvent.Completed(
                messageId = "msg-1",
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
            streamThrow?.let { throw it }
            if (hangAfterStreamEvents) {
                awaitCancellation()
            }
        }
    }

    override suspend fun submitFeedback(
        spaceId: String,
        conversationId: String,
        messageId: String,
        rating: AskFeedbackRating,
    ): Result<Unit> {
        submitFeedbackCallCount++
        lastFeedbackSpaceId = spaceId
        lastFeedbackConversationId = conversationId
        lastFeedbackMessageId = messageId
        lastFeedbackRating = rating
        submitFeedbackGate?.await()
        return submitFeedbackResult
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
