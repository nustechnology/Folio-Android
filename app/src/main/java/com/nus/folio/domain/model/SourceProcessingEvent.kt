package com.nus.folio.domain.model

enum class SourceProcessingState {
    ADDED,
    EXTRACTING_TEXT,
    INDEXING_EVIDENCE,
    READY,
    FAILED,
}

data class SourceProcessingEvent(
    val sourceId: String,
    val state: SourceProcessingState,
    val progress: Int,
) {
    val isTerminal: Boolean
        get() = state == SourceProcessingState.READY || state == SourceProcessingState.FAILED

    /** Number of completed steps in the 4-step processing sheet (0..4). */
    fun completedStepCount(): Int = when (state) {
        SourceProcessingState.ADDED -> 0
        SourceProcessingState.EXTRACTING_TEXT -> 1
        SourceProcessingState.INDEXING_EVIDENCE -> 2
        SourceProcessingState.READY,
        SourceProcessingState.FAILED,
        -> 4
    }

    companion object {
        fun parseState(raw: String): SourceProcessingState =
            when (raw.trim().lowercase()) {
                "extracting_text" -> SourceProcessingState.EXTRACTING_TEXT
                "indexing_evidence" -> SourceProcessingState.INDEXING_EVIDENCE
                "ready" -> SourceProcessingState.READY
                "failed", "error" -> SourceProcessingState.FAILED
                else -> SourceProcessingState.ADDED
            }
    }
}
