package com.nus.folio.data.util

internal object SourceMimeTypes {
    fun forExtension(extension: String): String = when (extension.lowercase()) {
        "pdf" -> "application/pdf"
        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        "epub" -> "application/epub+zip"
        "md" -> "text/markdown"
        "txt" -> "text/plain"
        "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        "csv" -> "text/csv"
        else -> "application/octet-stream"
    }
}
