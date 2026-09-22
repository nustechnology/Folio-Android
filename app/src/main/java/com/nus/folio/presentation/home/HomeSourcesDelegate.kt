package com.nus.folio.presentation.home

import com.nus.folio.domain.model.CreateSourceRequest
import com.nus.folio.domain.model.Source
import com.nus.folio.domain.model.SourceFilter
import com.nus.folio.domain.model.SourcePaging
import com.nus.folio.domain.model.SourceProcessingState
import com.nus.folio.domain.model.SourceSort
import com.nus.folio.domain.model.SourceStatus
import com.nus.folio.domain.model.SourceType
import com.nus.folio.domain.model.toApiSourceType
import com.nus.folio.domain.repository.SourceFileBytesReader
import com.nus.folio.domain.usecase.CreateSourceUseCase
import com.nus.folio.domain.usecase.DeleteSourceUseCase
import com.nus.folio.domain.usecase.GetCurrentSessionUseCase
import com.nus.folio.domain.usecase.GetSourcesUseCase
import com.nus.folio.domain.usecase.ObserveSourceProcessingUseCase
import com.nus.folio.domain.usecase.RetrySourceUseCase
import com.nus.folio.domain.usecase.UpdateSourceUseCase
import com.nus.folio.domain.util.AddSourceInputRules
import com.nus.folio.presentation.home.bottomsheet.AddSourceDraft
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

/**
 * Owns Sources-tab state: load/reload, add/edit/delete, processing observation,
 * options, open detail, and source filter/sort handlers.
 */
internal class HomeSourcesDelegate(
    private val spaceId: String,
    private val state: MutableStateFlow<HomeUiState>,
    private val scope: CoroutineScope,
    private val getSourcesUseCase: GetSourcesUseCase,
    private val createSourceUseCase: CreateSourceUseCase,
    private val observeSourceProcessingUseCase: ObserveSourceProcessingUseCase,
    private val updateSourceUseCase: UpdateSourceUseCase,
    private val deleteSourceUseCase: DeleteSourceUseCase,
    private val retrySourceUseCase: RetrySourceUseCase,
    private val sourceFileBytesReader: SourceFileBytesReader,
    private val getCurrentSessionUseCase: GetCurrentSessionUseCase,
    private val openSourceDelayMs: Long,
    private val searchDebounceMs: Long,
    private val createMinDelayMs: Long,
    private val filterSkeletonMinDelayMs: Long,
    private val pageLimit: Int = SourcePaging.DEFAULT_LIMIT,
    private val applyAskScope: (sourceId: String?, forceReset: Boolean) -> Unit,
) {

    private var openSourceJob: Job? = null
    private var processingObserveJob: Job? = null
    /** In-flight sources reload (search debounce, filter/sort, pull-to-refresh, load-more). */
    private var sourcesLoadJob: Job? = null

    fun loadSourcesOnly() {
        sourcesLoadJob?.cancel()
        sourcesLoadJob = scope.launch {
            loadSourcesInternal(reset = true)
        }
    }

    fun onRefreshSources() {
        if (state.value.isRefreshingSources) return
        sourcesLoadJob?.cancel()
        sourcesLoadJob = scope.launch {
            state.update {
                it.copy(
                    isRefreshingSources = true,
                    isFilteringSources = false,
                    isLoadingMoreSources = false,
                )
            }
            try {
                loadSourcesInternal(reset = true)
            } finally {
                state.update { it.copy(isRefreshingSources = false) }
            }
        }
    }

    fun onLoadMore() {
        val current = state.value
        if (
            current.isLoading ||
            current.isRefreshingSources ||
            current.isLoadingMoreSources ||
            current.isFilteringSources ||
            !current.sourcesHasMore ||
            current.selectedTab != HomeTab.SOURCES
        ) {
            return
        }
        sourcesLoadJob?.cancel()
        sourcesLoadJob = scope.launch {
            loadSourcesInternal(reset = false)
        }
    }

    private suspend fun loadSourcesInternal(reset: Boolean) {
        val enforceMinSkeleton = reset && state.value.isFilteringSources
        val current = state.value
        val page = if (reset) {
            SourcePaging.DEFAULT_PAGE
        } else {
            current.sourcesCurrentPage + 1
        }
        if (reset) {
            state.update {
                it.copy(isLoadingMoreSources = false)
            }
        } else {
            state.update { it.copy(isLoadingMoreSources = true) }
        }

        coroutineScope {
            val resultDeferred = async {
                getSourcesUseCase(
                    spaceId = spaceId,
                    sourceType = current.selectedFilter.toApiSourceType(),
                    search = current.searchQuery.trim().takeIf { it.isNotEmpty() },
                    sort = current.selectedSort,
                    page = page,
                    limit = pageLimit,
                )
            }
            if (enforceMinSkeleton) {
                delay(filterSkeletonMinDelayMs)
            }
            resultDeferred.await().fold(
                onSuccess = { library ->
                    state.update { existing ->
                        val merged = if (reset) {
                            library.sources
                        } else {
                            val existingIds = existing.allSources.mapTo(HashSet()) { it.id }
                            existing.allSources + library.sources.filterNot { it.id in existingIds }
                        }
                        val next = existing.copy(
                            sourcesError = null,
                            isFilteringSources = false,
                            isLoadingMoreSources = false,
                            allSources = merged,
                            allCount = library.allCount,
                            sourcesCurrentPage = library.page,
                            sourcesHasMore = library.hasMore,
                        )
                        next.copy(visibleSources = next.allSources)
                    }
                },
                onFailure = { throwable ->
                    state.update {
                        if (reset) {
                            it.copy(
                                sourcesError = throwable.message.orEmpty(),
                                isFilteringSources = false,
                                isLoadingMoreSources = false,
                            )
                        } else {
                            it.copy(isLoadingMoreSources = false)
                        }
                    }
                },
            )
        }
    }

    fun cancelSearchJob() {
        sourcesLoadJob?.cancel()
        state.update {
            it.copy(
                isFilteringSources = false,
                isLoadingMoreSources = false,
            )
        }
    }

    fun scheduleSearchReload() {
        sourcesLoadJob?.cancel()
        sourcesLoadJob = scope.launch {
            delay(searchDebounceMs)
            loadSourcesInternal(reset = true)
        }
    }

    fun onFilterSelected(filter: SourceFilter) {
        if (filter == state.value.selectedFilter) return
        state.update {
            it.copy(
                selectedFilter = filter,
                isFilteringSources = true,
                sourcesError = null,
            )
        }
        loadSourcesOnly()
    }

    fun onFilterSortClick() {
        state.update { it.copy(showSortSheet = true) }
    }

    fun onSortSheetDismiss() {
        state.update { it.copy(showSortSheet = false) }
    }

    fun onSortSelected(sort: SourceSort) {
        if (sort == state.value.selectedSort) {
            state.update { it.copy(showSortSheet = false) }
            return
        }
        sourcesLoadJob?.cancel()
        state.update {
            it.copy(
                showSortSheet = false,
                selectedSort = sort,
                isFilteringSources = true,
                sourcesError = null,
            )
        }
        loadSourcesOnly()
    }

    fun onAddSourceClick() {
        if (state.value.isCreatingSource) return
        state.update { it.copy(showAddSourceSheet = true) }
    }

    fun onAddSourceSheetDismiss() {
        if (state.value.isCreatingSource) return
        state.update { it.copy(showAddSourceSheet = false) }
    }

    fun onAddSourceFileSelected() {
        state.update { it.copy(userMessage = HomeUserMessage.SOURCE_FILE_SELECTED) }
    }

    fun onAddSourceFileSelectionFailed(error: AddSourceInputRules.FileValidationError) {
        state.update {
            it.copy(
                actionError = when (error) {
                    AddSourceInputRules.FileValidationError.UNSUPPORTED_FORMAT ->
                        HomeActionError.FILE_UNSUPPORTED
                    AddSourceInputRules.FileValidationError.SIZE_EXCEEDED ->
                        HomeActionError.FILE_TOO_LARGE
                },
            )
        }
    }

    fun onAddSourceSubmit(draft: AddSourceDraft) {
        if (state.value.isCreatingSource) return

        scope.launch {
            state.update { it.copy(isCreatingSource = true) }

            val request = try {
                buildCreateRequest(draft)
            } catch (cancelled: CancellationException) {
                state.update { it.copy(isCreatingSource = false) }
                throw cancelled
            } catch (error: Exception) {
                state.update {
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
                    state.update { it.copy(showAddSourceSheet = false) }
                    onSourceCreated(created, fallbackTitle = title)
                }
                .onFailure { throwable ->
                    state.update {
                        it.copy(
                            isCreatingSource = false,
                            actionError = throwable.toHomeActionError(),
                        )
                    }
                }
        }
    }

    /**
     * Shared post-create path for Add Source and Convert Note → source.
     * Opens the processing sheet, toasts success, refreshes the list, and observes SSE.
     */
    fun onSourceCreated(created: Source, fallbackTitle: String) {
        val displayTitle = created.title.ifBlank { fallbackTitle }
        state.update {
            it.copy(
                isCreatingSource = false,
                processingSourceId = created.id,
                processingSourceTitle = displayTitle,
                processingProgress = 0,
                processingState = SourceProcessingState.ADDED,
                userMessage = HomeUserMessage.SOURCE_CREATED,
            )
        }
        queueRefreshSourcesAfterCreate()
        startObservingProcessing(created.id)
    }

    private fun startObservingProcessing(sourceId: String) {
        processingObserveJob?.cancel()
        processingObserveJob = scope.launch {
            try {
                observeSourceProcessingUseCase()
                    .filter { it.sourceId == sourceId }
                    .transformWhile { event ->
                        emit(event)
                        !event.isTerminal
                    }
                    .collect { event ->
                        state.update {
                            it.copy(
                                processingProgress = event.progress,
                                processingState = event.state,
                            )
                        }
                        if (event.isTerminal) {
                            queueRefreshSourcesAfterCreate()
                        }
                    }
            } catch (_: Exception) {
                // Keep last known progress; user can dismiss the sheet.
            }
        }
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

    private fun queueRefreshSourcesAfterCreate() {
        sourcesLoadJob?.cancel()
        sourcesLoadJob = scope.launch {
            loadSourcesInternal(reset = true)
        }
    }

    fun onSourceProcessingDismiss() {
        stopProcessingObservation()
        state.update {
            it.copy(
                processingSourceId = null,
                processingSourceTitle = null,
                processingProgress = 0,
                processingState = null,
            )
        }
    }

    fun onSourceProcessingOpenSource() {
        val sourceId = state.value.processingSourceId
        stopProcessingObservation()
        state.update {
            it.copy(
                processingSourceId = null,
                processingSourceTitle = null,
                processingProgress = 0,
                processingState = null,
                openSourceDetailId = sourceId,
                openSourceDetailHighlight = null,
            )
        }
    }

    fun onSourceProcessingAsk() {
        val sourceId = state.value.processingSourceId
        stopProcessingObservation()
        state.update {
            it.copy(
                processingSourceId = null,
                processingSourceTitle = null,
                processingProgress = 0,
                processingState = null,
                selectedTab = HomeTab.ASK,
            )
        }
        if (sourceId != null) {
            applyAskScope(sourceId, true)
        }
    }

    fun onSourceProcessingRetry() {
        val sourceId = state.value.processingSourceId ?: return
        scope.launch {
            retrySourceUseCase(sourceId)
                .onSuccess {
                    state.update {
                        it.copy(
                            processingProgress = 0,
                            processingState = SourceProcessingState.ADDED,
                        )
                    }
                    startObservingProcessing(sourceId)
                }
                .onFailure { throwable ->
                    state.update {
                        it.copy(actionError = throwable.toHomeActionError())
                    }
                }
        }
    }

    fun onSourceOptionsClick(source: Source) {
        state.update { it.copy(optionsSource = source) }
    }

    fun onSourceOptionsDismiss() {
        state.update { it.copy(optionsSource = null) }
    }

    fun onEditSourceClick(source: Source) {
        state.update {
            it.copy(optionsSource = null, editingSource = source)
        }
    }

    fun onSourceClick(source: Source) {
        when (source.status) {
            SourceStatus.READY -> openReadySource(source)
            SourceStatus.PROCESSING -> openProcessingSheet(source)
            SourceStatus.FAILED -> openFailedProcessingSheet(source)
        }
    }

    private fun openReadySource(source: Source) {
        if (state.value.isOpeningSource) return
        openSourceJob?.cancel()
        openSourceJob = scope.launch {
            state.update {
                it.copy(
                    isOpeningSource = true,
                    openSourceDetailId = null,
                    openSourceDetailHighlight = null,
                )
            }
            delay(openSourceDelayMs)
            state.update {
                it.copy(
                    isOpeningSource = false,
                    openSourceDetailId = source.id,
                    openSourceDetailHighlight = null,
                )
            }
        }
    }

    private fun openProcessingSheet(source: Source) {
        state.update {
            it.copy(
                processingSourceId = source.id,
                processingSourceTitle = source.title,
                processingProgress = 0,
                processingState = SourceProcessingState.ADDED,
            )
        }
        startObservingProcessing(source.id)
    }

    private fun openFailedProcessingSheet(source: Source) {
        stopProcessingObservation()
        state.update {
            it.copy(
                processingSourceId = source.id,
                processingSourceTitle = source.title,
                processingProgress = 100,
                processingState = SourceProcessingState.FAILED,
            )
        }
    }

    fun onOpenSourceDetailHandled() {
        state.update { it.copy(openSourceDetailId = null, openSourceDetailHighlight = null) }
    }

    fun onEditSourceDismiss() {
        state.update { it.copy(editingSource = null) }
    }

    fun onEditSourceSave(title: String, author: String) {
        if (title.isBlank()) return
        val editing = state.value.editingSource ?: return
        val trimmedTitle = AddSourceInputRules.limitTitle(title.trim())
        val trimmedAuthor = AddSourceInputRules.limitAuthor(author.trim())
        val updated = editing.copy(title = trimmedTitle, author = trimmedAuthor)

        scope.launch {
            updateSourceUseCase(updated, null)
                .onSuccess { saved ->
                    state.update { current ->
                        val updatedSources = current.allSources.map { source ->
                            if (source.id == saved.id) saved else source
                        }
                        val next = current.copy(
                            allSources = updatedSources,
                            editingSource = null,
                            userMessage = HomeUserMessage.SOURCE_UPDATED,
                        )
                        next.copy(visibleSources = filterSources(next))
                    }
                }
                .onFailure { throwable ->
                    state.update {
                        it.copy(actionError = throwable.toHomeActionError())
                    }
                }
        }
    }

    fun onDeleteSourceClick(source: Source) {
        state.update { it.copy(optionsSource = null, deletingSource = source) }
    }

    fun onDeleteSourceDismiss() {
        state.update { it.copy(deletingSource = null) }
    }

    fun onDeleteSourceConfirm() {
        val deleting = state.value.deletingSource ?: return

        scope.launch {
            deleteSourceUseCase(deleting.id)
                .onSuccess {
                    state.update { current ->
                        val updatedSources = current.allSources.filterNot { it.id == deleting.id }
                        val next = current.copy(
                            allSources = updatedSources,
                            allCount = (current.allCount - 1).coerceAtLeast(0),
                            deletingSource = null,
                            userMessage = HomeUserMessage.SOURCE_DELETED,
                        )
                        next.copy(visibleSources = filterSources(next))
                    }
                }
                .onFailure { throwable ->
                    state.update {
                        it.copy(actionError = throwable.toHomeActionError())
                    }
                }
        }
    }
}
