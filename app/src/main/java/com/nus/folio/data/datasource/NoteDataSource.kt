package com.nus.folio.data.datasource

import com.nus.folio.domain.model.Note
import com.nus.folio.domain.model.NoteLibrary
import kotlinx.coroutines.delay

class NoteDataSource {

    suspend fun fetchNotes(spaceId: String): NoteLibrary {
        delay(200)
        val notes = sampleNotes.filter { it.spaceId == spaceId }
        return NoteLibrary(
            notes = notes,
            allCount = notes.size,
            pinnedCount = notes.count { it.isPinned },
            unfiledCount = notes.count { it.project.isNullOrBlank() },
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
                spaceId = "2",
            ),
            Note(
                id = "2",
                title = "Literature Review Outline",
                project = "Dissertation Research",
                updatedLabel = "Updated 2d ago",
                isPinned = true,
                spaceId = "1",
            ),
            Note(
                id = "3",
                title = "Turing Test — Key Takeaways",
                project = "Dissertation Research",
                updatedLabel = "Updated 3d ago",
                isPinned = false,
                spaceId = "1",
            ),
            Note(
                id = "4",
                title = "Policy Implications",
                project = "Urban Mobility",
                updatedLabel = "Updated 4d ago",
                isPinned = false,
                spaceId = "2",
            ),
            Note(
                id = "5",
                title = "Teaching Prep — Week 7",
                project = null,
                updatedLabel = "Updated 5d ago",
                isPinned = false,
                spaceId = "4",
            ),
            Note(
                id = "6",
                title = "Archival methods memo",
                project = "History of Science",
                updatedLabel = "Updated 6d ago",
                isPinned = true,
                spaceId = "3",
            ),
        )
    }
}
