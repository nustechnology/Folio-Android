package com.nus.folio.presentation.space

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nus.folio.domain.model.Space
import com.nus.folio.domain.model.SpacePaging
import com.nus.folio.domain.model.SpaceSort
import com.nus.folio.domain.usecase.CreateSpaceUseCase
import com.nus.folio.domain.usecase.DeleteSpaceUseCase
import com.nus.folio.domain.usecase.GetCurrentSessionUseCase
import com.nus.folio.domain.usecase.GetSpacesUseCase
import com.nus.folio.domain.usecase.RefreshAuthSessionUseCase
import com.nus.folio.domain.usecase.SyncCurrentUserUseCase
import com.nus.folio.domain.usecase.UpdateSpaceUseCase
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
    private val updateSpaceUseCase: UpdateSpaceUseCase,
    private val deleteSpaceUseCase: DeleteSpaceUseCase,
    private val getCurrentSessionUseCase: GetCurrentSessionUseCase,
    private val syncCurrentUserUseCase: SyncCurrentUserUseCase,
    private val refreshAuthSessionUseCase: RefreshAuthSessionUseCase,
    private val searchDebounceMs: Long = SEARCH_DEBOUNCE_MS,
    private val createMinDelayMs: Long = CREATE_MIN_DELAY_MS,
    private val loadMinDelayMs: Long = LOAD_MIN_DELAY_MS,
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
        if (state.isLoading || state.isRefreshing || state.isLoadingMore || !state.hasMore) return

        spacesLoadJob?.cancel()
        spacesLoadJob = viewModelScope.launch {
            loadSpacesInternal(
                searchQuery = state.searchQuery,
                sort = state.selectedSort,
                reset = false,
            )
        }
    }

    fun onRefresh() {
        if (_uiState.value.isRefreshing) return
        spacesLoadJob?.cancel()
        spacesLoadJob = viewModelScope.launch {
            loadSpacesInternal(
                searchQuery = _uiState.value.searchQuery,
                sort = _uiState.value.selectedSort,
                reset = true,
                showFullScreenLoading = false,
            )
        }
    }

    fun onRetry() {
        spacesLoadJob?.cancel()
        spacesLoadJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    isRefreshing = false,
                    isLoadingMore = false,
                    error = null,
                )
            }
            val refresh = refreshAuthSessionUseCase()
            if (refresh.isFailure && getCurrentSessionUseCase() == null) {
                _uiState.update {
                    it.copy(isLoading = false, requiresReauth = true)
                }
                return@launch
            }
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
        showFullScreenLoading: Boolean = reset,
    ) {
        val page = if (reset) {
            SpacePaging.DEFAULT_PAGE
        } else {
            _uiState.value.currentPage + 1
        }
        if (reset) {
            _uiState.update {
                it.copy(
                    isLoading = showFullScreenLoading,
                    isRefreshing = !showFullScreenLoading,
                    isLoadingMore = false,
                    error = null,
                )
            }
        } else {
            _uiState.update { it.copy(isLoadingMore = true) }
        }

        val loadingStartedAt = System.currentTimeMillis()
        val result = getSpacesUseCase(
            searchQuery = searchQuery.trim().ifEmpty { null },
            sort = sort,
            page = page,
            limit = pageLimit,
        )
        currentCoroutineContext().ensureActive()
        if (showFullScreenLoading) {
            val elapsed = System.currentTimeMillis() - loadingStartedAt
            delay((loadMinDelayMs - elapsed).coerceAtLeast(0L))
            currentCoroutineContext().ensureActive()
        }
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
                        isRefreshing = false,
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
                            isRefreshing = false,
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

    /** Clears the search field and reloads the full space list (e.g. when leaving Spaces). */
    fun clearSearch() {
        if (_uiState.value.searchQuery.isEmpty()) return
        spacesLoadJob?.cancel()
        _uiState.update { it.copy(searchQuery = "") }
        loadSpaces(searchQuery = "")
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
                deletingSpace = null,
                isDeletingSpace = false,
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
        if (_uiState.value.isUpdatingSpace) return
        _uiState.update { it.copy(renamingSpace = null) }
    }

    fun onRenameSpaceSave(name: String, researchObjective: String) {
        if (name.isBlank() || _uiState.value.isUpdatingSpace) return
        val renaming = _uiState.value.renamingSpace ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingSpace = true, actionError = null) }
            val result = coroutineScope {
                val updateDeferred = async {
                    updateSpaceUseCase(
                        spaceId = renaming.id,
                        name = name,
                        researchObjective = researchObjective,
                    )
                }
                delay(createMinDelayMs)
                updateDeferred.await()
            }
            result
                .onSuccess { updated ->
                    _uiState.update { state ->
                        val updatedSpaces = state.allSpaces.map { space ->
                            if (space.id == updated.id) updated else space
                        }
                        state.copy(
                            isUpdatingSpace = false,
                            allSpaces = updatedSpaces,
                            visibleSpaces = updatedSpaces,
                            renamingSpace = null,
                            userMessage = SpaceUserMessage.SPACE_UPDATED,
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isUpdatingSpace = false,
                            actionError = throwable.message ?: "Failed to update space",
                        )
                    }
                }
        }
    }

    fun onDeleteSpaceClick() {
        _uiState.update { state ->
            val space = state.optionsSpace ?: return@update state
            state.copy(
                optionsSpace = null,
                deletingSpace = space,
            )
        }
    }

    fun onDeleteSpaceDismiss() {
        if (_uiState.value.isDeletingSpace) return
        _uiState.update { it.copy(deletingSpace = null) }
    }

    fun onDeleteSpaceConfirm() {
        if (_uiState.value.isDeletingSpace) return
        val deleting = _uiState.value.deletingSpace ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isDeletingSpace = true, actionError = null) }
            deleteSpaceUseCase(deleting.id)
                .onSuccess {
                    _uiState.update { state ->
                        val updatedSpaces = state.allSpaces.filterNot { it.id == deleting.id }
                        state.copy(
                            allSpaces = updatedSpaces,
                            visibleSpaces = updatedSpaces,
                            deletingSpace = null,
                            isDeletingSpace = false,
                            userMessage = SpaceUserMessage.SPACE_DELETED,
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            deletingSpace = null,
                            isDeletingSpace = false,
                            actionError = throwable.message ?: "Failed to delete space",
                        )
                    }
                }
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
        private val updateSpaceUseCase: UpdateSpaceUseCase,
        private val deleteSpaceUseCase: DeleteSpaceUseCase,
        private val getCurrentSessionUseCase: GetCurrentSessionUseCase,
        private val syncCurrentUserUseCase: SyncCurrentUserUseCase,
        private val refreshAuthSessionUseCase: RefreshAuthSessionUseCase,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SpaceViewModel(
                getSpacesUseCase = getSpacesUseCase,
                createSpaceUseCase = createSpaceUseCase,
                updateSpaceUseCase = updateSpaceUseCase,
                deleteSpaceUseCase = deleteSpaceUseCase,
                getCurrentSessionUseCase = getCurrentSessionUseCase,
                syncCurrentUserUseCase = syncCurrentUserUseCase,
                refreshAuthSessionUseCase = refreshAuthSessionUseCase,
            ) as T
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
        const val CREATE_MIN_DELAY_MS = 1_500L
        const val LOAD_MIN_DELAY_MS = 1_000L
    }
}
