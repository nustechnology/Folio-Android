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
    USER_CREATED,
    SAVED_ANSWER,
}

/** API `origin` query value for list notes; null means omit (All). */
fun NoteFilter.toApiOrigin(): String? = when (this) {
    NoteFilter.ALL -> null
    NoteFilter.USER_CREATED -> "UserCreated"
    NoteFilter.SAVED_ANSWER -> "SavedAssistantAnswer"
}

/**
 * Note counts are aggregate when the backend returns totals.
 * Category fields fall back to page-local counts when aggregates are omitted.
 */
data class NoteLibrary(
    val notes: List<Note>,
    val allCount: Int,
    val userCreatedCount: Int,
    val savedAnswerCount: Int,
    val page: Int = NotePaging.DEFAULT_PAGE,
    val limit: Int = NotePaging.DEFAULT_LIMIT,
    val hasMore: Boolean = false,
)

object NotePaging {
    const val DEFAULT_LIMIT = 10
    const val DEFAULT_PAGE = 1
}
