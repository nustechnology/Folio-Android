package com.nus.folio.data.repository

import com.nus.folio.data.datasource.AskDataSource
import com.nus.folio.domain.model.AskFeedbackRating
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AskRepositoryImplTest {

    private val repository = AskRepositoryImpl(AskDataSource())

    @Test
    fun `getSuggestedQuestions returns success list for source`() = runTest {
        val result = repository.getSuggestedQuestions(spaceId = "1", sourceId = "1")

        assertTrue(result.isSuccess)
        val suggestions = result.getOrNull()
        assertEquals(3, suggestions?.questions?.size)
        assertEquals(true, suggestions?.isDynamic)
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
    }
}
