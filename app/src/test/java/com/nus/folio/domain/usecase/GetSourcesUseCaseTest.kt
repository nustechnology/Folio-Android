package com.nus.folio.domain.usecase

import com.nus.folio.testing.FakeSourceRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetSourcesUseCaseTest {

    private val repository = FakeSourceRepository()
    private val useCase = GetSourcesUseCase(repository)

    @Test
    fun `invoke returns library on success`() = runTest {
        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals(128, result.getOrNull()?.allCount)
        assertEquals(7, result.getOrNull()?.sources?.size)
        assertEquals(1, repository.getSourcesCallCount)
    }

    @Test
    fun `invoke returns failure when repository fails`() = runTest {
        repository.getSourcesResult = Result.failure(IllegalStateException("offline"))

        val result = useCase()

        assertTrue(result.isFailure)
        assertEquals("offline", result.exceptionOrNull()?.message)
    }
}
