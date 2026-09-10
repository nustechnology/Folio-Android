package com.nus.folio.domain.model

data class AskCitation(
    val index: Int,
    val sourceId: String,
    val sourceTitle: String,
    val sourceType: SourceType = SourceType.FILE,
    val fileExtension: String = "",
    /** Human-readable location, e.g. "Page 14". */
    val locationLabel: String = "",
    /** Exact passage to highlight in the citation preview and Source Reader. */
    val evidenceText: String = "",
)

/** Incremental events from an Ask answer stream. */
sealed interface AskStreamEvent {
    /** Sent once generation starts. [conversationId] continues the thread on the next request. */
    data class Started(
        val conversationId: String,
        val messageId: String,
    ) : AskStreamEvent

    data class Delta(val text: String) : AskStreamEvent

    data class Citations(val citations: List<AskCitation>) : AskStreamEvent

    data class Completed(
        val messageId: String = "",
        /** Post-processed answer; when non-null, replaces accumulated [Delta] text. */
        val content: String? = null,
        val citations: List<AskCitation> = emptyList(),
        val limitation: String? = null,
        val stopped: Boolean = false,
    ) : AskStreamEvent
}

data class AskSuggestions(
    val questions: List<String>,
    val isDynamic: Boolean = false,
)

/** Wire value for POST .../messages/{messageId}/feedback. */
enum class AskFeedbackRating {
    USEFUL,
    NOT_USEFUL,
    ;

    fun toApiValue(): String = when (this) {
        USEFUL -> "useful"
        NOT_USEFUL -> "not_useful"
    }
}

data class CreateNoteRequest(
    val spaceId: String,
    val title: String,
    val content: String,
    val origin: NoteOrigin = NoteOrigin.USER_CREATED,
    val citationCount: Int = 0,
    val citations: List<AskCitation> = emptyList(),
    val project: String? = null,
    /** Ask conversation id sent as `origin.conversationId` when saving an assistant answer. */
    val conversationId: String? = null,
    /** Ask message id sent as `origin.messageId` when saving an assistant answer. */
    val messageId: String? = null,
)
