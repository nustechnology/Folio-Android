package com.nus.folio.presentation.space

import com.nus.folio.domain.model.Space

data class SpaceAccountItem(
    val id: String,
    val displayName: String,
    val email: String,
    val isSelected: Boolean = false,
    val isPlaceholder: Boolean = false,
)

data class SpaceUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val allSpaces: List<Space> = emptyList(),
    val visibleSpaces: List<Space> = emptyList(),
    val accounts: List<SpaceAccountItem> = emptyList(),
    val searchQuery: String = "",
    val showAddSheet: Boolean = false,
    val showAccountSheet: Boolean = false,
    val optionsSpace: Space? = null,
    val renamingSpace: Space? = null,
    val userMessage: SpaceUserMessage? = null,
)

enum class SpaceUserMessage {
    SPACE_CREATED,
    SPACE_UPDATED,
    SPACE_DELETED,
    ADD_SPACE_NOT_SUPPORTED,
    RENAME_SPACE_NOT_SUPPORTED,
    DELETE_SPACE_NOT_SUPPORTED,
}
