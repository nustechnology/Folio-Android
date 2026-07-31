package com.nus.folio.presentation.home

import com.nus.folio.domain.model.NoteFilter
import com.nus.folio.domain.model.SourceFilter
import com.nus.folio.domain.model.SourceType
import com.nus.folio.domain.usecase.DeleteSourceUseCase
import com.nus.folio.domain.usecase.GetAskTopicsUseCase
import com.nus.folio.domain.usecase.GetNotesUseCase
import com.nus.folio.domain.usecase.GetSourcesUseCase
import com.nus.folio.domain.usecase.UpdateSourceUseCase
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

    private fun createViewModel(
        spaceId: String = "1",
        spaceTitle: String = "Dissertation Research",
    ): HomeViewModel =
        HomeViewModel(
            spaceId = spaceId,
            spaceTitle = spaceTitle,
            getSourcesUseCase = GetSourcesUseCase(sourceRepository),
            updateSourceUseCase = UpdateSourceUseCase(sourceRepository),
            deleteSourceUseCase = DeleteSourceUseCase(sourceRepository),
            getAskTopicsUseCase = GetAskTopicsUseCase(askRepository),
            getNotesUseCase = GetNotesUseCase(noteRepository),
        )

    @Test
    fun `init loads space-scoped home successfully`() {
        val viewModel = createViewModel()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("1", viewModel.uiState.value.spaceId)
        assertEquals("Dissertation Research", viewModel.uiState.value.spaceTitle)
        assertNull(viewModel.uiState.value.sourcesError)
        assertNull(viewModel.uiState.value.askError)
        assertNull(viewModel.uiState.value.notesError)
        assertEquals(4, viewModel.uiState.value.visibleSources.size)
        assertEquals(4, viewModel.uiState.value.allCount)
        assertEquals(0, viewModel.uiState.value.textCount)
        assertEquals(1, sourceRepository.getSourcesCallCount)
        assertEquals("1", sourceRepository.lastSpaceId)
        assertEquals(2, viewModel.uiState.value.visibleAskTopics.size)
        assertEquals(1, askRepository.getAskTopicsCallCount)
        assertEquals("1", askRepository.lastSpaceId)
        assertEquals(2, viewModel.uiState.value.visibleNotes.size)
        assertEquals(2, viewModel.uiState.value.notesAllCount)
        assertEquals(1, noteRepository.getNotesCallCount)
        assertEquals("1", noteRepository.lastSpaceId)
    }

    @Test
    fun `different spaces load different content`() {
        val dissertation = createViewModel(spaceId = "1")
        val teaching = createViewModel(spaceId = "4", spaceTitle = "Teaching Prep")

        assertEquals(4, dissertation.uiState.value.visibleSources.size)
        assertEquals(2, teaching.uiState.value.visibleSources.size)
        assertEquals(2, dissertation.uiState.value.visibleAskTopics.size)
        assertEquals(1, teaching.uiState.value.visibleAskTopics.size)
        assertEquals(2, dissertation.uiState.value.visibleNotes.size)
        assertEquals(1, teaching.uiState.value.visibleNotes.size)
    }

    @Test
    fun `init sets sourcesError when sources load fails`() {
        sourceRepository.getSourcesResult = Result.failure(IllegalStateException("offline"))

        val viewModel = createViewModel()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("offline", viewModel.uiState.value.sourcesError)
        assertNull(viewModel.uiState.value.askError)
        assertNull(viewModel.uiState.value.notesError)
        assertEquals(2, viewModel.uiState.value.visibleAskTopics.size)
        assertEquals(2, viewModel.uiState.value.visibleNotes.size)
    }

    @Test
    fun `onFilterSelected PDF shows only pdf`() {
        val viewModel = createViewModel()

        viewModel.onFilterSelected(SourceFilter.PDF)

        assertEquals(SourceFilter.PDF, viewModel.uiState.value.selectedFilter)
        assertEquals(3, viewModel.uiState.value.visibleSources.size)
        assertTrue(viewModel.uiState.value.visibleSources.all { it.type == SourceType.PDF })
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
        val viewModel = createViewModel(spaceId = "4", spaceTitle = "Teaching Prep")

        viewModel.onFilterSelected(SourceFilter.TEXT)

        assertEquals(1, viewModel.uiState.value.visibleSources.size)
        assertEquals(SourceType.TEXT, viewModel.uiState.value.visibleSources.first().type)
    }

    @Test
    fun `onNoteFilterSelected PINNED shows only pinned notes`() {
        val viewModel = createViewModel()

        viewModel.onNoteFilterSelected(NoteFilter.PINNED)

        assertEquals(NoteFilter.PINNED, viewModel.uiState.value.selectedNoteFilter)
        assertEquals(1, viewModel.uiState.value.visibleNotes.size)
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

        viewModel.onSearchQueryChange("Turing")

        assertEquals(1, viewModel.uiState.value.visibleAskTopics.size)
        assertEquals("Turing and modern AI", viewModel.uiState.value.visibleAskTopics.first().title)
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
        assertEquals(4, viewModel.uiState.value.visibleSources.size)
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
        assertEquals(4, viewModel.uiState.value.visibleSources.size)
        assertEquals(2, viewModel.uiState.value.visibleAskTopics.size)
    }

    @Test
    fun `loadSources reloads library`() {
        val viewModel = createViewModel()

        viewModel.loadSources()

        assertTrue(sourceRepository.getSourcesCallCount >= 2)
        assertEquals(4, viewModel.uiState.value.visibleSources.size)
        assertEquals(2, viewModel.uiState.value.visibleAskTopics.size)
        assertEquals(2, viewModel.uiState.value.visibleNotes.size)
    }

    @Test
    fun `onAddSourceSubmit surfaces not-supported message`() {
        val viewModel = createViewModel()

        viewModel.onAddSourceSubmit(
            AddSourceDraft.Pdf(
                displayName = "paper.pdf",
                uri = null,
            ),
        )

        assertEquals(
            HomeUserMessage.ADD_SOURCE_NOT_SUPPORTED,
            viewModel.uiState.value.userMessage,
        )
    }

    @Test
    fun `onEditSourceClick opens editing source`() {
        val viewModel = createViewModel()
        val source = viewModel.uiState.value.visibleSources.first()

        viewModel.onEditSourceClick(source)

        assertEquals(source, viewModel.uiState.value.editingSource)
    }

    @Test
    fun `onEditSourceDismiss clears editing source`() {
        val viewModel = createViewModel()
        val source = viewModel.uiState.value.visibleSources.first()
        viewModel.onEditSourceClick(source)

        viewModel.onEditSourceDismiss()

        assertNull(viewModel.uiState.value.editingSource)
    }

    @Test
    fun `onEditSourceSave updates title and author`() {
        val viewModel = createViewModel()
        val source = viewModel.uiState.value.visibleSources.first()
        viewModel.onEditSourceClick(source)

        viewModel.onEditSourceSave(title = "Updated title", author = "Updated author")

        assertNull(viewModel.uiState.value.editingSource)
        assertEquals(HomeUserMessage.SOURCE_UPDATED, viewModel.uiState.value.userMessage)
        val updated = viewModel.uiState.value.visibleSources.first { it.id == source.id }
        assertEquals("Updated title", updated.title)
        assertEquals("Updated author", updated.author)
        assertEquals(1, sourceRepository.updateSourceCallCount)
    }

    @Test
    fun `onEditSourceSave ignores blank title`() {
        val viewModel = createViewModel()
        val source = viewModel.uiState.value.visibleSources.first()
        viewModel.onEditSourceClick(source)

        viewModel.onEditSourceSave(title = "   ", author = "Updated author")

        assertEquals(source, viewModel.uiState.value.editingSource)
        assertNull(viewModel.uiState.value.userMessage)
        val unchanged = viewModel.uiState.value.visibleSources.first { it.id == source.id }
        assertEquals(source.title, unchanged.title)
        assertEquals(source.author, unchanged.author)
        assertEquals(0, sourceRepository.updateSourceCallCount)
    }

    @Test
    fun `onEditSourceSave keeps list unchanged when update fails`() {
        val viewModel = createViewModel()
        val source = viewModel.uiState.value.visibleSources.first()
        viewModel.onEditSourceClick(source)
        sourceRepository.updateSourceResult = Result.failure(IllegalStateException("offline"))

        viewModel.onEditSourceSave(title = "Updated title", author = "Updated author")

        assertEquals(HomeUserMessage.SOURCE_UPDATE_FAILED, viewModel.uiState.value.userMessage)
        val unchanged = viewModel.uiState.value.visibleSources.first { it.id == source.id }
        assertEquals(source.title, unchanged.title)
        assertEquals(source.author, unchanged.author)
    }

    @Test
    fun `onEditSourceSave retains edited fields after loadSources`() {
        val viewModel = createViewModel()
        val source = viewModel.uiState.value.visibleSources.first()
        val loadsBeforeReload = sourceRepository.getSourcesCallCount
        viewModel.onEditSourceClick(source)

        viewModel.onEditSourceSave(title = "Persisted title", author = "Persisted author")
        viewModel.onUserMessageShown()
        viewModel.loadSources()

        assertEquals(loadsBeforeReload + 1, sourceRepository.getSourcesCallCount)
        assertEquals(1, sourceRepository.updateSourceCallCount)
        assertEquals(source.id, sourceRepository.lastUpdatedSource?.id)
        val reloaded = viewModel.uiState.value.allSources.first { it.id == source.id }
        assertEquals("Persisted title", reloaded.title)
        assertEquals("Persisted author", reloaded.author)
        assertEquals(
            "Persisted title",
            viewModel.uiState.value.visibleSources.first { it.id == source.id }.title,
        )
    }

    @Test
    fun `onDeleteSourceClick opens delete confirmation`() {
        val viewModel = createViewModel()
        val source = viewModel.uiState.value.visibleSources.first()

        viewModel.onDeleteSourceClick(source)

        assertEquals(source, viewModel.uiState.value.deletingSource)
    }

    @Test
    fun `onDeleteSourceDismiss clears deleting source`() {
        val viewModel = createViewModel()
        val source = viewModel.uiState.value.visibleSources.first()
        viewModel.onDeleteSourceClick(source)

        viewModel.onDeleteSourceDismiss()

        assertNull(viewModel.uiState.value.deletingSource)
    }

    @Test
    fun `onDeleteSourceConfirm removes source and sets deleted message`() {
        val viewModel = createViewModel()
        val source = viewModel.uiState.value.visibleSources.first()
        val previousAllCount = viewModel.uiState.value.allCount
        viewModel.onDeleteSourceClick(source)

        viewModel.onDeleteSourceConfirm()

        assertNull(viewModel.uiState.value.deletingSource)
        assertEquals(HomeUserMessage.SOURCE_DELETED, viewModel.uiState.value.userMessage)
        assertTrue(viewModel.uiState.value.allSources.none { it.id == source.id })
        assertTrue(viewModel.uiState.value.visibleSources.none { it.id == source.id })
        assertEquals(previousAllCount - 1, viewModel.uiState.value.allCount)
        assertEquals(1, sourceRepository.deleteSourceCallCount)
        assertEquals(source.id, sourceRepository.lastDeletedSourceId)
    }

    @Test
    fun `onDeleteSourceConfirm keeps list unchanged when delete fails`() {
        val viewModel = createViewModel()
        val source = viewModel.uiState.value.visibleSources.first()
        val previousAllCount = viewModel.uiState.value.allCount
        viewModel.onDeleteSourceClick(source)
        sourceRepository.deleteSourceResult = Result.failure(IllegalStateException("offline"))

        viewModel.onDeleteSourceConfirm()

        assertEquals(HomeUserMessage.SOURCE_DELETE_FAILED, viewModel.uiState.value.userMessage)
        assertTrue(viewModel.uiState.value.allSources.any { it.id == source.id })
        assertEquals(previousAllCount, viewModel.uiState.value.allCount)
    }

    @Test
    fun `onDeleteSourceConfirm excludes deleted source after loadSources`() {
        val viewModel = createViewModel()
        val source = viewModel.uiState.value.visibleSources.first()
        val previousAllCount = viewModel.uiState.value.allCount
        val loadsBeforeReload = sourceRepository.getSourcesCallCount
        viewModel.onDeleteSourceClick(source)

        viewModel.onDeleteSourceConfirm()
        viewModel.onUserMessageShown()
        viewModel.loadSources()

        assertEquals(loadsBeforeReload + 1, sourceRepository.getSourcesCallCount)
        assertEquals(1, sourceRepository.deleteSourceCallCount)
        assertEquals(source.id, sourceRepository.lastDeletedSourceId)
        assertEquals(previousAllCount - 1, viewModel.uiState.value.allCount)
        assertTrue(viewModel.uiState.value.allSources.none { it.id == source.id })
        assertTrue(viewModel.uiState.value.visibleSources.none { it.id == source.id })
    }

    @Test
    fun `onNoteOptionsClick shows options sheet for note`() {
        val viewModel = createViewModel()
        val note = viewModel.uiState.value.visibleNotes.first()

        viewModel.onNoteOptionsClick(note)

        assertEquals(note, viewModel.uiState.value.optionsNote)
    }

    @Test
    fun `onViewNoteClick dismisses options and sets message`() {
        val viewModel = createViewModel()
        val note = viewModel.uiState.value.visibleNotes.first()
        viewModel.onNoteOptionsClick(note)

        viewModel.onViewNoteClick()

        assertNull(viewModel.uiState.value.optionsNote)
        assertEquals(HomeUserMessage.VIEW_NOTE_NOT_SUPPORTED, viewModel.uiState.value.userMessage)
    }

    @Test
    fun `onUserMessageShown clears message`() {
        val viewModel = createViewModel()
        viewModel.onAddSourceSubmit(
            AddSourceDraft.Web(
                url = "https://example.com",
                title = "Example",
                author = "Author",
            ),
        )

        viewModel.onUserMessageShown()

        assertNull(viewModel.uiState.value.userMessage)
    }
}
