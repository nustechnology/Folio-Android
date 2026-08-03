package com.nus.folio.data.datasource

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteDataSourceTest {

    private val dataSource = NoteDataSource()

    @Test
    fun `fetchNotes returns space-scoped library`() = runTest {
        val library = dataSource.fetchNotes("1")

        assertEquals(2, library.allCount)
        assertEquals(2, library.notes.size)
        assertEquals(1, library.pinnedCount)
        assertEquals(0, library.unfiledCount)
        assertTrue(library.notes.all { it.spaceId == "1" })
        assertTrue(library.notes.any { it.title.contains("Literature") })
    }

    @Test
    fun `fetchNotes returns different content for another space`() = runTest {
        val library = dataSource.fetchNotes("4")

        assertEquals(1, library.notes.size)
        assertEquals(1, library.unfiledCount)
        assertTrue(library.notes.all { it.spaceId == "4" })
    }

    @Test
    fun `updateNote persists title and content for later fetch`() = runTest {
        val original = dataSource.fetchNotes("1").notes.first { it.id == "2" }

        val updated = dataSource.updateNote(
            original.copy(title = "Updated title", content = "Updated content"),
        )
        val library = dataSource.fetchNotes("1")

        assertEquals("Updated title", updated.title)
        assertEquals("Updated content", updated.content)
        val reloaded = library.notes.first { it.id == "2" }
        assertEquals("Updated title", reloaded.title)
        assertEquals("Updated content", reloaded.content)
    }

    @Test
    fun `deleteNote removes note from later fetch`() = runTest {
        dataSource.deleteNote("2")
        val library = dataSource.fetchNotes("1")

        assertEquals(1, library.allCount)
        assertTrue(library.notes.none { it.id == "2" })
    }
}
