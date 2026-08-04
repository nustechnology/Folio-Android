package com.nus.folio.presentation.space

import com.nus.folio.domain.model.Space
import com.nus.folio.domain.model.SpaceSort

data class SpaceAccountItem(
    val id: String,
    val displayName: String,
    val email: String,
    val isSelected: Boolean = false,
)

data class SpaceUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isCreatingSpace: Boolean = false,
    val error: String? = null,
    val actionError: String? = null,
    val allSpaces: List<Space> = emptyList(),
    val visibleSpaces: List<Space> = emptyList(),
    val accounts: List<SpaceAccountItem> = emptyList(),
    val searchQuery: String = "",
    val selectedSort: SpaceSort = SpaceSort.DEFAULT,
    val currentPage: Int = 0,
    val hasMore: Boolean = true,
    val showAddSheet: Boolean = false,
    val showAccountSheet: Boolean = false,
    val showSortSheet: Boolean = false,
    val optionsSpace: Space? = null,
    val renamingSpace: Space? = null,
    val userMessage: SpaceUserMessage? = null,
)

enum class SpaceUserMessage {
    SPACE_CREATED,
    SPACE_UPDATED,
    DELETE_SPACE_NOT_SUPPORTED,
}
