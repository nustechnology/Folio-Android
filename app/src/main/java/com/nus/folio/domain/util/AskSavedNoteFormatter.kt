package com.nus.folio.domain.util

/**
 * Formats an Ask assistant answer into note title/body when saving to Notes (AC2).
 */
object AskSavedNoteFormatter {

    fun titleFromQuestion(
        question: String,
        fallback: String = DEFAULT_TITLE,
        maxLength: Int = NoteInputRules.MAX_TITLE_LENGTH,
    ): String =
        question.trim()
            .take(maxLength)
            .trim()
            .ifBlank { fallback }

    /** Answer body stored on the note; keeps inline `[n]` citation markers. */
    fun body(content: String): String = content.trim()

    const val DEFAULT_TITLE = "Saved answer"
}
