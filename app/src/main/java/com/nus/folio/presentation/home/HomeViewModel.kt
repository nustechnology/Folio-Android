package com.nus.folio.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nus.folio.domain.model.AskTopic
import com.nus.folio.domain.model.Note
import com.nus.folio.domain.model.NoteFilter
import com.nus.folio.domain.model.Source
import com.nus.folio.domain.model.SourceFilter
import com.nus.folio.domain.model.SourceType
import com.nus.folio.domain.usecase.GetAskTopicsUseCase
import com.nus.folio.domain.usecase.GetNotesUseCase
import com.nus.folio.domain.usecase.GetSourcesUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val getSourcesUseCase: GetSourcesUseCase,
    private val getAskTopicsUseCase: GetAskTopicsUseCase,
    private val getNotesUseCase: GetNotesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHome()
    }

    fun loadSources() {
        loadHome()
    }

    fun loadHome() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, sourcesError = null, askError = null, notesError = null)
            }

            val sourcesDeferred = async { getSourcesUseCase() }
            val askDeferred = async { getAskTopicsUseCase() }
            val notesDeferred = async { getNotesUseCase() }

            val sourcesResult = sourcesDeferred.await()
            val askResult = askDeferred.await()
            val notesResult = notesDeferred.await()

            _uiState.update { state ->
                var next = state.copy(isLoading = false)

                next = sourcesResult.fold(
                    onSuccess = { library ->
                        next.copy(
                            sourcesError = null,
                            allSources = library.sources,
                            allCount = library.allCount,
                            papersCount = library.papersCount,
                            booksCount = library.booksCount,
                            webCount = library.webCount,
                            textCount = library.textCount,
                        )
                    },
                    onFailure = { throwable ->
                        next.copy(sourcesError = throwable.message.orEmpty())
                    },
                )

                next = askResult.fold(
                    onSuccess = { topics -> next.copy(askError = null, allAskTopics = topics) },
                    onFailure = { throwable ->
                        next.copy(askError = throwable.message.orEmpty())
                    },
                )

                next = notesResult.fold(
                    onSuccess = { library ->
                        next.copy(
                            notesError = null,
                            allNotes = library.notes,
                            notesAllCount = library.allCount,
                            notesPinnedCount = library.pinnedCount,
                            notesUnfiledCount = library.unfiledCount,
                        )
                    },
                    onFailure = { throwable ->
                        next.copy(notesError = throwable.message.orEmpty())
                    },
                )

                next.copy(
                    visibleSources = filterSources(next),
                    visibleAskTopics = filterAskTopics(next),
                    visibleNotes = filterNotes(next),
                )
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { state ->
            val next = state.copy(searchQuery = query)
            next.copy(
                visibleSources = filterSources(next),
                visibleAskTopics = filterAskTopics(next),
                visibleNotes = filterNotes(next),
            )
        }
    }

    fun onSearchClick() {
        _uiState.update { state ->
            val showingSearch = !state.isSearchVisible
            if (showingSearch) {
                state.copy(isSearchVisible = true)
            } else {
                val next = state.copy(isSearchVisible = false, searchQuery = "")
                next.copy(
                    visibleSources = filterSources(next),
                    visibleAskTopics = filterAskTopics(next),
                    visibleNotes = filterNotes(next),
                )
            }
        }
    }

    fun onFilterSelected(filter: SourceFilter) {
        _uiState.update { state ->
            val next = state.copy(selectedFilter = filter)
            next.copy(visibleSources = filterSources(next))
        }
    }

    fun onNoteFilterSelected(filter: NoteFilter) {
        _uiState.update { state ->
            val next = state.copy(selectedNoteFilter = filter)
            next.copy(visibleNotes = filterNotes(next))
        }
    }

    fun onTabSelected(tab: HomeTab) {
        _uiState.update { state ->
            val next = state.copy(
                selectedTab = tab,
                isSearchVisible = false,
                searchQuery = "",
            )
            next.copy(
                visibleSources = filterSources(next),
                visibleAskTopics = filterAskTopics(next),
                visibleNotes = filterNotes(next),
            )
        }
    }

    fun onAddSourceSubmit(
        @Suppress("UNUSED_PARAMETER") tab: AddSourceTab,
        @Suppress("UNUSED_PARAMETER") uriString: String?,
        @Suppress("UNUSED_PARAMETER") value: String,
    ) {
        _uiState.update {
            it.copy(userMessage = HomeUserMessage.ADD_SOURCE_NOT_SUPPORTED)
        }
    }

    fun onUserMessageShown() {
        _uiState.update { it.copy(userMessage = null) }
    }

    private fun filterSources(state: HomeUiState): List<Source> {
        val byType = when (state.selectedFilter) {
            SourceFilter.ALL -> state.allSources
            SourceFilter.PAPERS -> state.allSources.filter { it.type == SourceType.PDF }
            SourceFilter.BOOKS -> state.allSources.filter { it.type == SourceType.BOOK }
            SourceFilter.WEB -> state.allSources.filter { it.type == SourceType.WEB }
            SourceFilter.TEXT -> state.allSources.filter { it.type == SourceType.TEXT }
        }
        val query = state.searchQuery.trim()
        if (query.isEmpty()) return byType
        return byType.filter { it.title.contains(query, ignoreCase = true) }
    }

    private fun filterAskTopics(state: HomeUiState): List<AskTopic> {
        val query = state.searchQuery.trim()
        if (query.isEmpty()) return state.allAskTopics
        return state.allAskTopics.filter { it.title.contains(query, ignoreCase = true) }
    }

    private fun filterNotes(state: HomeUiState): List<Note> {
        val byFilter = when (state.selectedNoteFilter) {
            NoteFilter.ALL -> state.allNotes
            NoteFilter.PINNED -> state.allNotes.filter { it.isPinned }
            NoteFilter.UNFILED -> state.allNotes.filter { it.project.isNullOrBlank() }
        }
        val query = state.searchQuery.trim()
        if (query.isEmpty()) return byFilter
        return byFilter.filter { note ->
            note.title.contains(query, ignoreCase = true) ||
                note.project.orEmpty().contains(query, ignoreCase = true)
        }
    }

    class Factory(
        private val getSourcesUseCase: GetSourcesUseCase,
        private val getAskTopicsUseCase: GetAskTopicsUseCase,
        private val getNotesUseCase: GetNotesUseCase,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(getSourcesUseCase, getAskTopicsUseCase, getNotesUseCase) as T
        }
    }
}
