package com.nus.folio.presentation.space

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nus.folio.domain.model.Space
import com.nus.folio.domain.model.SpacePaging
import com.nus.folio.domain.model.SpaceSort
import com.nus.folio.domain.usecase.CreateSpaceUseCase
import com.nus.folio.domain.usecase.GetCurrentSessionUseCase
import com.nus.folio.domain.usecase.GetSpacesUseCase
import com.nus.folio.domain.usecase.RefreshAuthSessionUseCase
import com.nus.folio.domain.usecase.SyncCurrentUserUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SpaceViewModel(
    private val getSpacesUseCase: GetSpacesUseCase,
    private val createSpaceUseCase: CreateSpaceUseCase,
    private val getCurrentSessionUseCase: GetCurrentSessionUseCase,
    private val syncCurrentUserUseCase: SyncCurrentUserUseCase,
    private val refreshAuthSessionUseCase: RefreshAuthSessionUseCase,
    private val searchDebounceMs: Long = SEARCH_DEBOUNCE_MS,
    private val createMinDelayMs: Long = CREATE_MIN_DELAY_MS,
    private val pageLimit: Int = SpacePaging.DEFAULT_LIMIT,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SpaceUiState(isLoading = true))
    val uiState: StateFlow<SpaceUiState> = _uiState.asStateFlow()

    private var spacesLoadJob: Job? = null

    init {
        viewModelScope.launch {
            syncCurrentUserUseCase()
            loadAccounts()
            loadSpaces()
        }
    }

    private fun loadAccounts() {
        val session = getCurrentSessionUseCase() ?: return
        val accounts = listOf(
            SpaceAccountItem(
                id = session.userId?.takeIf { it.isNotBlank() } ?: session.email,
                displayName = session.displayName,
                email = session.email,
                isSelected = true,
            ),
        )
        _uiState.update { it.copy(accounts = accounts) }
    }

    fun loadSpaces(
        searchQuery: String = _uiState.value.searchQuery,
        sort: SpaceSort = _uiState.value.selectedSort,
    ) {
        spacesLoadJob?.cancel()
        spacesLoadJob = viewModelScope.launch {
            loadSpacesInternal(searchQuery = searchQuery, sort = sort, reset = true)
        }
    }

    fun onLoadMore() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.hasMore) return

        spacesLoadJob?.cancel()
        spacesLoadJob = viewModelScope.launch {
            loadSpacesInternal(
                searchQuery = state.searchQuery,
                sort = state.selectedSort,
                reset = false,
            )
        }
    }

    fun onRetry() {
        spacesLoadJob?.cancel()
        spacesLoadJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    isLoadingMore = false,
                    error = null,
                )
            }
            refreshAuthSessionUseCase()
            loadAccounts()
            loadSpacesInternal(
                searchQuery = _uiState.value.searchQuery,
                sort = _uiState.value.selectedSort,
                reset = true,
            )
        }
    }

    private suspend fun loadSpacesInternal(
        searchQuery: String,
        sort: SpaceSort,
        reset: Boolean,
    ) {
        val page = if (reset) {
            SpacePaging.DEFAULT_PAGE
        } else {
            _uiState.value.currentPage + 1
        }
        if (reset) {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    isLoadingMore = false,
                    error = null,
                )
            }
        } else {
            _uiState.update { it.copy(isLoadingMore = true) }
        }

        val result = getSpacesUseCase(
            searchQuery = searchQuery.trim().ifEmpty { null },
            sort = sort,
            page = page,
            limit = pageLimit,
        )
        currentCoroutineContext().ensureActive()
        result
            .onSuccess { spacePage ->
                _uiState.update { state ->
                    val merged = if (reset) {
                        spacePage.spaces
                    } else {
                        val existingIds = state.allSpaces.mapTo(HashSet()) { it.id }
                        state.allSpaces + spacePage.spaces.filterNot { it.id in existingIds }
                    }
                    state.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        error = null,
                        allSpaces = merged,
                        visibleSpaces = merged,
                        selectedSort = sort,
                        currentPage = spacePage.page,
                        hasMore = spacePage.hasMore,
                    )
                }
            }
            .onFailure { throwable ->
                _uiState.update { state ->
                    if (reset) {
                        state.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            error = throwable.message ?: "Failed to load spaces",
                        )
                    } else {
                        // Keep existing list visible; load-more failure is non-blocking.
                        state.copy(isLoadingMore = false)
                    }
                }
            }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        spacesLoadJob?.cancel()
        spacesLoadJob = viewModelScope.launch {
            delay(searchDebounceMs)
            loadSpacesInternal(
                searchQuery = query,
                sort = _uiState.value.selectedSort,
                reset = true,
            )
        }
    }

    fun onFilterSortClick() {
        _uiState.update { it.copy(showSortSheet = true) }
    }

    fun onSortSheetDismiss() {
        _uiState.update { it.copy(showSortSheet = false) }
    }

    fun onSortSelected(sort: SpaceSort) {
        if (sort == _uiState.value.selectedSort) {
            _uiState.update { it.copy(showSortSheet = false) }
            return
        }
        _uiState.update { it.copy(showSortSheet = false, selectedSort = sort) }
        loadSpaces(sort = sort)
    }

    fun onAddClick() {
        _uiState.update { it.copy(showAddSheet = true, actionError = null) }
    }

    fun onAddSheetDismiss() {
        if (_uiState.value.isCreatingSpace) return
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
            state.copy(
                accounts = accounts,
                showAccountSheet = false,
                optionsSpace = null,
                renamingSpace = null,
            )
        }
        loadSpaces()
    }

    fun onAddSpaceSubmit(name: String, objective: String) {
        if (name.isBlank() || _uiState.value.isCreatingSpace) return

        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingSpace = true, actionError = null) }
            val result = coroutineScope {
                val createDeferred = async {
                    createSpaceUseCase(name = name, researchObjective = objective)
                }
                delay(createMinDelayMs)
                createDeferred.await()
            }
            result
                .onSuccess { created ->
                    // Drop any in-flight search/load so a stale query cannot overwrite this list.
                    spacesLoadJob?.cancel()
                    _uiState.update { state ->
                        val spaces = listOf(created) + state.allSpaces.filterNot { it.id == created.id }
                        state.copy(
                            isCreatingSpace = false,
                            showAddSheet = false,
                            allSpaces = spaces,
                            visibleSpaces = spaces,
                            searchQuery = "",
                            userMessage = SpaceUserMessage.SPACE_CREATED,
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isCreatingSpace = false,
                            actionError = throwable.message ?: "Failed to create space",
                        )
                    }
                }
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
            val updatedSpaces = state.allSpaces.map { space ->
                if (space.id == renaming.id) space.copy(title = name) else space
            }
            state.copy(
                allSpaces = updatedSpaces,
                visibleSpaces = updatedSpaces,
                renamingSpace = null,
                userMessage = SpaceUserMessage.SPACE_UPDATED,
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

    fun onActionErrorShown() {
        _uiState.update { it.copy(actionError = null) }
    }

    class Factory(
        private val getSpacesUseCase: GetSpacesUseCase,
        private val createSpaceUseCase: CreateSpaceUseCase,
        private val getCurrentSessionUseCase: GetCurrentSessionUseCase,
        private val syncCurrentUserUseCase: SyncCurrentUserUseCase,
        private val refreshAuthSessionUseCase: RefreshAuthSessionUseCase,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SpaceViewModel(
                getSpacesUseCase = getSpacesUseCase,
                createSpaceUseCase = createSpaceUseCase,
                getCurrentSessionUseCase = getCurrentSessionUseCase,
                syncCurrentUserUseCase = syncCurrentUserUseCase,
                refreshAuthSessionUseCase = refreshAuthSessionUseCase,
            ) as T
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
        const val CREATE_MIN_DELAY_MS = 1_500L
    }
}
