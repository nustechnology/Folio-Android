package com.nus.folio.data.datasource

import com.nus.folio.data.network.AskApi
import com.nus.folio.data.network.UnauthorizedException
import com.nus.folio.domain.model.AskCitation
import com.nus.folio.domain.model.AskFeedbackRating
import com.nus.folio.domain.model.AskStreamEvent
import com.nus.folio.domain.model.AskSuggestions
import com.nus.folio.domain.model.SourceType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AskDataSourceCreateTest {

    private class FakeAskApi : AskApi {
        var lastAccessToken: String? = null
        var lastSpaceId: String? = null
        var lastQuestion: String? = null
        var lastSourceId: String? = null
        var lastConversationId: String? = null
        var lastSuggestionsSpaceId: String? = null
        var lastSuggestionsSourceId: String? = null
        var lastFeedbackSpaceId: String? = null
        var lastFeedbackConversationId: String? = null
        var lastFeedbackMessageId: String? = null
        var lastFeedbackRating: String? = null
        var streamCallCount = 0
        var suggestionsCallCount = 0
        var feedbackCallCount = 0
        var failUnauthorizedOnce = false
        var suggestions: AskSuggestions = AskSuggestions(
            questions = listOf(
                "What is Turing's main claim about machine intelligence?",
                "How does the imitation game define thinking?",
                "Which objections does Turing anticipate?",
            ),
            isDynamic = true,
        )
        var events: List<AskStreamEvent> = listOf(
            AskStreamEvent.Started("conv-1", "msg-1"),
            AskStreamEvent.Delta("Hello [1]."),
            AskStreamEvent.Completed(
                messageId = "msg-1",
                content = "Hello [1].",
                citations = listOf(
                    AskCitation(
                        index = 1,
                        sourceId = "src-1",
                        sourceTitle = "Paper",
                        sourceType = SourceType.FILE,
                    ),
                ),
            ),
        )

        override suspend fun getSuggestions(
            accessToken: String,
            spaceId: String,
            sourceId: String?,
        ): AskSuggestions {
            suggestionsCallCount++
            lastAccessToken = accessToken
            lastSuggestionsSpaceId = spaceId
            lastSuggestionsSourceId = sourceId
            if (failUnauthorizedOnce && accessToken == "expired-token") {
                failUnauthorizedOnce = false
                throw UnauthorizedException("Get ask suggestions failed (HTTP 401)")
            }
            return suggestions
        }

        override fun streamAnswer(
            accessToken: String,
            spaceId: String,
            question: String,
            sourceId: String?,
            conversationId: String?,
        ): Flow<AskStreamEvent> {
            streamCallCount++
            lastAccessToken = accessToken
            lastSpaceId = spaceId
            lastQuestion = question
            lastSourceId = sourceId
            lastConversationId = conversationId
            if (failUnauthorizedOnce && accessToken == "expired-token") {
                failUnauthorizedOnce = false
                throw UnauthorizedException("Ask failed (HTTP 401)")
            }
            return flow {
                for (event in events) emit(event)
            }
        }

        override suspend fun submitFeedback(
            accessToken: String,
            spaceId: String,
            conversationId: String,
            messageId: String,
            rating: String,
        ) {
            feedbackCallCount++
            lastAccessToken = accessToken
            lastFeedbackSpaceId = spaceId
            lastFeedbackConversationId = conversationId
            lastFeedbackMessageId = messageId
            lastFeedbackRating = rating
            if (failUnauthorizedOnce && accessToken == "expired-token") {
                failUnauthorizedOnce = false
                throw UnauthorizedException("Submit ask feedback failed (HTTP 401)")
            }
        }
    }

    @Test
    fun `fetchSuggestedQuestions forwards space and source to API`() = runTest {
        val api = FakeAskApi()
        val dataSource = AskDataSource(
            accessTokenProvider = { "access-token" },
            askApi = api,
        )

        val result = dataSource.fetchSuggestedQuestions(
            spaceId = "space-1",
            sourceId = "src-9",
        )

        assertEquals(1, api.suggestionsCallCount)
        assertEquals("access-token", api.lastAccessToken)
        assertEquals("space-1", api.lastSuggestionsSpaceId)
        assertEquals("src-9", api.lastSuggestionsSourceId)
        assertEquals(3, result.questions.size)
        assertTrue(result.isDynamic)
    }

    @Test
    fun `fetchSuggestedQuestions omits sourceId for space scope`() = runTest {
        val api = FakeAskApi().apply {
            suggestions = AskSuggestions(
                questions = listOf(
                    "Summarize all the evidence.",
                    "What problems appear most often?",
                    "Where do the sources disagree?",
                ),
                isDynamic = false,
            )
        }
        val dataSource = AskDataSource(
            accessTokenProvider = { "access-token" },
            askApi = api,
        )

        val result = dataSource.fetchSuggestedQuestions(spaceId = "space-1", sourceId = null)

        assertEquals(null, api.lastSuggestionsSourceId)
        assertEquals(false, result.isDynamic)
        assertEquals(3, result.questions.size)
    }

    @Test
    fun `fetchSuggestedQuestions retries once after 401`() = runTest {
        val api = FakeAskApi().apply { failUnauthorizedOnce = true }
        var token = "expired-token"
        val dataSource = AskDataSource(
            accessTokenProvider = { token },
            refreshAccessToken = {
                token = "fresh-token"
                token
            },
            askApi = api,
        )

        val result = dataSource.fetchSuggestedQuestions(spaceId = "space-1", sourceId = "1")

        assertEquals(2, api.suggestionsCallCount)
        assertEquals("fresh-token", api.lastAccessToken)
        assertTrue(result.isDynamic)
    }

    @Test
    fun `streamAnswer forwards params to API`() = runTest {
        val api = FakeAskApi()
        val dataSource = AskDataSource(
            accessTokenProvider = { "access-token" },
            askApi = api,
        )

        val events = dataSource.streamAnswer(
            spaceId = "space-1",
            question = "What problems appear most often?",
            sourceId = "src-9",
            conversationId = "conv-1",
        ).toList()

        assertEquals(1, api.streamCallCount)
        assertEquals("access-token", api.lastAccessToken)
        assertEquals("space-1", api.lastSpaceId)
        assertEquals("What problems appear most often?", api.lastQuestion)
        assertEquals("src-9", api.lastSourceId)
        assertEquals("conv-1", api.lastConversationId)
        assertEquals(3, events.size)
        assertTrue(events.last() is AskStreamEvent.Completed)
    }

    @Test
    fun `streamAnswer retries once after 401`() = runTest {
        val api = FakeAskApi().apply { failUnauthorizedOnce = true }
        var token = "expired-token"
        val dataSource = AskDataSource(
            accessTokenProvider = { token },
            refreshAccessToken = {
                token = "fresh-token"
                token
            },
            askApi = api,
        )

        val events = dataSource.streamAnswer(
            spaceId = "space-1",
            question = "Question",
            sourceId = null,
        ).toList()

        assertEquals(2, api.streamCallCount)
        assertEquals("fresh-token", api.lastAccessToken)
        assertTrue(events.last() is AskStreamEvent.Completed)
    }

    @Test
    fun `streamAnswer retries with newer provider token without refresh`() = runTest {
        val api = FakeAskApi().apply { failUnauthorizedOnce = true }
        var token = "expired-token"
        var refreshCallCount = 0
        val dataSource = AskDataSource(
            accessTokenProvider = {
                token.also {
                    if (it == "expired-token") token = "fresh-token"
                }
            },
            refreshAccessToken = {
                refreshCallCount++
                "refreshed-token"
            },
            askApi = api,
        )

        val events = dataSource.streamAnswer(
            spaceId = "space-1",
            question = "Question",
            sourceId = null,
        ).toList()

        assertEquals(0, refreshCallCount)
        assertEquals(2, api.streamCallCount)
        assertEquals("fresh-token", api.lastAccessToken)
        assertTrue(events.last() is AskStreamEvent.Completed)
    }

    @Test(expected = IllegalStateException::class)
    fun `streamAnswer requires authentication`() = runTest {
        val dataSource = AskDataSource(
            accessTokenProvider = { null },
            askApi = FakeAskApi(),
        )
        dataSource.streamAnswer(
            spaceId = "space-1",
            question = "Question",
            sourceId = null,
        ).toList()
    }

    @Test
    fun `submitFeedback forwards useful rating to API`() = runTest {
        val api = FakeAskApi()
        val dataSource = AskDataSource(
            accessTokenProvider = { "access-token" },
            askApi = api,
        )

        dataSource.submitFeedback(
            spaceId = "space-1",
            conversationId = "conv-1",
            messageId = "msg-1",
            rating = AskFeedbackRating.USEFUL,
        )

        assertEquals(1, api.feedbackCallCount)
        assertEquals("access-token", api.lastAccessToken)
        assertEquals("space-1", api.lastFeedbackSpaceId)
        assertEquals("conv-1", api.lastFeedbackConversationId)
        assertEquals("msg-1", api.lastFeedbackMessageId)
        assertEquals("useful", api.lastFeedbackRating)
    }

    @Test
    fun `submitFeedback retries once after 401`() = runTest {
        val api = FakeAskApi().apply { failUnauthorizedOnce = true }
        var token = "expired-token"
        val dataSource = AskDataSource(
            accessTokenProvider = { token },
            refreshAccessToken = {
                token = "fresh-token"
                token
            },
            askApi = api,
        )

        dataSource.submitFeedback(
            spaceId = "space-1",
            conversationId = "conv-1",
            messageId = "msg-1",
            rating = AskFeedbackRating.NOT_USEFUL,
        )

        assertEquals(2, api.feedbackCallCount)
        assertEquals("fresh-token", api.lastAccessToken)
        assertEquals("not_useful", api.lastFeedbackRating)
    }
}
