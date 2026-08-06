package com.nus.folio.domain.usecase

import com.nus.folio.testing.FakeAskRepository
import com.nus.folio.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class GetAskSuggestionsUseCaseTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeAskRepository()
    private val useCase = GetAskSuggestionsUseCase(repository)

    @Test
    fun `invoke returns suggestions for source with metadata`() = runTest {
        val result = useCase("1")

        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrThrow().size)
        assertEquals(1, repository.getSuggestedQuestionsCallCount)
        assertEquals("1", repository.lastSuggestedSourceId)
    }

    @Test
    fun `invoke returns empty when metadata unavailable`() = runTest {
        val result = useCase("10")

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().isEmpty())
    }

    @Test
    fun `invoke forwards repository failure`() = runTest {
        repository.getSuggestedQuestionsResult = Result.failure(IllegalStateException("offline"))

        val result = useCase("1")

        assertTrue(result.isFailure)
    }
}
