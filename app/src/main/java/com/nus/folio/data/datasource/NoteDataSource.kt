package com.nus.folio.data.datasource

import com.nus.folio.domain.model.Note
import com.nus.folio.domain.model.NoteLibrary
import kotlinx.coroutines.delay

class NoteDataSource {

    suspend fun fetchNotes(): NoteLibrary {
        delay(200)
        return NoteLibrary(
            notes = sampleNotes,
            allCount = sampleNotes.size,
            pinnedCount = sampleNotes.count { it.isPinned },
            unfiledCount = sampleNotes.count { it.project.isNullOrBlank() },
        )
    }

    companion object {
        private val sampleNotes = listOf(
            Note(
                id = "1",
                title = "Research Question Draft",
                project = "Urban Mobility",
                updatedLabel = "Updated 1d ago",
                isPinned = true,
            ),
            Note(
                id = "2",
                title = "Literature Review Outline",
                project = "Dissertation Research",
                updatedLabel = "Updated 2d ago",
                isPinned = true,
            ),
            Note(
                id = "3",
                title = "Turing Test — Key Takeaways",
                project = "Dissertation Research",
                updatedLabel = "Updated 3d ago",
                isPinned = false,
            ),
            Note(
                id = "4",
                title = "Policy Implications",
                project = "Urban Mobility",
                updatedLabel = "Updated 4d ago",
                isPinned = false,
            ),
            Note(
                id = "5",
                title = "Teaching Prep — Week 7",
                project = null,
                updatedLabel = "Updated 5d ago",
                isPinned = false,
            ),
        )
    }
}
