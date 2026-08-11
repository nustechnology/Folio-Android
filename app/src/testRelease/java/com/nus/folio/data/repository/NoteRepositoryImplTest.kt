package com.nus.folio.data.repository

import com.nus.folio.data.datasource.NoteDataSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteRepositoryImplTest {

    private val repository = NoteRepositoryImpl(NoteDataSource())

    @Test
    fun `getNotes returns success library for space`() = runTest {
        val result = repository.getNotes("1")

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.allCount)
        assertEquals(2, result.getOrNull()?.notes?.size)
        assertEquals(1, result.getOrNull()?.userCreatedCount)
        assertEquals(false, result.getOrNull()?.hasMore)
    }

    @Test
    fun `getNotes pages and searches locally`() = runTest {
        val page1 = repository.getNotes(spaceId = "1", page = 1, limit = 1).getOrThrow()
        val page2 = repository.getNotes(spaceId = "1", page = 2, limit = 1).getOrThrow()
        val search = repository.getNotes(spaceId = "1", search = "Literature").getOrThrow()

        assertEquals(1, page1.notes.size)
        assertTrue(page1.hasMore)
        assertEquals(1, page2.notes.size)
        assertTrue(!page2.hasMore)
        assertEquals(1, search.notes.size)
        assertEquals("Literature Review Outline", search.notes.first().title)
    }

    @Test
    fun `getNote returns detail for space note`() = runTest {
        val result = repository.getNote(spaceId = "1", noteId = "2")

        assertTrue(result.isSuccess)
        assertEquals("Literature Review Outline", result.getOrThrow().title)
    }

    @Test
    fun `updateNote returns success and persists`() = runTest {
        val original = repository.getNotes("1").getOrNull()!!.notes.first { it.id == "2" }
        val updated = original.copy(title = "Updated title", content = "Updated content")

        val result = repository.updateNote(updated)

        assertTrue(result.isSuccess)
        val saved = result.getOrNull()!!
        // Release NoteDataSource refreshes updatedLabel on persist.
        assertEquals(updated.copy(updatedLabel = saved.updatedLabel), saved)
        assertNotEquals(original.updatedLabel, saved.updatedLabel)
        assertEquals(
            saved,
            repository.getNotes("1").getOrNull()!!.notes.first { it.id == "2" },
        )
    }

    @Test
    fun `deleteNote returns success and persists`() = runTest {
        val result = repository.deleteNote(spaceId = "1", noteId = "2")

        assertTrue(result.isSuccess)
        assertEquals(1, repository.getNotes("1").getOrNull()?.allCount)
        assertTrue(repository.getNotes("1").getOrNull()!!.notes.none { it.id == "2" })
    }
}
