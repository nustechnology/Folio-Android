package com.nus.folio.domain.model

data class Note(
    val id: String,
    val title: String,
    val project: String?,
    val updatedLabel: String,
    val isPinned: Boolean,
)

enum class NoteFilter {
    ALL,
    PINNED,
    UNFILED,
}

data class NoteLibrary(
    val notes: List<Note>,
    val allCount: Int,
    val pinnedCount: Int,
    val unfiledCount: Int,
)
