package com.nus.folio.presentation.home

import com.nus.folio.domain.model.CreateNoteRequest
import com.nus.folio.domain.model.Note
import com.nus.folio.domain.model.NoteFilter
import com.nus.folio.domain.model.NoteOrigin
import com.nus.folio.domain.model.NotePaging
import com.nus.folio.domain.model.NoteSort
import com.nus.folio.domain.model.Source
import com.nus.folio.domain.model.toApiOrigin
import com.nus.folio.domain.usecase.ConvertNoteToSourceUseCase
import com.nus.folio.domain.usecase.CreateNoteUseCase
import com.nus.folio.domain.usecase.DeleteNoteUseCase
import com.nus.folio.domain.usecase.GetNoteDetailUseCase
import com.nus.folio.domain.usecase.GetNotesUseCase
import com.nus.folio.domain.usecase.UpdateNoteUseCase
import com.nus.folio.domain.util.AddSourceInputRules
import com.nus.folio.domain.util.NoteInputRules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Owns Notes-tab state: note create/view/edit/convert/delete sheets, note filter/sort,
 * search debounce, and paged load-more.
 */
internal class HomeNotesDelegate(
    private val spaceId: String,
    private val state: MutableStateFlow<HomeUiState>,
    private val scope: CoroutineScope,
    private val getNotesUseCase: GetNotesUseCase,
    private val getNoteDetailUseCase: GetNoteDetailUseCase,
    private val createNoteUseCase: CreateNoteUseCase,
    private val updateNoteUseCase: UpdateNoteUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase,
    private val convertNoteToSourceUseCase: ConvertNoteToSourceUseCase,
    private val onSourceCreated: (Source, fallbackTitle: String) -> Unit,
    private val searchDebounceMs: Long,
    private val pageLimit: Int = NotePaging.DEFAULT_LIMIT,
    private val createMinDelayMs: Long = CREATE_MIN_DELAY_MS,
) {

    private var notesLoadJob: Job? = null
    private var noteDetailJob: Job? = null

    fun cancelSearchJob() {
        notesLoadJob?.cancel()
        notesLoadJob = null
        state.update { it.copy(isLoadingMoreNotes = false, isSearchingNotes = false) }
    }

    fun scheduleSearchReload() {
        notesLoadJob?.cancel()
        state.update { it.copy(isSearchingNotes = true) }
        notesLoadJob = scope.launch {
            delay(searchDebounceMs)
            loadNotesInternal(reset = true, showFullScreenLoading = false)
        }
    }

    fun loadNotesOnly() {
        notesLoadJob?.cancel()
        notesLoadJob = scope.launch {
            loadNotesInternal(reset = true, showFullScreenLoading = false)
        }
    }

    fun onRefreshNotes() {
        if (state.value.isRefreshingNotes) return
        notesLoadJob?.cancel()
        notesLoadJob = scope.launch {
            state.update { it.copy(isRefreshingNotes = true, isLoadingMoreNotes = false) }
            try {
                loadNotesInternal(reset = true, showFullScreenLoading = false)
            } finally {
                state.update { it.copy(isRefreshingNotes = false) }
            }
        }
    }

    fun onLoadMore() {
        val current = state.value
        if (
            current.isLoading ||
            current.isRefreshingNotes ||
            current.isLoadingMoreNotes ||
            !current.notesHasMore ||
            current.selectedTab != HomeTab.NOTES
        ) {
            return
        }
        notesLoadJob?.cancel()
        notesLoadJob = scope.launch {
            loadNotesInternal(reset = false, showFullScreenLoading = false)
        }
    }

    private suspend fun loadNotesInternal(
        reset: Boolean,
        showFullScreenLoading: Boolean,
    ) {
        val current = state.value
        val page = if (reset) {
            NotePaging.DEFAULT_PAGE
        } else {
            current.notesCurrentPage + 1
        }
        if (reset) {
            state.update {
                it.copy(
                    isLoadingMoreNotes = false,
                    notesError = if (showFullScreenLoading) null else it.notesError,
                )
            }
        } else {
            state.update { it.copy(isLoadingMoreNotes = true) }
        }

        val result = getNotesUseCase(
            spaceId = spaceId,
            search = current.searchQuery.trim().takeIf { it.isNotEmpty() },
            sort = current.selectedNoteSort,
            origin = current.selectedNoteFilter.toApiOrigin(),
            page = page,
            limit = pageLimit,
        )

        result
            .onSuccess { library ->
                state.update { ui ->
                    val merged = if (reset) {
                        library.notes
                    } else {
                        val existingIds = ui.allNotes.mapTo(HashSet()) { it.id }
                        ui.allNotes + library.notes.filterNot { it.id in existingIds }
                    }
                    val next = ui.copy(
                        notesError = null,
                        allNotes = merged,
                        notesAllCount = library.allCount,
                        notesUserCreatedCount = library.userCreatedCount,
                        notesSavedAnswerCount = library.savedAnswerCount,
                        notesCurrentPage = library.page,
                        notesHasMore = library.hasMore,
                        isLoadingMoreNotes = false,
                        isSearchingNotes = false,
                    )
                    next.copy(visibleNotes = filterNotes(next))
                }
            }
            .onFailure { throwable ->
                state.update { ui ->
                    if (reset) {
                        ui.copy(
                            isLoadingMoreNotes = false,
                            isSearchingNotes = false,
                            notesError = throwable.message.orEmpty(),
                        )
                    } else {
                        ui.copy(isLoadingMoreNotes = false)
                    }
                }
            }
    }

    fun onNoteFilterSelected(filter: NoteFilter) {
        if (filter == state.value.selectedNoteFilter) return
        state.update { it.copy(selectedNoteFilter = filter) }
        loadNotesOnly()
    }

    fun onFilterSortClick() {
        state.update { it.copy(showNoteSortSheet = true) }
    }

    fun onSortSheetDismiss() {
        state.update { it.copy(showNoteSortSheet = false) }
    }

    fun onSortSelected(sort: NoteSort) {
        if (sort == state.value.selectedNoteSort) {
            state.update { it.copy(showNoteSortSheet = false) }
            return
        }
        state.update { it.copy(showNoteSortSheet = false, selectedNoteSort = sort) }
        loadNotesOnly()
    }

    fun onAddNoteClick() {
        if (state.value.isCreatingNote) return
        state.update { it.copy(showAddNoteSheet = true) }
    }

    fun onAddNoteSheetDismiss() {
        if (state.value.isCreatingNote) return
        state.update { it.copy(showAddNoteSheet = false) }
    }

    fun onAddNoteSubmit(
        title: String,
        content: String,
    ) {
        if (state.value.isCreatingNote) return
        if (NoteInputRules.titleValidationError(title) != null) return
        val trimmedContent = content.trim()
        if (NoteInputRules.contentValidationError(trimmedContent) != null) return
        val resolvedTitle = NoteInputRules.resolveTitle(title)

        state.update { it.copy(isCreatingNote = true) }
        scope.launch {
            val result = coroutineScope {
                val createDeferred = async {
                    createNoteUseCase(
                        CreateNoteRequest(
                            spaceId = spaceId,
                            title = resolvedTitle,
                            content = trimmedContent,
                            origin = NoteOrigin.USER_CREATED,
                            project = state.value.spaceTitle.ifBlank { null },
                        ),
                    )
                }
                delay(createMinDelayMs)
                createDeferred.await()
            }
            result
                .onSuccess { created ->
                    state.update { current ->
                        val alreadyExists = current.allNotes.any { it.id == created.id }
                        val belongsToActiveSearch = created.matchesActiveNotesSearch(current.searchQuery)
                        val shouldApplyLocally = belongsToActiveSearch
                        val notes = if (shouldApplyLocally) {
                            listOf(created) + current.allNotes.filterNot { it.id == created.id }
                        } else {
                            current.allNotes
                        }
                        val next = current.copy(
                            allNotes = notes,
                            notesAllCount = if (shouldApplyLocally && !alreadyExists) {
                                current.notesAllCount + 1
                            } else {
                                current.notesAllCount
                            },
                            notesUserCreatedCount = if (
                                shouldApplyLocally &&
                                !alreadyExists &&
                                created.origin == NoteOrigin.USER_CREATED
                            ) {
                                current.notesUserCreatedCount + 1
                            } else {
                                current.notesUserCreatedCount
                            },
                            notesSavedAnswerCount = if (
                                shouldApplyLocally &&
                                !alreadyExists &&
                                created.origin == NoteOrigin.SAVED_ANSWER
                            ) {
                                current.notesSavedAnswerCount + 1
                            } else {
                                current.notesSavedAnswerCount
                            },
                            showAddNoteSheet = false,
                            isCreatingNote = false,
                            userMessage = HomeUserMessage.NOTE_SAVED,
                        )
                        next.copy(visibleNotes = filterNotes(next))
                    }
                }
                .onFailure { throwable ->
                    state.update {
                        it.copy(
                            isCreatingNote = false,
                            actionError = throwable.toHomeActionError(),
                        )
                    }
                }
        }
    }

    fun onNoteClick(note: Note) {
        state.update { it.copy(viewingNote = note) }
        loadNoteDetail(noteId = note.id)
    }

    fun onViewNoteDismiss() {
        noteDetailJob?.cancel()
        state.update { it.copy(viewingNote = null, isLoadingNoteDetail = false) }
    }

    fun onNoteOptionsClick(note: Note) {
        state.update { it.copy(optionsNote = note) }
    }

    fun onNoteOptionsDismiss() {
        state.update { it.copy(optionsNote = null) }
    }

    fun onViewNoteClick() {
        val note = state.value.optionsNote ?: return
        state.update {
            it.copy(
                optionsNote = null,
                viewingNote = note,
            )
        }
        loadNoteDetail(noteId = note.id)
    }

    fun onEditNoteClick() {
        val note = state.value.viewingNote ?: state.value.optionsNote ?: return
        state.update { current ->
            current.copy(
                optionsNote = null,
                editingNote = note,
            )
        }
        loadNoteDetail(noteId = note.id)
    }

    fun onEditNoteDismiss() {
        noteDetailJob?.cancel()
        state.update {
            it.copy(
                editingNote = null,
                viewingNote = null,
                isLoadingNoteDetail = false,
            )
        }
    }

    private fun loadNoteDetail(noteId: String) {
        noteDetailJob?.cancel()
        noteDetailJob = scope.launch {
            state.update { it.copy(isLoadingNoteDetail = true) }
            getNoteDetailUseCase(spaceId, noteId)
                .onSuccess { detail ->
                    state.update { current ->
                        val existing = current.allNotes.firstOrNull { it.id == detail.id }
                        val merged = detail.copy(
                            project = detail.project ?: existing?.project,
                            citations = detail.citations.ifEmpty {
                                existing?.citations.orEmpty()
                            },
                        )
                        val updatedNotes = current.allNotes.map { note ->
                            if (note.id == merged.id) merged else note
                        }
                        val next = current.copy(
                            isLoadingNoteDetail = false,
                            allNotes = updatedNotes,
                            viewingNote = if (current.viewingNote?.id == noteId) {
                                merged
                            } else {
                                current.viewingNote
                            },
                            editingNote = if (current.editingNote?.id == noteId) {
                                merged
                            } else {
                                current.editingNote
                            },
                        )
                        next.copy(visibleNotes = filterNotes(next))
                    }
                }
                .onFailure { throwable ->
                    state.update {
                        it.copy(
                            isLoadingNoteDetail = false,
                            actionError = throwable.toHomeActionError(),
                        )
                    }
                }
        }
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
                .onFailure { throwable ->
                    state.update {
                        it.copy(actionError = throwable.toHomeActionError())
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

    fun onConvertNoteCreate(title: String, snapshot: String) {
        val converting = state.value.convertingNote ?: return
        val trimmedTitle = AddSourceInputRules.limitTitle(
            title.trim().ifBlank { AddSourceInputRules.defaultManualTitle() },
        )
        // Snapshot remains UI-preview only; the API snapshots note content server-side.
        if (snapshot.trim().isBlank()) return
        val convertSpaceId = converting.spaceId.ifBlank { spaceId }

        state.update {
            it.copy(
                convertingNote = null,
                viewingNote = null,
                isCreatingSource = true,
            )
        }

        scope.launch {
            convertNoteToSourceUseCase(
                spaceId = convertSpaceId,
                noteId = converting.id,
                title = trimmedTitle,
            ).onSuccess { created ->
                onSourceCreated(created, trimmedTitle)
            }.onFailure { throwable ->
                state.update {
                    it.copy(
                        isCreatingSource = false,
                        actionError = throwable.toHomeActionError(),
                    )
                }
            }
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
        val deleteSpaceId = deleting.spaceId.ifBlank { spaceId }

        scope.launch {
            deleteNoteUseCase(spaceId = deleteSpaceId, noteId = deleting.id)
                .onSuccess {
                    state.update { current ->
                        val existedInActiveSearch = current.allNotes.any { it.id == deleting.id }
                        val updatedNotes = if (existedInActiveSearch) {
                            current.allNotes.filterNot { it.id == deleting.id }
                        } else {
                            current.allNotes
                        }
                        val next = current.copy(
                            allNotes = updatedNotes,
                            notesAllCount = if (existedInActiveSearch) {
                                (current.notesAllCount - 1).coerceAtLeast(0)
                            } else {
                                current.notesAllCount
                            },
                            notesUserCreatedCount = if (
                                existedInActiveSearch &&
                                deleting.origin == NoteOrigin.USER_CREATED
                            ) {
                                (current.notesUserCreatedCount - 1).coerceAtLeast(0)
                            } else {
                                current.notesUserCreatedCount
                            },
                            notesSavedAnswerCount = if (
                                existedInActiveSearch &&
                                deleting.origin == NoteOrigin.SAVED_ANSWER
                            ) {
                                (current.notesSavedAnswerCount - 1).coerceAtLeast(0)
                            } else {
                                current.notesSavedAnswerCount
                            },
                            deletingNote = null,
                            viewingNote = null,
                            editingNote = null,
                            userMessage = HomeUserMessage.NOTE_DELETED,
                        )
                        next.copy(visibleNotes = filterNotes(next))
                    }
                }
                .onFailure { throwable ->
                    state.update {
                        it.copy(actionError = throwable.toHomeActionError())
                    }
                }
        }
    }

    private fun Note.matchesActiveNotesSearch(searchQuery: String): Boolean {
        val query = searchQuery.trim()
        if (query.isBlank()) return true
        return title.contains(query, ignoreCase = true) ||
            content.contains(query, ignoreCase = true) ||
            project?.contains(query, ignoreCase = true) == true
    }

    companion object {
        private const val CREATE_MIN_DELAY_MS = 1_000L
    }
}
