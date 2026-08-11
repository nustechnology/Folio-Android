package com.nus.folio.domain.usecase

import com.nus.folio.domain.model.SourceStatus
import com.nus.folio.domain.model.SourceType
import com.nus.folio.testing.FakeNoteRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConvertNoteToSourceUseCaseTest {

    private val repository = FakeNoteRepository()
    private val useCase = ConvertNoteToSourceUseCase(repository)

    @Test
    fun `invoke converts note via repository`() = runTest {
        val result = useCase(
            spaceId = "1",
            noteId = "2",
            title = "Transformer scaling — field notes",
        )

        assertTrue(result.isSuccess)
        assertEquals(1, repository.convertNoteToSourceCallCount)
        assertEquals("1", repository.lastConvertSpaceId)
        assertEquals("2", repository.lastConvertNoteId)
        assertEquals("Transformer scaling — field notes", repository.lastConvertTitle)
        val source = result.getOrThrow()
        assertEquals("Transformer scaling — field notes", source.title)
        assertEquals(SourceType.TEXT, source.type)
        assertEquals(SourceStatus.PROCESSING, source.status)
        assertEquals("1", source.spaceId)
    }

    @Test
    fun `invoke forwards repository failure`() = runTest {
        repository.convertNoteToSourceResult = Result.failure(IllegalStateException("offline"))

        val result = useCase(spaceId = "1", noteId = "2", title = "Title")

        assertTrue(result.isFailure)
        assertEquals("offline", result.exceptionOrNull()?.message)
    }
}
