package com.nus.folio.presentation.sourcedetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nus.folio.domain.model.SourceFileLocation
import com.nus.folio.domain.model.SourceProcessingState
import com.nus.folio.domain.model.SourceStatus
import com.nus.folio.domain.model.SourceType
import com.nus.folio.domain.usecase.DeleteSourceUseCase
import com.nus.folio.domain.usecase.GetSourceDetailUseCase
import com.nus.folio.domain.usecase.GetSourcePreviewUrlUseCase
import com.nus.folio.domain.usecase.ObserveSourceProcessingUseCase
import com.nus.folio.domain.usecase.RetrySourceUseCase
import com.nus.folio.domain.usecase.UpdateSourceUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SourceDetailViewModel(
    private val spaceId: String,
    private val sourceId: String,
    private val getSourceDetailUseCase: GetSourceDetailUseCase,
    private val getSourcePreviewUrlUseCase: GetSourcePreviewUrlUseCase,
    private val retrySourceUseCase: RetrySourceUseCase,
    private val observeSourceProcessingUseCase: ObserveSourceProcessingUseCase,
    private val updateSourceUseCase: UpdateSourceUseCase,
    private val deleteSourceUseCase: DeleteSourceUseCase,
    private val contentRevealDelayMs: Long = CONTENT_REVEAL_DELAY_MS,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SourceDetailUiState(isLoading = true, isContentLoading = true),
    )
    val uiState: StateFlow<SourceDetailUiState> = _uiState.asStateFlow()

    private var processingObserveJob: Job? = null
    private var previewFetchJob: Job? = null
    private var loadDetailJob: Job? = null

    init {
        loadDetail()
    }

    fun loadDetail() {
        loadDetailJob?.cancel()
        loadDetailJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = it.detail == null,
                    isContentLoading = true,
                    error = null,
                    previewUrl = null,
                )
            }
            val startedAtMs = System.currentTimeMillis()
            getSourceDetailUseCase(spaceId, sourceId)
                .onSuccess { detail ->
                    val revealContent = detail.status == SourceStatus.READY
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRetrying = false,
                            error = null,
                            detail = detail,
                            selectedSheetIndex = 0,
                            isContentLoading = revealContent,
                        )
                    }
                    if (detail.status == SourceStatus.PROCESSING) {
                        startObservingProcessing()
                    }
                    if (detail.status != SourceStatus.FAILED) {
                        fetchPreviewUrl()
                    }
                    if (revealContent) {
                        val elapsedMs = System.currentTimeMillis() - startedAtMs
                        val remainingMs = contentRevealDelayMs - elapsedMs
                        if (remainingMs > 0L) {
                            delay(remainingMs)
                        }
                        _uiState.update { it.copy(isContentLoading = false) }
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isContentLoading = false,
                            isRetrying = false,
                            error = throwable.message ?: "Failed to load source",
                            previewUrl = null,
                        )
                    }
                }
        }
    }

    fun onRetryProcessing() {
        if (_uiState.value.isRetrying) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRetrying = true) }
            retrySourceUseCase(sourceId)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            isRetrying = false,
                            detail = state.detail?.copy(status = SourceStatus.PROCESSING),
                            previewUrl = null,
                        )
                    }
                    startObservingProcessing()
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isRetrying = false,
                            actionError = throwable.toActionErrorMessage(),
                        )
                    }
                }
        }
    }

    private fun startObservingProcessing() {
        processingObserveJob?.cancel()
        processingObserveJob = viewModelScope.launch {
            try {
                observeSourceProcessingUseCase()
                    .filter { it.sourceId == sourceId }
                    .transformWhile { event ->
                        emit(event)
                        !event.isTerminal
                    }
                    .collect { event ->
                        if (event.state == SourceProcessingState.READY ||
                            event.state == SourceProcessingState.FAILED
                        ) {
                            loadDetail()
                        }
                    }
            } catch (_: Exception) {
                // Keep last known detail; user can retry manually.
            }
        }
    }

    private fun fetchPreviewUrl() {
        previewFetchJob?.cancel()
        previewFetchJob = viewModelScope.launch {
            getSourcePreviewUrlUseCase(sourceId)
                .onSuccess { url ->
                    _uiState.update { it.copy(previewUrl = url?.takeIf { value -> value.isNotBlank() }) }
                }
                .onFailure {
                    // Preview is optional — hide the nav icon when unavailable.
                    _uiState.update { it.copy(previewUrl = null) }
                }
        }
    }

    fun onSheetSelected(index: Int) {
        _uiState.update { it.copy(selectedSheetIndex = index) }
    }

    fun onOpenOriginalClick() {
        val previewUrl = _uiState.value.previewUrl?.takeIf { it.isNotBlank() } ?: return
        _uiState.update {
            it.copy(openOriginalRequest = SourceFileLocation.Remote(previewUrl))
        }
    }

    fun onOpenOriginalResult(result: SourceOriginalOpenResult) {
        if (result == SourceOriginalOpenResult.NO_APP) {
            _uiState.update { it.copy(userMessage = SourceDetailUserMessage.OPEN_ORIGINAL_NO_APP) }
        } else if (result == SourceOriginalOpenResult.FAILED) {
            _uiState.update { it.copy(userMessage = SourceDetailUserMessage.OPEN_ORIGINAL_FAILED) }
        }
        _uiState.update { it.copy(openOriginalRequest = null) }
    }

    fun onEditSourceClick() {
        val detail = _uiState.value.detail ?: return
        _uiState.update {
            it.copy(
                editingSource = detail.toSource(),
                editingSourceContent = detail.plainContent.orEmpty(),
            )
        }
    }

    fun onEditSourceDismiss() {
        if (_uiState.value.isUpdatingSource) return
        _uiState.update { it.copy(editingSource = null, editingSourceContent = "") }
    }

    fun onEditSourceSave(title: String, author: String, content: String) {
        if (_uiState.value.isUpdatingSource) return
        val editing = _uiState.value.editingSource ?: return
        val trimmedTitle = title.trim()
        if (trimmedTitle.isBlank()) return
        val trimmedAuthor = author.trim()
        val contentToSend = if (editing.type == SourceType.TEXT) content.trim() else null

        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingSource = true, actionError = null) }
            updateSourceUseCase(
                source = editing.copy(
                    title = trimmedTitle,
                    author = trimmedAuthor,
                ),
                content = contentToSend,
            ).onSuccess { updated ->
                _uiState.update { current ->
                    val detail = current.detail
                    current.copy(
                        isUpdatingSource = false,
                        detail = detail?.copy(
                            title = updated.title,
                            author = updated.author,
                            status = updated.status,
                            fileExtension = updated.fileExtension,
                            plainContent = if (updated.type == SourceType.TEXT) {
                                contentToSend ?: detail.plainContent
                            } else {
                                detail.plainContent
                            },
                        ),
                        editingSource = null,
                        editingSourceContent = "",
                        userMessage = SourceDetailUserMessage.SOURCE_UPDATED,
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isUpdatingSource = false,
                        actionError = throwable.toActionErrorMessage(),
                    )
                }
            }
        }
    }

    fun onDeleteSourceClick() {
        val detail = _uiState.value.detail ?: return
        _uiState.update { it.copy(deletingSource = detail.toSource()) }
    }

    fun onDeleteSourceDismiss() {
        if (_uiState.value.isDeletingSource) return
        _uiState.update { it.copy(deletingSource = null) }
    }

    fun onDeleteSourceConfirm() {
        if (_uiState.value.isDeletingSource) return
        val deleting = _uiState.value.deletingSource ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isDeletingSource = true, actionError = null) }
            deleteSourceUseCase(deleting.id)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            deletingSource = null,
                            isDeletingSource = false,
                            userMessage = SourceDetailUserMessage.SOURCE_DELETED,
                            sourceDeleted = true,
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            deletingSource = null,
                            isDeletingSource = false,
                            actionError = throwable.toActionErrorMessage(),
                        )
                    }
                }
        }
    }

    fun onSourceDeletedHandled() {
        _uiState.update { it.copy(sourceDeleted = false) }
    }

    fun onUserMessageShown() {
        _uiState.update { it.copy(userMessage = null) }
    }

    fun onActionErrorShown() {
        _uiState.update { it.copy(actionError = null) }
    }

    class Factory(
        private val spaceId: String,
        private val sourceId: String,
        private val getSourceDetailUseCase: GetSourceDetailUseCase,
        private val getSourcePreviewUrlUseCase: GetSourcePreviewUrlUseCase,
        private val retrySourceUseCase: RetrySourceUseCase,
        private val observeSourceProcessingUseCase: ObserveSourceProcessingUseCase,
        private val updateSourceUseCase: UpdateSourceUseCase,
        private val deleteSourceUseCase: DeleteSourceUseCase,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SourceDetailViewModel::class.java)) {
                return SourceDetailViewModel(
                    spaceId,
                    sourceId,
                    getSourceDetailUseCase,
                    getSourcePreviewUrlUseCase,
                    retrySourceUseCase,
                    observeSourceProcessingUseCase,
                    updateSourceUseCase,
                    deleteSourceUseCase,
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    companion object {
        private const val CONTENT_REVEAL_DELAY_MS = 1_000L
    }
}

private fun com.nus.folio.domain.model.SourceDetail.toSource(): com.nus.folio.domain.model.Source =
    com.nus.folio.domain.model.Source(
        id = id,
        title = title,
        type = type,
        author = author,
        addedLabel = addedLabel,
        status = status,
        spaceId = spaceId,
        fileExtension = fileExtension,
    )

private fun Throwable.toActionErrorMessage(): String =
    message?.takeIf { it.isNotBlank() } ?: "Something went wrong. Please try again."
