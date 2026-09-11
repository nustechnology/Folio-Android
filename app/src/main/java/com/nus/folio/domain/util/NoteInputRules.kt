package com.nus.folio.domain.util

/**
 * Client-side rules for manual note create/edit (AC1).
 */
object NoteInputRules {
    const val MAX_TITLE_LENGTH = 150
    const val MIN_CONTENT_LENGTH = 1
    const val MAX_CONTENT_LENGTH = 20_000
    const val DEFAULT_TITLE = "Untitled Note"

    enum class TitleValidationError {
        TOO_LONG,
    }

    enum class ContentValidationError {
        EMPTY,
        TOO_LONG,
    }

    fun resolveTitle(title: String): String =
        title.trim().ifBlank { DEFAULT_TITLE }

    fun limitTitle(value: String): String = value.take(MAX_TITLE_LENGTH)

    fun titleValidationError(title: String): TitleValidationError? =
        if (title.length > MAX_TITLE_LENGTH) TitleValidationError.TOO_LONG else null

    fun contentValidationError(content: String): ContentValidationError? = when {
        content.length > MAX_CONTENT_LENGTH -> ContentValidationError.TOO_LONG
        content.isBlank() -> ContentValidationError.EMPTY
        else -> null
    }

    fun canSave(title: String, content: String): Boolean =
        titleValidationError(title) == null &&
            contentValidationError(content) == null
}
