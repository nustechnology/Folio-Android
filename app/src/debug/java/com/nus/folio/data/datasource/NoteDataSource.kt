package com.nus.folio.data.datasource

import com.nus.folio.data.network.NotesApi
import com.nus.folio.data.network.NotesApiClient
import com.nus.folio.data.network.UnauthorizedException
import com.nus.folio.domain.model.CreateNoteRequest
import com.nus.folio.domain.model.Note
import com.nus.folio.domain.model.NoteLibrary
import com.nus.folio.domain.model.NoteOrigin
import com.nus.folio.domain.model.NotePaging
import com.nus.folio.domain.model.NoteSort
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Debug notes data source — list/create/detail/update/delete via the real API.
 * On HTTP 401, refreshes the access token once and retries.
 */
class NoteDataSource(
    private val notesApi: NotesApi = NotesApiClient(),
    private val accessTokenProvider: () -> String? = { null },
    private val refreshAccessToken: suspend () -> String? = { null },
) {

    private val mutex = Mutex()
    private val notes: MutableList<Note> = NoteSampleData.mutableDefaultNotes()

    suspend fun fetchNotes(
        spaceId: String,
        search: String? = null,
        sort: NoteSort = NoteSort.DEFAULT,
        page: Int = NotePaging.DEFAULT_PAGE,
        limit: Int = NotePaging.DEFAULT_LIMIT,
    ): NoteLibrary {
        require(spaceId.isNotBlank()) { "Space id is required" }
        val library = withAuthRetry { accessToken ->
            notesApi.listNotes(
                accessToken = accessToken,
                spaceId = spaceId.trim(),
                sort = sort.apiValue,
                search = search?.trim()?.takeIf { it.isNotEmpty() },
                page = page.coerceAtLeast(1),
                limit = limit.coerceAtLeast(1),
            )
        }
        mutex.withLock {
            for (note in library.notes) {
                upsertLocked(note, preferFront = false)
            }
        }
        return library
    }

    suspend fun fetchNote(spaceId: String, noteId: String): Note {
        require(spaceId.isNotBlank()) { "Space id is required" }
        require(noteId.isNotBlank()) { "Note id is required" }
        val detail = withAuthRetry { accessToken ->
            notesApi.getNote(
                accessToken = accessToken,
                spaceId = spaceId.trim(),
                noteId = noteId.trim(),
            )
        }
        return mutex.withLock {
            val cached = notes.firstOrNull { it.id == detail.id }
            val merged = detail.copy(
                project = detail.project ?: cached?.project,
                // Preserve client-only fields when detail payload omits originType / citationCount.
                origin = when {
                    detail.origin != NoteOrigin.USER_CREATED -> detail.origin
                    else -> cached?.origin ?: detail.origin
                },
                citationCount = detail.citationCount.takeIf { it > 0 }
                    ?: cached?.citationCount
                    ?: detail.citationCount,
                citations = detail.citations.ifEmpty { cached?.citations.orEmpty() },
            )
            upsertLocked(merged, preferFront = false)
            merged
        }
    }

    suspend fun createNote(request: CreateNoteRequest): Note {
        require(request.spaceId.isNotBlank()) { "Space id is required" }
        require(request.title.isNotBlank()) { "Title is required" }
        require(request.content.isNotBlank()) { "Content is required" }
        val trimmedTitle = request.title.trim()
        val trimmedContent = request.content.trim()
        val remote = withAuthRetry { accessToken ->
            notesApi.createNote(
                accessToken = accessToken,
                spaceId = request.spaceId.trim(),
                title = trimmedTitle,
                content = trimmedContent,
            )
        }
        val created = remote.copy(
            // Preserve client-only / Ask-save fields the create payload does not send.
            project = request.project,
            origin = request.origin,
            citationCount = request.citationCount.takeIf { it > 0 } ?: remote.citationCount,
            citations = request.citations,
        )
        mutex.withLock {
            upsertLocked(created, preferFront = true)
        }
        return created
    }

    suspend fun updateNote(note: Note): Note {
        require(note.id.isNotBlank()) { "Note id is required" }
        require(note.spaceId.isNotBlank()) { "Space id is required" }
        require(note.title.isNotBlank()) { "Title is required" }
        require(note.content.isNotBlank()) { "Content is required" }
        val trimmedTitle = note.title.trim()
        val trimmedContent = note.content.trim()
        val remote = withAuthRetry { accessToken ->
            notesApi.updateNote(
                accessToken = accessToken,
                spaceId = note.spaceId.trim(),
                noteId = note.id.trim(),
                title = trimmedTitle,
                content = trimmedContent,
            )
        }
        val updated = remote.copy(
            title = trimmedTitle,
            content = trimmedContent,
            // Preserve client-only fields the PATCH payload may omit.
            project = note.project ?: remote.project,
            origin = note.origin,
            citationCount = note.citationCount.takeIf { it > 0 } ?: remote.citationCount,
            citations = note.citations.ifEmpty { remote.citations },
            spaceId = note.spaceId.ifBlank { remote.spaceId },
            isPinned = note.isPinned,
        )
        mutex.withLock {
            upsertLocked(updated, preferFront = false)
        }
        return updated
    }

    suspend fun deleteNote(noteId: String) {
        require(noteId.isNotBlank()) { "Note id is required" }
        val trimmedId = noteId.trim()
        val spaceId = mutex.withLock {
            notes.firstOrNull { it.id == trimmedId }?.spaceId
        }?.takeIf { it.isNotBlank() }
            ?: throw NoSuchElementException("Note not found: $trimmedId")
        withAuthRetry { accessToken ->
            notesApi.deleteNote(
                accessToken = accessToken,
                spaceId = spaceId,
                noteId = trimmedId,
            )
        }
        mutex.withLock {
            notes.removeAll { it.id == trimmedId }
        }
    }

    private fun upsertLocked(note: Note, preferFront: Boolean = false) {
        val index = notes.indexOfFirst { it.id == note.id }
        if (index >= 0) {
            notes[index] = note
        } else if (preferFront) {
            notes.add(0, note)
        } else {
            notes.add(note)
        }
    }

    private suspend fun <T> withAuthRetry(block: suspend (accessToken: String) -> T): T {
        val accessToken = requireAccessToken()
        return try {
            block(accessToken)
        } catch (unauthorized: UnauthorizedException) {
            // Another concurrent 401 may have already refreshed the session.
            val latestToken = accessTokenProvider()?.takeIf { it.isNotBlank() }
            val tokenToRetry = if (latestToken != null && latestToken != accessToken) {
                latestToken
            } else {
                refreshAccessToken()?.takeIf { it.isNotBlank() } ?: throw unauthorized
            }
            block(tokenToRetry)
        }
    }

    private fun requireAccessToken(): String =
        accessTokenProvider()?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Not authenticated")
}
