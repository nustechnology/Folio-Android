package com.nus.folio.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nus.folio.domain.model.AskCitation
import com.nus.folio.domain.model.AskConversation
import com.nus.folio.domain.model.AskConversationPaging
import com.nus.folio.domain.model.Note
import com.nus.folio.domain.model.NoteFilter
import com.nus.folio.domain.model.NotePaging
import com.nus.folio.domain.model.NoteSort
import com.nus.folio.domain.model.Source
import com.nus.folio.domain.model.SourceFilter
import com.nus.folio.domain.model.SourcePaging
import com.nus.folio.domain.model.SourceSort
import com.nus.folio.domain.model.SpacePaging
import com.nus.folio.domain.model.toApiOrigin
import com.nus.folio.domain.model.toApiSourceType
import com.nus.folio.domain.repository.SourceFileBytesReader
import com.nus.folio.domain.usecase.ConvertNoteToSourceUseCase
import com.nus.folio.domain.usecase.CreateNoteUseCase
import com.nus.folio.domain.usecase.CreateSourceUseCase
import com.nus.folio.domain.usecase.DeleteAskConversationUseCase
import com.nus.folio.domain.usecase.DeleteNoteUseCase
import com.nus.folio.domain.usecase.DeleteSourceUseCase
import com.nus.folio.domain.usecase.GetAskConversationUseCase
import com.nus.folio.domain.usecase.GetAskConversationsUseCase
import com.nus.folio.domain.usecase.GetAskSuggestionsUseCase
import com.nus.folio.domain.usecase.GetCurrentSessionUseCase
import com.nus.folio.domain.usecase.GetNotebookUseCase
import com.nus.folio.domain.usecase.GetNoteDetailUseCase
import com.nus.folio.domain.usecase.GetNotesUseCase
import com.nus.folio.domain.usecase.GetSpacesUseCase
import com.nus.folio.domain.usecase.SaveNotebookUseCase
import com.nus.folio.domain.usecase.GetSourceDetailUseCase
import com.nus.folio.domain.usecase.GetSourcesUseCase
import com.nus.folio.domain.usecase.ObserveSourceProcessingUseCase
import com.nus.folio.domain.usecase.RefreshAuthSessionUseCase
import com.nus.folio.domain.usecase.RetrySourceUseCase
import com.nus.folio.domain.usecase.StreamAskAnswerUseCase
import com.nus.folio.domain.usecase.SubmitAskFeedbackUseCase
import com.nus.folio.domain.usecase.UpdateAskConversationUseCase
import com.nus.folio.domain.usecase.UpdateNoteUseCase
import com.nus.folio.domain.usecase.UpdateSourceUseCase
import com.nus.folio.domain.util.AddSourceInputRules
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
 * [HomeNotesDelegate], and [HomeNotebookDelegate].
 */
class HomeViewModel(
    private val spaceId: String,
    spaceTitle: String,
    researchObjective: String = "",
    private val getSourcesUseCase: GetSourcesUseCase,
    private val createSourceUseCase: CreateSourceUseCase,
    private val observeSourceProcessingUseCase: ObserveSourceProcessingUseCase,
    private val updateSourceUseCase: UpdateSourceUseCase,
    private val deleteSourceUseCase: DeleteSourceUseCase,
    private val getSourceDetailUseCase: GetSourceDetailUseCase,
    private val getAskSuggestionsUseCase: GetAskSuggestionsUseCase,
    private val getAskConversationsUseCase: GetAskConversationsUseCase,
    private val getAskConversationUseCase: GetAskConversationUseCase,
    private val updateAskConversationUseCase: UpdateAskConversationUseCase,
    private val deleteAskConversationUseCase: DeleteAskConversationUseCase,
    private val streamAskAnswerUseCase: StreamAskAnswerUseCase,
    private val submitAskFeedbackUseCase: SubmitAskFeedbackUseCase,
    private val getNotesUseCase: GetNotesUseCase,
    private val getNoteDetailUseCase: GetNoteDetailUseCase,
    private val createNoteUseCase: CreateNoteUseCase,
    private val updateNoteUseCase: UpdateNoteUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase,
    private val convertNoteToSourceUseCase: ConvertNoteToSourceUseCase,
    private val getNotebookUseCase: GetNotebookUseCase,
    private val saveNotebookUseCase: SaveNotebookUseCase,
    private val getSpacesUseCase: GetSpacesUseCase,
    private val sourceFileBytesReader: SourceFileBytesReader,
    private val refreshAuthSessionUseCase: RefreshAuthSessionUseCase,
    private val getCurrentSessionUseCase: GetCurrentSessionUseCase,
    private val retrySourceUseCase: RetrySourceUseCase,
    private val openSourceDelayMs: Long = OPEN_SOURCE_DELAY_MS,
    private val searchDebounceMs: Long = SEARCH_DEBOUNCE_MS,
    private val createMinDelayMs: Long = CREATE_MIN_DELAY_MS,
    private val createNoteMinDelayMs: Long = CREATE_NOTE_MIN_DELAY_MS,
    private val loadMinDelayMs: Long = LOAD_MIN_DELAY_MS,
    private val filterSkeletonMinDelayMs: Long = FILTER_SKELETON_MIN_DELAY_MS,
    private val notebookSaveDebounceMs: Long = NOTEBOOK_SAVE_DEBOUNCE_MS,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        HomeUiState(
            spaceId = spaceId,
            spaceTitle = spaceTitle,
            spaceResearchObjective = researchObjective,
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
        getAskConversationsUseCase = getAskConversationsUseCase,
        getAskConversationUseCase = getAskConversationUseCase,
        updateAskConversationUseCase = updateAskConversationUseCase,
        deleteAskConversationUseCase = deleteAskConversationUseCase,
        submitAskFeedbackUseCase = submitAskFeedbackUseCase,
        createNoteUseCase = createNoteUseCase,
        searchDebounceMs = searchDebounceMs,
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
        sourceFileBytesReader = sourceFileBytesReader,
        getCurrentSessionUseCase = getCurrentSessionUseCase,
        openSourceDelayMs = openSourceDelayMs,
        searchDebounceMs = searchDebounceMs,
        createMinDelayMs = createMinDelayMs,
        filterSkeletonMinDelayMs = filterSkeletonMinDelayMs,
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
        convertNoteToSourceUseCase = convertNoteToSourceUseCase,
        onSourceCreated = sources::onSourceCreated,
        searchDebounceMs = searchDebounceMs,
        createMinDelayMs = createNoteMinDelayMs,
        filterSkeletonMinDelayMs = filterSkeletonMinDelayMs,
    )

    private val notebook = HomeNotebookDelegate(
        spaceId = spaceId,
        state = _uiState,
        scope = viewModelScope,
        getNotebookUseCase = getNotebookUseCase,
        saveNotebookUseCase = saveNotebookUseCase,
        saveDebounceMs = notebookSaveDebounceMs,
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
                    notesError = null,
                    askConversationsError = null,
                    notebookError = null,
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
                it.copy(
                    isLoading = true,
                    sourcesError = null,
                    notesError = null,
                    askConversationsError = null,
                    notebookError = null,
                )
            }

            val loadingStartedAt = System.currentTimeMillis()
            val state = _uiState.value
            val sourcesDeferred = async {
                getSourcesUseCase(
                    spaceId = spaceId,
                    sourceType = state.selectedFilter.toApiSourceType(),
                    search = state.searchQuery.trim().takeIf { it.isNotEmpty() },
                    sort = state.selectedSort,
                    page = SourcePaging.DEFAULT_PAGE,
                    limit = SourcePaging.DEFAULT_LIMIT,
                )
            }
            val notesDeferred = async {
                getNotesUseCase(
                    spaceId = spaceId,
                    search = state.searchQuery.trim().takeIf { it.isNotEmpty() },
                    sort = state.selectedNoteSort,
                    origin = state.selectedNoteFilter.toApiOrigin(),
                    page = NotePaging.DEFAULT_PAGE,
                    limit = NotePaging.DEFAULT_LIMIT,
                )
            }
            val conversationsDeferred = async {
                getAskConversationsUseCase(
                    spaceId = spaceId,
                    search = state.searchQuery.trim().takeIf { it.isNotEmpty() },
                    page = AskConversationPaging.DEFAULT_PAGE,
                    limit = AskConversationPaging.DEFAULT_LIMIT,
                )
            }
            val spaceDeferred = async {
                resolveSpaceResearchObjective()
            }

            val sourcesResult = sourcesDeferred.await()
            val notesResult = notesDeferred.await()
            val conversationsResult = conversationsDeferred.await()
            spaceDeferred.await()

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
                            sourcesCurrentPage = library.page,
                            sourcesHasMore = library.hasMore,
                            isLoadingMoreSources = false,
                        )
                    },
                    onFailure = { throwable ->
                        next.copy(sourcesError = throwable.message.orEmpty())
                    },
                )

                next = notesResult.fold(
                    onSuccess = { library ->
                        next.copy(
                            notesError = null,
                            allNotes = library.notes,
                            notesAllCount = library.allCount,
                            notesUserCreatedCount = library.userCreatedCount,
                            notesSavedAnswerCount = library.savedAnswerCount,
                            notesCurrentPage = library.page,
                            notesHasMore = library.hasMore,
                            isLoadingMoreNotes = false,
                        )
                    },
                    onFailure = { throwable ->
                        next.copy(notesError = throwable.message.orEmpty())
                    },
                )

                next = conversationsResult.fold(
                    onSuccess = { library ->
                        next.copy(
                            askConversationsError = null,
                            askConversations = library.conversations,
                            askConversationsCurrentPage = library.page,
                            askConversationsHasMore = library.hasMore,
                            isRefreshingAskConversations = false,
                            isLoadingMoreAskConversations = false,
                            isSearchingAskConversations = false,
                        )
                    },
                    onFailure = {
                        next.copy(askConversationsError = "")
                    },
                )

                next.copy(
                    visibleSources = next.allSources,
                    visibleNotes = filterNotes(next),
                )
            }
            notebook.loadNotebook()
        }
    }

    private suspend fun resolveSpaceResearchObjective() {
        if (_uiState.value.spaceResearchObjective.isNotBlank()) return
        val titleQuery = _uiState.value.spaceTitle.trim().takeIf { it.isNotEmpty() }
        val space = getSpacesUseCase(
            searchQuery = null,
            page = SpacePaging.DEFAULT_PAGE,
            limit = SpacePaging.DEFAULT_LIMIT,
        ).getOrNull()?.spaces?.firstOrNull { it.id == spaceId }
            ?: titleQuery?.let { query ->
                getSpacesUseCase(
                    searchQuery = query,
                    page = SpacePaging.DEFAULT_PAGE,
                    limit = SpacePaging.DEFAULT_LIMIT,
                ).getOrNull()?.spaces?.firstOrNull { it.id == spaceId }
            }
            ?: return
        val objective = space.description.trim()
        if (objective.isEmpty()) return
        _uiState.update { current ->
            current.copy(
                spaceResearchObjective = objective,
                spaceTitle = current.spaceTitle.ifBlank { space.title },
            )
        }
    }

    fun onResearchObjectiveAvailable(researchObjective: String) {
        val trimmed = researchObjective.trim()
        if (trimmed.isEmpty()) return
        if (_uiState.value.spaceResearchObjective == trimmed) return
        _uiState.update { it.copy(spaceResearchObjective = trimmed) }
        notebook.ensureDefaultContent()
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { state ->
            val next = state.copy(
                searchQuery = query,
                isSearchingNotes = state.selectedTab == HomeTab.NOTES,
                isSearchingAskConversations = state.selectedTab == HomeTab.ASK,
            )
            next.copy(
                visibleNotes = filterNotes(next),
                visibleSources = next.allSources,
            )
        }
        when (_uiState.value.selectedTab) {
            HomeTab.SOURCES -> sources.scheduleSearchReload()
            HomeTab.NOTES -> notes.scheduleSearchReload()
            HomeTab.ASK -> ask.scheduleSearchReload()
            HomeTab.NOTEBOOK -> Unit
        }
    }

    /** Clears the search field and reloads unfiltered lists (e.g. when leaving Home). */
    fun clearSearch() {
        sources.cancelSearchJob()
        notes.cancelSearchJob()
        ask.cancelSearchJob()
        val previousQuery = _uiState.value.searchQuery
        if (previousQuery.isBlank()) return
        val previousTab = _uiState.value.selectedTab
        _uiState.update { state ->
            val next = state.copy(
                searchQuery = "",
                isSearchingNotes = false,
                isSearchingAskConversations = false,
            )
            next.copy(
                visibleSources = next.allSources,
                visibleNotes = filterNotes(next),
            )
        }
        when (previousTab) {
            HomeTab.SOURCES -> sources.loadSourcesOnly()
            HomeTab.NOTES -> notes.loadNotesOnly()
            HomeTab.ASK -> ask.loadConversationsOnly()
            HomeTab.NOTEBOOK -> Unit
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

    fun onLoadMoreSources() = sources.onLoadMore()

    fun onLoadMoreConversations() = ask.onLoadMore()

    fun onRefreshNotes() = notes.onRefreshNotes()

    fun onTabSelected(tab: HomeTab) {
        sources.cancelSearchJob()
        notes.cancelSearchJob()
        ask.cancelSearchJob()
        val previousQuery = _uiState.value.searchQuery
        val previousTab = _uiState.value.selectedTab
        val wasAskChatOpen = _uiState.value.isAskChatOpen
        _uiState.update { state ->
            val next = state.copy(
                selectedTab = tab,
                searchQuery = "",
                isSearchingAskConversations = false,
            )
            next.copy(
                visibleSources = next.allSources,
                visibleNotes = filterNotes(next),
            )
        }
        // Exit open Ask chat when leaving or selecting Ask so header matches the tab.
        // Do this before search-clear reloads: onAskChatBack already loads conversations.
        if (previousTab == HomeTab.ASK || tab == HomeTab.ASK) {
            ask.onAskChatBack()
        }
        if (previousQuery.isNotBlank()) {
            // Search was cleared — reload the tab that owned the query.
            if (previousTab == HomeTab.SOURCES) {
                sources.loadSourcesOnly()
            }
            if (previousTab == HomeTab.NOTES) {
                notes.loadNotesOnly()
            }
            // Ask chat-open path already reloaded via onAskChatBack; avoid a second job.
            if (previousTab == HomeTab.ASK && !wasAskChatOpen) {
                ask.loadConversationsOnly()
            }
        }
        if (tab == HomeTab.NOTEBOOK) {
            notebook.loadNotebook()
            notebook.ensureDefaultContent()
        }
    }

    fun onAddSourceClick() = sources.onAddSourceClick()

    fun onAddSourceSheetDismiss() = sources.onAddSourceSheetDismiss()

    fun onAddSourceFileSelected() = sources.onAddSourceFileSelected()

    fun onAddSourceFileSelectionFailed(error: AddSourceInputRules.FileValidationError) =
        sources.onAddSourceFileSelectionFailed(error)

    fun onAddSourceSubmit(draft: AddSourceDraft) = sources.onAddSourceSubmit(draft)

    fun onSourceProcessingDismiss() = sources.onSourceProcessingDismiss()

    fun onSourceProcessingOpenSource() = sources.onSourceProcessingOpenSource()

    fun onSourceProcessingAsk() = sources.onSourceProcessingAsk()

    fun onSourceProcessingRetry() = sources.onSourceProcessingRetry()

    fun onAddNoteClick() = notes.onAddNoteClick()

    fun onAddNoteSheetDismiss() = notes.onAddNoteSheetDismiss()

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

    fun onAskSaveAsNoteConfirm(title: String, content: String) =
        ask.onAskSaveAsNoteConfirm(title, content)

    fun onAskFeedback(messageId: String, useful: Boolean) = ask.onAskFeedback(messageId, useful)

    fun onAskCitationClick(citation: AskCitation) = ask.onAskCitationClick(citation)

    fun onCitationPreviewDismiss() = ask.onCitationPreviewDismiss()

    fun onCitationOpenInSource() = ask.onCitationOpenInSource()

    fun onInfoToastShown() {
        _uiState.update { it.copy(infoToast = null) }
    }

    fun onAskScopeOptionSelected(sourceId: String?) = ask.onAskScopeOptionSelected(sourceId)

    fun onNewConversation() = ask.onNewConversation()

    fun onAskChatBack() = ask.onAskChatBack()

    fun onRefreshConversations() = ask.onRefreshConversations()

    fun onConversationClick(conversation: AskConversation) = ask.onConversationClick(conversation)

    fun onConversationOptionsClick(conversation: AskConversation) =
        ask.onConversationOptionsClick(conversation)

    fun onConversationOptionsDismiss() = ask.onConversationOptionsDismiss()

    fun onRenameConversationClick() = ask.onRenameConversationClick()

    fun onRenameConversationDismiss() = ask.onRenameConversationDismiss()

    fun onRenameConversationSave(title: String) = ask.onRenameConversationSave(title)

    fun onDeleteConversationClick() = ask.onDeleteConversationClick()

    fun onDeleteConversationDismiss() = ask.onDeleteConversationDismiss()

    fun onDeleteConversationConfirm() = ask.onDeleteConversationConfirm()

    fun onAskSourceSelected(sourceId: String) = ask.onAskSourceSelected(sourceId)

    fun onNotebookAddClick() = notebook.onNotebookAddClick()

    fun onNotebookActionsDismiss() = notebook.onNotebookActionsDismiss()

    fun onCopyNotebookClick() = notebook.onCopyNotebookClick()

    fun onExportNotebookClick() = notebook.onExportNotebookClick()

    fun onNotebookExportDismiss() = notebook.onNotebookExportDismiss()

    fun onNotebookExportConfirm(
        format: NotebookExportFormat,
    ) = notebook.onNotebookExportConfirm(format)

    fun onNotebookContentChange(content: String) = notebook.onNotebookContentChange(content)

    fun onRetryNotebookSave() = notebook.retryNotebookSave()

    fun onRetryNotebookLoad() = notebook.loadNotebook()

    fun onPendingNotebookCopyHandled() = notebook.onPendingNotebookCopyHandled()

    fun onPendingNotebookExportHandled() = notebook.onPendingNotebookExportHandled()

    fun onNotebookExportSucceeded() = notebook.onNotebookExportSucceeded()

    fun onNotebookExportPickerLaunched() = notebook.onNotebookExportPickerLaunched()

    fun onNotebookExportFailed() = notebook.onNotebookExportFailed()

    fun onNotebookPrintSubmitted() = notebook.onNotebookPrintSubmitted()

    fun onNotebookPrintAdapterInvalidated() = notebook.onNotebookPrintAdapterInvalidated()

    fun onPendingNotebookPrintHandled() = notebook.onPendingNotebookPrintHandled()

    fun onSourceOptionsClick(source: Source) = sources.onSourceOptionsClick(source)

    fun onSourceOptionsDismiss() = sources.onSourceOptionsDismiss()

    fun onEditSourceClick(source: Source) = sources.onEditSourceClick(source)

    fun onSourceClick(source: Source) = sources.onSourceClick(source)

    fun onOpenSourceDetailHandled() = sources.onOpenSourceDetailHandled()

    fun onEditSourceDismiss() = sources.onEditSourceDismiss()

    fun onEditSourceSave(title: String, author: String) =
        sources.onEditSourceSave(title, author)

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
        private val researchObjective: String = "",
        private val getSourcesUseCase: GetSourcesUseCase,
        private val createSourceUseCase: CreateSourceUseCase,
        private val observeSourceProcessingUseCase: ObserveSourceProcessingUseCase,
        private val updateSourceUseCase: UpdateSourceUseCase,
        private val deleteSourceUseCase: DeleteSourceUseCase,
        private val getSourceDetailUseCase: GetSourceDetailUseCase,
        private val getAskSuggestionsUseCase: GetAskSuggestionsUseCase,
        private val getAskConversationsUseCase: GetAskConversationsUseCase,
        private val getAskConversationUseCase: GetAskConversationUseCase,
        private val updateAskConversationUseCase: UpdateAskConversationUseCase,
        private val deleteAskConversationUseCase: DeleteAskConversationUseCase,
        private val streamAskAnswerUseCase: StreamAskAnswerUseCase,
        private val submitAskFeedbackUseCase: SubmitAskFeedbackUseCase,
        private val getNotesUseCase: GetNotesUseCase,
        private val getNoteDetailUseCase: GetNoteDetailUseCase,
        private val createNoteUseCase: CreateNoteUseCase,
        private val updateNoteUseCase: UpdateNoteUseCase,
        private val deleteNoteUseCase: DeleteNoteUseCase,
        private val convertNoteToSourceUseCase: ConvertNoteToSourceUseCase,
        private val getNotebookUseCase: GetNotebookUseCase,
        private val saveNotebookUseCase: SaveNotebookUseCase,
        private val getSpacesUseCase: GetSpacesUseCase,
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
                researchObjective = researchObjective,
                getSourcesUseCase = getSourcesUseCase,
                createSourceUseCase = createSourceUseCase,
                observeSourceProcessingUseCase = observeSourceProcessingUseCase,
                updateSourceUseCase = updateSourceUseCase,
                deleteSourceUseCase = deleteSourceUseCase,
                getSourceDetailUseCase = getSourceDetailUseCase,
                getAskSuggestionsUseCase = getAskSuggestionsUseCase,
                getAskConversationsUseCase = getAskConversationsUseCase,
                getAskConversationUseCase = getAskConversationUseCase,
                updateAskConversationUseCase = updateAskConversationUseCase,
                deleteAskConversationUseCase = deleteAskConversationUseCase,
                streamAskAnswerUseCase = streamAskAnswerUseCase,
                submitAskFeedbackUseCase = submitAskFeedbackUseCase,
                getNotesUseCase = getNotesUseCase,
                getNoteDetailUseCase = getNoteDetailUseCase,
                createNoteUseCase = createNoteUseCase,
                updateNoteUseCase = updateNoteUseCase,
                deleteNoteUseCase = deleteNoteUseCase,
                convertNoteToSourceUseCase = convertNoteToSourceUseCase,
                getNotebookUseCase = getNotebookUseCase,
                saveNotebookUseCase = saveNotebookUseCase,
                getSpacesUseCase = getSpacesUseCase,
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
        private const val NOTEBOOK_SAVE_DEBOUNCE_MS = 600L
        private const val CREATE_MIN_DELAY_MS = 1_500L
        private const val CREATE_NOTE_MIN_DELAY_MS = 1_000L
        private const val LOAD_MIN_DELAY_MS = 1_000L
        private const val FILTER_SKELETON_MIN_DELAY_MS = 500L
    }
}
