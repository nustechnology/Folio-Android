package com.nus.folio.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nus.folio.domain.model.AskTopic
import com.nus.folio.domain.model.CreateSourceRequest
import com.nus.folio.domain.model.Note
import com.nus.folio.domain.model.NoteFilter
import com.nus.folio.domain.model.Source
import com.nus.folio.domain.model.SourceFilter
import com.nus.folio.domain.model.SourceProcessingState
import com.nus.folio.domain.model.SourceSort
import com.nus.folio.domain.model.SourceType
import com.nus.folio.domain.model.toApiSourceType
import com.nus.folio.domain.repository.SourceFileBytesReader
import com.nus.folio.domain.usecase.CreateSourceUseCase
import com.nus.folio.domain.usecase.DeleteNoteUseCase
import com.nus.folio.domain.usecase.DeleteSourceUseCase
import com.nus.folio.domain.usecase.GetAskTopicsUseCase
import com.nus.folio.domain.usecase.GetCurrentSessionUseCase
import com.nus.folio.domain.usecase.GetNotesUseCase
import com.nus.folio.domain.usecase.GetSourceDetailUseCase
import com.nus.folio.domain.usecase.GetSourcesUseCase
import com.nus.folio.domain.usecase.ObserveSourceProcessingUseCase
import com.nus.folio.domain.usecase.RefreshAuthSessionUseCase
import com.nus.folio.domain.usecase.RetrySourceUseCase
import com.nus.folio.domain.usecase.UpdateNoteUseCase
import com.nus.folio.domain.usecase.UpdateSourceUseCase
import com.nus.folio.domain.util.AddSourceInputRules
import com.nus.folio.presentation.home.bottomsheet.AddSourceDraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    private val getNotesUseCase: GetNotesUseCase,
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
        ),
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var openSourceJob: Job? = null
    private var processingObserveJob: Job? = null
    /** In-flight sources reload (search debounce, filter/sort, pull-to-refresh, tab clear). */
    private var sourcesLoadJob: Job? = null

    init {
        loadHome()
    }

    fun loadSources() {
        loadSourcesOnly()
    }

    fun onRefreshSources() {
        if (_uiState.value.isRefreshingSources) return
        sourcesLoadJob?.cancel()
        sourcesLoadJob = viewModelScope.launch {
            _uiState.update { it.copy(isRefreshingSources = true) }
            try {
                reloadSources()
            } finally {
                _uiState.update { it.copy(isRefreshingSources = false) }
            }
        }
    }

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
            val notesDeferred = async { getNotesUseCase(spaceId) }

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

    private fun loadSourcesOnly() {
        sourcesLoadJob?.cancel()
        sourcesLoadJob = viewModelScope.launch {
            reloadSources()
        }
    }

    private suspend fun reloadSources() {
        val state = _uiState.value
        getSourcesUseCase(
            spaceId = spaceId,
            sourceType = state.selectedFilter.toApiSourceType(),
            search = state.searchQuery.trim().takeIf { it.isNotEmpty() },
            sort = state.selectedSort,
        ).fold(
            onSuccess = { library ->
                _uiState.update { current ->
                    val next = current.copy(
                        sourcesError = null,
                        allSources = library.sources,
                        allCount = library.allCount,
                    )
                    next.copy(visibleSources = next.allSources)
                }
            },
            onFailure = { throwable ->
                _uiState.update {
                    it.copy(sourcesError = throwable.message.orEmpty())
                }
            },
        )
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { state ->
            val next = state.copy(searchQuery = query)
            next.copy(
                visibleAskTopics = filterAskTopics(next),
                visibleNotes = filterNotes(next),
                visibleSources = next.allSources,
            )
        }
        if (_uiState.value.selectedTab != HomeTab.SOURCES) return
        sourcesLoadJob?.cancel()
        sourcesLoadJob = viewModelScope.launch {
            delay(searchDebounceMs)
            reloadSources()
        }
    }

    fun onFilterSelected(filter: SourceFilter) {
        if (filter == _uiState.value.selectedFilter) return
        _uiState.update { it.copy(selectedFilter = filter) }
        loadSourcesOnly()
    }

    fun onFilterSortClick() {
        _uiState.update { it.copy(showSortSheet = true) }
    }

    fun onSortSheetDismiss() {
        _uiState.update { it.copy(showSortSheet = false) }
    }

    fun onSortSelected(sort: SourceSort) {
        if (sort == _uiState.value.selectedSort) {
            _uiState.update { it.copy(showSortSheet = false) }
            return
        }
        _uiState.update { it.copy(showSortSheet = false, selectedSort = sort) }
        loadSourcesOnly()
    }

    fun onNoteFilterSelected(filter: NoteFilter) {
        _uiState.update { state ->
            val next = state.copy(selectedNoteFilter = filter)
            next.copy(visibleNotes = filterNotes(next))
        }
    }

    fun onTabSelected(tab: HomeTab) {
        val previousQuery = _uiState.value.searchQuery
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
            // Search was cleared — reload sources without the query.
            loadSourcesOnly()
        } else {
            // Drop any pending debounced search for the Sources tab.
            sourcesLoadJob?.cancel()
            sourcesLoadJob = null
        }
    }

    fun onAddSourceClick() {
        if (_uiState.value.isCreatingSource) return
        _uiState.update { it.copy(showAddSourceSheet = true) }
    }

    fun onAddSourceSheetDismiss() {
        if (_uiState.value.isCreatingSource) return
        _uiState.update { it.copy(showAddSourceSheet = false) }
    }

    fun onAddSourceSubmit(draft: AddSourceDraft) {
        if (_uiState.value.isCreatingSource) return

        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingSource = true) }

            val request = try {
                buildCreateRequest(draft)
            } catch (cancelled: CancellationException) {
                _uiState.update { it.copy(isCreatingSource = false) }
                throw cancelled
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isCreatingSource = false,
                        actionError = error.toHomeActionError(),
                    )
                }
                return@launch
            }

            val title = when (draft) {
                is AddSourceDraft.File -> draft.displayName.ifBlank { "Untitled file" }
                is AddSourceDraft.Web -> draft.title.ifBlank { draft.url }
                is AddSourceDraft.Text ->
                    draft.title.ifBlank { AddSourceInputRules.defaultManualTitle() }
            }

            val result = coroutineScope {
                val createDeferred = async { createSourceUseCase(request) }
                delay(createMinDelayMs)
                createDeferred.await()
            }

            result
                .onSuccess { created ->
                    val displayTitle = created.title.ifBlank { title }
                    _uiState.update {
                        it.copy(
                            isCreatingSource = false,
                            showAddSourceSheet = false,
                            processingSourceId = created.id,
                            processingSourceTitle = displayTitle,
                            processingProgress = 0,
                            processingState = SourceProcessingState.ADDED,
                            userMessage = HomeUserMessage.SOURCE_CREATED,
                        )
                    }
                    // Observe before refresh: release SharedFlow has no replay, so
                    // events emitted during reload would otherwise be dropped.
                    startObservingProcessing(created.id)
                    refreshSourcesAfterCreate()
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isCreatingSource = false,
                            actionError = throwable.toHomeActionError(),
                        )
                    }
                }
        }
    }

    private suspend fun startObservingProcessing(sourceId: String) {
        processingObserveJob?.cancel()
        val subscribed = CompletableDeferred<Unit>()
        processingObserveJob = viewModelScope.launch {
            try {
                observeSourceProcessingUseCase()
                    .onStart { subscribed.complete(Unit) }
                    .filter { it.sourceId == sourceId }
                    .transformWhile { event ->
                        emit(event)
                        !event.isTerminal
                    }
                    .collect { event ->
                        _uiState.update {
                            it.copy(
                                processingProgress = event.progress,
                                processingState = event.state,
                            )
                        }
                        if (event.isTerminal) {
                            refreshSourcesAfterCreate()
                        }
                    }
            } catch (cancelled: CancellationException) {
                subscribed.complete(Unit)
                throw cancelled
            } catch (_: Exception) {
                subscribed.complete(Unit)
                // Keep last known progress; user can dismiss the sheet.
            }
        }
        subscribed.await()
    }

    private fun stopProcessingObservation() {
        processingObserveJob?.cancel()
        processingObserveJob = null
    }

    private suspend fun buildCreateRequest(draft: AddSourceDraft): CreateSourceRequest =
        when (draft) {
            is AddSourceDraft.Web -> {
                val url = draft.url.trim()
                CreateSourceRequest.Web(
                    spaceId = spaceId,
                    sourceUrl = url,
                    // Blank title: backend fetches HTML <title> during processing.
                    title = AddSourceInputRules.limitTitle(draft.title.trim()),
                    author = AddSourceInputRules.limitAuthor(
                        draft.author.trim().ifBlank {
                            AddSourceInputRules.defaultWebAuthor(url)
                        },
                    ),
                )
            }
            is AddSourceDraft.Text -> {
                val session = getCurrentSessionUseCase()
                CreateSourceRequest.Manual(
                    spaceId = spaceId,
                    title = AddSourceInputRules.limitTitle(
                        draft.title.trim().ifBlank {
                            AddSourceInputRules.defaultManualTitle()
                        },
                    ),
                    author = AddSourceInputRules.limitAuthor(
                        draft.author.trim().ifBlank {
                            AddSourceInputRules.currentUserDisplayName(
                                displayName = session?.displayName,
                                email = session?.email,
                            )
                        },
                    ),
                    content = draft.content,
                )
            }
            is AddSourceDraft.File -> {
                val uri = draft.uri?.toString()?.takeIf { it.isNotBlank() }
                    ?: throw IllegalArgumentException(ERROR_FILE_REQUIRED)
                val file = withContext(Dispatchers.IO) { sourceFileBytesReader.read(uri) }
                if (!AddSourceInputRules.isSupportedExtension(file.fileName)) {
                    throw IllegalArgumentException(ERROR_FILE_UNSUPPORTED)
                }
                if (file.bytes.size > AddSourceInputRules.MAX_FILE_BYTES) {
                    throw IllegalArgumentException(ERROR_FILE_TOO_LARGE)
                }
                val session = getCurrentSessionUseCase()
                val author = AddSourceInputRules.limitAuthor(
                    draft.author.trim().ifBlank {
                        AddSourceInputRules.currentUserDisplayName(
                            displayName = session?.displayName,
                            email = session?.email,
                        )
                    },
                )
                CreateSourceRequest.File(
                    spaceId = spaceId,
                    title = draft.displayName.trim().ifBlank { file.fileName },
                    author = author,
                    fileName = file.fileName,
                    mimeType = file.mimeType,
                    bytes = file.bytes,
                )
            }
        }

    private suspend fun refreshSourcesAfterCreate() {
        sourcesLoadJob?.cancel()
        val job = viewModelScope.launch {
            reloadSources()
        }
        sourcesLoadJob = job
        job.join()
    }

    fun onSourceProcessingDismiss() {
        stopProcessingObservation()
        _uiState.update {
            it.copy(
                processingSourceId = null,
                processingSourceTitle = null,
                processingProgress = 0,
                processingState = null,
            )
        }
    }

    fun onSourceProcessingOpenSource() {
        val sourceId = _uiState.value.processingSourceId
        stopProcessingObservation()
        _uiState.update {
            it.copy(
                processingSourceId = null,
                processingSourceTitle = null,
                processingProgress = 0,
                processingState = null,
                openSourceDetailId = sourceId,
            )
        }
    }

    fun onSourceProcessingAsk() {
        stopProcessingObservation()
        _uiState.update {
            it.copy(
                processingSourceId = null,
                processingSourceTitle = null,
                processingProgress = 0,
                processingState = null,
                selectedTab = HomeTab.ASK,
            )
        }
    }

    fun onSourceProcessingRetry() {
        val sourceId = _uiState.value.processingSourceId ?: return
        viewModelScope.launch {
            retrySourceUseCase(sourceId)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            processingProgress = 0,
                            processingState = SourceProcessingState.ADDED,
                        )
                    }
                    startObservingProcessing(sourceId)
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(actionError = throwable.toHomeActionError())
                    }
                }
        }
    }

    fun onAddNoteSubmit(
        @Suppress("UNUSED_PARAMETER") title: String,
        @Suppress("UNUSED_PARAMETER") content: String,
    ) {
        _uiState.update {
            it.copy(userMessage = HomeUserMessage.ADD_NOTE_NOT_SUPPORTED)
        }
    }

    fun onAskSubmit() {
        _uiState.update {
            it.copy(userMessage = HomeUserMessage.ASK_NOT_SUPPORTED)
        }
    }

    fun onAskScopeSelected(scope: AskScope) {
        _uiState.update { it.copy(askScope = scope) }
    }

    fun onAskSourceSelected(sourceId: String) {
        _uiState.update {
            it.copy(
                askSourceId = sourceId,
                askScope = AskScope.CURRENT_SOURCE,
            )
        }
    }

    fun onNotebookAddClick() {
        _uiState.update { it.copy(showNotebookActions = true) }
    }

    fun onNotebookActionsDismiss() {
        _uiState.update { it.copy(showNotebookActions = false) }
    }

    fun onCopyNotebookClick() {
        _uiState.update {
            it.copy(
                showNotebookActions = false,
                userMessage = HomeUserMessage.COPY_NOTEBOOK_NOT_SUPPORTED,
            )
        }
    }

    fun onExportNotebookClick() {
        _uiState.update {
            it.copy(
                showNotebookActions = false,
                showNotebookExport = true,
            )
        }
    }

    fun onNotebookExportDismiss() {
        _uiState.update { it.copy(showNotebookExport = false) }
    }

    fun onNotebookExportConfirm(
        @Suppress("UNUSED_PARAMETER") format: NotebookExportFormat,
    ) {
        _uiState.update {
            it.copy(
                showNotebookExport = false,
                userMessage = HomeUserMessage.EXPORT_NOTEBOOK_NOT_SUPPORTED,
            )
        }
    }

    fun onSourceOptionsClick(source: Source) {
        _uiState.update { it.copy(optionsSource = source) }
    }

    fun onSourceOptionsDismiss() {
        _uiState.update { it.copy(optionsSource = null) }
    }

    fun onEditSourceClick(source: Source) {
        _uiState.update { it.copy(optionsSource = null) }
        if (source.type != SourceType.TEXT) {
            _uiState.update {
                it.copy(editingSource = source, editingSourceContent = "")
            }
            return
        }
        viewModelScope.launch {
            val content = getSourceDetailUseCase(spaceId, source.id)
                .getOrNull()
                ?.plainContent
                .orEmpty()
            _uiState.update {
                it.copy(editingSource = source, editingSourceContent = content)
            }
        }
    }

    fun onSourceClick(source: Source) {
        if (_uiState.value.isOpeningSource) return
        openSourceJob?.cancel()
        openSourceJob = viewModelScope.launch {
            _uiState.update {
                it.copy(isOpeningSource = true, openSourceDetailId = null)
            }
            delay(openSourceDelayMs)
            _uiState.update {
                it.copy(isOpeningSource = false, openSourceDetailId = source.id)
            }
        }
    }

    fun onOpenSourceDetailHandled() {
        _uiState.update { it.copy(openSourceDetailId = null) }
    }

    fun onEditSourceDismiss() {
        _uiState.update { it.copy(editingSource = null, editingSourceContent = "") }
    }

    fun onEditSourceSave(title: String, author: String, content: String = "") {
        if (title.isBlank()) return
        val editing = _uiState.value.editingSource ?: return
        val trimmedTitle = AddSourceInputRules.limitTitle(title.trim())
        val trimmedAuthor = AddSourceInputRules.limitAuthor(author.trim())
        val contentToSend = if (editing.type == SourceType.TEXT) {
            if (!AddSourceInputRules.isContentValid(content)) return
            content.trim()
        } else {
            null
        }
        val updated = editing.copy(title = trimmedTitle, author = trimmedAuthor)

        viewModelScope.launch {
            updateSourceUseCase(updated, contentToSend)
                .onSuccess { saved ->
                    _uiState.update { state ->
                        val updatedSources = state.allSources.map { source ->
                            if (source.id == saved.id) saved else source
                        }
                        val next = state.copy(
                            allSources = updatedSources,
                            editingSource = null,
                            editingSourceContent = "",
                            userMessage = HomeUserMessage.SOURCE_UPDATED,
                        )
                        next.copy(visibleSources = filterSources(next))
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(actionError = throwable.toHomeActionError())
                    }
                }
        }
    }

    fun onDeleteSourceClick(source: Source) {
        _uiState.update { it.copy(optionsSource = null, deletingSource = source) }
    }

    fun onDeleteSourceDismiss() {
        _uiState.update { it.copy(deletingSource = null) }
    }

    fun onDeleteSourceConfirm() {
        val deleting = _uiState.value.deletingSource ?: return

        viewModelScope.launch {
            deleteSourceUseCase(deleting.id)
                .onSuccess {
                    _uiState.update { state ->
                        val updatedSources = state.allSources.filterNot { it.id == deleting.id }
                        val next = state.copy(
                            allSources = updatedSources,
                            allCount = updatedSources.size,
                            deletingSource = null,
                            userMessage = HomeUserMessage.SOURCE_DELETED,
                        )
                        next.copy(visibleSources = filterSources(next))
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(actionError = throwable.toHomeActionError())
                    }
                }
        }
    }

    fun onNoteClick(note: Note) {
        _uiState.update { it.copy(viewingNote = note) }
    }

    fun onViewNoteDismiss() {
        _uiState.update { it.copy(viewingNote = null) }
    }

    fun onNoteOptionsClick(note: Note) {
        _uiState.update { it.copy(optionsNote = note) }
    }

    fun onNoteOptionsDismiss() {
        _uiState.update { it.copy(optionsNote = null) }
    }

    fun onViewNoteClick() {
        _uiState.update { state ->
            val note = state.optionsNote ?: return@update state
            state.copy(
                optionsNote = null,
                viewingNote = note,
            )
        }
    }

    fun onEditNoteClick() {
        _uiState.update { state ->
            val note = state.viewingNote ?: state.optionsNote ?: return@update state
            state.copy(
                optionsNote = null,
                editingNote = note,
            )
        }
    }

    fun onEditNoteDismiss() {
        _uiState.update { it.copy(editingNote = null, viewingNote = null) }
    }

    fun onEditNoteSave(title: String, content: String) {
        if (title.isBlank() || content.isBlank()) return
        val editing = _uiState.value.editingNote ?: return
        val updated = editing.copy(title = title.trim(), content = content.trim())

        viewModelScope.launch {
            updateNoteUseCase(updated)
                .onSuccess { saved ->
                    _uiState.update { state ->
                        val updatedNotes = state.allNotes.map { note ->
                            if (note.id == saved.id) saved else note
                        }
                        val next = state.copy(
                            allNotes = updatedNotes,
                            editingNote = null,
                            viewingNote = if (state.viewingNote?.id == saved.id) saved else state.viewingNote,
                            userMessage = HomeUserMessage.NOTE_UPDATED,
                        )
                        next.copy(visibleNotes = filterNotes(next))
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(userMessage = HomeUserMessage.EDIT_NOTE_NOT_SUPPORTED)
                    }
                }
        }
    }

    fun onConvertNoteClick() {
        _uiState.update { state ->
            val note = state.viewingNote ?: state.optionsNote ?: return@update state
            state.copy(
                optionsNote = null,
                convertingNote = note,
            )
        }
    }

    fun onConvertNoteDismiss() {
        _uiState.update { it.copy(convertingNote = null, viewingNote = null) }
    }

    fun onConvertNoteCreate(
        @Suppress("UNUSED_PARAMETER") title: String,
        @Suppress("UNUSED_PARAMETER") snapshot: String,
    ) {
        _uiState.update {
            it.copy(
                convertingNote = null,
                viewingNote = null,
                userMessage = HomeUserMessage.CONVERT_NOTE_NOT_SUPPORTED,
            )
        }
    }

    fun onDeleteNoteClick() {
        _uiState.update { state ->
            val note = state.editingNote ?: state.optionsNote ?: return@update state
            state.copy(
                optionsNote = null,
                editingNote = null,
                deletingNote = note,
            )
        }
    }

    fun onDeleteNoteDismiss() {
        _uiState.update { it.copy(deletingNote = null, viewingNote = null) }
    }

    fun onDeleteNoteConfirm() {
        val deleting = _uiState.value.deletingNote ?: return

        viewModelScope.launch {
            deleteNoteUseCase(deleting.id)
                .onSuccess {
                    _uiState.update { state ->
                        val updatedNotes = state.allNotes.filterNot { it.id == deleting.id }
                        val next = state.copy(
                            allNotes = updatedNotes,
                            notesAllCount = updatedNotes.size,
                            notesPinnedCount = updatedNotes.count { it.isPinned },
                            notesUnfiledCount = updatedNotes.count { it.project.isNullOrBlank() },
                            deletingNote = null,
                            viewingNote = null,
                            editingNote = null,
                            userMessage = HomeUserMessage.NOTE_DELETED,
                        )
                        next.copy(visibleNotes = filterNotes(next))
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(userMessage = HomeUserMessage.DELETE_NOTE_NOT_SUPPORTED)
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

    private fun filterSources(state: HomeUiState): List<Source> = state.allSources

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
                note.content.contains(query, ignoreCase = true) ||
                note.project.orEmpty().contains(query, ignoreCase = true)
        }
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
        private val getNotesUseCase: GetNotesUseCase,
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
                getNotesUseCase = getNotesUseCase,
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

        /** Allowlisted validation keys — never shown raw; mapped in [toHomeActionError]. */
        internal const val ERROR_FILE_REQUIRED = "FILE_REQUIRED"
        internal const val ERROR_FILE_UNSUPPORTED = "FILE_UNSUPPORTED"
        internal const val ERROR_FILE_TOO_LARGE = "FILE_TOO_LARGE"
    }
}

/**
 * Maps failures to safe UI codes. Never surfaces [Throwable.message] (CWE-209).
 */
private fun Throwable.toHomeActionError(): HomeActionError = when (this) {
    is java.io.IOException -> HomeActionError.NETWORK
    is IllegalArgumentException -> when (message) {
        HomeViewModel.ERROR_FILE_REQUIRED -> HomeActionError.FILE_REQUIRED
        HomeViewModel.ERROR_FILE_UNSUPPORTED -> HomeActionError.FILE_UNSUPPORTED
        HomeViewModel.ERROR_FILE_TOO_LARGE -> HomeActionError.FILE_TOO_LARGE
        else -> HomeActionError.GENERIC
    }
    else -> HomeActionError.GENERIC
}