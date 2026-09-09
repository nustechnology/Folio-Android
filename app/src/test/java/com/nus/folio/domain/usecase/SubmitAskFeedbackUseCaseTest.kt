package com.nus.folio.domain.usecase

import com.nus.folio.domain.model.AskFeedbackRating
import com.nus.folio.testing.FakeAskRepository
import com.nus.folio.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SubmitAskFeedbackUseCaseTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeAskRepository()
    private val useCase = SubmitAskFeedbackUseCase(repository)

    @Test
    fun `invoke forwards useful rating to repository`() = runTest {
        val result = useCase(
            spaceId = "space-1",
            conversationId = "conv-1",
            messageId = "msg-1",
            rating = AskFeedbackRating.USEFUL,
        )

        assertTrue(result.isSuccess)
        assertEquals(1, repository.submitFeedbackCallCount)
        assertEquals("space-1", repository.lastFeedbackSpaceId)
        assertEquals("conv-1", repository.lastFeedbackConversationId)
        assertEquals("msg-1", repository.lastFeedbackMessageId)
        assertEquals(AskFeedbackRating.USEFUL, repository.lastFeedbackRating)
    }

    @Test
    fun `invoke forwards repository failure`() = runTest {
        repository.submitFeedbackResult = Result.failure(IllegalStateException("offline"))

        val result = useCase(
            spaceId = "space-1",
            conversationId = "conv-1",
            messageId = "msg-1",
            rating = AskFeedbackRating.NOT_USEFUL,
        )

        assertTrue(result.isFailure)
        assertEquals(AskFeedbackRating.NOT_USEFUL, repository.lastFeedbackRating)
    }
}
