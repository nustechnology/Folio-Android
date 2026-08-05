package com.nus.folio.domain.usecase

import com.nus.folio.testing.FakeSourceRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RetrySourceUseCaseTest {

    private val repository = FakeSourceRepository()
    private val useCase = RetrySourceUseCase(repository)

    @Test
    fun `invoke retries source by id`() = runTest {
        val result = useCase("1")

        assertTrue(result.isSuccess)
        assertEquals(1, repository.retrySourceCallCount)
        assertEquals("1", repository.lastRetriedSourceId)
    }

    @Test
    fun `invoke returns failure from repository`() = runTest {
        repository.retrySourceResult = Result.failure(IllegalStateException("offline"))

        val result = useCase("1")

        assertTrue(result.isFailure)
        assertEquals("offline", result.exceptionOrNull()?.message)
    }
}
