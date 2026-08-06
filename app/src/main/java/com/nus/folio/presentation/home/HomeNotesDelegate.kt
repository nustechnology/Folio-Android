package com.nus.folio.presentation.home

import com.nus.folio.domain.model.CreateNoteRequest
import com.nus.folio.domain.model.Note
import com.nus.folio.domain.model.NoteFilter
import com.nus.folio.domain.model.NoteOrigin
import com.nus.folio.domain.usecase.CreateNoteUseCase
import com.nus.folio.domain.usecase.DeleteNoteUseCase
import com.nus.folio.domain.usecase.UpdateNoteUseCase
import com.nus.folio.domain.util.NoteInputRules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Owns Notes-tab state: note create/view/edit/convert/delete sheets, note filter,
 * and the Notebook stub actions.
 */
internal class HomeNotesDelegate(
    private val spaceId: String,
    private val state: MutableStateFlow<HomeUiState>,
    private val scope: CoroutineScope,
    private val createNoteUseCase: CreateNoteUseCase,
    private val updateNoteUseCase: UpdateNoteUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase,
) {

    fun onNoteFilterSelected(filter: NoteFilter) {
        state.update { current ->
            val next = current.copy(selectedNoteFilter = filter)
            next.copy(visibleNotes = filterNotes(next))
        }
    }

    fun onAddNoteSubmit(
        title: String,
        content: String,
    ) {
        if (NoteInputRules.titleValidationError(title) != null) return
        val trimmedContent = content.trim()
        if (NoteInputRules.contentValidationError(trimmedContent) != null) return
        val resolvedTitle = NoteInputRules.resolveTitle(title)

        scope.launch {
            createNoteUseCase(
                CreateNoteRequest(
                    spaceId = spaceId,
                    title = resolvedTitle,
                    content = trimmedContent,
                    origin = NoteOrigin.USER_CREATED,
                    project = state.value.spaceTitle.ifBlank { null },
                ),
            ).onSuccess { created ->
                state.update { current ->
                    val notes = listOf(created) + current.allNotes
                    val next = current.copy(
                        allNotes = notes,
                        notesAllCount = notes.size,
                        notesPinnedCount = notes.count { note -> note.isPinned },
                        notesUnfiledCount = notes.count { note -> note.project.isNullOrBlank() },
                        userMessage = HomeUserMessage.NOTE_SAVED,
                    )
                    next.copy(visibleNotes = filterNotes(next))
                }
            }.onFailure { throwable ->
                state.update {
                    it.copy(actionError = throwable.toHomeActionError())
                }
            }
        }
    }

    fun onNoteClick(note: Note) {
        state.update { it.copy(viewingNote = note) }
    }

    fun onViewNoteDismiss() {
        state.update { it.copy(viewingNote = null) }
    }

    fun onNoteOptionsClick(note: Note) {
        state.update { it.copy(optionsNote = note) }
    }

    fun onNoteOptionsDismiss() {
        state.update { it.copy(optionsNote = null) }
    }

    fun onViewNoteClick() {
        state.update { current ->
            val note = current.optionsNote ?: return@update current
            current.copy(
                optionsNote = null,
                viewingNote = note,
            )
        }
    }

    fun onEditNoteClick() {
        state.update { current ->
            val note = current.viewingNote ?: current.optionsNote ?: return@update current
            current.copy(
                optionsNote = null,
                editingNote = note,
            )
        }
    }

    fun onEditNoteDismiss() {
        state.update { it.copy(editingNote = null, viewingNote = null) }
    }

    fun onEditNoteSave(title: String, content: String) {
        if (NoteInputRules.titleValidationError(title) != null) return
        val trimmedContent = content.trim()
        if (NoteInputRules.contentValidationError(trimmedContent) != null) return
        val editing = state.value.editingNote ?: return
        val updated = editing.copy(
            title = NoteInputRules.resolveTitle(title),
            content = trimmedContent,
        )

        scope.launch {
            updateNoteUseCase(updated)
                .onSuccess { saved ->
                    state.update { current ->
                        val updatedNotes = current.allNotes.map { note ->
                            if (note.id == saved.id) saved else note
                        }
                        val next = current.copy(
                            allNotes = updatedNotes,
                            editingNote = null,
                            viewingNote = if (current.viewingNote?.id == saved.id) saved else current.viewingNote,
                            userMessage = HomeUserMessage.NOTE_UPDATED,
                        )
                        next.copy(visibleNotes = filterNotes(next))
                    }
                }
                .onFailure {
                    state.update {
                        it.copy(userMessage = HomeUserMessage.EDIT_NOTE_NOT_SUPPORTED)
                    }
                }
        }
    }

    fun onConvertNoteClick() {
        state.update { current ->
            val note = current.viewingNote ?: current.optionsNote ?: return@update current
            current.copy(
                optionsNote = null,
                convertingNote = note,
            )
        }
    }

    fun onConvertNoteDismiss() {
        state.update { it.copy(convertingNote = null, viewingNote = null) }
    }

    fun onConvertNoteCreate(
        @Suppress("UNUSED_PARAMETER") title: String,
        @Suppress("UNUSED_PARAMETER") snapshot: String,
    ) {
        state.update {
            it.copy(
                convertingNote = null,
                viewingNote = null,
                userMessage = HomeUserMessage.CONVERT_NOTE_NOT_SUPPORTED,
            )
        }
    }

    fun onDeleteNoteClick() {
        state.update { current ->
            val note = current.editingNote ?: current.optionsNote ?: return@update current
            current.copy(
                optionsNote = null,
                editingNote = null,
                deletingNote = note,
            )
        }
    }

    fun onDeleteNoteDismiss() {
        state.update { it.copy(deletingNote = null, viewingNote = null) }
    }

    fun onDeleteNoteConfirm() {
        val deleting = state.value.deletingNote ?: return

        scope.launch {
            deleteNoteUseCase(deleting.id)
                .onSuccess {
                    state.update { current ->
                        val updatedNotes = current.allNotes.filterNot { it.id == deleting.id }
                        val next = current.copy(
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
                    state.update {
                        it.copy(userMessage = HomeUserMessage.DELETE_NOTE_NOT_SUPPORTED)
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
        state.update {
            it.copy(
                showNotebookActions = false,
                userMessage = HomeUserMessage.COPY_NOTEBOOK_NOT_SUPPORTED,
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

    fun onNotebookExportConfirm(
        @Suppress("UNUSED_PARAMETER") format: NotebookExportFormat,
    ) {
        state.update {
            it.copy(
                showNotebookExport = false,
                userMessage = HomeUserMessage.EXPORT_NOTEBOOK_NOT_SUPPORTED,
            )
        }
    }
}
