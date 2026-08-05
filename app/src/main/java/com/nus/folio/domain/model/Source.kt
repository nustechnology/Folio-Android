package com.nus.folio.domain.model

data class Source(
    val id: String,
    val title: String,
    val type: SourceType,
    val author: String,
    val addedLabel: String,
    val status: SourceStatus,
    val spaceId: String,
    val fileExtension: String = "",
)

enum class SourceType {
    FILE,
    BOOK,
    WEB,
    TEXT,
}

enum class SourceStatus {
    READY,
    PROCESSING,
    FAILED,
}

enum class SourceFilter {
    ALL,
    FILE,
    WEB,
    TEXT,
}

data class SourceLibrary(
    val sources: List<Source>,
    val allCount: Int,
    val papersCount: Int,
    val booksCount: Int,
    val webCount: Int,
    val textCount: Int,
)
