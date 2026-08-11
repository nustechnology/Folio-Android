package com.nus.folio.data.datasource

import com.nus.folio.domain.model.CreateNoteRequest
import com.nus.folio.domain.model.Note
import com.nus.folio.domain.model.NoteLibrary
import com.nus.folio.domain.model.NoteOrigin
import com.nus.folio.domain.model.NotePaging
import com.nus.folio.domain.model.NoteSort
import com.nus.folio.domain.model.Source
import com.nus.folio.domain.model.SourceStatus
import com.nus.folio.domain.model.SourceType
import com.nus.folio.domain.util.NoteUpdatedLabelFormatter
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Release stub — notes list/create/detail APIs are not wired yet; uses local sample data.
 */
class NoteDataSource(
    @Suppress("UNUSED_PARAMETER")
    accessTokenProvider: () -> String? = { null },
    @Suppress("UNUSED_PARAMETER")
    refreshAccessToken: suspend () -> String? = { null },
) {

    private data class NoteTimeline(
        val createdAtMs: Long,
        val updatedAtMs: Long,
    )

    private val mutex = Mutex()
    private val notes: MutableList<Note> = NoteSampleData.mutableDefaultNotes()
    private val noteTimelines: MutableMap<String, NoteTimeline> = mutableMapOf<String, NoteTimeline>().apply {
        val seededNow = System.currentTimeMillis()
        notes.forEachIndexed { index, note ->
            val timestamp = seededNow - index * 60_000L
            this[note.id] = NoteTimeline(
                createdAtMs = timestamp,
                updatedAtMs = timestamp,
            )
        }
    }

    suspend fun fetchNotes(
        spaceId: String,
        search: String? = null,
        sort: NoteSort = NoteSort.DEFAULT,
        origin: String? = null,
        page: Int = NotePaging.DEFAULT_PAGE,
        limit: Int = NotePaging.DEFAULT_LIMIT,
    ): NoteLibrary {
        delay(200)
        return mutex.withLock {
            var scoped = notes.filter { it.spaceId == spaceId }
            val query = search?.trim().orEmpty()
            if (query.isNotEmpty()) {
                scoped = scoped.filter { note ->
                    note.title.contains(query, ignoreCase = true) ||
                        note.content.contains(query, ignoreCase = true) ||
                        note.project.orEmpty().contains(query, ignoreCase = true)
                }
            }
            scoped = when (origin?.trim()) {
                "UserCreated" -> scoped.filter { it.origin == NoteOrigin.USER_CREATED }
                "SavedAssistantAnswer" -> scoped.filter { it.origin == NoteOrigin.SAVED_ANSWER }
                else -> scoped
            }
            scoped = when (sort) {
                NoteSort.ALPHABETICAL_AZ -> scoped.sortedBy { it.title.lowercase() }
                NoteSort.ALPHABETICAL_ZA -> scoped.sortedByDescending { it.title.lowercase() }
                NoteSort.RECENTLY_CREATED -> scoped.sortedByDescending { timelineFor(it.id).createdAtMs }
                NoteSort.RECENTLY_UPDATED -> scoped.sortedByDescending { timelineFor(it.id).updatedAtMs }
            }
            val safePage = page.coerceAtLeast(1)
            val safeLimit = limit.coerceAtLeast(1)
            val offset = (safePage - 1) * safeLimit
            val pageItems = scoped.drop(offset).take(safeLimit)
            NoteLibrary(
                notes = pageItems,
                allCount = scoped.size,
                userCreatedCount = scoped.count { it.origin == NoteOrigin.USER_CREATED },
                savedAnswerCount = scoped.count { it.origin == NoteOrigin.SAVED_ANSWER },
                page = safePage,
                limit = safeLimit,
                hasMore = offset + pageItems.size < scoped.size,
            )
        }
    }

    suspend fun fetchNote(spaceId: String, noteId: String): Note {
        delay(200)
        return mutex.withLock {
            notes.firstOrNull { it.id == noteId && it.spaceId == spaceId }
                ?: throw NoSuchElementException("Note not found: $noteId")
        }
    }

    suspend fun createNote(request: CreateNoteRequest): Note {
        delay(200)
        return mutex.withLock {
            val now = System.currentTimeMillis()
            val note = Note(
                id = UUID.randomUUID().toString(),
                title = request.title,
                content = request.content,
                project = request.project,
                updatedLabel = NoteUpdatedLabelFormatter.formatNow(),
                isPinned = false,
                spaceId = request.spaceId,
                origin = request.origin,
                citationCount = request.citationCount,
                citations = request.citations,
            )
            notes.add(0, note)
            noteTimelines[note.id] = NoteTimeline(
                createdAtMs = now,
                updatedAtMs = now,
            )
            note
        }
    }

    suspend fun updateNote(note: Note): Note {
        delay(200)
        return mutex.withLock {
            val index = notes.indexOfFirst { it.id == note.id }
            if (index < 0) {
                throw NoSuchElementException("Note not found: ${note.id}")
            }
            val existing = notes[index]
            val previousTimeline = timelineFor(existing.id)
            val updated = note.copy(updatedLabel = NoteUpdatedLabelFormatter.formatNow())
            notes[index] = updated
            noteTimelines[updated.id] = previousTimeline.copy(updatedAtMs = System.currentTimeMillis())
            updated
        }
    }

    suspend fun deleteNote(spaceId: String, noteId: String) {
        delay(200)
        require(spaceId.isNotBlank()) { "Space id is required" }
        require(noteId.isNotBlank()) { "Note id is required" }
        mutex.withLock {
            val removed = notes.removeAll { it.id == noteId && it.spaceId == spaceId }
            if (!removed) {
                throw NoSuchElementException("Note not found: $noteId")
            }
            noteTimelines.remove(noteId)
        }
    }

    suspend fun convertNoteToSource(spaceId: String, noteId: String, title: String): Source {
        delay(200)
        require(spaceId.isNotBlank()) { "Space id is required" }
        require(noteId.isNotBlank()) { "Note id is required" }
        require(title.isNotBlank()) { "Title is required" }
        return mutex.withLock {
            notes.firstOrNull { it.id == noteId && it.spaceId == spaceId }
                ?: throw NoSuchElementException("Note not found: $noteId")
            Source(
                id = UUID.randomUUID().toString(),
                title = title.trim(),
                type = SourceType.TEXT,
                author = "",
                addedLabel = "Added just now",
                status = SourceStatus.PROCESSING,
                spaceId = spaceId.trim(),
            )
        }
    }

    private fun timelineFor(noteId: String): NoteTimeline {
        noteTimelines[noteId]?.let { return it }
        val now = System.currentTimeMillis()
        return NoteTimeline(
            createdAtMs = now,
            updatedAtMs = now,
        ).also { noteTimelines[noteId] = it }
    }
}
