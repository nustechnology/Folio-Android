package com.nus.folio.domain.model

sealed class SourceFileLocation {
    data class Local(
        val absolutePath: String,
        val fileName: String,
        val mimeType: String,
    ) : SourceFileLocation()

    data class Remote(
        val url: String,
    ) : SourceFileLocation()
}
