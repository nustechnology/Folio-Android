package com.nus.folio.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nus.folio.domain.model.AskCitation
import com.nus.folio.domain.model.Note
import com.nus.folio.domain.model.NoteFilter
import com.nus.folio.domain.model.NotePaging
import com.nus.folio.domain.model.NoteSort
import com.nus.folio.domain.model.Source
import com.nus.folio.domain.model.SourceFilter
import com.nus.folio.domain.model.SourceSort
import com.nus.folio.domain.model.toApiSourceType
import com.nus.folio.domain.repository.SourceFileBytesReader
import com.nus.folio.domain.usecase.CreateNoteUseCase
import com.nus.folio.domain.usecase.CreateSourceUseCase
import com.nus.folio.domain.usecase.DeleteNoteUseCase
import com.nus.folio.domain.usecase.DeleteSourceUseCase
import com.nus.folio.domain.usecase.GetAskSuggestionsUseCase
import com.nus.folio.domain.usecase.GetAskTopicsUseCase
import com.nus.folio.domain.usecase.GetCurrentSessionUseCase
import com.nus.folio.domain.usecase.GetNoteDetailUseCase
import com.nus.folio.domain.usecase.GetNotesUseCase
import com.nus.folio.domain.usecase.GetSourceDetailUseCase
import com.nus.folio.domain.usecase.GetSourcesUseCase
import com.nus.folio.domain.usecase.ObserveSourceProcessingUseCase
import com.nus.folio.domain.usecase.RefreshAuthSessionUseCase
import com.nus.folio.domain.usecase.RetrySourceUseCase
import com.nus.folio.domain.usecase.StreamAskAnswerUseCase
import com.nus.folio.domain.usecase.UpdateNoteUseCase
import com.nus.folio.domain.usecase.UpdateSourceUseCase
import com.nus.folio.presentation.home.bottomsheet.AddSourceDraft
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Home screen façade. Owns [uiState] and the shared load/search/tab handlers,
 * and forwards tab-specific actions to [HomeSourcesDelegate], [HomeAskDelegate],
 * and [HomeNotesDelegate].
 */
class HomeViewModel(
    private val spaceId: String,
    spaceTitle: String,
    private val getSourcesUseCase: GetSourcesUseCase,
    private val createSourceUseCase: CreateSourceUseCase,
    private val observeSourceProcessingUseCase: ObserveSourceProcessingUseCase,
    private val updateSourceUseCase: UpdateSourceUseCase,
    private val deleteSourceUseCase: DeleteSourceUseCase,
    private val getSourceDetailUseCase: GetSourceDetailUseCase,
    private val getAskTopicsUseCase: GetAskTopicsUseCase,
    private val getAskSuggestionsUseCase: GetAskSuggestionsUseCase,
    private val streamAskAnswerUseCase: StreamAskAnswerUseCase,
    private val getNotesUseCase: GetNotesUseCase,
    private val getNoteDetailUseCase: GetNoteDetailUseCase,
    private val createNoteUseCase: CreateNoteUseCase,
    private val updateNoteUseCase: UpdateNoteUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase,
    private val sourceFileBytesReader: SourceFileBytesReader,
    private val refreshAuthSessionUseCase: RefreshAuthSessionUseCase,
    private val getCurrentSessionUseCase: GetCurrentSessionUseCase,
    private val retrySourceUseCase: RetrySourceUseCase,
    private val openSourceDelayMs: Long = OPEN_SOURCE_DELAY_MS,
    private val searchDebounceMs: Long = SEARCH_DEBOUNCE_MS,
    private val createMinDelayMs: Long = CREATE_MIN_DELAY_MS,
    private val loadMinDelayMs: Long = LOAD_MIN_DELAY_MS,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        HomeUiState(
            spaceId = spaceId,
            spaceTitle = spaceTitle,
            isLoading = true,
        ).withCurrentUser(getCurrentSessionUseCase()),
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val ask = HomeAskDelegate(
        spaceId = spaceId,
        state = _uiState,
        scope = viewModelScope,
        streamAskAnswerUseCase = streamAskAnswerUseCase,
        getAskSuggestionsUseCase = getAskSuggestionsUseCase,
        createNoteUseCase = createNoteUseCase,
    )

    private val sources = HomeSourcesDelegate(
        spaceId = spaceId,
        state = _uiState,
        scope = viewModelScope,
        getSourcesUseCase = getSourcesUseCase,
        createSourceUseCase = createSourceUseCase,
        observeSourceProcessingUseCase = observeSourceProcessingUseCase,
        updateSourceUseCase = updateSourceUseCase,
        deleteSourceUseCase = deleteSourceUseCase,
        retrySourceUseCase = retrySourceUseCase,
        getSourceDetailUseCase = getSourceDetailUseCase,
        sourceFileBytesReader = sourceFileBytesReader,
        getCurrentSessionUseCase = getCurrentSessionUseCase,
        openSourceDelayMs = openSourceDelayMs,
        searchDebounceMs = searchDebounceMs,
        createMinDelayMs = createMinDelayMs,
        applyAskScope = ask::applyAskScope,
    )

    private val notes = HomeNotesDelegate(
        spaceId = spaceId,
        state = _uiState,
        scope = viewModelScope,
        getNotesUseCase = getNotesUseCase,
        getNoteDetailUseCase = getNoteDetailUseCase,
        createNoteUseCase = createNoteUseCase,
        updateNoteUseCase = updateNoteUseCase,
        deleteNoteUseCase = deleteNoteUseCase,
        createSourceUseCase = createSourceUseCase,
        getCurrentSessionUseCase = getCurrentSessionUseCase,
        onSourceCreated = sources::onSourceCreated,
        searchDebounceMs = searchDebounceMs,
    )

    init {
        loadHome()
    }

    fun loadSources() = sources.loadSourcesOnly()

    fun onRefreshSources() = sources.onRefreshSources()

    fun onRetry() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    sourcesError = null,
                    askError = null,
                    notesError = null,
                )
            }
            val refresh = refreshAuthSessionUseCase()
            if (refresh.isFailure && getCurrentSessionUseCase() == null) {
                _uiState.update {
                    it.copy(isLoading = false, requiresReauth = true)
                }
                return@launch
            }
            _uiState.update { it.withCurrentUser(getCurrentSessionUseCase()) }
            loadHome()
        }
    }

    fun loadHome() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, sourcesError = null, askError = null, notesError = null)
            }

            val loadingStartedAt = System.currentTimeMillis()
            val state = _uiState.value
            val sourcesDeferred = async {
                getSourcesUseCase(
                    spaceId = spaceId,
                    sourceType = state.selectedFilter.toApiSourceType(),
                    search = state.searchQuery.trim().takeIf { it.isNotEmpty() },
                    sort = state.selectedSort,
                )
            }
            val askDeferred = async { getAskTopicsUseCase(spaceId) }
            val notesDeferred = async {
                getNotesUseCase(
                    spaceId = spaceId,
                    search = state.searchQuery.trim().takeIf { it.isNotEmpty() },
                    sort = state.selectedNoteSort,
                    page = NotePaging.DEFAULT_PAGE,
                    limit = NotePaging.DEFAULT_LIMIT,
                )
            }

            val sourcesResult = sourcesDeferred.await()
            val askResult = askDeferred.await()
            val notesResult = notesDeferred.await()

            val elapsed = System.currentTimeMillis() - loadingStartedAt
            delay((loadMinDelayMs - elapsed).coerceAtLeast(0L))

            _uiState.update { current ->
                var next = current.copy(isLoading = false)

                next = sourcesResult.fold(
                    onSuccess = { library ->
                        next.copy(
                            sourcesError = null,
                            allSources = library.sources,
                            allCount = library.allCount,
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
                            notesCurrentPage = library.page,
                            notesHasMore = library.hasMore,
                            isLoadingMoreNotes = false,
                        )
                    },
                    onFailure = { throwable ->
                        next.copy(notesError = throwable.message.orEmpty())
                    },
                )

                next.copy(
                    visibleSources = next.allSources,
                    visibleAskTopics = filterAskTopics(next),
                    visibleNotes = filterNotes(next),
                )
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { state ->
            val next = state.copy(
                searchQuery = query,
                isSearchingNotes = state.selectedTab == HomeTab.NOTES,
            )
            next.copy(
                visibleAskTopics = filterAskTopics(next),
                visibleNotes = filterNotes(next),
                visibleSources = next.allSources,
            )
        }
        when (_uiState.value.selectedTab) {
            HomeTab.SOURCES -> sources.scheduleSearchReload()
            HomeTab.NOTES -> notes.scheduleSearchReload()
            HomeTab.ASK, HomeTab.NOTEBOOK -> Unit
        }
    }

    fun onFilterSelected(filter: SourceFilter) = sources.onFilterSelected(filter)

    fun onFilterSortClick() {
        when (_uiState.value.selectedTab) {
            HomeTab.SOURCES -> sources.onFilterSortClick()
            HomeTab.NOTES -> notes.onFilterSortClick()
            HomeTab.ASK, HomeTab.NOTEBOOK -> Unit
        }
    }

    fun onSortSheetDismiss() {
        sources.onSortSheetDismiss()
        notes.onSortSheetDismiss()
    }

    fun onSortSelected(sort: SourceSort) = sources.onSortSelected(sort)

    fun onNoteSortSelected(sort: NoteSort) = notes.onSortSelected(sort)

    fun onNoteFilterSelected(filter: NoteFilter) = notes.onNoteFilterSelected(filter)

    fun onLoadMoreNotes() = notes.onLoadMore()

    fun onRefreshNotes() = notes.onRefreshNotes()

    fun onTabSelected(tab: HomeTab) {
        sources.cancelSearchJob()
        notes.cancelSearchJob()
        val previousQuery = _uiState.value.searchQuery
        val previousTab = _uiState.value.selectedTab
        _uiState.update { state ->
            val next = state.copy(
                selectedTab = tab,
                searchQuery = "",
            )
            next.copy(
                visibleSources = next.allSources,
                visibleAskTopics = filterAskTopics(next),
                visibleNotes = filterNotes(next),
            )
        }
        if (previousQuery.isNotBlank()) {
            // Search was cleared — reload the tab that owned the query.
            if (previousTab == HomeTab.SOURCES) {
                sources.loadSourcesOnly()
            }
            if (previousTab == HomeTab.NOTES) {
                notes.loadNotesOnly()
            }
        }
    }

    fun onAddSourceClick() = sources.onAddSourceClick()

    fun onAddSourceSheetDismiss() = sources.onAddSourceSheetDismiss()

    fun onAddSourceSubmit(draft: AddSourceDraft) = sources.onAddSourceSubmit(draft)

    fun onSourceProcessingDismiss() = sources.onSourceProcessingDismiss()

    fun onSourceProcessingOpenSource() = sources.onSourceProcessingOpenSource()

    fun onSourceProcessingAsk() = sources.onSourceProcessingAsk()

    fun onSourceProcessingRetry() = sources.onSourceProcessingRetry()

    fun onAddNoteSubmit(
        title: String,
        content: String,
    ) = notes.onAddNoteSubmit(title, content)

    fun onAskSubmit(question: String) = ask.onAskSubmit(question)

    fun onAskUserEnterAnimationFinished(messageId: String) =
        ask.onAskUserEnterAnimationFinished(messageId)

    fun onAskStop() = ask.onAskStop()

    fun onAskSaveAsNote(messageId: String) = ask.onAskSaveAsNote(messageId)

    fun onAskSaveAsNoteDismiss() = ask.onAskSaveAsNoteDismiss()

    fun onAskSaveAsNoteConfirm(title: String) = ask.onAskSaveAsNoteConfirm(title)

    fun onAskFeedback(messageId: String, useful: Boolean) = ask.onAskFeedback(messageId, useful)

    fun onAskCitationClick(citation: AskCitation) = ask.onAskCitationClick(citation)

    fun onCitationPreviewDismiss() = ask.onCitationPreviewDismiss()

    fun onCitationOpenInSource() = ask.onCitationOpenInSource()

    fun onInfoToastShown() {
        _uiState.update { it.copy(infoToast = null) }
    }

    fun onAskScopeOptionSelected(sourceId: String?) = ask.onAskScopeOptionSelected(sourceId)

    fun onNewConversation() = ask.onNewConversation()

    fun onAskSourceSelected(sourceId: String) = ask.onAskSourceSelected(sourceId)

    fun onNotebookAddClick() = notes.onNotebookAddClick()

    fun onNotebookActionsDismiss() = notes.onNotebookActionsDismiss()

    fun onCopyNotebookClick() = notes.onCopyNotebookClick()

    fun onExportNotebookClick() = notes.onExportNotebookClick()

    fun onNotebookExportDismiss() = notes.onNotebookExportDismiss()

    fun onNotebookExportConfirm(
        format: NotebookExportFormat,
    ) = notes.onNotebookExportConfirm(format)

    fun onSourceOptionsClick(source: Source) = sources.onSourceOptionsClick(source)

    fun onSourceOptionsDismiss() = sources.onSourceOptionsDismiss()

    fun onEditSourceClick(source: Source) = sources.onEditSourceClick(source)

    fun onSourceClick(source: Source) = sources.onSourceClick(source)

    fun onOpenSourceDetailHandled() = sources.onOpenSourceDetailHandled()

    fun onEditSourceDismiss() = sources.onEditSourceDismiss()

    fun onEditSourceSave(title: String, author: String, content: String = "") =
        sources.onEditSourceSave(title, author, content)

    fun onDeleteSourceClick(source: Source) = sources.onDeleteSourceClick(source)

    fun onDeleteSourceDismiss() = sources.onDeleteSourceDismiss()

    fun onDeleteSourceConfirm() = sources.onDeleteSourceConfirm()

    fun onNoteClick(note: Note) = notes.onNoteClick(note)

    fun onViewNoteDismiss() = notes.onViewNoteDismiss()

    fun onNoteOptionsClick(note: Note) = notes.onNoteOptionsClick(note)

    fun onNoteOptionsDismiss() = notes.onNoteOptionsDismiss()

    fun onViewNoteClick() = notes.onViewNoteClick()

    fun onEditNoteClick() = notes.onEditNoteClick()

    fun onEditNoteDismiss() = notes.onEditNoteDismiss()

    fun onEditNoteSave(title: String, content: String) = notes.onEditNoteSave(title, content)

    fun onConvertNoteClick() = notes.onConvertNoteClick()

    fun onConvertNoteDismiss() = notes.onConvertNoteDismiss()

    fun onConvertNoteCreate(
        title: String,
        snapshot: String,
    ) = notes.onConvertNoteCreate(title, snapshot)

    fun onDeleteNoteClick() = notes.onDeleteNoteClick()

    fun onDeleteNoteDismiss() = notes.onDeleteNoteDismiss()

    fun onDeleteNoteConfirm() = notes.onDeleteNoteConfirm()

    fun onUserMessageShown() {
        _uiState.update { it.copy(userMessage = null) }
    }

    fun onActionErrorShown() {
        _uiState.update { it.copy(actionError = null) }
    }

    class Factory(
        private val spaceId: String,
        private val spaceTitle: String,
        private val getSourcesUseCase: GetSourcesUseCase,
        private val createSourceUseCase: CreateSourceUseCase,
        private val observeSourceProcessingUseCase: ObserveSourceProcessingUseCase,
        private val updateSourceUseCase: UpdateSourceUseCase,
        private val deleteSourceUseCase: DeleteSourceUseCase,
        private val getSourceDetailUseCase: GetSourceDetailUseCase,
        private val getAskTopicsUseCase: GetAskTopicsUseCase,
        private val getAskSuggestionsUseCase: GetAskSuggestionsUseCase,
        private val streamAskAnswerUseCase: StreamAskAnswerUseCase,
        private val getNotesUseCase: GetNotesUseCase,
        private val getNoteDetailUseCase: GetNoteDetailUseCase,
        private val createNoteUseCase: CreateNoteUseCase,
        private val updateNoteUseCase: UpdateNoteUseCase,
        private val deleteNoteUseCase: DeleteNoteUseCase,
        private val sourceFileBytesReader: SourceFileBytesReader,
        private val refreshAuthSessionUseCase: RefreshAuthSessionUseCase,
        private val getCurrentSessionUseCase: GetCurrentSessionUseCase,
        private val retrySourceUseCase: RetrySourceUseCase,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(
                spaceId = spaceId,
                spaceTitle = spaceTitle,
                getSourcesUseCase = getSourcesUseCase,
                createSourceUseCase = createSourceUseCase,
                observeSourceProcessingUseCase = observeSourceProcessingUseCase,
                updateSourceUseCase = updateSourceUseCase,
                deleteSourceUseCase = deleteSourceUseCase,
                getSourceDetailUseCase = getSourceDetailUseCase,
                getAskTopicsUseCase = getAskTopicsUseCase,
                getAskSuggestionsUseCase = getAskSuggestionsUseCase,
                streamAskAnswerUseCase = streamAskAnswerUseCase,
                getNotesUseCase = getNotesUseCase,
                getNoteDetailUseCase = getNoteDetailUseCase,
                createNoteUseCase = createNoteUseCase,
                updateNoteUseCase = updateNoteUseCase,
                deleteNoteUseCase = deleteNoteUseCase,
                sourceFileBytesReader = sourceFileBytesReader,
                refreshAuthSessionUseCase = refreshAuthSessionUseCase,
                getCurrentSessionUseCase = getCurrentSessionUseCase,
                retrySourceUseCase = retrySourceUseCase,
            ) as T
        }
    }

    companion object {
        private const val OPEN_SOURCE_DELAY_MS = 500L
        private const val SEARCH_DEBOUNCE_MS = 300L
        private const val CREATE_MIN_DELAY_MS = 1_500L
        private const val LOAD_MIN_DELAY_MS = 1_000L
    }
}
