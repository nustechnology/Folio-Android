package com.nus.folio.domain.usecase

import com.nus.folio.testing.FakeNoteRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeleteNoteUseCaseTest {

    private val repository = FakeNoteRepository()
    private val useCase = DeleteNoteUseCase(repository)

    @Test
    fun `invoke deletes note on success`() = runTest {
        val result = useCase("2")

        assertTrue(result.isSuccess)
        assertEquals(1, repository.deleteNoteCallCount)
        assertEquals("2", repository.lastDeletedNoteId)
    }

    @Test
    fun `invoke returns failure when repository fails`() = runTest {
        repository.deleteNoteResult = Result.failure(IllegalStateException("offline"))

        val result = useCase("2")

        assertTrue(result.isFailure)
        assertEquals("offline", result.exceptionOrNull()?.message)
    }
}
