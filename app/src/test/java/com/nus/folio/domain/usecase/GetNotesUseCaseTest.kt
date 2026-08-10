package com.nus.folio.domain.usecase

import com.nus.folio.domain.model.NoteSort
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
        assertEquals(NoteSort.DEFAULT, repository.lastSort)
        assertEquals(1, repository.lastPage)
    }

    @Test
    fun `invoke forwards search sort and paging`() = runTest {
        val result = useCase(
            spaceId = "1",
            search = "Literature",
            sort = NoteSort.ALPHABETICAL_AZ,
            page = 2,
            limit = 1,
        )

        assertTrue(result.isSuccess)
        assertEquals("Literature", repository.lastSearch)
        assertEquals(NoteSort.ALPHABETICAL_AZ, repository.lastSort)
        assertEquals(2, repository.lastPage)
        assertEquals(1, repository.lastLimit)
    }

    @Test
    fun `invoke returns failure when repository fails`() = runTest {
        repository.getNotesResult = Result.failure(IllegalStateException("offline"))

        val result = useCase("1")

        assertTrue(result.isFailure)
        assertEquals("offline", result.exceptionOrNull()?.message)
    }
}
