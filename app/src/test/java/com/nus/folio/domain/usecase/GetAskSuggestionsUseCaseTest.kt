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
        val result = useCase(spaceId = "space-1", sourceId = "1")

        assertTrue(result.isSuccess)
        val suggestions = result.getOrThrow()
        assertEquals(3, suggestions.questions.size)
        assertTrue(suggestions.isDynamic)
        assertEquals(1, repository.getSuggestedQuestionsCallCount)
        assertEquals("space-1", repository.lastSuggestedSpaceId)
        assertEquals("1", repository.lastSuggestedSourceId)
    }

    @Test
    fun `invoke returns empty when metadata unavailable`() = runTest {
        val result = useCase(spaceId = "space-1", sourceId = "10")

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().questions.isEmpty())
        assertEquals(false, result.getOrThrow().isDynamic)
    }

    @Test
    fun `invoke forwards space scope when sourceId is null`() = runTest {
        val result = useCase(spaceId = "space-1", sourceId = null)

        assertTrue(result.isSuccess)
        assertEquals(null, repository.lastSuggestedSourceId)
        assertEquals("space-1", repository.lastSuggestedSpaceId)
        assertEquals(false, result.getOrThrow().isDynamic)
    }

    @Test
    fun `invoke forwards repository failure`() = runTest {
        repository.getSuggestedQuestionsResult = Result.failure(IllegalStateException("offline"))

        val result = useCase(spaceId = "space-1", sourceId = "1")

        assertTrue(result.isFailure)
    }
}
