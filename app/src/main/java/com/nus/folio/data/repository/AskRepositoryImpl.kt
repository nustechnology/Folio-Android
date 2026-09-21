package com.nus.folio.data.repository

import com.nus.folio.data.datasource.AskDataSource
import com.nus.folio.domain.model.AskConversation
import com.nus.folio.domain.model.AskConversationDetail
import com.nus.folio.domain.model.AskConversationLibrary
import com.nus.folio.domain.model.AskFeedbackRating
import com.nus.folio.domain.model.AskStreamEvent
import com.nus.folio.domain.model.AskSuggestions
import com.nus.folio.domain.repository.AskRepository
import kotlinx.coroutines.flow.Flow
import kotlin.coroutines.cancellation.CancellationException

class AskRepositoryImpl(
    private val dataSource: AskDataSource,
) : AskRepository {

    override suspend fun getSuggestedQuestions(
        spaceId: String,
        sourceId: String?,
    ): Result<AskSuggestions> =
        try {
            Result.success(
                dataSource.fetchSuggestedQuestions(
                    spaceId = spaceId,
                    sourceId = sourceId,
                ),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    override fun streamAnswer(
        spaceId: String,
        question: String,
        sourceId: String?,
        conversationId: String?,
    ): Flow<AskStreamEvent> =
        dataSource.streamAnswer(
            spaceId = spaceId,
            question = question,
            sourceId = sourceId,
            conversationId = conversationId,
        )

    override suspend fun submitFeedback(
        spaceId: String,
        conversationId: String,
        messageId: String,
        rating: AskFeedbackRating,
    ): Result<Unit> =
        try {
            dataSource.submitFeedback(
                spaceId = spaceId,
                conversationId = conversationId,
                messageId = messageId,
                rating = rating,
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun getConversations(
        spaceId: String,
        search: String?,
        page: Int,
        limit: Int,
    ): Result<AskConversationLibrary> =
        try {
            Result.success(
                dataSource.fetchConversations(
                    spaceId = spaceId,
                    search = search,
                    page = page,
                    limit = limit,
                ),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun getConversation(
        spaceId: String,
        conversationId: String,
    ): Result<AskConversationDetail> =
        try {
            Result.success(dataSource.fetchConversation(spaceId, conversationId))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun updateConversation(
        spaceId: String,
        conversationId: String,
        title: String,
    ): Result<AskConversation> =
        try {
            Result.success(
                dataSource.updateConversation(spaceId, conversationId, title),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun deleteConversation(
        spaceId: String,
        conversationId: String,
    ): Result<Unit> =
        try {
            dataSource.deleteConversation(spaceId, conversationId)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
}
