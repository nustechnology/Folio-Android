package com.nus.folio.domain.model

data class Note(
    val id: String,
    val title: String,
    val content: String,
    val project: String?,
    val updatedLabel: String,
    val isPinned: Boolean,
    val spaceId: String,
    val origin: NoteOrigin = NoteOrigin.USER_CREATED,
    val citationCount: Int = 0,
)

enum class NoteOrigin {
    USER_CREATED,
    SAVED_ANSWER,
}

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
