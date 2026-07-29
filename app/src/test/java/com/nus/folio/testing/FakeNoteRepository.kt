package com.nus.folio.testing

import com.nus.folio.domain.model.Note
import com.nus.folio.domain.model.NoteLibrary
import com.nus.folio.domain.repository.NoteRepository

class FakeNoteRepository : NoteRepository {

    var getNotesResult: Result<NoteLibrary> = Result.success(sampleLibrary)
    var getNotesCallCount = 0

    override suspend fun getNotes(): Result<NoteLibrary> {
        getNotesCallCount++
        return getNotesResult
    }

    companion object {
        private val sampleNotes = listOf(
            Note("1", "Research Question Draft", "Urban Mobility", "Updated 1d ago", true),
            Note("2", "Literature Review Outline", "Dissertation Research", "Updated 2d ago", true),
            Note("3", "Turing Test — Key Takeaways", "Dissertation Research", "Updated 3d ago", false),
            Note("4", "Policy Implications", "Urban Mobility", "Updated 4d ago", false),
            Note("5", "Teaching Prep — Week 7", null, "Updated 5d ago", false),
        )
        val sampleLibrary = NoteLibrary(
            notes = sampleNotes,
            allCount = sampleNotes.size,
            pinnedCount = sampleNotes.count { it.isPinned },
            unfiledCount = sampleNotes.count { it.project.isNullOrBlank() },
        )
    }
}
