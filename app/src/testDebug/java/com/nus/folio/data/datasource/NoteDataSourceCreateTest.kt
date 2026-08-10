package com.nus.folio.data.datasource

import com.nus.folio.data.network.NotesApi
import com.nus.folio.data.network.UnauthorizedException
import com.nus.folio.domain.model.CreateNoteRequest
import com.nus.folio.domain.model.Note
import com.nus.folio.domain.model.NoteLibrary
import com.nus.folio.domain.model.NoteOrigin
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteDataSourceCreateTest {

    private class FakeNotesApi : NotesApi {
        var lastAccessToken: String? = null
        var lastSpaceId: String? = null
        var lastTitle: String? = null
        var lastContent: String? = null
        var lastSort: String? = null
        var lastSearch: String? = null
        var lastPage: Int? = null
        var lastLimit: Int? = null
        var lastNoteId: String? = null
        var createCallCount = 0
        var listCallCount = 0
        var getCallCount = 0
        var updateCallCount = 0
        var deleteCallCount = 0
        var failUnauthorizedOnce = false
        var onUnauthorized: (() -> Unit)? = null
        var remoteNote: Note = Note(
            id = "note-remote-1",
            title = "Remote title",
            content = "<p>Remote content</p>",
            project = null,
            updatedLabel = "Aug 6, 06:17",
            isPinned = false,
            spaceId = "space-1",
            origin = NoteOrigin.USER_CREATED,
            citationCount = 0,
        )
        var listLibrary: NoteLibrary = NoteLibrary(
            notes = listOf(remoteNote),
            allCount = 1,
            pinnedCount = 0,
            unfiledCount = 1,
            page = 1,
            limit = 10,
            hasMore = false,
        )

        override suspend fun listNotes(
            accessToken: String,
            spaceId: String,
            sort: String,
            search: String?,
            page: Int,
            limit: Int,
        ): NoteLibrary {
            listCallCount++
            lastAccessToken = accessToken
            lastSpaceId = spaceId
            lastSort = sort
            lastSearch = search
            lastPage = page
            lastLimit = limit
            if (failUnauthorizedOnce && accessToken == "expired-token") {
                failUnauthorizedOnce = false
                onUnauthorized?.invoke()
                throw UnauthorizedException("Get notes failed (HTTP 401)")
            }
            return listLibrary.copy(
                notes = listLibrary.notes.map { it.copy(spaceId = spaceId) },
                page = page,
                limit = limit,
            )
        }

        override suspend fun getNote(
            accessToken: String,
            spaceId: String,
            noteId: String,
        ): Note {
            lastAccessToken = accessToken
            lastSpaceId = spaceId
            lastNoteId = noteId
            getCallCount++
            if (failUnauthorizedOnce && accessToken == "expired-token") {
                failUnauthorizedOnce = false
                onUnauthorized?.invoke()
                throw UnauthorizedException("Get note failed (HTTP 401)")
            }
            return remoteNote.copy(id = noteId, spaceId = spaceId)
        }

        override suspend fun createNote(
            accessToken: String,
            spaceId: String,
            title: String,
            content: String,
        ): Note {
            createCallCount++
            lastAccessToken = accessToken
            lastSpaceId = spaceId
            lastTitle = title
            lastContent = content
            if (failUnauthorizedOnce && accessToken == "expired-token") {
                failUnauthorizedOnce = false
                onUnauthorized?.invoke()
                throw UnauthorizedException("Create note failed (HTTP 401)")
            }
            return remoteNote.copy(
                title = title,
                content = content,
                spaceId = spaceId,
            )
        }

        override suspend fun updateNote(
            accessToken: String,
            spaceId: String,
            noteId: String,
            title: String,
            content: String,
        ): Note {
            updateCallCount++
            lastAccessToken = accessToken
            lastSpaceId = spaceId
            lastNoteId = noteId
            lastTitle = title
            lastContent = content
            if (failUnauthorizedOnce && accessToken == "expired-token") {
                failUnauthorizedOnce = false
                onUnauthorized?.invoke()
                throw UnauthorizedException("Update note failed (HTTP 401)")
            }
            return remoteNote.copy(
                id = noteId,
                title = title,
                content = content,
                spaceId = spaceId,
                updatedLabel = "Aug 7, 13:00",
            )
        }

        override suspend fun deleteNote(
            accessToken: String,
            spaceId: String,
            noteId: String,
        ) {
            deleteCallCount++
            lastAccessToken = accessToken
            lastSpaceId = spaceId
            lastNoteId = noteId
            if (failUnauthorizedOnce && accessToken == "expired-token") {
                failUnauthorizedOnce = false
                onUnauthorized?.invoke()
                throw UnauthorizedException("Delete note failed (HTTP 401)")
            }
        }
    }

    @Test
    fun `fetchNotes calls API and returns library`() = runTest {
        val api = FakeNotesApi()
        val dataSource = NoteDataSource(
            notesApi = api,
            accessTokenProvider = { "access-token" },
        )

        val library = dataSource.fetchNotes(
            spaceId = "space-1",
            search = "scaling",
            sort = com.nus.folio.domain.model.NoteSort.ALPHABETICAL_AZ,
            page = 2,
            limit = 5,
        )

        assertEquals(1, api.listCallCount)
        assertEquals("access-token", api.lastAccessToken)
        assertEquals("space-1", api.lastSpaceId)
        assertEquals("scaling", api.lastSearch)
        assertEquals("alphabetical-az", api.lastSort)
        assertEquals(2, api.lastPage)
        assertEquals(5, api.lastLimit)
        assertEquals(1, library.notes.size)
        assertEquals("note-remote-1", library.notes.first().id)
    }

    @Test
    fun `fetchNote calls API and upserts locally`() = runTest {
        val api = FakeNotesApi()
        val dataSource = NoteDataSource(
            notesApi = api,
            accessTokenProvider = { "access-token" },
        )

        val detail = dataSource.fetchNote(spaceId = "space-1", noteId = "note-remote-1")

        assertEquals(1, api.getCallCount)
        assertEquals("access-token", api.lastAccessToken)
        assertEquals("space-1", api.lastSpaceId)
        assertEquals("note-remote-1", api.lastNoteId)
        assertEquals("note-remote-1", detail.id)
        assertEquals("<p>Remote content</p>", detail.content)
    }

    @Test
    fun `fetchNote preserves cached saved-answer origin and citationCount`() = runTest {
        val api = FakeNotesApi()
        val dataSource = NoteDataSource(
            notesApi = api,
            accessTokenProvider = { "access-token" },
        )
        dataSource.createNote(
            CreateNoteRequest(
                spaceId = "space-1",
                title = "Saved answer",
                content = "Body",
                origin = NoteOrigin.SAVED_ANSWER,
                citationCount = 4,
            ),
        )
        // Detail payload defaults to USER_CREATED / 0 when originType is omitted.
        api.remoteNote = api.remoteNote.copy(
            origin = NoteOrigin.USER_CREATED,
            citationCount = 0,
        )

        val detail = dataSource.fetchNote(spaceId = "space-1", noteId = "note-remote-1")

        assertEquals(NoteOrigin.SAVED_ANSWER, detail.origin)
        assertEquals(4, detail.citationCount)
    }

    @Test
    fun `createNote calls API and upserts locally`() = runTest {
        val api = FakeNotesApi()
        val dataSource = NoteDataSource(
            notesApi = api,
            accessTokenProvider = { "access-token" },
        )

        val created = dataSource.createNote(
            CreateNoteRequest(
                spaceId = "space-1",
                title = "Key findings",
                content = "<p>Scaling laws</p>",
                project = "Dissertation Research",
            ),
        )

        assertEquals(1, api.createCallCount)
        assertEquals("access-token", api.lastAccessToken)
        assertEquals("space-1", api.lastSpaceId)
        assertEquals("Key findings", api.lastTitle)
        assertEquals("<p>Scaling laws</p>", api.lastContent)
        assertEquals("note-remote-1", created.id)
        assertEquals("Dissertation Research", created.project)
        assertEquals(NoteOrigin.USER_CREATED, created.origin)

        val library = dataSource.fetchNotes("space-1")
        assertTrue(library.notes.any { it.id == "note-remote-1" })
        assertEquals(1, api.listCallCount)
    }

    @Test
    fun `createNote preserves saved-answer origin and citations`() = runTest {
        val api = FakeNotesApi()
        val dataSource = NoteDataSource(
            notesApi = api,
            accessTokenProvider = { "access-token" },
        )

        val created = dataSource.createNote(
            CreateNoteRequest(
                spaceId = "space-1",
                title = "Saved answer",
                content = "Body",
                origin = NoteOrigin.SAVED_ANSWER,
                citationCount = 3,
            ),
        )

        assertEquals(NoteOrigin.SAVED_ANSWER, created.origin)
        assertEquals(3, created.citationCount)
    }

    @Test
    fun `createNote retries after unauthorized`() = runTest {
        val api = FakeNotesApi().apply { failUnauthorizedOnce = true }
        var token = "expired-token"
        val dataSource = NoteDataSource(
            notesApi = api,
            accessTokenProvider = { token },
            refreshAccessToken = {
                token = "fresh-token"
                token
            },
        )

        val created = dataSource.createNote(
            CreateNoteRequest(
                spaceId = "space-1",
                title = "Retry note",
                content = "Body",
            ),
        )

        assertEquals(2, api.createCallCount)
        assertEquals("fresh-token", api.lastAccessToken)
        assertEquals("note-remote-1", created.id)
    }

    @Test
    fun `createNote reuses already refreshed token without calling refresh`() = runTest {
        var token = "expired-token"
        var refreshCallCount = 0
        val api = FakeNotesApi().apply {
            failUnauthorizedOnce = true
            // Concurrent refresh lands a new access token before this caller retries.
            onUnauthorized = { token = "fresh-token" }
        }
        val dataSource = NoteDataSource(
            notesApi = api,
            accessTokenProvider = { token },
            refreshAccessToken = {
                refreshCallCount++
                token = "from-refresh"
                token
            },
        )

        val created = dataSource.createNote(
            CreateNoteRequest(
                spaceId = "space-1",
                title = "Reuse token",
                content = "Body",
            ),
        )

        assertEquals(0, refreshCallCount)
        assertEquals(2, api.createCallCount)
        assertEquals("fresh-token", api.lastAccessToken)
        assertEquals("note-remote-1", created.id)
    }

    @Test(expected = UnauthorizedException::class)
    fun `createNote rethrows when refresh unavailable`() = runTest {
        val api = FakeNotesApi().apply { failUnauthorizedOnce = true }
        val dataSource = NoteDataSource(
            notesApi = api,
            accessTokenProvider = { "expired-token" },
            refreshAccessToken = { null },
        )

        dataSource.createNote(
            CreateNoteRequest(
                spaceId = "space-1",
                title = "Title",
                content = "Body",
            ),
        )
    }

    @Test(expected = IllegalStateException::class)
    fun `createNote requires authentication`() = runTest {
        val dataSource = NoteDataSource(
            notesApi = FakeNotesApi(),
            accessTokenProvider = { null },
        )

        dataSource.createNote(
            CreateNoteRequest(
                spaceId = "space-1",
                title = "Title",
                content = "Body",
            ),
        )
    }

    @Test
    fun `updateNote calls API and upserts locally`() = runTest {
        val api = FakeNotesApi()
        val dataSource = NoteDataSource(
            notesApi = api,
            accessTokenProvider = { "access-token" },
        )

        val updated = dataSource.updateNote(
            Note(
                id = "note-remote-1",
                title = "Edited title",
                content = "Edited body",
                project = "Dissertation Research",
                updatedLabel = "old",
                isPinned = true,
                spaceId = "space-1",
                origin = NoteOrigin.SAVED_ANSWER,
                citationCount = 2,
            ),
        )

        assertEquals(1, api.updateCallCount)
        assertEquals("access-token", api.lastAccessToken)
        assertEquals("space-1", api.lastSpaceId)
        assertEquals("note-remote-1", api.lastNoteId)
        assertEquals("Edited title", api.lastTitle)
        assertEquals("Edited body", api.lastContent)
        assertEquals("Edited title", updated.title)
        assertEquals("Edited body", updated.content)
        assertEquals("Dissertation Research", updated.project)
        assertEquals(NoteOrigin.SAVED_ANSWER, updated.origin)
        assertEquals(2, updated.citationCount)
        assertTrue(updated.isPinned)
    }

    @Test
    fun `updateNote retries after unauthorized`() = runTest {
        val api = FakeNotesApi().apply { failUnauthorizedOnce = true }
        var token = "expired-token"
        val dataSource = NoteDataSource(
            notesApi = api,
            accessTokenProvider = { token },
            refreshAccessToken = {
                token = "fresh-token"
                token
            },
        )

        val updated = dataSource.updateNote(
            Note(
                id = "note-remote-1",
                title = "Retry title",
                content = "Retry body",
                project = null,
                updatedLabel = "old",
                isPinned = false,
                spaceId = "space-1",
            ),
        )

        assertEquals(2, api.updateCallCount)
        assertEquals("fresh-token", api.lastAccessToken)
        assertEquals("Retry title", updated.title)
    }

    @Test
    fun `deleteNote calls API and removes local cache`() = runTest {
        val api = FakeNotesApi()
        val dataSource = NoteDataSource(
            notesApi = api,
            accessTokenProvider = { "access-token" },
        )
        // Seed cache via fetch so delete can resolve spaceId.
        dataSource.fetchNotes("space-1")

        dataSource.deleteNote("note-remote-1")

        assertEquals(1, api.deleteCallCount)
        assertEquals("access-token", api.lastAccessToken)
        assertEquals("space-1", api.lastSpaceId)
        assertEquals("note-remote-1", api.lastNoteId)
    }

    @Test
    fun `deleteNote retries after unauthorized`() = runTest {
        val api = FakeNotesApi()
        var token = "expired-token"
        val dataSource = NoteDataSource(
            notesApi = api,
            accessTokenProvider = { token },
            refreshAccessToken = {
                token = "fresh-token"
                token
            },
        )
        token = "access-token"
        dataSource.fetchNotes("space-1")
        token = "expired-token"
        api.failUnauthorizedOnce = true

        dataSource.deleteNote("note-remote-1")

        assertEquals(2, api.deleteCallCount)
        assertEquals("fresh-token", api.lastAccessToken)
    }

    @Test(expected = NoSuchElementException::class)
    fun `deleteNote throws when note is not cached`() = runTest {
        val dataSource = NoteDataSource(
            notesApi = FakeNotesApi(),
            accessTokenProvider = { "access-token" },
        )

        dataSource.deleteNote("missing-note")
    }
}
