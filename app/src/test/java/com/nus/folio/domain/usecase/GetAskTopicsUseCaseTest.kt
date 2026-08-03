package com.nus.folio.domain.usecase

import com.nus.folio.testing.FakeAskRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetAskTopicsUseCaseTest {

    private val repository = FakeAskRepository()
    private val useCase = GetAskTopicsUseCase(repository)

    @Test
    fun `invoke returns space-scoped topics on success`() = runTest {
        val result = useCase("1")

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.size)
        assertEquals("Core dissertation arguments", result.getOrNull()?.first()?.title)
        assertEquals(1, repository.getAskTopicsCallCount)
        assertEquals("1", repository.lastSpaceId)
    }

    @Test
    fun `invoke returns failure when repository fails`() = runTest {
        repository.getAskTopicsResult = Result.failure(IllegalStateException("offline"))

        val result = useCase("1")

        assertTrue(result.isFailure)
        assertEquals("offline", result.exceptionOrNull()?.message)
    }
}
