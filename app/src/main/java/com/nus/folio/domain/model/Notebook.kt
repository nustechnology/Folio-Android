package com.nus.folio.domain.model

data class Notebook(
    val spaceId: String,
    val content: String,
    val updatedAtMillis: Long = 0L,
    /** True when content came from local cache after a transport failure. */
    val isStale: Boolean = false,
    /**
     * True when HTML→markdown dropped or unwrapped unsupported tags.
     * The editor must not PUT this document back.
     */
    val isReadOnly: Boolean = false,
    /** Monotonic local revision; used so an older PUT cannot clear a newer draft. */
    val revision: Long = 0L,
)
