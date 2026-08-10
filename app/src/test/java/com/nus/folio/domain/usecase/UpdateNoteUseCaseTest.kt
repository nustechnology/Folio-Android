package com.nus.folio.domain.usecase

import com.nus.folio.testing.FakeNoteRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateNoteUseCaseTest {

    private val repository = FakeNoteRepository()
    private val useCase = UpdateNoteUseCase(repository)

    @Test
    fun `invoke updates note on success`() = runTest {
        val original = FakeNoteRepository.sampleNotes.first { it.id == "2" }
        val updated = original.copy(title = "Updated title", content = "Updated content")

        val result = useCase(updated)

        assertTrue(result.isSuccess)
        val saved = result.getOrNull()!!
        // FakeNoteRepository refreshes updatedLabel on persist, matching release datasource.
        assertEquals(updated.copy(updatedLabel = saved.updatedLabel), saved)
        assertNotEquals(original.updatedLabel, saved.updatedLabel)
        assertEquals(1, repository.updateNoteCallCount)
        assertEquals(updated, repository.lastUpdatedNote)
    }

    @Test
    fun `invoke returns failure when repository fails`() = runTest {
        repository.updateNoteResult = Result.failure(IllegalStateException("offline"))
        val note = FakeNoteRepository.sampleNotes.first().copy(title = "Nope")

        val result = useCase(note)

        assertTrue(result.isFailure)
        assertEquals("offline", result.exceptionOrNull()?.message)
    }
}
