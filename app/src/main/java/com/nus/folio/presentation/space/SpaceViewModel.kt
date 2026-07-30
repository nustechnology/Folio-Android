package com.nus.folio.presentation.space

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nus.folio.domain.model.Space
import com.nus.folio.domain.usecase.GetSpacesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SpaceViewModel(
    private val getSpacesUseCase: GetSpacesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SpaceUiState(isLoading = true))
    val uiState: StateFlow<SpaceUiState> = _uiState.asStateFlow()

    init {
        loadSpaces()
    }

    fun loadSpaces() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            getSpacesUseCase()
                .onSuccess { spaces ->
                    _uiState.update { state ->
                        val next = state.copy(
                            isLoading = false,
                            error = null,
                            allSpaces = spaces,
                        )
                        next.copy(visibleSpaces = filterSpaces(next))
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.message ?: "Failed to load spaces",
                        )
                    }
                }
        }
    }

    fun onSearchClick() {
        _uiState.update { state ->
            val visible = !state.isSearchVisible
            val next = state.copy(
                isSearchVisible = visible,
                searchQuery = if (visible) state.searchQuery else "",
            )
            next.copy(visibleSpaces = filterSpaces(next))
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { state ->
            val next = state.copy(searchQuery = query)
            next.copy(visibleSpaces = filterSpaces(next))
        }
    }

    fun onAddClick() {
        _uiState.update { it.copy(showAddSheet = true) }
    }

    fun onAddSheetDismiss() {
        _uiState.update { it.copy(showAddSheet = false) }
    }

    fun onAddSpaceSubmit(name: String, objective: String) {
        _uiState.update {
            it.copy(userMessage = SpaceUserMessage.ADD_SPACE_NOT_SUPPORTED)
        }
    }

    fun onSpaceOptionsClick(space: Space) {
        _uiState.update { it.copy(optionsSpace = space) }
    }

    fun onSpaceOptionsDismiss() {
        _uiState.update { it.copy(optionsSpace = null) }
    }

    fun onRenameSpaceClick() {
        _uiState.update {
            it.copy(
                optionsSpace = null,
                userMessage = SpaceUserMessage.RENAME_SPACE_NOT_SUPPORTED,
            )
        }
    }

    fun onDeleteSpaceClick() {
        _uiState.update {
            it.copy(
                optionsSpace = null,
                userMessage = SpaceUserMessage.DELETE_SPACE_NOT_SUPPORTED,
            )
        }
    }

    fun onUserMessageShown() {
        _uiState.update { it.copy(userMessage = null) }
    }

    private fun filterSpaces(state: SpaceUiState): List<Space> {
        val query = state.searchQuery.trim()
        if (query.isEmpty()) return state.allSpaces
        return state.allSpaces.filter { it.title.contains(query, ignoreCase = true) }
    }

    class Factory(
        private val getSpacesUseCase: GetSpacesUseCase,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SpaceViewModel(getSpacesUseCase) as T
        }
    }
}
