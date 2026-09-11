package com.nus.folio.testing

import com.nus.folio.domain.model.AskCitation
import com.nus.folio.domain.model.AskConversation
import com.nus.folio.domain.model.AskConversationDetail
import com.nus.folio.domain.model.AskConversationLibrary
import com.nus.folio.domain.model.AskConversationMessage
import com.nus.folio.domain.model.AskConversationRole
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

    var getConversationsResult: Result<AskConversationLibrary>? = null
    var getConversationsCallCount = 0
    var lastConversationsSpaceId: String? = null
    var lastConversationsSearch: String? = null
    var lastConversationsPage: Int? = null
    var lastConversationsLimit: Int? = null
    var conversationsBySpace: Map<String, List<AskConversation>> = sampleConversations

    var getConversationResult: Result<AskConversationDetail>? = null
    var getConversationCallCount = 0
    var lastConversationSpaceId: String? = null
    var lastConversationId: String? = null
    var conversationDetails: Map<String, AskConversationDetail> = sampleConversationDetails

    var deleteConversationResult: Result<Unit> = Result.success(Unit)
    var deleteConversationCallCount = 0
    var lastDeletedConversationId: String? = null

    var updateConversationResult: Result<AskConversation>? = null
    var updateConversationCallCount = 0
    var lastUpdatedConversationId: String? = null
    var lastUpdatedConversationTitle: String? = null

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

    override suspend fun getConversations(
        spaceId: String,
        search: String?,
        page: Int,
        limit: Int,
    ): Result<AskConversationLibrary> {
        getConversationsCallCount++
        lastConversationsSpaceId = spaceId
        lastConversationsSearch = search
        lastConversationsPage = page
        lastConversationsLimit = limit
        getConversationsResult?.let { return it }
        val all = conversationsBySpace[spaceId].orEmpty()
        val filtered = search?.trim()?.takeIf { it.isNotEmpty() }?.let { query ->
            all.filter { it.title.contains(query, ignoreCase = true) }
        } ?: all
        val fromIndex = ((page - 1) * limit).coerceAtLeast(0)
        val pageItems = filtered.drop(fromIndex).take(limit)
        val totalPages = if (filtered.isEmpty()) {
            1
        } else {
            (filtered.size + limit - 1) / limit
        }
        return Result.success(
            AskConversationLibrary(
                conversations = pageItems,
                page = page,
                limit = limit,
                totalCount = filtered.size,
                hasMore = page < totalPages,
            ),
        )
    }

    override suspend fun getConversation(
        spaceId: String,
        conversationId: String,
    ): Result<AskConversationDetail> {
        getConversationCallCount++
        lastConversationSpaceId = spaceId
        lastConversationId = conversationId
        getConversationResult?.let { return it }
        conversationDetails[conversationId]?.let { return Result.success(it) }
        val summary = conversationsBySpace[spaceId].orEmpty().firstOrNull { it.id == conversationId }
            ?: AskConversation(
                id = conversationId,
                title = "Conversation",
                dateLabel = "Aug 20, 07:54",
                spaceId = spaceId,
            )
        return Result.success(
            AskConversationDetail(
                conversation = summary,
                messages = emptyList(),
            ),
        )
    }

    override suspend fun updateConversation(
        spaceId: String,
        conversationId: String,
        title: String,
    ): Result<AskConversation> {
        updateConversationCallCount++
        lastConversationSpaceId = spaceId
        lastUpdatedConversationId = conversationId
        lastUpdatedConversationTitle = title
        val updated = AskConversation(
            id = conversationId,
            title = title,
            dateLabel = conversationsBySpace[spaceId].orEmpty()
                .firstOrNull { it.id == conversationId }
                ?.dateLabel
                .orEmpty(),
            spaceId = spaceId,
            sourceId = conversationsBySpace[spaceId].orEmpty()
                .firstOrNull { it.id == conversationId }
                ?.sourceId,
        )
        val result = updateConversationResult ?: Result.success(updated)
        if (result.isSuccess) {
            conversationsBySpace = conversationsBySpace.mapValues { (id, conversations) ->
                if (id != spaceId) {
                    conversations
                } else {
                    conversations.map { conversation ->
                        if (conversation.id == conversationId) {
                            conversation.copy(title = title)
                        } else {
                            conversation
                        }
                    }
                }
            }
        }
        return result
    }

    override suspend fun deleteConversation(
        spaceId: String,
        conversationId: String,
    ): Result<Unit> {
        deleteConversationCallCount++
        lastConversationSpaceId = spaceId
        lastDeletedConversationId = conversationId
        if (deleteConversationResult.isSuccess) {
            conversationsBySpace = conversationsBySpace.mapValues { (_, conversations) ->
                conversations.filterNot { it.id == conversationId }
            }
        }
        return deleteConversationResult
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

        val sampleConversations = mapOf(
            "1" to listOf(
                AskConversation(
                    id = "conv-1",
                    title = "What were the operating costs in Q4?",
                    dateLabel = "Aug 20, 07:54",
                    spaceId = "1",
                    sourceId = "1",
                ),
                AskConversation(
                    id = "conv-2",
                    title = "Summarize all the evidence.",
                    dateLabel = "Aug 19, 14:12",
                    spaceId = "1",
                ),
            ),
        )

        val sampleConversationDetails = mapOf(
            "conv-1" to AskConversationDetail(
                conversation = AskConversation(
                    id = "conv-1",
                    title = "What were the operating costs in Q4?",
                    dateLabel = "Aug 20, 07:54",
                    spaceId = "1",
                    sourceId = "1",
                ),
                messages = listOf(
                    AskConversationMessage(
                        id = "user-1",
                        role = AskConversationRole.USER,
                        content = "What were the operating costs in Q4?",
                    ),
                    AskConversationMessage(
                        id = "msg-1",
                        role = AskConversationRole.ASSISTANT,
                        content = "Operating costs in Q4 were driven by staffing and infrastructure [1].",
                        citations = listOf(
                            AskCitation(
                                index = 1,
                                sourceId = "1",
                                sourceTitle = "Onboarding Benchmark Report",
                                sourceType = SourceType.FILE,
                                locationLabel = "Page 14",
                                evidenceText = "Q4 operating costs",
                            ),
                        ),
                        feedback = AskFeedbackRating.USEFUL,
                    ),
                ),
            ),
        )
    }
}
