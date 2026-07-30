package com.nus.folio.presentation.space

import com.nus.folio.domain.model.Space

data class SpaceUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val allSpaces: List<Space> = emptyList(),
    val visibleSpaces: List<Space> = emptyList(),
    val searchQuery: String = "",
    val isSearchVisible: Boolean = false,
    val showAddSheet: Boolean = false,
    val optionsSpace: Space? = null,
    val userMessage: SpaceUserMessage? = null,
)

enum class SpaceUserMessage {
    ADD_SPACE_NOT_SUPPORTED,
    RENAME_SPACE_NOT_SUPPORTED,
    DELETE_SPACE_NOT_SUPPORTED,
}
