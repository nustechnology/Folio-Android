package com.nus.folio.data.repository

import com.nus.folio.data.datasource.AskDataSource
import com.nus.folio.data.network.AskApi
import com.nus.folio.domain.model.AskConversation
import com.nus.folio.domain.model.AskConversationDetail
import com.nus.folio.domain.model.AskConversationLibrary
import com.nus.folio.domain.model.AskFeedbackRating
import com.nus.folio.domain.model.AskStreamEvent
import com.nus.folio.domain.model.AskSuggestions
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.coroutines.cancellation.CancellationException

class AskRepositoryImplTest {

    private class RecordingAskApi : AskApi {
        var lastSuggestionsSpaceId: String? = null
        var lastSuggestionsSourceId: String? = null
        var lastFeedbackConversationId: String? = null
        var lastFeedbackMessageId: String? = null
        var lastFeedbackRating: String? = null
        var lastListSpaceId: String? = null
        var lastListSearch: String? = null
        var lastListPage: Int? = null
        var lastListLimit: Int? = null
        var lastGetConversationId: String? = null
        var lastUpdateTitle: String? = null
        var lastDeleteConversationId: String? = null
        var throwOnList: Throwable? = null

        override suspend fun getSuggestions(
            accessToken: String,
            spaceId: String,
            sourceId: String?,
        ): AskSuggestions {
            lastSuggestionsSpaceId = spaceId
            lastSuggestionsSourceId = sourceId
            return AskSuggestions(
                questions = listOf("Q1", "Q2", "Q3"),
                isDynamic = true,
            )
        }

        override fun streamAnswer(
            accessToken: String,
            spaceId: String,
            question: String,
            sourceId: String?,
            conversationId: String?,
        ): Flow<AskStreamEvent> = emptyFlow()

        override suspend fun submitFeedback(
            accessToken: String,
            spaceId: String,
            conversationId: String,
            messageId: String,
            rating: String,
        ) {
            lastFeedbackConversationId = conversationId
            lastFeedbackMessageId = messageId
            lastFeedbackRating = rating
        }

        override suspend fun listConversations(
            accessToken: String,
            spaceId: String,
            search: String?,
            page: Int,
            limit: Int,
        ): AskConversationLibrary {
            throwOnList?.let { throw it }
            lastListSpaceId = spaceId
            lastListSearch = search
            lastListPage = page
            lastListLimit = limit
            return AskConversationLibrary(
                conversations = listOf(
                    AskConversation(
                        id = "conv-1",
                        title = "Prior thread",
                        dateLabel = "Updated just now",
                        spaceId = spaceId,
                    ),
                ),
                page = page,
                limit = limit,
                totalCount = 1,
                hasMore = false,
            )
        }

        override suspend fun getConversation(
            accessToken: String,
            spaceId: String,
            conversationId: String,
        ): AskConversationDetail {
            lastGetConversationId = conversationId
            return AskConversationDetail(
                conversation = AskConversation(
                    id = conversationId,
                    title = "Loaded thread",
                    dateLabel = "Updated just now",
                    spaceId = spaceId,
                ),
                messages = emptyList(),
            )
        }

        override suspend fun updateConversation(
            accessToken: String,
            spaceId: String,
            conversationId: String,
            title: String,
        ): AskConversation {
            lastUpdateTitle = title
            return AskConversation(
                id = conversationId,
                title = title,
                dateLabel = "Updated just now",
                spaceId = spaceId,
            )
        }

        override suspend fun deleteConversation(
            accessToken: String,
            spaceId: String,
            conversationId: String,
        ) {
            lastDeleteConversationId = conversationId
        }
    }

    private val api = RecordingAskApi()
    private val repository = AskRepositoryImpl(
        AskDataSource(
            askApi = api,
            accessTokenProvider = { "token" },
        ),
    )

    @Test
    fun `getSuggestedQuestions returns success list for source`() = runTest {
        val result = repository.getSuggestedQuestions(spaceId = "1", sourceId = "src-1")

        assertTrue(result.isSuccess)
        val suggestions = result.getOrNull()
        assertEquals(3, suggestions?.questions?.size)
        assertEquals(true, suggestions?.isDynamic)
        assertEquals("1", api.lastSuggestionsSpaceId)
        assertEquals("src-1", api.lastSuggestionsSourceId)
    }

    @Test
    fun `submitFeedback returns success`() = runTest {
        val result = repository.submitFeedback(
            spaceId = "1",
            conversationId = "conv-1",
            messageId = "msg-1",
            rating = AskFeedbackRating.USEFUL,
        )

        assertTrue(result.isSuccess)
        assertEquals("conv-1", api.lastFeedbackConversationId)
        assertEquals("msg-1", api.lastFeedbackMessageId)
        assertEquals("useful", api.lastFeedbackRating)
    }

    @Test
    fun `getConversations returns success library`() = runTest {
        val result = repository.getConversations(
            spaceId = "1",
            search = "prior",
            page = 2,
            limit = 5,
        )

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.conversations?.size)
        assertEquals("Prior thread", result.getOrNull()?.conversations?.first()?.title)
        assertEquals("1", api.lastListSpaceId)
        assertEquals("prior", api.lastListSearch)
        assertEquals(2, api.lastListPage)
        assertEquals(5, api.lastListLimit)
    }

    @Test
    fun `getConversation returns success detail`() = runTest {
        val result = repository.getConversation(spaceId = "1", conversationId = "conv-9")

        assertTrue(result.isSuccess)
        assertEquals("conv-9", result.getOrThrow().conversation.id)
        assertEquals("conv-9", api.lastGetConversationId)
    }

    @Test
    fun `updateConversation returns renamed conversation`() = runTest {
        val result = repository.updateConversation(
            spaceId = "1",
            conversationId = "conv-1",
            title = "Renamed",
        )

        assertTrue(result.isSuccess)
        assertEquals("Renamed", result.getOrThrow().title)
        assertEquals("Renamed", api.lastUpdateTitle)
    }

    @Test
    fun `deleteConversation returns success`() = runTest {
        val result = repository.deleteConversation(spaceId = "1", conversationId = "conv-1")

        assertTrue(result.isSuccess)
        assertEquals("conv-1", api.lastDeleteConversationId)
    }

    @Test
    fun `getConversations rethrows CancellationException`() = runTest {
        api.throwOnList = CancellationException("cancelled")
        try {
            repository.getConversations(spaceId = "1")
            fail("expected CancellationException")
        } catch (_: CancellationException) {
            // expected — must not be wrapped in Result.failure
        }
    }

    @Test
    fun `getConversations wraps other exceptions in failure`() = runTest {
        api.throwOnList = IOException("network")
        val result = repository.getConversations(spaceId = "1")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
    }
}
