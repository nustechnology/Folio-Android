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

/**
 * Parsed `structuredContent` payload from the Source API.
 * See backend guide: document HTML, spreadsheet grids, or slide cards.
 */
sealed class StructuredContent {
    data class Document(
        val html: String,
    ) : StructuredContent()

    data class Sheets(
        val sheets: List<StructuredSheet>,
    ) : StructuredContent()

    data class Slides(
        val slides: List<StructuredSlide>,
    ) : StructuredContent()
}

data class StructuredSheet(
    val name: String,
    val headers: List<String> = emptyList(),
    val rows: List<List<String>> = emptyList(),
)

data class StructuredSlide(
    val slideNumber: Int,
    val title: String = "",
    val bullets: List<String> = emptyList(),
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
    val structuredContent: StructuredContent? = null,
)
