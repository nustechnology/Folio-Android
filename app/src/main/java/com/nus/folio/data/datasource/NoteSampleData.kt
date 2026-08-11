package com.nus.folio.data.datasource

import com.nus.folio.domain.model.Note
import com.nus.folio.domain.model.NoteOrigin

/**
 * Shared sample catalog used by debug/release [NoteDataSource].
 */
internal object NoteSampleData {

    fun mutableDefaultNotes(): MutableList<Note> = samples.toMutableList()

    private val samples = listOf(
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
