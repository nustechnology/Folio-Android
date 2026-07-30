package com.nus.folio.domain.usecase

import com.nus.folio.testing.FakeNoteRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetNotesUseCaseTest {

    private val repository = FakeNoteRepository()
    private val useCase = GetNotesUseCase(repository)

    @Test
    fun `invoke returns space-scoped library on success`() = runTest {
        val result = useCase("1")

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.allCount)
        assertEquals(2, result.getOrNull()?.notes?.size)
        assertEquals(1, repository.getNotesCallCount)
        assertEquals("1", repository.lastSpaceId)
    }

    @Test
    fun `invoke returns failure when repository fails`() = runTest {
        repository.getNotesResult = Result.failure(IllegalStateException("offline"))

        val result = useCase("1")

        assertTrue(result.isFailure)
        assertEquals("offline", result.exceptionOrNull()?.message)
    }
}
