package com.nus.folio.presentation.home

import com.nus.folio.domain.model.NoteFilter
import com.nus.folio.domain.model.SourceFilter
import com.nus.folio.domain.model.SourceType
import com.nus.folio.domain.usecase.GetAskTopicsUseCase
import com.nus.folio.domain.usecase.GetNotesUseCase
import com.nus.folio.domain.usecase.GetSourcesUseCase
import com.nus.folio.testing.FakeAskRepository
import com.nus.folio.testing.FakeNoteRepository
import com.nus.folio.testing.FakeSourceRepository
import com.nus.folio.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val sourceRepository = FakeSourceRepository()
    private val askRepository = FakeAskRepository()
    private val noteRepository = FakeNoteRepository()

    private fun createViewModel(): HomeViewModel =
        HomeViewModel(
            getSourcesUseCase = GetSourcesUseCase(sourceRepository),
            getAskTopicsUseCase = GetAskTopicsUseCase(askRepository),
            getNotesUseCase = GetNotesUseCase(noteRepository),
        )

    @Test
    fun `init loads sources successfully`() {
        val viewModel = createViewModel()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.sourcesError)
        assertNull(viewModel.uiState.value.askError)
        assertNull(viewModel.uiState.value.notesError)
        assertEquals(7, viewModel.uiState.value.visibleSources.size)
        assertEquals(128, viewModel.uiState.value.allCount)
        assertEquals(6, viewModel.uiState.value.textCount)
        assertEquals(1, sourceRepository.getSourcesCallCount)
        assertEquals(4, viewModel.uiState.value.visibleAskTopics.size)
        assertEquals(1, askRepository.getAskTopicsCallCount)
        assertEquals(5, viewModel.uiState.value.visibleNotes.size)
        assertEquals(5, viewModel.uiState.value.notesAllCount)
        assertEquals(1, noteRepository.getNotesCallCount)
    }

    @Test
    fun `init sets sourcesError when sources load fails`() {
        sourceRepository.getSourcesResult = Result.failure(IllegalStateException("offline"))

        val viewModel = createViewModel()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("offline", viewModel.uiState.value.sourcesError)
        assertNull(viewModel.uiState.value.askError)
        assertNull(viewModel.uiState.value.notesError)
        assertEquals(4, viewModel.uiState.value.visibleAskTopics.size)
        assertEquals(5, viewModel.uiState.value.visibleNotes.size)
    }

    @Test
    fun `onFilterSelected BOOKS shows only books`() {
        val viewModel = createViewModel()

        viewModel.onFilterSelected(SourceFilter.BOOKS)

        assertEquals(SourceFilter.BOOKS, viewModel.uiState.value.selectedFilter)
        assertEquals(1, viewModel.uiState.value.visibleSources.size)
        assertEquals(SourceType.BOOK, viewModel.uiState.value.visibleSources.first().type)
    }

    @Test
    fun `onFilterSelected WEB shows only web`() {
        val viewModel = createViewModel()

        viewModel.onFilterSelected(SourceFilter.WEB)

        assertEquals(1, viewModel.uiState.value.visibleSources.size)
        assertEquals(SourceType.WEB, viewModel.uiState.value.visibleSources.first().type)
    }

    @Test
    fun `onFilterSelected TEXT shows only text`() {
        val viewModel = createViewModel()

        viewModel.onFilterSelected(SourceFilter.TEXT)

        assertEquals(1, viewModel.uiState.value.visibleSources.size)
        assertEquals(SourceType.TEXT, viewModel.uiState.value.visibleSources.first().type)
    }

    @Test
    fun `onNoteFilterSelected PINNED shows only pinned notes`() {
        val viewModel = createViewModel()

        viewModel.onNoteFilterSelected(NoteFilter.PINNED)

        assertEquals(NoteFilter.PINNED, viewModel.uiState.value.selectedNoteFilter)
        assertEquals(2, viewModel.uiState.value.visibleNotes.size)
        assertTrue(viewModel.uiState.value.visibleNotes.all { it.isPinned })
    }

    @Test
    fun `onSearchQueryChange filters by title`() {
        val viewModel = createViewModel()

        viewModel.onSearchQueryChange("Turing")

        assertEquals(1, viewModel.uiState.value.visibleSources.size)
        assertTrue(viewModel.uiState.value.visibleSources.first().title.contains("Turing"))
    }

    @Test
    fun `onSearchQueryChange filters ask topics by title`() {
        val viewModel = createViewModel()

        viewModel.onSearchQueryChange("Policy")

        assertEquals(1, viewModel.uiState.value.visibleAskTopics.size)
        assertEquals("Public Policy Insights", viewModel.uiState.value.visibleAskTopics.first().title)
    }

    @Test
    fun `onSearchQueryChange filters notes by title`() {
        val viewModel = createViewModel()

        viewModel.onSearchQueryChange("Literature")

        assertEquals(1, viewModel.uiState.value.visibleNotes.size)
        assertEquals("Literature Review Outline", viewModel.uiState.value.visibleNotes.first().title)
    }

    @Test
    fun `onTabSelected updates selected tab`() {
        val viewModel = createViewModel()

        viewModel.onTabSelected(HomeTab.ASK)

        assertEquals(HomeTab.ASK, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun `onSearchClick toggles search visibility`() {
        val viewModel = createViewModel()

        viewModel.onSearchClick()
        assertTrue(viewModel.uiState.value.isSearchVisible)

        viewModel.onSearchClick()
        assertFalse(viewModel.uiState.value.isSearchVisible)
    }

    @Test
    fun `onSearchClick dismiss clears query and restores lists`() {
        val viewModel = createViewModel()
        viewModel.onSearchClick()
        viewModel.onSearchQueryChange("Turing")
        assertEquals(1, viewModel.uiState.value.visibleSources.size)

        viewModel.onSearchClick()

        assertFalse(viewModel.uiState.value.isSearchVisible)
        assertEquals("", viewModel.uiState.value.searchQuery)
        assertEquals(7, viewModel.uiState.value.visibleSources.size)
    }

    @Test
    fun `onTabSelected hides search field`() {
        val viewModel = createViewModel()
        viewModel.onSearchClick()

        viewModel.onTabSelected(HomeTab.NOTES)

        assertFalse(viewModel.uiState.value.isSearchVisible)
        assertEquals(HomeTab.NOTES, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun `onTabSelected clears query and restores lists`() {
        val viewModel = createViewModel()
        viewModel.onSearchClick()
        viewModel.onSearchQueryChange("Turing")

        viewModel.onTabSelected(HomeTab.ASK)

        assertEquals("", viewModel.uiState.value.searchQuery)
        assertEquals(7, viewModel.uiState.value.visibleSources.size)
        assertEquals(4, viewModel.uiState.value.visibleAskTopics.size)
    }

    @Test
    fun `loadSources reloads library`() {
        val viewModel = createViewModel()

        viewModel.loadSources()

        assertTrue(sourceRepository.getSourcesCallCount >= 2)
        assertEquals(7, viewModel.uiState.value.visibleSources.size)
        assertEquals(4, viewModel.uiState.value.visibleAskTopics.size)
        assertEquals(5, viewModel.uiState.value.visibleNotes.size)
    }

    @Test
    fun `onAddSourceSubmit surfaces not-supported message`() {
        val viewModel = createViewModel()

        viewModel.onAddSourceSubmit(AddSourceTab.PDF, "content://doc/1", "paper.pdf")

        assertEquals(
            HomeUserMessage.ADD_SOURCE_NOT_SUPPORTED,
            viewModel.uiState.value.userMessage,
        )
    }

    @Test
    fun `onUserMessageShown clears message`() {
        val viewModel = createViewModel()
        viewModel.onAddSourceSubmit(AddSourceTab.WEB, null, "https://example.com")

        viewModel.onUserMessageShown()

        assertNull(viewModel.uiState.value.userMessage)
    }
}
