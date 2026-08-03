package com.nus.folio.domain.model

enum class SourceContentFormat {
    DOCUMENT,
    SLIDES,
    SHEET,
}

data class SourceSheetTab(
    val id: String,
    val name: String,
    val htmlTable: String,
)

data class SourceDetail(
    val id: String,
    val title: String,
    val author: String,
    val addedLabel: String,
    val type: SourceType,
    val status: SourceStatus,
    val spaceId: String,
    val fileExtension: String,
    val contentFormat: SourceContentFormat,
    val originalFileName: String,
    val htmlContent: String? = null,
    val sheets: List<SourceSheetTab> = emptyList(),
)
