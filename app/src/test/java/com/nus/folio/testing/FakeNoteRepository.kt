package com.nus.folio.testing

import com.nus.folio.domain.model.Note
import com.nus.folio.domain.model.NoteLibrary
import com.nus.folio.domain.repository.NoteRepository

class FakeNoteRepository : NoteRepository {

    var getNotesResult: Result<NoteLibrary>? = null
    var getNotesCallCount = 0
    var lastSpaceId: String? = null

    override suspend fun getNotes(spaceId: String): Result<NoteLibrary> {
        getNotesCallCount++
        lastSpaceId = spaceId
        getNotesResult?.let { return it }
        val notes = sampleNotes.filter { it.spaceId == spaceId }
        return Result.success(
            NoteLibrary(
                notes = notes,
                allCount = notes.size,
                pinnedCount = notes.count { it.isPinned },
                unfiledCount = notes.count { it.project.isNullOrBlank() },
            ),
        )
    }

    companion object {
        private val sampleNotes = listOf(
            Note("1", "Research Question Draft", "Urban Mobility", "Updated 1d ago", true, "2"),
            Note("2", "Literature Review Outline", "Dissertation Research", "Updated 2d ago", true, "1"),
            Note("3", "Turing Test — Key Takeaways", "Dissertation Research", "Updated 3d ago", false, "1"),
            Note("4", "Policy Implications", "Urban Mobility", "Updated 4d ago", false, "2"),
            Note("5", "Teaching Prep — Week 7", null, "Updated 5d ago", false, "4"),
            Note("6", "Archival methods memo", "History of Science", "Updated 6d ago", true, "3"),
        )

        val sampleLibrary = NoteLibrary(
            notes = sampleNotes,
            allCount = sampleNotes.size,
            pinnedCount = sampleNotes.count { it.isPinned },
            unfiledCount = sampleNotes.count { it.project.isNullOrBlank() },
        )
    }
}
