package com.nus.folio.data.datasource

import com.nus.folio.domain.model.Note
import com.nus.folio.domain.model.NoteLibrary
import com.nus.folio.domain.model.NoteOrigin
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class NoteDataSource {

    private val mutex = Mutex()
    private val notes: MutableList<Note> = defaultNotes.toMutableList()

    suspend fun fetchNotes(spaceId: String): NoteLibrary {
        delay(200)
        return libraryFor(spaceId)
    }

    suspend fun updateNote(note: Note): Note {
        delay(200)
        return mutex.withLock {
            val index = notes.indexOfFirst { it.id == note.id }
            if (index < 0) {
                throw NoSuchElementException("Note not found: ${note.id}")
            }
            notes[index] = note
            note
        }
    }

    suspend fun deleteNote(noteId: String) {
        delay(200)
        mutex.withLock {
            val removed = notes.removeAll { it.id == noteId }
            if (!removed) {
                throw NoSuchElementException("Note not found: $noteId")
            }
        }
    }

    private suspend fun libraryFor(spaceId: String): NoteLibrary =
        mutex.withLock {
            val scoped = notes.filter { it.spaceId == spaceId }
            NoteLibrary(
                notes = scoped,
                allCount = scoped.size,
                pinnedCount = scoped.count { it.isPinned },
                unfiledCount = scoped.count { it.project.isNullOrBlank() },
            )
        }

    companion object {
        private val defaultNotes = listOf(
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
    }
}
