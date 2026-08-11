package com.nus.folio.domain.util

object NotebookMarkdownExporter {
    fun normalize(content: String): String =
        content
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .trimEnd()
            .let { if (it.isEmpty()) it else "$it\n" }
}
