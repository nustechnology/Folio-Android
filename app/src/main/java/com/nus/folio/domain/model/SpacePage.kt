package com.nus.folio.domain.model

data class SpacePage(
    val spaces: List<Space>,
    val page: Int,
    val limit: Int,
    val hasMore: Boolean,
)

object SpacePaging {
    const val DEFAULT_LIMIT = 20
    const val DEFAULT_PAGE = 1
}
