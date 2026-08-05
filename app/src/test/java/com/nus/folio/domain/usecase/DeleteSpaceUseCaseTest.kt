package com.nus.folio.domain.usecase

import com.nus.folio.testing.FakeSpaceRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeleteSpaceUseCaseTest {

    private val repository = FakeSpaceRepository()
    private val useCase = DeleteSpaceUseCase(repository)

    @Test
    fun `invoke deletes space on success`() = runTest {
        val result = useCase("1")

        assertTrue(result.isSuccess)
        assertEquals(1, repository.deleteSpaceCallCount)
        assertEquals("1", repository.lastDeletedSpaceId)
    }

    @Test
    fun `invoke returns failure when repository fails`() = runTest {
        repository.deleteSpaceResult = Result.failure(IllegalStateException("offline"))

        val result = useCase("1")

        assertTrue(result.isFailure)
        assertEquals("offline", result.exceptionOrNull()?.message)
    }
}
