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
import com.nus.folio.domain.usecase.DeleteNoteUseCase
import com.nus.folio.domain.usecase.DeleteSourceUseCase
import com.nus.folio.domain.usecase.GetAskTopicsUseCase
import com.nus.folio.domain.usecase.GetNotesUseCase
import com.nus.folio.domain.usecase.GetSourcesUseCase
import com.nus.folio.domain.usecase.UpdateNoteUseCase
import com.nus.folio.domain.usecase.UpdateSourceUseCase
import com.nus.folio.presentation.home.bottomsheet.AddSourceDraft
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val spaceId: String,
    spaceTitle: String,
    private val getSourcesUseCase: GetSourcesUseCase,
    private val updateSourceUseCase: UpdateSourceUseCase,
    private val deleteSourceUseCase: DeleteSourceUseCase,
    private val getAskTopicsUseCase: GetAskTopicsUseCase,
    private val getNotesUseCase: GetNotesUseCase,
    private val updateNoteUseCase: UpdateNoteUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase,
    private val openSourceDelayMs: Long = OPEN_SOURCE_DELAY_MS,
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

            val sourcesDeferred = async { getSourcesUseCase(spaceId) }
            val askDeferred = async { getAskTopicsUseCase(spaceId) }
            val notesDeferred = async { getNotesUseCase(spaceId) }

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
                searchQuery = "",
            )
            next.copy(
                visibleSources = filterSources(next),
                visibleAskTopics = filterAskTopics(next),
                visibleNotes = filterNotes(next),
            )
        }
    }

    fun onAddSourceSubmit(draft: AddSourceDraft) {
        val title = when (draft) {
            is AddSourceDraft.Pdf -> draft.displayName.ifBlank {
                "Untitled PDF"
            }
            is AddSourceDraft.Web -> draft.title.ifBlank { draft.url }
            is AddSourceDraft.Text -> draft.title
        }
        _uiState.update {
            it.copy(processingSourceTitle = title)
        }
    }

    fun onSourceProcessingDismiss() {
        _uiState.update { it.copy(processingSourceTitle = null) }
    }

    fun onSourceProcessingOpenSource() {
        _uiState.update { it.copy(processingSourceTitle = null) }
    }

    fun onSourceProcessingAsk() {
        _uiState.update {
            it.copy(
                processingSourceTitle = null,
                selectedTab = HomeTab.ASK,
            )
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

    fun onEditSourceClick(source: Source) {
        _uiState.update { it.copy(editingSource = source) }
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
        _uiState.update { it.copy(editingSource = null) }
    }

    fun onEditSourceSave(title: String, author: String) {
        if (title.isBlank()) return
        val editing = _uiState.value.editingSource ?: return
        val updated = editing.copy(title = title.trim(), author = author.trim())

        viewModelScope.launch {
            updateSourceUseCase(updated)
                .onSuccess { saved ->
                    _uiState.update { state ->
                        val updatedSources = state.allSources.map { source ->
                            if (source.id == saved.id) saved else source
                        }
                        val next = state.copy(
                            allSources = updatedSources,
                            editingSource = null,
                            userMessage = HomeUserMessage.SOURCE_UPDATED,
                        )
                        next.copy(visibleSources = filterSources(next))
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(userMessage = HomeUserMessage.SOURCE_UPDATE_FAILED)
                    }
                }
        }
    }

    fun onDeleteSourceClick(source: Source) {
        _uiState.update { it.copy(deletingSource = source) }
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
                .onFailure {
                    _uiState.update {
                        it.copy(userMessage = HomeUserMessage.SOURCE_DELETE_FAILED)
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

    private fun filterSources(state: HomeUiState): List<Source> {
        val byType = when (state.selectedFilter) {
            SourceFilter.ALL -> state.allSources
            SourceFilter.PDF -> state.allSources.filter { it.type == SourceType.PDF }
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
                note.content.contains(query, ignoreCase = true) ||
                note.project.orEmpty().contains(query, ignoreCase = true)
        }
    }

    class Factory(
        private val spaceId: String,
        private val spaceTitle: String,
        private val getSourcesUseCase: GetSourcesUseCase,
        private val updateSourceUseCase: UpdateSourceUseCase,
        private val deleteSourceUseCase: DeleteSourceUseCase,
        private val getAskTopicsUseCase: GetAskTopicsUseCase,
        private val getNotesUseCase: GetNotesUseCase,
        private val updateNoteUseCase: UpdateNoteUseCase,
        private val deleteNoteUseCase: DeleteNoteUseCase,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(
                spaceId = spaceId,
                spaceTitle = spaceTitle,
                getSourcesUseCase = getSourcesUseCase,
                updateSourceUseCase = updateSourceUseCase,
                deleteSourceUseCase = deleteSourceUseCase,
                getAskTopicsUseCase = getAskTopicsUseCase,
                getNotesUseCase = getNotesUseCase,
                updateNoteUseCase = updateNoteUseCase,
                deleteNoteUseCase = deleteNoteUseCase,
            ) as T
        }
    }

    companion object {
        private const val OPEN_SOURCE_DELAY_MS = 1_500L
    }
}
