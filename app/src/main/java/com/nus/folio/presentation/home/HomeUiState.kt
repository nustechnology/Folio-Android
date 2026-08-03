package com.nus.folio.presentation.home

import com.nus.folio.R
import com.nus.folio.domain.model.AskTopic
import com.nus.folio.domain.model.Note
import com.nus.folio.domain.model.NoteFilter
import com.nus.folio.domain.model.Source
import com.nus.folio.domain.model.SourceFilter

data class HomeUiState(
    val spaceId: String = "",
    val spaceTitle: String = "",
    val isLoading: Boolean = false,
    val sourcesError: String? = null,
    val askError: String? = null,
    val notesError: String? = null,
    val searchQuery: String = "",
    val selectedFilter: SourceFilter = SourceFilter.ALL,
    val selectedNoteFilter: NoteFilter = NoteFilter.ALL,
    val selectedTab: HomeTab = HomeTab.SOURCES,
    val allSources: List<Source> = emptyList(),
    val visibleSources: List<Source> = emptyList(),
    val allAskTopics: List<AskTopic> = emptyList(),
    val visibleAskTopics: List<AskTopic> = emptyList(),
    val allNotes: List<Note> = emptyList(),
    val visibleNotes: List<Note> = emptyList(),
    val allCount: Int = 0,
    val papersCount: Int = 0,
    val booksCount: Int = 0,
    val webCount: Int = 0,
    val textCount: Int = 0,
    val notesAllCount: Int = 0,
    val notesPinnedCount: Int = 0,
    val notesUnfiledCount: Int = 0,
    val optionsNote: Note? = null,
    val viewingNote: Note? = null,
    val editingNote: Note? = null,
    val convertingNote: Note? = null,
    val deletingNote: Note? = null,
    val editingSource: Source? = null,
    val deletingSource: Source? = null,
    val showNotebookActions: Boolean = false,
    val showNotebookExport: Boolean = false,
    val processingSourceTitle: String? = null,
    val isOpeningSource: Boolean = false,
    val openSourceDetailId: String? = null,
    val userMessage: HomeUserMessage? = null,
    val askScope: AskScope = AskScope.CURRENT_SOURCE,
    val askSourceId: String? = null,
)

enum class AskScope {
    ENTIRE_SPACE,
    CURRENT_SOURCE,
}

fun HomeUiState.askSourceTitle(): String =
    askSourceId
        ?.let { id -> allSources.find { it.id == id }?.title }
        ?: allSources.firstOrNull()?.title.orEmpty()

fun HomeUiState.askScopeChipLabelRes(): Int = when (askScope) {
    AskScope.ENTIRE_SPACE -> R.string.answer_scope_entire_space
    AskScope.CURRENT_SOURCE -> R.string.home_ask_current_source
}

enum class HomeUserMessage {
    SOURCE_CREATED,
    SOURCE_UPDATED,
    SOURCE_DELETED,
    SOURCE_UPDATE_FAILED,
    SOURCE_DELETE_FAILED,
    NOTE_CREATED,
    NOTE_UPDATED,
    NOTE_DELETED,
    ADD_SOURCE_NOT_SUPPORTED,
    ASK_NOT_SUPPORTED,
    ADD_NOTE_NOT_SUPPORTED,
    ADD_NOTEBOOK_NOT_SUPPORTED,
    COPY_NOTEBOOK_NOT_SUPPORTED,
    EXPORT_NOTEBOOK_NOT_SUPPORTED,
    VIEW_NOTE_NOT_SUPPORTED,
    EDIT_NOTE_NOT_SUPPORTED,
    CONVERT_NOTE_NOT_SUPPORTED,
    DELETE_NOTE_NOT_SUPPORTED,
}

enum class HomeTab {
    SOURCES,
    ASK,
    NOTES,
    NOTEBOOK,
}

enum class NotebookExportFormat {
    MARKDOWN,
    PRINT_PDF,
}
