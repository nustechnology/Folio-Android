package com.nus.folio.presentation.home

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
    val isSearchVisible: Boolean = false,
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
    val userMessage: HomeUserMessage? = null,
)

enum class HomeUserMessage {
    ADD_SOURCE_NOT_SUPPORTED,
    ASK_NOT_SUPPORTED,
    ADD_NOTE_NOT_SUPPORTED,
    ADD_NOTEBOOK_NOT_SUPPORTED,
    EDIT_SOURCE_NOT_SUPPORTED,
    DELETE_SOURCE_NOT_SUPPORTED,
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
