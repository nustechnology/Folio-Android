package com.nus.folio.domain.util

object NotebookInputRules {
    const val MAX_CONTENT_LENGTH = 100_000

    fun clampContent(content: String): String =
        content.take(MAX_CONTENT_LENGTH)
}
