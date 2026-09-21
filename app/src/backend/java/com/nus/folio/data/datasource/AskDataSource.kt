package com.nus.folio.data.datasource

import com.nus.folio.data.network.AskApi
import com.nus.folio.data.network.AskApiClient
import com.nus.folio.data.network.UnauthorizedException
import com.nus.folio.domain.model.AskConversation
import com.nus.folio.domain.model.AskConversationDetail
import com.nus.folio.domain.model.AskConversationLibrary
import com.nus.folio.domain.model.AskConversationPaging
import com.nus.folio.domain.model.AskFeedbackRating
import com.nus.folio.domain.model.AskStreamEvent
import com.nus.folio.domain.model.AskSuggestions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/**
 * Ask data source — suggestions and streaming answers via the real API.
 * On HTTP 401, refreshes the access token once and retries.
 */
class AskDataSource(
    private val accessTokenProvider: () -> String? = { null },
    private val refreshAccessToken: suspend () -> String? = { null },
    private val askApi: AskApi = AskApiClient(),
) {

    suspend fun fetchSuggestedQuestions(
        spaceId: String,
        sourceId: String?,
    ): AskSuggestions {
        require(spaceId.isNotBlank()) { "Space id is required" }
        val trimmedSourceId = sourceId?.trim()?.takeIf { it.isNotEmpty() }
        return withAuthRetry { accessToken ->
            askApi.getSuggestions(
                accessToken = accessToken,
                spaceId = spaceId.trim(),
                sourceId = trimmedSourceId,
            )
        }
    }

    fun streamAnswer(
        spaceId: String,
        question: String,
        sourceId: String?,
        conversationId: String? = null,
    ): Flow<AskStreamEvent> {
        require(spaceId.isNotBlank()) { "Space id is required" }
        require(question.isNotBlank()) { "Question is required" }
        val trimmedSpaceId = spaceId.trim()
        val trimmedQuestion = question.trim()
        val trimmedSourceId = sourceId?.trim()?.takeIf { it.isNotEmpty() }
        val trimmedConversationId = conversationId?.trim()?.takeIf { it.isNotEmpty() }
        return flow {
            var token = requireAccessToken()
            try {
                emitAll(
                    askApi.streamAnswer(
                        accessToken = token,
                        spaceId = trimmedSpaceId,
                        question = trimmedQuestion,
                        sourceId = trimmedSourceId,
                        conversationId = trimmedConversationId,
                    ),
                )
            } catch (unauthorized: UnauthorizedException) {
                val latestToken = accessTokenProvider()?.takeIf { it.isNotBlank() }
                token = if (latestToken != null && latestToken != token) {
                    latestToken
                } else {
                    refreshAccessToken()?.takeIf { it.isNotBlank() } ?: throw unauthorized
                }
                emitAll(
                    askApi.streamAnswer(
                        accessToken = token,
                        spaceId = trimmedSpaceId,
                        question = trimmedQuestion,
                        sourceId = trimmedSourceId,
                        conversationId = trimmedConversationId,
                    ),
                )
            }
        }
    }

    suspend fun submitFeedback(
        spaceId: String,
        conversationId: String,
        messageId: String,
        rating: AskFeedbackRating,
    ) {
        require(spaceId.isNotBlank()) { "Space id is required" }
        require(conversationId.isNotBlank()) { "Conversation id is required" }
        require(messageId.isNotBlank()) { "Message id is required" }
        withAuthRetry { accessToken ->
            askApi.submitFeedback(
                accessToken = accessToken,
                spaceId = spaceId.trim(),
                conversationId = conversationId.trim(),
                messageId = messageId.trim(),
                rating = rating.toApiValue(),
            )
        }
    }

    suspend fun fetchConversations(
        spaceId: String,
        search: String? = null,
        page: Int = AskConversationPaging.DEFAULT_PAGE,
        limit: Int = AskConversationPaging.DEFAULT_LIMIT,
    ): AskConversationLibrary {
        require(spaceId.isNotBlank()) { "Space id is required" }
        return withAuthRetry { accessToken ->
            askApi.listConversations(
                accessToken = accessToken,
                spaceId = spaceId.trim(),
                search = search?.trim()?.takeIf { it.isNotEmpty() },
                page = page.coerceAtLeast(AskConversationPaging.DEFAULT_PAGE),
                limit = limit.coerceAtLeast(1),
            )
        }
    }

    suspend fun fetchConversation(
        spaceId: String,
        conversationId: String,
    ): AskConversationDetail {
        require(spaceId.isNotBlank()) { "Space id is required" }
        require(conversationId.isNotBlank()) { "Conversation id is required" }
        return withAuthRetry { accessToken ->
            askApi.getConversation(
                accessToken = accessToken,
                spaceId = spaceId.trim(),
                conversationId = conversationId.trim(),
            )
        }
    }

    suspend fun updateConversation(
        spaceId: String,
        conversationId: String,
        title: String,
    ): AskConversation {
        require(spaceId.isNotBlank()) { "Space id is required" }
        require(conversationId.isNotBlank()) { "Conversation id is required" }
        val trimmedTitle = title.trim()
        require(trimmedTitle.isNotEmpty()) { "Title is required" }
        return withAuthRetry { accessToken ->
            askApi.updateConversation(
                accessToken = accessToken,
                spaceId = spaceId.trim(),
                conversationId = conversationId.trim(),
                title = trimmedTitle,
            )
        }
    }

    suspend fun deleteConversation(
        spaceId: String,
        conversationId: String,
    ) {
        require(spaceId.isNotBlank()) { "Space id is required" }
        require(conversationId.isNotBlank()) { "Conversation id is required" }
        withAuthRetry { accessToken ->
            askApi.deleteConversation(
                accessToken = accessToken,
                spaceId = spaceId.trim(),
                conversationId = conversationId.trim(),
            )
        }
    }

    private suspend fun <T> withAuthRetry(block: suspend (accessToken: String) -> T): T {
        val accessToken = requireAccessToken()
        return try {
            block(accessToken)
        } catch (unauthorized: UnauthorizedException) {
            val latestToken = accessTokenProvider()?.takeIf { it.isNotBlank() }
            val tokenToRetry = if (latestToken != null && latestToken != accessToken) {
                latestToken
            } else {
                refreshAccessToken()?.takeIf { it.isNotBlank() } ?: throw unauthorized
            }
            block(tokenToRetry)
        }
    }

    private fun requireAccessToken(): String =
        accessTokenProvider()?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Not authenticated")
}
