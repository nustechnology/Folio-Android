package com.nus.folio.domain.model

data class Source(
    val id: String,
    val title: String,
    val type: SourceType,
    val addedLabel: String,
    val status: SourceStatus,
    val spaceId: String,
)

enum class SourceType {
    PDF,
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
    PDF,
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
