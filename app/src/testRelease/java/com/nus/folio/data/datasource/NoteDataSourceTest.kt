package com.nus.folio.data.datasource

import com.nus.folio.domain.model.CreateNoteRequest
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
        assertEquals(1, library.userCreatedCount)
        assertEquals(1, library.savedAnswerCount)
        assertTrue(library.notes.all { it.spaceId == "1" })
        assertTrue(library.notes.any { it.title.contains("Literature") })
    }

    @Test
    fun `fetchNotes returns different content for another space`() = runTest {
        val library = dataSource.fetchNotes("4")

        assertEquals(1, library.notes.size)
        assertEquals(1, library.userCreatedCount)
        assertEquals(0, library.savedAnswerCount)
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
        dataSource.deleteNote(spaceId = "1", noteId = "2")
        val library = dataSource.fetchNotes("1")

        assertEquals(1, library.allCount)
        assertTrue(library.notes.none { it.id == "2" })
    }

    @Test
    fun `createNote adds note locally`() = runTest {
        val created = dataSource.createNote(
            CreateNoteRequest(
                spaceId = "1",
                title = "New note",
                content = "Body",
            ),
        )
        val library = dataSource.fetchNotes("1")

        assertEquals("New note", created.title)
        assertTrue(library.notes.any { it.id == created.id })
        assertEquals(3, library.allCount)
    }

    @Test
    fun `convertNoteToSource returns text source from note title`() = runTest {
        val source = dataSource.convertNoteToSource(
            spaceId = "1",
            noteId = "2",
            title = "Transformer scaling — field notes",
        )

        assertEquals("Transformer scaling — field notes", source.title)
        assertEquals(com.nus.folio.domain.model.SourceType.TEXT, source.type)
        assertEquals("1", source.spaceId)
        assertEquals(com.nus.folio.domain.model.SourceStatus.PROCESSING, source.status)
    }
}
