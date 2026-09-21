package com.nus.folio.data.repository

import com.nus.folio.data.datasource.NoteDataSource
import com.nus.folio.data.network.NotesApi
import com.nus.folio.domain.model.Note
import com.nus.folio.domain.model.NoteLibrary
import com.nus.folio.domain.model.NoteOrigin
import com.nus.folio.domain.model.NotePaging
import com.nus.folio.domain.model.NoteSort
import com.nus.folio.domain.model.Source
import com.nus.folio.domain.model.SourceStatus
import com.nus.folio.domain.model.SourceType
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.coroutines.cancellation.CancellationException

class NoteRepositoryImplTest {

    private class RecordingNotesApi : NotesApi {
        private val notes = mutableListOf(
            Note(
                id = "1",
                title = "Research Questions",
                content = "Body 1",
                project = null,
                updatedLabel = "Updated 1d ago",
                isPinned = false,
                spaceId = "1",
                origin = NoteOrigin.USER_CREATED,
            ),
            Note(
                id = "2",
                title = "Literature Review Outline",
                content = "Body 2",
                project = null,
                updatedLabel = "Updated 2d ago",
                isPinned = false,
                spaceId = "1",
                origin = NoteOrigin.USER_CREATED,
            ),
        )

        var lastSearch: String? = null
        var lastPage: Int? = null
        var lastLimit: Int? = null
        var throwOnList: Throwable? = null

        override suspend fun listNotes(
            accessToken: String,
            spaceId: String,
            sort: String,
            search: String?,
            origin: String?,
            page: Int,
            limit: Int,
        ): NoteLibrary {
            throwOnList?.let { throw it }
            lastSearch = search
            lastPage = page
            lastLimit = limit
            val filtered = notes.filter { note ->
                search.isNullOrBlank() || note.title.contains(search, ignoreCase = true)
            }
            val from = ((page - 1) * limit).coerceAtLeast(0)
            val pageNotes = filtered.drop(from).take(limit)
            return NoteLibrary(
                notes = pageNotes,
                allCount = filtered.size,
                userCreatedCount = filtered.count { it.origin == NoteOrigin.USER_CREATED },
                savedAnswerCount = filtered.count { it.origin == NoteOrigin.SAVED_ANSWER },
                page = page,
                limit = limit,
                hasMore = from + pageNotes.size < filtered.size,
            )
        }

        override suspend fun getNote(
            accessToken: String,
            spaceId: String,
            noteId: String,
        ): Note = notes.first { it.id == noteId }

        override suspend fun createNote(
            accessToken: String,
            spaceId: String,
            title: String,
            content: String,
            conversationId: String?,
            messageId: String?,
        ): Note = error("unused")

        override suspend fun updateNote(
            accessToken: String,
            spaceId: String,
            noteId: String,
            title: String,
            content: String,
        ): Note {
            val index = notes.indexOfFirst { it.id == noteId }
            val updated = notes[index].copy(
                title = title,
                content = content,
                updatedLabel = "Updated just now",
            )
            notes[index] = updated
            return updated
        }

        override suspend fun deleteNote(
            accessToken: String,
            spaceId: String,
            noteId: String,
        ) {
            notes.removeAll { it.id == noteId }
        }

        override suspend fun convertNoteToSource(
            accessToken: String,
            spaceId: String,
            noteId: String,
            title: String,
        ): Source = Source(
            id = "src-$noteId",
            title = title,
            type = SourceType.TEXT,
            author = "",
            addedLabel = "Added just now",
            status = SourceStatus.READY,
            spaceId = spaceId,
        )
    }

    private val api = RecordingNotesApi()
    private val repository = NoteRepositoryImpl(
        NoteDataSource(
            notesApi = api,
            accessTokenProvider = { "token" },
        ),
    )

    @Test
    fun `getNotes returns success library for space`() = runTest {
        val result = repository.getNotes("1")

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.allCount)
        assertEquals(2, result.getOrNull()?.notes?.size)
        assertEquals(2, result.getOrNull()?.userCreatedCount)
        assertEquals(false, result.getOrNull()?.hasMore)
        assertEquals(NotePaging.DEFAULT_PAGE, api.lastPage)
        assertEquals(NotePaging.DEFAULT_LIMIT, api.lastLimit)
    }

    @Test
    fun `getNotes pages and searches`() = runTest {
        val page1 = repository.getNotes(spaceId = "1", page = 1, limit = 1).getOrThrow()
        val page2 = repository.getNotes(spaceId = "1", page = 2, limit = 1).getOrThrow()
        val search = repository.getNotes(spaceId = "1", search = "Literature").getOrThrow()

        assertEquals(1, page1.notes.size)
        assertTrue(page1.hasMore)
        assertEquals(1, page2.notes.size)
        assertTrue(!page2.hasMore)
        assertEquals(1, search.notes.size)
        assertEquals("Literature Review Outline", search.notes.first().title)
        assertEquals("Literature", api.lastSearch)
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
        assertEquals("Updated title", saved.title)
        assertEquals("Updated content", saved.content)
        assertEquals(
            saved.title,
            repository.getNotes("1").getOrNull()!!.notes.first { it.id == "2" }.title,
        )
    }

    @Test
    fun `deleteNote returns success and persists`() = runTest {
        val result = repository.deleteNote(spaceId = "1", noteId = "2")

        assertTrue(result.isSuccess)
        assertEquals(1, repository.getNotes("1").getOrNull()?.allCount)
        assertTrue(repository.getNotes("1").getOrNull()!!.notes.none { it.id == "2" })
    }

    @Test
    fun `getNotes rethrows CancellationException`() = runTest {
        api.throwOnList = CancellationException("cancelled")
        try {
            repository.getNotes("1")
            fail("expected CancellationException")
        } catch (_: CancellationException) {
            // expected
        }
    }

    @Test
    fun `getNotes wraps other exceptions in failure`() = runTest {
        api.throwOnList = IOException("network")
        val result = repository.getNotes("1")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
    }

    @Test
    fun `getNotes uses default sort when omitted`() = runTest {
        val result = repository.getNotes(spaceId = "1", sort = NoteSort.DEFAULT)

        assertTrue(result.isSuccess)
    }
}
