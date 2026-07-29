package com.nus.folio.presentation.home

import com.nus.folio.domain.model.AskTopic
import com.nus.folio.domain.model.Note
import com.nus.folio.domain.model.NoteFilter
import com.nus.folio.domain.model.Source
import com.nus.folio.domain.model.SourceFilter

data class HomeUiState(
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
    val userMessage: HomeUserMessage? = null,
)

enum class HomeUserMessage {
    ADD_SOURCE_NOT_SUPPORTED,
}

enum class HomeTab {
    SOURCES,
    ASK,
    NOTES,
    ACCOUNT,
}
