package com.nus.folio.testing

import com.nus.folio.domain.model.CreateNoteRequest
import com.nus.folio.domain.model.Note
import com.nus.folio.domain.model.NoteLibrary
import com.nus.folio.domain.model.NoteOrigin
import com.nus.folio.domain.repository.NoteRepository
import com.nus.folio.domain.util.NoteUpdatedLabelFormatter

class FakeNoteRepository : NoteRepository {

    var getNotesResult: Result<NoteLibrary>? = null
    var createNoteResult: Result<Note>? = null
    var updateNoteResult: Result<Note>? = null
    var deleteNoteResult: Result<Unit>? = null
    var getNotesCallCount = 0
    var createNoteCallCount = 0
    var updateNoteCallCount = 0
    var deleteNoteCallCount = 0
    var lastSpaceId: String? = null
    var lastCreatedRequest: CreateNoteRequest? = null
    var lastUpdatedNote: Note? = null
    var lastDeletedNoteId: String? = null

    private val notes: MutableList<Note> = sampleNotes.toMutableList()

    override suspend fun getNotes(spaceId: String): Result<NoteLibrary> {
        getNotesCallCount++
        lastSpaceId = spaceId
        getNotesResult?.let { return it }
        return Result.success(libraryFor(spaceId))
    }

    override suspend fun createNote(request: CreateNoteRequest): Result<Note> {
        createNoteCallCount++
        lastCreatedRequest = request
        createNoteResult?.let { return it }
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
        notes[index] = note
        return Result.success(note)
    }

    override suspend fun deleteNote(noteId: String): Result<Unit> {
        deleteNoteCallCount++
        lastDeletedNoteId = noteId
        deleteNoteResult?.let { return it }
        val removed = notes.removeAll { it.id == noteId }
        if (!removed) {
            return Result.failure(NoSuchElementException("Note not found: $noteId"))
        }
        return Result.success(Unit)
    }

    private fun libraryFor(spaceId: String): NoteLibrary {
        val scoped = notes.filter { it.spaceId == spaceId }
        return NoteLibrary(
            notes = scoped,
            allCount = scoped.size,
            pinnedCount = scoped.count { it.isPinned },
            unfiledCount = scoped.count { it.project.isNullOrBlank() },
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
}
