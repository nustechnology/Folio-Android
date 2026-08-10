package com.nus.folio.domain.usecase

import com.nus.folio.testing.FakeNoteRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetNoteDetailUseCaseTest {

    private val repository = FakeNoteRepository()
    private val useCase = GetNoteDetailUseCase(repository)

    @Test
    fun `invoke returns note detail on success`() = runTest {
        val result = useCase(spaceId = "1", noteId = "2")

        assertTrue(result.isSuccess)
        assertEquals("2", result.getOrThrow().id)
        assertEquals("Literature Review Outline", result.getOrThrow().title)
        assertEquals(1, repository.getNoteCallCount)
        assertEquals("1", repository.lastSpaceId)
        assertEquals("2", repository.lastNoteId)
    }

    @Test
    fun `invoke forwards repository failure`() = runTest {
        repository.getNoteResult = Result.failure(IllegalStateException("offline"))

        val result = useCase(spaceId = "1", noteId = "2")

        assertTrue(result.isFailure)
        assertEquals("offline", result.exceptionOrNull()?.message)
    }
}
