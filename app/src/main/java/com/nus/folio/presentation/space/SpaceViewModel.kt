package com.nus.folio.presentation.space

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nus.folio.domain.model.Space
import com.nus.folio.domain.usecase.GetCurrentSessionUseCase
import com.nus.folio.domain.usecase.GetSpacesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SpaceViewModel(
    private val getSpacesUseCase: GetSpacesUseCase,
    private val getCurrentSessionUseCase: GetCurrentSessionUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SpaceUiState(isLoading = true))
    val uiState: StateFlow<SpaceUiState> = _uiState.asStateFlow()

    /** Spaces belonging to the signed-in (primary) account. Empty mock account has none. */
    private var primarySpaces: List<Space> = emptyList()

    init {
        loadAccounts()
        loadSpaces()
    }

    private fun loadAccounts() {
        val session = getCurrentSessionUseCase()
        val primary = if (session == null) {
            null
        } else {
            SpaceAccountItem(
                id = session.email,
                displayName = session.displayName,
                email = session.email,
                isSelected = true,
            )
        }
        val emptyAccount = SpaceAccountItem(
            id = PLACEHOLDER_ACCOUNT_ID,
            displayName = PLACEHOLDER_ACCOUNT_DISPLAY_NAME,
            email = PLACEHOLDER_ACCOUNT_EMAIL,
            isSelected = primary == null,
            isPlaceholder = true,
        )
        val accounts = buildList {
            if (primary != null) add(primary)
            add(emptyAccount)
        }
        _uiState.update { it.copy(accounts = accounts) }
    }

    fun loadSpaces() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            getSpacesUseCase()
                .onSuccess { spaces ->
                    primarySpaces = spaces
                    _uiState.update { state ->
                        val next = state.copy(
                            isLoading = false,
                            error = null,
                            allSpaces = spacesForSelectedAccount(state),
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

    fun onAccountClick() {
        _uiState.update { it.copy(showAccountSheet = true) }
    }

    fun onAccountSheetDismiss() {
        _uiState.update { it.copy(showAccountSheet = false) }
    }

    fun onAccountSelected(accountId: String) {
        _uiState.update { state ->
            if (state.accounts.none { it.id == accountId }) return@update state
            val accounts = state.accounts.map { account ->
                account.copy(isSelected = account.id == accountId)
            }
            val next = state.copy(
                accounts = accounts,
                allSpaces = spacesForAccountId(accountId, accounts),
                showAccountSheet = false,
                optionsSpace = null,
                renamingSpace = null,
            )
            next.copy(visibleSpaces = filterSpaces(next))
        }
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
        _uiState.update { state ->
            val space = state.optionsSpace ?: return@update state
            state.copy(
                optionsSpace = null,
                renamingSpace = space,
            )
        }
    }

    fun onRenameSpaceDismiss() {
        _uiState.update { it.copy(renamingSpace = null) }
    }

    fun onRenameSpaceSave(name: String) {
        if (name.isBlank()) return

        _uiState.update { state ->
            val renaming = state.renamingSpace ?: return@update state
            if (isPlaceholderAccountSelected(state)) return@update state

            val updatedSpaces = state.allSpaces.map { space ->
                if (space.id == renaming.id) space.copy(title = name) else space
            }
            primarySpaces = updatedSpaces
            val next = state.copy(
                allSpaces = updatedSpaces,
                renamingSpace = null,
                userMessage = SpaceUserMessage.SPACE_UPDATED,
            )
            next.copy(visibleSpaces = filterSpaces(next))
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

    private fun selectedAccountId(state: SpaceUiState): String? =
        state.accounts.firstOrNull { it.isSelected }?.id

    private fun isPlaceholderAccountSelected(state: SpaceUiState): Boolean =
        state.accounts.any { it.isSelected && it.isPlaceholder }

    private fun spacesForSelectedAccount(state: SpaceUiState): List<Space> =
        spacesForAccountId(selectedAccountId(state), state.accounts)

    private fun spacesForAccountId(
        accountId: String?,
        accounts: List<SpaceAccountItem>,
    ): List<Space> {
        val account = accounts.firstOrNull { it.id == accountId }
        return if (account?.isPlaceholder == true) emptyList() else primarySpaces
    }

    private fun filterSpaces(state: SpaceUiState): List<Space> {
        val query = state.searchQuery.trim()
        if (query.isEmpty()) return state.allSpaces
        return state.allSpaces.filter { it.title.contains(query, ignoreCase = true) }
    }

    class Factory(
        private val getSpacesUseCase: GetSpacesUseCase,
        private val getCurrentSessionUseCase: GetCurrentSessionUseCase,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SpaceViewModel(
                getSpacesUseCase = getSpacesUseCase,
                getCurrentSessionUseCase = getCurrentSessionUseCase,
            ) as T
        }
    }

    companion object {
        /** Stable id that cannot collide with a real account email. */
        const val PLACEHOLDER_ACCOUNT_ID = "placeholder:empty-account"
        const val PLACEHOLDER_ACCOUNT_EMAIL = "jordan@folio.app"
        const val PLACEHOLDER_ACCOUNT_DISPLAY_NAME = "Jordan Lee"
    }
}
