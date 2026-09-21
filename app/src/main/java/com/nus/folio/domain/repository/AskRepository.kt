package com.nus.folio.domain.repository

import com.nus.folio.domain.model.AskConversation
import com.nus.folio.domain.model.AskConversationDetail
import com.nus.folio.domain.model.AskConversationLibrary
import com.nus.folio.domain.model.AskConversationPaging
import com.nus.folio.domain.model.AskFeedbackRating
import com.nus.folio.domain.model.AskStreamEvent
import com.nus.folio.domain.model.AskSuggestions
import kotlinx.coroutines.flow.Flow

interface AskRepository {
    /**
     * Three suggested questions for [spaceId].
     * When [sourceId] is non-null, scopes to that source (`scope=source`); otherwise the space.
     * Dynamic questions are drafted from a ready source's text; other cases return generics
     * with [AskSuggestions.isDynamic] false (caller may show localized fallbacks).
     */
    suspend fun getSuggestedQuestions(
        spaceId: String,
        sourceId: String?,
    ): Result<AskSuggestions>

    /**
     * Streams a grounded answer for [question] within [spaceId].
     * When [sourceId] is non-null, grounds on that source; otherwise the entire space.
     * Pass [conversationId] from the previous [AskStreamEvent.Started] to continue the thread.
     */
    fun streamAnswer(
        spaceId: String,
        question: String,
        sourceId: String?,
        conversationId: String? = null,
    ): Flow<AskStreamEvent>

    /**
     * Records thumbs-up / thumbs-down for an assistant [messageId] in [conversationId].
     * [AskFeedbackRating.USEFUL] maps to `"useful"`; [AskFeedbackRating.NOT_USEFUL] to `"not_useful"`.
     */
    suspend fun submitFeedback(
        spaceId: String,
        conversationId: String,
        messageId: String,
        rating: AskFeedbackRating,
    ): Result<Unit>

    /** Conversations in [spaceId], newest first when the API provides timestamps. */
    suspend fun getConversations(
        spaceId: String,
        search: String? = null,
        page: Int = AskConversationPaging.DEFAULT_PAGE,
        limit: Int = AskConversationPaging.DEFAULT_LIMIT,
    ): Result<AskConversationLibrary>

    /** Full message thread for [conversationId] in [spaceId]. */
    suspend fun getConversation(
        spaceId: String,
        conversationId: String,
    ): Result<AskConversationDetail>

    suspend fun updateConversation(
        spaceId: String,
        conversationId: String,
        title: String,
    ): Result<AskConversation>

    suspend fun deleteConversation(
        spaceId: String,
        conversationId: String,
    ): Result<Unit>
}
