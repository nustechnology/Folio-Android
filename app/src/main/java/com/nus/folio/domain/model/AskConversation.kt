package com.nus.folio.domain.model

data class AskConversation(
    val id: String,
    val title: String,
    val dateLabel: String,
    val spaceId: String = "",
    /** Set when the thread is scoped to one source; null means entire space. */
    val sourceId: String? = null,
)

data class AskConversationLibrary(
    val conversations: List<AskConversation>,
    val page: Int = AskConversationPaging.DEFAULT_PAGE,
    val limit: Int = AskConversationPaging.DEFAULT_LIMIT,
    val totalCount: Int = 0,
    val hasMore: Boolean = false,
)

object AskConversationPaging {
    const val DEFAULT_LIMIT = 10
    const val DEFAULT_PAGE = 1
}

data class AskConversationDetail(
    val conversation: AskConversation,
    val messages: List<AskConversationMessage>,
)

data class AskConversationMessage(
    val id: String,
    val role: AskConversationRole,
    val content: String,
    val citations: List<AskCitation> = emptyList(),
    val limitation: String? = null,
    val stopped: Boolean = false,
    val feedback: AskFeedbackRating? = null,
    val savedNoteId: String? = null,
)

enum class AskConversationRole {
    USER,
    ASSISTANT,
}
