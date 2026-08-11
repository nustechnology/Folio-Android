package com.nus.folio.presentation.home

import com.nus.folio.domain.usecase.GetNotebookUseCase
import com.nus.folio.domain.usecase.SaveNotebookUseCase
import com.nus.folio.domain.util.NotebookFilename
import com.nus.folio.domain.util.NotebookInputRules
import com.nus.folio.domain.util.NotebookMarkdownExporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class HomeNotebookDelegate(
    private val spaceId: String,
    private val state: MutableStateFlow<HomeUiState>,
    private val scope: CoroutineScope,
    private val getNotebookUseCase: GetNotebookUseCase,
    private val saveNotebookUseCase: SaveNotebookUseCase,
    private val saveDebounceMs: Long,
) {
    private var saveJob: Job? = null
    private var loadJob: Job? = null
    /** Avoid re-fetching on every Notebook tab select for this space-scoped delegate. */
    private var hasLoadedNotebook = false

    fun loadNotebook() {
        if (hasLoadedNotebook) return
        loadJob?.cancel()
        loadJob = scope.launch {
            state.update { it.copy(isLoadingNotebook = true) }
            getNotebookUseCase(spaceId)
                .onSuccess { notebook ->
                    hasLoadedNotebook = true
                    val resolvedContent = notebook.content.ifBlank {
                        defaultNotebookTemplate(state.value.spaceTitle)
                    }
                    state.update { current ->
                        // Keep in-progress edits (debounce may still be pending) instead of
                        // replacing them with an older stored snapshot.
                        if (hasUnsavedNotebookEdits(current)) {
                            current.copy(isLoadingNotebook = false)
                        } else {
                            current.copy(
                                notebookContent = resolvedContent,
                                notebookSaveStatus = NotebookSaveStatus.IDLE,
                                isLoadingNotebook = false,
                            )
                        }
                    }
                }
                .onFailure {
                    state.update { it.copy(isLoadingNotebook = false) }
                }
        }
    }

    fun onNotebookContentChange(content: String) {
        val clamped = NotebookInputRules.clampContent(content)
        state.update {
            it.copy(
                notebookContent = clamped,
                notebookSaveStatus = NotebookSaveStatus.SAVING,
            )
        }
        scheduleSave(contentToSave = clamped, debounce = true)
    }

    fun retryNotebookSave() {
        if (state.value.notebookSaveStatus != NotebookSaveStatus.FAILED) return
        val content = state.value.notebookContent
        state.update { it.copy(notebookSaveStatus = NotebookSaveStatus.SAVING) }
        scheduleSave(contentToSave = content, debounce = false)
    }

    private fun scheduleSave(contentToSave: String, debounce: Boolean) {
        saveJob?.cancel()
        saveJob = scope.launch {
            if (debounce) {
                delay(saveDebounceMs)
            }
            saveNotebookUseCase(spaceId, contentToSave)
                .onSuccess { saved ->
                    state.update { current ->
                        if (current.notebookContent == contentToSave) {
                            current.copy(
                                notebookContent = saved.content,
                                notebookSaveStatus = NotebookSaveStatus.SAVED,
                            )
                        } else {
                            current
                        }
                    }
                }
                .onFailure {
                    state.update { current ->
                        if (current.notebookContent == contentToSave) {
                            current.copy(notebookSaveStatus = NotebookSaveStatus.FAILED)
                        } else {
                            current
                        }
                    }
                }
        }
    }

    fun onNotebookAddClick() {
        state.update { it.copy(showNotebookActions = true) }
    }

    fun onNotebookActionsDismiss() {
        state.update { it.copy(showNotebookActions = false) }
    }

    fun onCopyNotebookClick() {
        val content = state.value.notebookContent
        state.update {
            it.copy(
                showNotebookActions = false,
                pendingNotebookCopy = content,
            )
        }
    }

    fun onExportNotebookClick() {
        state.update {
            it.copy(
                showNotebookActions = false,
                showNotebookExport = true,
            )
        }
    }

    fun onNotebookExportDismiss() {
        state.update { it.copy(showNotebookExport = false) }
    }

    fun onNotebookExportConfirm(format: NotebookExportFormat) {
        val content = NotebookMarkdownExporter.normalize(state.value.notebookContent)
        when (format) {
            NotebookExportFormat.MARKDOWN -> {
                val filename = NotebookFilename.forSpace(state.value.spaceTitle)
                state.update {
                    it.copy(
                        showNotebookExport = false,
                        pendingNotebookExport = NotebookExportRequest(
                            filename = filename,
                            markdown = content,
                        ),
                        notebookExportPickerLaunched = false,
                    )
                }
            }
            NotebookExportFormat.PRINT_PDF -> {
                state.update {
                    it.copy(
                        showNotebookExport = false,
                        pendingNotebookPrint = NotebookPrintRequest(
                            id = System.nanoTime(),
                            markdown = content,
                        ),
                    )
                }
            }
        }
    }

    fun onPendingNotebookCopyHandled() {
        state.update {
            it.copy(
                pendingNotebookCopy = null,
                userMessage = HomeUserMessage.NOTEBOOK_COPIED,
            )
        }
    }

    fun onNotebookExportPickerLaunched() {
        state.update { it.copy(notebookExportPickerLaunched = true) }
    }

    fun onPendingNotebookExportHandled() {
        state.update {
            it.copy(
                pendingNotebookExport = null,
                notebookExportPickerLaunched = false,
            )
        }
    }

    fun onNotebookExportSucceeded() {
        state.update {
            it.copy(
                pendingNotebookExport = null,
                notebookExportPickerLaunched = false,
                userMessage = HomeUserMessage.NOTEBOOK_EXPORTED,
            )
        }
    }

    fun onNotebookExportFailed() {
        state.update {
            it.copy(
                pendingNotebookExport = null,
                notebookExportPickerLaunched = false,
                actionError = HomeActionError.EXPORT_FAILED,
            )
        }
    }

    fun onNotebookPrintSubmitted() = Unit

    /**
     * Activity is recreating: keep markdown but bump [NotebookPrintRequest.id] so print relaunches
     * with a fresh WebView and [android.print.PrintDocumentAdapter].
     */
    fun onNotebookPrintAdapterInvalidated() {
        val request = state.value.pendingNotebookPrint ?: return
        state.update {
            it.copy(pendingNotebookPrint = request.copy(id = System.nanoTime()))
        }
    }

    fun onPendingNotebookPrintHandled() {
        state.update { it.copy(pendingNotebookPrint = null) }
    }

    private fun hasUnsavedNotebookEdits(current: HomeUiState): Boolean =
        current.notebookSaveStatus == NotebookSaveStatus.SAVING ||
            current.notebookSaveStatus == NotebookSaveStatus.FAILED ||
            saveJob?.isActive == true

    private fun defaultNotebookTemplate(spaceTitle: String): String {
        val resolvedTitle = spaceTitle.trim().ifBlank { "Untitled Research" }
        return """
            # Title
            $resolvedTitle

            ## Research Objective

        """.trimIndent()
    }
}
