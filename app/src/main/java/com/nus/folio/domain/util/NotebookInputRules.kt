package com.nus.folio.domain.util

object NotebookInputRules {
    const val MAX_CONTENT_LENGTH = 100_000
    const val MAX_HTML_LENGTH = 1_000_000
    const val MAX_JSON_BODY_BYTES = 2 * 1024 * 1024

    fun clampContent(content: String): String =
        content.take(MAX_CONTENT_LENGTH)
}
