package com.nus.folio.testing

import com.nus.folio.domain.model.CreateNoteRequest
import com.nus.folio.domain.model.Note
import com.nus.folio.domain.model.NoteLibrary
import com.nus.folio.domain.model.NoteOrigin
import com.nus.folio.domain.model.NotePaging
import com.nus.folio.domain.model.NoteSort
import com.nus.folio.domain.repository.NoteRepository
import com.nus.folio.domain.util.NoteUpdatedLabelFormatter

class FakeNoteRepository : NoteRepository {
    private data class NoteTimeline(
        val createdAtMs: Long,
        val updatedAtMs: Long,
    )

    var getNotesResult: Result<NoteLibrary>? = null
    var createNoteResult: Result<Note>? = null
    var updateNoteResult: Result<Note>? = null
    var deleteNoteResult: Result<Unit>? = null
    var getNotesCallCount = 0
    var getNoteCallCount = 0
    var createNoteCallCount = 0
    var updateNoteCallCount = 0
    var deleteNoteCallCount = 0
    var lastSpaceId: String? = null
    var lastNoteId: String? = null
    var lastSearch: String? = null
    var lastSort: NoteSort? = null
    var lastPage: Int? = null
    var lastLimit: Int? = null
    var lastCreatedRequest: CreateNoteRequest? = null
    var lastUpdatedNote: Note? = null
    var lastDeletedNoteId: String? = null
    var getNoteResult: Result<Note>? = null

    private val notes: MutableList<Note> = sampleNotes.toMutableList()
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

    override suspend fun getNotes(
        spaceId: String,
        search: String?,
        sort: NoteSort,
        page: Int,
        limit: Int,
    ): Result<NoteLibrary> {
        getNotesCallCount++
        lastSpaceId = spaceId
        lastSearch = search
        lastSort = sort
        lastPage = page
        lastLimit = limit
        getNotesResult?.let { return it }
        return Result.success(libraryFor(spaceId, search, sort, page, limit))
    }

    override suspend fun getNote(spaceId: String, noteId: String): Result<Note> {
        getNoteCallCount++
        lastSpaceId = spaceId
        lastNoteId = noteId
        getNoteResult?.let { return it }
        val note = notes.firstOrNull { it.id == noteId && it.spaceId == spaceId }
            ?: return Result.failure(NoSuchElementException("Note not found: $noteId"))
        return Result.success(note)
    }

    override suspend fun createNote(request: CreateNoteRequest): Result<Note> {
        createNoteCallCount++
        lastCreatedRequest = request
        createNoteResult?.let { return it }
        val now = System.currentTimeMillis()
        val note = Note(
            id = "created-${createNoteCallCount}",
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
        return Result.success(note)
    }

    override suspend fun updateNote(note: Note): Result<Note> {
        updateNoteCallCount++
        lastUpdatedNote = note
        updateNoteResult?.let { return it }
        val index = notes.indexOfFirst { it.id == note.id }
        if (index < 0) {
            return Result.failure(NoSuchElementException("Note not found: ${note.id}"))
        }
        val previousTimeline = timelineFor(note.id)
        val updated = note.copy(updatedLabel = NoteUpdatedLabelFormatter.formatNow())
        notes[index] = updated
        noteTimelines[note.id] = previousTimeline.copy(updatedAtMs = System.currentTimeMillis())
        return Result.success(updated)
    }

    override suspend fun deleteNote(noteId: String): Result<Unit> {
        deleteNoteCallCount++
        lastDeletedNoteId = noteId
        deleteNoteResult?.let { return it }
        val removed = notes.removeAll { it.id == noteId }
        if (!removed) {
            return Result.failure(NoSuchElementException("Note not found: $noteId"))
        }
        noteTimelines.remove(noteId)
        return Result.success(Unit)
    }

    private fun libraryFor(
        spaceId: String,
        search: String?,
        sort: NoteSort,
        page: Int,
        limit: Int,
    ): NoteLibrary {
        var scoped = notes.filter { it.spaceId == spaceId }
        val query = search?.trim().orEmpty()
        if (query.isNotEmpty()) {
            scoped = scoped.filter { note ->
                note.title.contains(query, ignoreCase = true) ||
                    note.content.contains(query, ignoreCase = true) ||
                    note.project.orEmpty().contains(query, ignoreCase = true)
            }
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
        return NoteLibrary(
            notes = pageItems,
            allCount = scoped.size,
            pinnedCount = scoped.count { it.isPinned },
            unfiledCount = scoped.count { it.project.isNullOrBlank() },
            page = safePage,
            limit = safeLimit,
            hasMore = offset + pageItems.size < scoped.size,
        )
    }

    companion object {
        val sampleNotes = listOf(
            Note(
                id = "1",
                title = "Research Question Draft",
                content = "How do informal transit networks reshape access in mid-sized cities?",
                project = "Urban Mobility",
                updatedLabel = "Updated 1d ago",
                isPinned = true,
                spaceId = "2",
                origin = NoteOrigin.USER_CREATED,
            ),
            Note(
                id = "2",
                title = "Literature Review Outline",
                content = "Map debates on machine intelligence, imitation games, and measurement.",
                project = "Dissertation Research",
                updatedLabel = "Updated 2d ago",
                isPinned = true,
                spaceId = "1",
                origin = NoteOrigin.USER_CREATED,
            ),
            Note(
                id = "3",
                title = "Turing Test — Key Takeaways",
                content = "The imitation game reframes intelligence as observable linguistic behavior.",
                project = "Dissertation Research",
                updatedLabel = "Updated 3d ago",
                isPinned = false,
                spaceId = "1",
                origin = NoteOrigin.SAVED_ANSWER,
                citationCount = 4,
            ),
            Note(
                id = "4",
                title = "Policy Implications",
                content = "Zoning reform alone underestimates last-mile coordination costs.",
                project = "Urban Mobility",
                updatedLabel = "Updated 4d ago",
                isPinned = false,
                spaceId = "2",
                origin = NoteOrigin.SAVED_ANSWER,
                citationCount = 2,
            ),
            Note(
                id = "5",
                title = "Teaching Prep — Week 7",
                content = "Seminar prompts on archival silence and source criticism.",
                project = null,
                updatedLabel = "Updated 5d ago",
                isPinned = false,
                spaceId = "4",
                origin = NoteOrigin.USER_CREATED,
            ),
            Note(
                id = "6",
                title = "Archival methods memo",
                content = "Prioritize provenance notes before transcription decisions.",
                project = "History of Science",
                updatedLabel = "Updated 6d ago",
                isPinned = true,
                spaceId = "3",
                origin = NoteOrigin.SAVED_ANSWER,
                citationCount = 6,
            ),
        )

        val sampleLibrary = NoteLibrary(
            notes = sampleNotes,
            allCount = sampleNotes.size,
            pinnedCount = sampleNotes.count { it.isPinned },
            unfiledCount = sampleNotes.count { it.project.isNullOrBlank() },
        )
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
