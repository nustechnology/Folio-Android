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
    data class Delta(val text: String) : AskStreamEvent

    data class Completed(
        val citations: List<AskCitation> = emptyList(),
        val limitation: String? = null,
    ) : AskStreamEvent
}

data class CreateNoteRequest(
    val spaceId: String,
    val title: String,
    val content: String,
    val origin: NoteOrigin = NoteOrigin.USER_CREATED,
    val citationCount: Int = 0,
    val citations: List<AskCitation> = emptyList(),
    val project: String? = null,
)
