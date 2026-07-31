package com.nus.folio.domain.usecase

import com.nus.folio.testing.FakeSourceRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeleteSourceUseCaseTest {

    private val repository = FakeSourceRepository()
    private val useCase = DeleteSourceUseCase(repository)

    @Test
    fun `invoke deletes source on success`() = runTest {
        val result = useCase("1")

        assertTrue(result.isSuccess)
        assertEquals(1, repository.deleteSourceCallCount)
        assertEquals("1", repository.lastDeletedSourceId)
    }

    @Test
    fun `invoke returns failure when repository fails`() = runTest {
        repository.deleteSourceResult = Result.failure(IllegalStateException("offline"))

        val result = useCase("1")

        assertTrue(result.isFailure)
        assertEquals("offline", result.exceptionOrNull()?.message)
    }
}
