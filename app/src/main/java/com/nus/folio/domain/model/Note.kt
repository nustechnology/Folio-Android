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
    val citations: List<AskCitation> = emptyList(),
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

/**
 * Note counts are aggregate only when the backend returns category totals.
 * Otherwise, all count fields are page-local for the currently loaded response page.
 */
data class NoteLibrary(
    val notes: List<Note>,
    val allCount: Int,
    val pinnedCount: Int,
    val unfiledCount: Int,
    val page: Int = NotePaging.DEFAULT_PAGE,
    val limit: Int = NotePaging.DEFAULT_LIMIT,
    val hasMore: Boolean = false,
)

object NotePaging {
    const val DEFAULT_LIMIT = 10
    const val DEFAULT_PAGE = 1
}
