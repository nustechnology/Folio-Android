package com.nus.folio.presentation.home

import com.nus.folio.domain.model.AskCitation
import com.nus.folio.domain.model.AskConversation
import com.nus.folio.domain.model.AskConversationDetail
import com.nus.folio.domain.model.AskConversationMessage
import com.nus.folio.domain.model.AskConversationRole
import com.nus.folio.domain.model.AskFeedbackRating
import com.nus.folio.domain.model.AskStreamEvent
import com.nus.folio.domain.model.AuthApiException
import com.nus.folio.domain.model.AuthSession
import com.nus.folio.domain.model.NoteFilter
import com.nus.folio.domain.model.NoteOrigin
import com.nus.folio.domain.model.NoteSort
import com.nus.folio.domain.model.Source
import com.nus.folio.domain.model.SourceFilter
import com.nus.folio.domain.model.SourceLibrary
import com.nus.folio.domain.model.SourceProcessingEvent
import com.nus.folio.domain.model.SourceProcessingState
import com.nus.folio.domain.model.SourceSort
import com.nus.folio.domain.model.SourceStatus
import com.nus.folio.domain.model.SourceType
import com.nus.folio.domain.repository.SourceFileBytes
import com.nus.folio.domain.repository.SourceFileBytesReader
import com.nus.folio.domain.usecase.ConvertNoteToSourceUseCase
import com.nus.folio.domain.usecase.CreateNoteUseCase
import com.nus.folio.domain.usecase.CreateSourceUseCase
import com.nus.folio.domain.usecase.DeleteAskConversationUseCase
import com.nus.folio.domain.usecase.DeleteNoteUseCase
import com.nus.folio.domain.usecase.DeleteSourceUseCase
import com.nus.folio.domain.usecase.GetAskConversationUseCase
import com.nus.folio.domain.usecase.GetAskConversationsUseCase
import com.nus.folio.domain.usecase.GetAskSuggestionsUseCase
import com.nus.folio.domain.usecase.GetCurrentSessionUseCase
import com.nus.folio.domain.usecase.GetNotebookUseCase
import com.nus.folio.domain.usecase.GetNoteDetailUseCase
import com.nus.folio.domain.usecase.GetNotesUseCase
import com.nus.folio.domain.usecase.GetSpacesUseCase
import com.nus.folio.domain.usecase.SaveNotebookUseCase
import com.nus.folio.domain.usecase.GetSourceDetailUseCase
import com.nus.folio.domain.usecase.GetSourcesUseCase
import com.nus.folio.domain.usecase.ObserveSourceProcessingUseCase
import com.nus.folio.domain.usecase.RefreshAuthSessionUseCase
import com.nus.folio.domain.usecase.RetrySourceUseCase
import com.nus.folio.domain.usecase.StreamAskAnswerUseCase
import com.nus.folio.domain.usecase.SubmitAskFeedbackUseCase
import com.nus.folio.domain.usecase.UpdateAskConversationUseCase
import com.nus.folio.domain.usecase.UpdateNoteUseCase
import com.nus.folio.domain.usecase.UpdateSourceUseCase
import com.nus.folio.domain.model.CreateSourceRequest
import com.nus.folio.domain.util.AddSourceInputRules
import com.nus.folio.domain.util.NotebookInputRules
import com.nus.folio.presentation.home.bottomsheet.AddSourceDraft
import com.nus.folio.presentation.home.notebook.NotebookExportHelper
import com.nus.folio.testing.FakeAskRepository
import com.nus.folio.testing.FakeAuthRepository
import com.nus.folio.testing.FakeNotebookRepository
import com.nus.folio.testing.FakeNoteRepository
import com.nus.folio.testing.FakeSourceRepository
import com.nus.folio.testing.FakeSpaceRepository
import com.nus.folio.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val sourceRepository = FakeSourceRepository()
    private val askRepository = FakeAskRepository()
    private val noteRepository = FakeNoteRepository()
    private val notebookRepository = FakeNotebookRepository()
    private val spaceRepository = FakeSpaceRepository()
    private val authRepository = FakeAuthRepository()
    private val sourceFileBytesReader = SourceFileBytesReader { uriString ->
        SourceFileBytes(
            fileName = uriString.substringAfterLast('/').ifBlank { "paper.pdf" },
            mimeType = "application/pdf",
            bytes = byteArrayOf(1, 2, 3),
        )
    }

    private fun createViewModel(
        spaceId: String = "1",
        spaceTitle: String = "Dissertation Research",
        researchObjective: String = "Primary research archive for doctoral thesis",
        openSourceDelayMs: Long = 0L,
        searchDebounceMs: Long = 0L,
        notebookSaveDebounceMs: Long = 0L,
    ): HomeViewModel =
        HomeViewModel(
            spaceId = spaceId,
            spaceTitle = spaceTitle,
            researchObjective = researchObjective,
            getSourcesUseCase = GetSourcesUseCase(sourceRepository),
            createSourceUseCase = CreateSourceUseCase(sourceRepository),
            observeSourceProcessingUseCase = ObserveSourceProcessingUseCase(sourceRepository),
            updateSourceUseCase = UpdateSourceUseCase(sourceRepository),
            deleteSourceUseCase = DeleteSourceUseCase(sourceRepository),
            getSourceDetailUseCase = GetSourceDetailUseCase(sourceRepository),
            getAskSuggestionsUseCase = GetAskSuggestionsUseCase(askRepository),
            getAskConversationsUseCase = GetAskConversationsUseCase(askRepository),
            getAskConversationUseCase = GetAskConversationUseCase(askRepository),
            updateAskConversationUseCase = UpdateAskConversationUseCase(askRepository),
            deleteAskConversationUseCase = DeleteAskConversationUseCase(askRepository),
            streamAskAnswerUseCase = StreamAskAnswerUseCase(askRepository),
            submitAskFeedbackUseCase = SubmitAskFeedbackUseCase(askRepository),
            getNotesUseCase = GetNotesUseCase(noteRepository),
            getNoteDetailUseCase = GetNoteDetailUseCase(noteRepository),
            createNoteUseCase = CreateNoteUseCase(noteRepository),
            updateNoteUseCase = UpdateNoteUseCase(noteRepository),
            deleteNoteUseCase = DeleteNoteUseCase(noteRepository),
            convertNoteToSourceUseCase = ConvertNoteToSourceUseCase(noteRepository),
            getNotebookUseCase = GetNotebookUseCase(notebookRepository),
            saveNotebookUseCase = SaveNotebookUseCase(notebookRepository),
            getSpacesUseCase = GetSpacesUseCase(spaceRepository),
            sourceFileBytesReader = sourceFileBytesReader,
            refreshAuthSessionUseCase = RefreshAuthSessionUseCase(authRepository),
            getCurrentSessionUseCase = GetCurrentSessionUseCase(authRepository),
            retrySourceUseCase = RetrySourceUseCase(sourceRepository),
            openSourceDelayMs = openSourceDelayMs,
            searchDebounceMs = searchDebounceMs,
            notebookSaveDebounceMs = notebookSaveDebounceMs,
            createMinDelayMs = 0L,
            createNoteMinDelayMs = 0L,
            loadMinDelayMs = 0L,
            filterSkeletonMinDelayMs = 0L,
        )

    @Test
    fun `init loads space-scoped home successfully`() {
        val viewModel = createViewModel()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("1", viewModel.uiState.value.spaceId)
        assertEquals("Dissertation Research", viewModel.uiState.value.spaceTitle)
        assertNull(viewModel.uiState.value.sourcesError)
        assertNull(viewModel.uiState.value.notesError)
        assertEquals(5, viewModel.uiState.value.visibleSources.size)
        assertEquals(5, viewModel.uiState.value.allCount)
        assertEquals(1, sourceRepository.getSourcesCallCount)
        assertEquals("1", sourceRepository.lastSpaceId)
        assertEquals(2, viewModel.uiState.value.visibleNotes.size)
        assertEquals(2, viewModel.uiState.value.notesAllCount)
        assertEquals(1, noteRepository.getNotesCallCount)
        assertEquals("1", noteRepository.lastSpaceId)
        assertEquals(2, viewModel.uiState.value.askConversations.size)
        assertFalse(viewModel.uiState.value.isAskChatOpen)
        assertEquals(1, askRepository.getConversationsCallCount)
        assertEquals("1", askRepository.lastConversationsSpaceId)
    }

    @Test
    fun `onSearchQueryChange searches conversations via API`() = runTest {
        val viewModel = createViewModel(searchDebounceMs = 0L)
        viewModel.onTabSelected(HomeTab.ASK)
        val loadsBefore = askRepository.getConversationsCallCount

        viewModel.onSearchQueryChange("operating")
        advanceUntilIdle()

        assertEquals("operating", askRepository.lastConversationsSearch)
        assertEquals(1, askRepository.lastConversationsPage)
        assertEquals(loadsBefore + 1, askRepository.getConversationsCallCount)
        assertEquals(1, viewModel.uiState.value.askConversations.size)
        assertEquals(
            "What were the operating costs in Q4?",
            viewModel.uiState.value.askConversations.first().title,
        )
        assertFalse(viewModel.uiState.value.isSearchingAskConversations)
    }

    @Test
    fun `onLoadMoreConversations appends next page`() = runTest {
        askRepository.conversationsBySpace = mapOf(
            "1" to             (1..12).map { index ->
                AskConversation(
                    id = "conv-$index",
                    title = "Conversation $index",
                    dateLabel = "Aug 20, 07:54",
                    spaceId = "1",
                )
            },
        )
        val viewModel = createViewModel()
        assertEquals(10, viewModel.uiState.value.askConversations.size)
        assertTrue(viewModel.uiState.value.askConversationsHasMore)

        viewModel.onTabSelected(HomeTab.ASK)
        viewModel.onLoadMoreConversations()
        advanceUntilIdle()

        assertEquals(12, viewModel.uiState.value.askConversations.size)
        assertEquals(2, askRepository.lastConversationsPage)
        assertFalse(viewModel.uiState.value.askConversationsHasMore)
        assertFalse(viewModel.uiState.value.isLoadingMoreAskConversations)
    }

    @Test
    fun `different spaces load different content`() {
        val dissertation = createViewModel(spaceId = "1")
        val teaching = createViewModel(spaceId = "4", spaceTitle = "Teaching Prep")

        assertEquals(5, dissertation.uiState.value.visibleSources.size)
        assertEquals(2, teaching.uiState.value.visibleSources.size)
        assertEquals(2, dissertation.uiState.value.visibleNotes.size)
        assertEquals(1, teaching.uiState.value.visibleNotes.size)
    }

    @Test
    fun `init sets sourcesError when sources load fails`() {
        sourceRepository.getSourcesResult = Result.failure(IllegalStateException("offline"))

        val viewModel = createViewModel()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("offline", viewModel.uiState.value.sourcesError)
        assertNull(viewModel.uiState.value.notesError)
        assertEquals(2, viewModel.uiState.value.visibleNotes.size)
    }

    @Test
    fun `onFilterSelected FILE shows only file`() {
        val viewModel = createViewModel()

        viewModel.onFilterSelected(SourceFilter.FILE)

        assertEquals(SourceFilter.FILE, viewModel.uiState.value.selectedFilter)
        assertEquals("File", sourceRepository.lastSourceType)
        assertEquals(4, viewModel.uiState.value.visibleSources.size)
        assertTrue(viewModel.uiState.value.visibleSources.all { it.type == SourceType.FILE })
        assertFalse(viewModel.uiState.value.isFilteringSources)
    }

    @Test
    fun `onFilterSelected shows filtering skeleton until API returns`() = runTest {
        val filterStarted = CompletableDeferred<Unit>()
        val releaseFilter = CompletableDeferred<Unit>()
        sourceRepository.getSourcesGate = { sourceType, _ ->
            if (sourceType == "File") {
                filterStarted.complete(Unit)
                releaseFilter.await()
            }
        }
        val viewModel = createViewModel()

        viewModel.onFilterSelected(SourceFilter.FILE)
        filterStarted.await()

        assertTrue(viewModel.uiState.value.isFilteringSources)

        releaseFilter.complete(Unit)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isFilteringSources)
        assertEquals(4, viewModel.uiState.value.visibleSources.size)
    }

    @Test
    fun `onFilterSortClick shows sort sheet`() {
        val viewModel = createViewModel()

        viewModel.onFilterSortClick()

        assertTrue(viewModel.uiState.value.showSortSheet)

        viewModel.onSortSheetDismiss()

        assertFalse(viewModel.uiState.value.showSortSheet)
    }

    @Test
    fun `onSortSelected reloads sources with selected sort`() {
        val viewModel = createViewModel()
        assertEquals(SourceSort.RECENTLY_ADDED, sourceRepository.lastSort)

        viewModel.onSortSelected(SourceSort.ALPHABETICAL_ZA)

        assertFalse(viewModel.uiState.value.showSortSheet)
        assertEquals(SourceSort.ALPHABETICAL_ZA, viewModel.uiState.value.selectedSort)
        assertEquals(SourceSort.ALPHABETICAL_ZA, sourceRepository.lastSort)
        assertFalse(viewModel.uiState.value.isFilteringSources)
    }

    @Test
    fun `onSortSelected shows filtering skeleton until API returns`() = runTest {
        val sortStarted = CompletableDeferred<Unit>()
        val releaseSort = CompletableDeferred<Unit>()
        sourceRepository.getSourcesGate = { _, _ ->
            if (sourceRepository.lastSort == SourceSort.ALPHABETICAL_ZA) {
                sortStarted.complete(Unit)
                releaseSort.await()
            }
        }
        val viewModel = createViewModel()

        viewModel.onSortSelected(SourceSort.ALPHABETICAL_ZA)
        sortStarted.await()

        assertTrue(viewModel.uiState.value.isFilteringSources)

        releaseSort.complete(Unit)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isFilteringSources)
        assertEquals(SourceSort.ALPHABETICAL_ZA, sourceRepository.lastSort)
    }

    @Test
    fun `onSortSelected same sort only dismisses sheet`() {
        val viewModel = createViewModel()
        val callsBefore = sourceRepository.getSourcesCallCount
        viewModel.onFilterSortClick()

        viewModel.onSortSelected(SourceSort.RECENTLY_ADDED)

        assertFalse(viewModel.uiState.value.showSortSheet)
        assertEquals(callsBefore, sourceRepository.getSourcesCallCount)
        assertFalse(viewModel.uiState.value.isFilteringSources)
    }

    @Test
    fun `onFilterSelected WEB shows only web`() {
        val viewModel = createViewModel()

        viewModel.onFilterSelected(SourceFilter.WEB)

        assertEquals("Web", sourceRepository.lastSourceType)
        assertEquals(1, viewModel.uiState.value.visibleSources.size)
        assertEquals(SourceType.WEB, viewModel.uiState.value.visibleSources.first().type)
    }

    @Test
    fun `onFilterSelected TEXT shows only text`() {
        val viewModel = createViewModel(spaceId = "4", spaceTitle = "Teaching Prep")

        viewModel.onFilterSelected(SourceFilter.TEXT)

        assertEquals("Manual", sourceRepository.lastSourceType)
        assertEquals(1, viewModel.uiState.value.visibleSources.size)
        assertEquals(SourceType.TEXT, viewModel.uiState.value.visibleSources.first().type)
    }

    @Test
    fun `newer filter result is not overwritten by slower older filter`() = runTest {
        val firstFilterStarted = CompletableDeferred<Unit>()
        val releaseFirstFilter = CompletableDeferred<Unit>()
        sourceRepository.getSourcesGate = { sourceType, _ ->
            if (sourceType == "File") {
                firstFilterStarted.complete(Unit)
                releaseFirstFilter.await()
            }
        }
        val viewModel = createViewModel()

        viewModel.onFilterSelected(SourceFilter.FILE)
        firstFilterStarted.await()
        viewModel.onFilterSelected(SourceFilter.WEB)
        releaseFirstFilter.complete(Unit)

        assertEquals(SourceFilter.WEB, viewModel.uiState.value.selectedFilter)
        assertEquals("Web", sourceRepository.lastSourceType)
        assertTrue(viewModel.uiState.value.visibleSources.all { it.type == SourceType.WEB })
        assertEquals(1, viewModel.uiState.value.visibleSources.size)
        assertFalse(viewModel.uiState.value.isFilteringSources)
    }

    @Test
    fun `onNoteFilterSelected USER_CREATED reloads via API origin`() {
        val viewModel = createViewModel()
        val callsBefore = noteRepository.getNotesCallCount

        viewModel.onNoteFilterSelected(NoteFilter.USER_CREATED)

        assertEquals(NoteFilter.USER_CREATED, viewModel.uiState.value.selectedNoteFilter)
        assertEquals("UserCreated", noteRepository.lastOrigin)
        assertEquals(callsBefore + 1, noteRepository.getNotesCallCount)
        assertEquals(1, viewModel.uiState.value.visibleNotes.size)
        assertTrue(viewModel.uiState.value.visibleNotes.all { it.origin == NoteOrigin.USER_CREATED })
        assertFalse(viewModel.uiState.value.isFilteringNotes)
    }

    @Test
    fun `onNoteFilterSelected shows filtering skeleton until API returns`() = runTest {
        val filterStarted = CompletableDeferred<Unit>()
        val releaseFilter = CompletableDeferred<Unit>()
        noteRepository.getNotesGate = { origin, _ ->
            if (origin == "UserCreated") {
                filterStarted.complete(Unit)
                releaseFilter.await()
            }
        }
        val viewModel = createViewModel()

        viewModel.onNoteFilterSelected(NoteFilter.USER_CREATED)
        filterStarted.await()

        assertTrue(viewModel.uiState.value.isFilteringNotes)

        releaseFilter.complete(Unit)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isFilteringNotes)
        assertEquals(1, viewModel.uiState.value.visibleNotes.size)
    }

    @Test
    fun `onNoteSortSelected shows filtering skeleton until API returns`() = runTest {
        val sortStarted = CompletableDeferred<Unit>()
        val releaseSort = CompletableDeferred<Unit>()
        noteRepository.getNotesGate = { _, _ ->
            if (noteRepository.lastSort == NoteSort.ALPHABETICAL_ZA) {
                sortStarted.complete(Unit)
                releaseSort.await()
            }
        }
        val viewModel = createViewModel()
        viewModel.onTabSelected(HomeTab.NOTES)

        viewModel.onNoteSortSelected(NoteSort.ALPHABETICAL_ZA)
        sortStarted.await()

        assertTrue(viewModel.uiState.value.isFilteringNotes)

        releaseSort.complete(Unit)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isFilteringNotes)
        assertEquals(NoteSort.ALPHABETICAL_ZA, noteRepository.lastSort)
    }

    @Test
    fun `onNoteFilterSelected SAVED_ANSWER reloads via API origin`() {
        val viewModel = createViewModel()

        viewModel.onNoteFilterSelected(NoteFilter.SAVED_ANSWER)

        assertEquals(NoteFilter.SAVED_ANSWER, viewModel.uiState.value.selectedNoteFilter)
        assertEquals("SavedAssistantAnswer", noteRepository.lastOrigin)
        assertEquals(1, viewModel.uiState.value.visibleNotes.size)
        assertTrue(viewModel.uiState.value.visibleNotes.all { it.origin == NoteOrigin.SAVED_ANSWER })
        assertFalse(viewModel.uiState.value.isFilteringNotes)
    }

    @Test
    fun `onSearchQueryChange filters by title`() {
        val viewModel = createViewModel()

        viewModel.onSearchQueryChange("Turing")

        assertEquals("Turing", sourceRepository.lastSearch)
        assertEquals(1, viewModel.uiState.value.visibleSources.size)
        assertTrue(viewModel.uiState.value.visibleSources.first().title.contains("Turing"))
    }

    @Test
    fun `onSearchQueryChange filters notes by title`() = runTest {
        val viewModel = createViewModel(searchDebounceMs = 0L)
        viewModel.onTabSelected(HomeTab.NOTES)
        val loadsBefore = noteRepository.getNotesCallCount

        viewModel.onSearchQueryChange("Literature")
        advanceUntilIdle()

        assertEquals("Literature", noteRepository.lastSearch)
        assertEquals(loadsBefore + 1, noteRepository.getNotesCallCount)
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
    fun `onTabSelected Ask returns to conversation list`() = runTest {
        val viewModel = createViewModel()
        viewModel.onTabSelected(HomeTab.ASK)
        viewModel.onConversationClick(viewModel.uiState.value.askConversations.first())
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isAskChatOpen)

        viewModel.onTabSelected(HomeTab.ASK)
        advanceUntilIdle()

        assertEquals(HomeTab.ASK, viewModel.uiState.value.selectedTab)
        assertFalse(viewModel.uiState.value.isAskChatOpen)
        assertTrue(viewModel.uiState.value.askMessages.isEmpty())
    }

    @Test
    fun `onTabSelected leaving Ask with search and open chat reloads conversations once`() = runTest {
        val viewModel = createViewModel(searchDebounceMs = 0L)
        viewModel.onTabSelected(HomeTab.ASK)
        viewModel.onSearchQueryChange("operating")
        advanceUntilIdle()
        viewModel.onConversationClick(viewModel.uiState.value.askConversations.first())
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isAskChatOpen)
        assertEquals("operating", viewModel.uiState.value.searchQuery)
        val loadsBefore = askRepository.getConversationsCallCount

        viewModel.onTabSelected(HomeTab.NOTES)
        advanceUntilIdle()

        assertEquals(loadsBefore + 1, askRepository.getConversationsCallCount)
        assertNull(askRepository.lastConversationsSearch)
        assertFalse(viewModel.uiState.value.isAskChatOpen)
    }

    @Test
    fun `onTabSelected leaving Ask with search and closed chat reloads conversations`() = runTest {
        val viewModel = createViewModel(searchDebounceMs = 0L)
        viewModel.onTabSelected(HomeTab.ASK)
        viewModel.onSearchQueryChange("operating")
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isAskChatOpen)
        val loadsBefore = askRepository.getConversationsCallCount

        viewModel.onTabSelected(HomeTab.NOTES)
        advanceUntilIdle()

        assertEquals(loadsBefore + 1, askRepository.getConversationsCallCount)
        assertNull(askRepository.lastConversationsSearch)
    }

    @Test
    fun `onSourceClick opens source detail after delay`() {
        val viewModel = createViewModel(openSourceDelayMs = 0L)
        val source = viewModel.uiState.value.visibleSources.first { it.status == SourceStatus.READY }

        viewModel.onSourceClick(source)

        assertFalse(viewModel.uiState.value.isOpeningSource)
        assertEquals(source.id, viewModel.uiState.value.openSourceDetailId)
        assertNull(viewModel.uiState.value.processingSourceId)

        viewModel.onOpenSourceDetailHandled()

        assertNull(viewModel.uiState.value.openSourceDetailId)
    }

    @Test
    fun `onSourceClick PROCESSING opens processing sheet`() {
        val viewModel = createViewModel(spaceId = "2", spaceTitle = "Urban Mobility", openSourceDelayMs = 0L)
        val source = viewModel.uiState.value.visibleSources.first { it.status == SourceStatus.PROCESSING }

        viewModel.onSourceClick(source)

        assertEquals(source.id, viewModel.uiState.value.processingSourceId)
        assertEquals(source.title, viewModel.uiState.value.processingSourceTitle)
        assertEquals(SourceProcessingState.ADDED, viewModel.uiState.value.processingState)
        assertNull(viewModel.uiState.value.openSourceDetailId)
    }

    @Test
    fun `onSourceClick FAILED opens processing sheet in failed state`() {
        val viewModel = createViewModel(openSourceDelayMs = 0L)
        val source = viewModel.uiState.value.visibleSources.first { it.status == SourceStatus.FAILED }

        viewModel.onSourceClick(source)

        assertEquals(source.id, viewModel.uiState.value.processingSourceId)
        assertEquals(source.title, viewModel.uiState.value.processingSourceTitle)
        assertEquals(SourceProcessingState.FAILED, viewModel.uiState.value.processingState)
        assertNull(viewModel.uiState.value.openSourceDetailId)
    }

    @Test
    fun `onTabSelected clears search query`() {
        val viewModel = createViewModel()
        viewModel.onSearchQueryChange("Turing")

        viewModel.onTabSelected(HomeTab.NOTES)

        assertEquals("", viewModel.uiState.value.searchQuery)
        assertEquals(HomeTab.NOTES, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun `onTabSelected clears query and restores lists`() {
        val viewModel = createViewModel()
        viewModel.onSearchQueryChange("Turing")

        viewModel.onTabSelected(HomeTab.ASK)

        assertEquals("", viewModel.uiState.value.searchQuery)
        assertEquals(5, viewModel.uiState.value.visibleSources.size)
    }

    @Test
    fun `clearSearch clears query and reloads sources`() {
        val viewModel = createViewModel()
        viewModel.onSearchQueryChange("Turing")
        val callsAfterSearch = sourceRepository.getSourcesCallCount

        viewModel.clearSearch()

        assertEquals("", viewModel.uiState.value.searchQuery)
        assertEquals(5, viewModel.uiState.value.visibleSources.size)
        assertTrue(sourceRepository.getSourcesCallCount > callsAfterSearch)
        assertNull(sourceRepository.lastSearch)
    }

    @Test
    fun `loadSources reloads library`() {
        val viewModel = createViewModel()

        viewModel.loadSources()

        assertTrue(sourceRepository.getSourcesCallCount >= 2)
        assertEquals(5, viewModel.uiState.value.visibleSources.size)
        assertEquals(2, viewModel.uiState.value.visibleNotes.size)
    }

    @Test
    fun `onLoadMoreSources appends next page`() {
        val pageOne = SourceLibrary(
            sources = listOf(
                Source("p1", "Page One", SourceType.FILE, "A", "Added 1d ago", SourceStatus.READY, "1", "pdf"),
            ),
            allCount = 2,
            papersCount = 2,
            booksCount = 0,
            webCount = 0,
            textCount = 0,
            page = 1,
            limit = 1,
            hasMore = true,
        )
        val pageTwo = SourceLibrary(
            sources = listOf(
                Source("p2", "Page Two", SourceType.FILE, "B", "Added 1d ago", SourceStatus.READY, "1", "pdf"),
            ),
            allCount = 2,
            papersCount = 2,
            booksCount = 0,
            webCount = 0,
            textCount = 0,
            page = 2,
            limit = 1,
            hasMore = false,
        )
        sourceRepository.getSourcesResult = Result.success(pageOne)
        val viewModel = createViewModel()
        assertEquals(1, viewModel.uiState.value.visibleSources.size)
        assertTrue(viewModel.uiState.value.sourcesHasMore)

        sourceRepository.getSourcesResult = Result.success(pageTwo)
        viewModel.onLoadMoreSources()

        assertEquals(listOf("p1", "p2"), viewModel.uiState.value.visibleSources.map { it.id })
        assertFalse(viewModel.uiState.value.sourcesHasMore)
        assertFalse(viewModel.uiState.value.isLoadingMoreSources)
        assertEquals(2, viewModel.uiState.value.sourcesCurrentPage)
    }

    @Test
    fun `onLoadMoreSources is ignored when hasMore is false`() {
        val viewModel = createViewModel()
        assertFalse(viewModel.uiState.value.sourcesHasMore)
        val callsBefore = sourceRepository.getSourcesCallCount

        viewModel.onLoadMoreSources()

        assertEquals(callsBefore, sourceRepository.getSourcesCallCount)
    }

    @Test
    fun `onRefreshSources reloads sources without full-screen loading`() {
        val viewModel = createViewModel()
        assertFalse(viewModel.uiState.value.isLoading)
        val loadsBefore = sourceRepository.getSourcesCallCount

        viewModel.onRefreshSources()

        assertEquals(loadsBefore + 1, sourceRepository.getSourcesCallCount)
        assertFalse(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.isRefreshingSources)
        assertEquals(5, viewModel.uiState.value.visibleSources.size)
    }

    @Test
    fun `onRefreshNotes reloads notes without full-screen loading`() {
        val viewModel = createViewModel()
        assertFalse(viewModel.uiState.value.isLoading)
        val loadsBefore = noteRepository.getNotesCallCount

        viewModel.onRefreshNotes()

        assertEquals(loadsBefore + 1, noteRepository.getNotesCallCount)
        assertFalse(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.isRefreshingNotes)
        assertTrue(viewModel.uiState.value.visibleNotes.isNotEmpty())
    }

    @Test
    fun `onRetry refreshes session then reloads home`() {
        sourceRepository.getSourcesResult = Result.failure(IllegalStateException("unauthorized"))
        val viewModel = createViewModel()
        assertEquals("unauthorized", viewModel.uiState.value.sourcesError)

        sourceRepository.getSourcesResult = null
        authRepository.refreshSessionResult = Result.success(
            AuthSession(
                email = "jordan@folio.app",
                accessToken = "access-refreshed",
                refreshToken = "refresh-refreshed",
            ),
        )

        viewModel.onRetry()

        assertEquals(1, authRepository.refreshSessionCallCount)
        assertNull(viewModel.uiState.value.sourcesError)
        assertEquals(5, viewModel.uiState.value.visibleSources.size)
        assertFalse(viewModel.uiState.value.requiresReauth)
    }

    @Test
    fun `onRetry requires reauth when refresh clears session`() {
        sourceRepository.getSourcesResult = Result.failure(IllegalStateException("unauthorized"))
        val viewModel = createViewModel()

        authRepository.refreshSessionResult = Result.failure(
            AuthApiException("Refresh token expired"),
        )

        viewModel.onRetry()

        assertEquals(1, authRepository.refreshSessionCallCount)
        assertTrue(viewModel.uiState.value.requiresReauth)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `onAddSourceSubmit applies manual defaults for blank title and author`() {
        authRepository.seedSession(
            AuthSession(email = "ada@folio.app", displayName = "Ada Lovelace"),
        )
        val viewModel = createViewModel()

        viewModel.onAddSourceSubmit(
            AddSourceDraft.Text(
                title = "",
                author = "",
                content = "Enough content for the minimum length.",
            ),
        )

        val request = sourceRepository.lastCreateRequest as CreateSourceRequest.Manual
        assertTrue(request.title.startsWith("Untitled Source - "))
        assertEquals("Ada Lovelace", request.author)
        assertEquals(
            request.title,
            viewModel.uiState.value.processingSourceTitle,
        )
    }

    @Test
    fun `onAddSourceSubmit applies web author default from domain`() {
        val viewModel = createViewModel()

        viewModel.onAddSourceSubmit(
            AddSourceDraft.Web(
                url = "https://www.example.org/article",
                title = "",
                author = "",
            ),
        )

        val request = sourceRepository.lastCreateRequest as CreateSourceRequest.Web
        assertEquals("https://www.example.org/article", request.sourceUrl)
        assertEquals("", request.title)
        assertEquals("example.org", request.author)
    }

    @Test
    fun `onAddSourceSubmit shows processing sheet with source title`() {
        val viewModel = createViewModel()

        viewModel.onAddSourceSubmit(
            AddSourceDraft.Text(
                title = "Memo",
                author = "Author",
                content = "Body content",
            ),
        )

        assertEquals("Memo", viewModel.uiState.value.processingSourceTitle)
        assertEquals(SourceProcessingState.ADDED, viewModel.uiState.value.processingState)
        assertEquals(0, viewModel.uiState.value.processingProgress)
        assertEquals(HomeUserMessage.SOURCE_CREATED, viewModel.uiState.value.userMessage)
        assertFalse(viewModel.uiState.value.showAddSourceSheet)
        assertFalse(viewModel.uiState.value.isCreatingSource)
        assertEquals(1, sourceRepository.createSourceCallCount)
        assertEquals(1, sourceRepository.observeSourceProcessingCallCount)
        assertEquals(6, viewModel.uiState.value.allCount)
    }

    @Test
    fun `onAddSourceClick opens add source sheet`() {
        val viewModel = createViewModel()

        viewModel.onAddSourceClick()

        assertTrue(viewModel.uiState.value.showAddSourceSheet)
    }

    @Test
    fun `onAddSourceFileSelected shows success toast`() {
        val viewModel = createViewModel()

        viewModel.onAddSourceFileSelected()

        assertEquals(HomeUserMessage.SOURCE_FILE_SELECTED, viewModel.uiState.value.userMessage)
    }

    @Test
    fun `onAddSourceFileSelectionFailed reports unsupported format`() {
        val viewModel = createViewModel()

        viewModel.onAddSourceFileSelectionFailed(
            AddSourceInputRules.FileValidationError.UNSUPPORTED_FORMAT,
        )

        assertEquals(HomeActionError.FILE_UNSUPPORTED, viewModel.uiState.value.actionError)
    }

    @Test
    fun `onAddSourceFileSelectionFailed reports file too large`() {
        val viewModel = createViewModel()

        viewModel.onAddSourceFileSelectionFailed(
            AddSourceInputRules.FileValidationError.SIZE_EXCEEDED,
        )

        assertEquals(HomeActionError.FILE_TOO_LARGE, viewModel.uiState.value.actionError)
    }

    @Test
    fun `processing events update progress for created source`() = runTest {
        val viewModel = createViewModel()
        viewModel.onAddSourceSubmit(
            AddSourceDraft.Text(
                title = "Memo",
                author = "Author",
                content = "Body content",
            ),
        )
        val sourceId = viewModel.uiState.value.processingSourceId!!

        sourceRepository.emitProcessingEvent(
            SourceProcessingEvent(sourceId, SourceProcessingState.EXTRACTING_TEXT, 25),
        )
        sourceRepository.emitProcessingEvent(
            SourceProcessingEvent("other-id", SourceProcessingState.READY, 100),
        )
        sourceRepository.emitProcessingEvent(
            SourceProcessingEvent(sourceId, SourceProcessingState.READY, 100),
        )

        assertEquals(100, viewModel.uiState.value.processingProgress)
        assertEquals(SourceProcessingState.READY, viewModel.uiState.value.processingState)
        assertEquals(sourceId, viewModel.uiState.value.processingSourceId)
    }

    @Test
    fun `onAddSourceSubmit shows failure when create fails`() {
        sourceRepository.createSourceResult = Result.failure(IllegalStateException("offline"))
        val viewModel = createViewModel()

        viewModel.onAddSourceSubmit(
            AddSourceDraft.Web(
                url = "https://example.com",
                title = "Article",
                author = "",
            ),
        )

        assertNull(viewModel.uiState.value.processingSourceTitle)
        assertNull(viewModel.uiState.value.userMessage)
        assertEquals(HomeActionError.GENERIC, viewModel.uiState.value.actionError)
    }

    @Test
    fun `onAddSourceSubmit fails when file uri is missing`() {
        val viewModel = createViewModel()

        viewModel.onAddSourceSubmit(
            AddSourceDraft.File(
                displayName = "paper.pdf",
                uri = null,
            ),
        )

        assertNull(viewModel.uiState.value.processingSourceTitle)
        assertNull(viewModel.uiState.value.userMessage)
        assertEquals(HomeActionError.FILE_REQUIRED, viewModel.uiState.value.actionError)
        assertEquals(0, sourceRepository.createSourceCallCount)
    }

    @Test
    fun `onSourceProcessingAsk dismisses sheet and opens ask tab`() {
        val viewModel = createViewModel()
        viewModel.onAddSourceSubmit(
            AddSourceDraft.Text(
                title = "Memo",
                author = "Author",
                content = "Body",
            ),
        )
        val sourceId = viewModel.uiState.value.processingSourceId!!

        viewModel.onSourceProcessingAsk()

        assertNull(viewModel.uiState.value.processingSourceTitle)
        assertNull(viewModel.uiState.value.processingSourceId)
        assertEquals(HomeTab.ASK, viewModel.uiState.value.selectedTab)
        assertEquals(AskScope.CURRENT_SOURCE, viewModel.uiState.value.askScope)
        assertEquals(sourceId, viewModel.uiState.value.askSourceId)
        assertEquals(1, viewModel.uiState.value.askConversationEpoch)
        assertTrue(viewModel.uiState.value.askMessages.isEmpty())
    }

    @Test
    fun `ask scope defaults to entire space`() {
        val viewModel = createViewModel()

        assertEquals(AskScope.ENTIRE_SPACE, viewModel.uiState.value.askScope)
        assertNull(viewModel.uiState.value.askSourceId)
        assertEquals(0, viewModel.uiState.value.askConversationEpoch)
    }

    @Test
    fun `onAskScopeOptionSelected switches to source and resets conversation`() {
        val viewModel = createViewModel()
        val sourceId = viewModel.uiState.value.allSources.first().id

        viewModel.onAskScopeOptionSelected(sourceId)

        assertEquals(AskScope.CURRENT_SOURCE, viewModel.uiState.value.askScope)
        assertEquals(sourceId, viewModel.uiState.value.askSourceId)
        assertEquals(1, viewModel.uiState.value.askConversationEpoch)
        assertTrue(viewModel.uiState.value.askMessages.isEmpty())
    }

    @Test
    fun `onAskScopeOptionSelected switching sources resets conversation`() {
        val viewModel = createViewModel()
        val sources = viewModel.uiState.value.allSources
        val firstId = sources[0].id
        val secondId = sources[1].id

        viewModel.onAskScopeOptionSelected(firstId)
        viewModel.onAskScopeOptionSelected(secondId)

        assertEquals(AskScope.CURRENT_SOURCE, viewModel.uiState.value.askScope)
        assertEquals(secondId, viewModel.uiState.value.askSourceId)
        assertEquals(2, viewModel.uiState.value.askConversationEpoch)
        assertTrue(viewModel.uiState.value.askMessages.isEmpty())
    }

    @Test
    fun `onAskScopeOptionSelected same option does not reset conversation`() {
        val viewModel = createViewModel()
        val sourceId = viewModel.uiState.value.allSources.first().id
        viewModel.onAskScopeOptionSelected(sourceId)
        val epochAfterFirst = viewModel.uiState.value.askConversationEpoch

        viewModel.onAskScopeOptionSelected(sourceId)

        assertEquals(epochAfterFirst, viewModel.uiState.value.askConversationEpoch)
    }

    @Test
    fun `onNewConversation clears messages and keeps scope`() = runTest {
        val viewModel = createViewModel()
        val sourceId = viewModel.uiState.value.allSources.first().id
        viewModel.onAskScopeOptionSelected(sourceId)
        viewModel.onAskSubmit("What problems appear most often?")
        advanceUntilIdle()
        val epochBefore = viewModel.uiState.value.askConversationEpoch
        assertTrue(viewModel.uiState.value.askMessages.isNotEmpty())

        viewModel.onNewConversation()

        assertTrue(viewModel.uiState.value.askMessages.isEmpty())
        assertTrue(viewModel.uiState.value.isAskChatOpen)
        assertEquals(AskScope.CURRENT_SOURCE, viewModel.uiState.value.askScope)
        assertEquals(sourceId, viewModel.uiState.value.askSourceId)
        assertEquals(epochBefore + 1, viewModel.uiState.value.askConversationEpoch)
        assertNull(viewModel.uiState.value.saveAskNoteDraft)
        assertNull(viewModel.uiState.value.savingAskMessageId)
        assertNull(viewModel.uiState.value.previewCitation)
    }

    @Test
    fun `onConversationClick opens chat with messages`() = runTest {
        val viewModel = createViewModel()
        val conversation = viewModel.uiState.value.askConversations.first()

        viewModel.onConversationClick(conversation)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isAskChatOpen)
        assertFalse(viewModel.uiState.value.isLoadingAskConversation)
        assertEquals(2, viewModel.uiState.value.askMessages.size)
        assertEquals("conv-1", askRepository.lastConversationId)
        assertEquals("1", askRepository.lastConversationSpaceId)
        assertEquals(AskFeedback.USEFUL, viewModel.uiState.value.askMessages.last().feedback)
        assertEquals("user-1", viewModel.uiState.value.askMessages.first().id)
        assertEquals(
            "What were the operating costs in Q4?",
            viewModel.uiState.value.askConversationTitle,
        )
        assertEquals(AskScope.CURRENT_SOURCE, viewModel.uiState.value.askScope)
        assertEquals("1", viewModel.uiState.value.askSourceId)
        assertEquals("1", askRepository.lastSuggestedSourceId)
    }

    @Test
    fun `onConversationClick filters out empty assistant messages`() = runTest {
        askRepository.conversationDetails = askRepository.conversationDetails + ("conv-empty" to AskConversationDetail(
            conversation = AskConversation(id = "conv-empty", title = "Empty bot msg", dateLabel = "Today"),
            messages = listOf(
                AskConversationMessage(id = "u1", role = AskConversationRole.USER, content = "Hi"),
                AskConversationMessage(id = "a1", role = AskConversationRole.ASSISTANT, content = ""),
            ),
        ))
        val viewModel = createViewModel()
        val emptyConv = AskConversation(id = "conv-empty", title = "Empty bot msg", dateLabel = "Today")

        viewModel.onConversationClick(emptyConv)
        advanceUntilIdle()

        val messages = viewModel.uiState.value.askMessages
        assertEquals(1, messages.size)
        assertEquals("u1", messages.first().id)
    }

    @Test
    fun `onConversationClick restores entire space when conversation has no sourceId`() = runTest {
        val viewModel = createViewModel()
        viewModel.onAskScopeOptionSelected("1")
        assertEquals(AskScope.CURRENT_SOURCE, viewModel.uiState.value.askScope)

        viewModel.onConversationClick(viewModel.uiState.value.askConversations[1])
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isAskChatOpen)
        assertEquals(AskScope.ENTIRE_SPACE, viewModel.uiState.value.askScope)
        assertNull(viewModel.uiState.value.askSourceId)
        assertNull(askRepository.lastSuggestedSourceId)
    }

    @Test
    fun `onConversationClick falls back to entire space when sourceId is missing from sources`() = runTest {
        val missingSourceConversation = AskConversation(
            id = "conv-missing-source",
            title = "Scoped to deleted source",
            dateLabel = "Aug 21, 08:00",
            spaceId = "1",
            sourceId = "missing-source-id",
        )
        askRepository.conversationsBySpace = askRepository.conversationsBySpace + mapOf(
            "1" to askRepository.conversationsBySpace.getValue("1") + missingSourceConversation,
        )
        askRepository.conversationDetails = askRepository.conversationDetails + mapOf(
            "conv-missing-source" to AskConversationDetail(
                conversation = missingSourceConversation,
                messages = emptyList(),
            ),
        )
        val viewModel = createViewModel()
        viewModel.onAskScopeOptionSelected("1")
        assertEquals(AskScope.CURRENT_SOURCE, viewModel.uiState.value.askScope)

        viewModel.onConversationClick(missingSourceConversation)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isAskChatOpen)
        assertEquals(AskScope.ENTIRE_SPACE, viewModel.uiState.value.askScope)
        assertNull(viewModel.uiState.value.askSourceId)
        assertNull(askRepository.lastSuggestedSourceId)
    }

    @Test
    fun `onConversationClick failure returns to list`() = runTest {
        askRepository.getConversationResult = Result.failure(java.io.IOException("offline"))
        val viewModel = createViewModel()
        val conversation = viewModel.uiState.value.askConversations.first()

        viewModel.onConversationClick(conversation)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isAskChatOpen)
        assertTrue(viewModel.uiState.value.askMessages.isEmpty())
        assertEquals(HomeActionError.NETWORK, viewModel.uiState.value.actionError)
    }

    @Test
    fun `onAskChatBack returns to conversation list`() = runTest {
        val viewModel = createViewModel()
        viewModel.onConversationClick(viewModel.uiState.value.askConversations.first())
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isAskChatOpen)
        val conversationsBeforeBack = askRepository.getConversationsCallCount

        viewModel.onAskChatBack()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isAskChatOpen)
        assertTrue(viewModel.uiState.value.askMessages.isEmpty())
        assertTrue(askRepository.getConversationsCallCount > conversationsBeforeBack)
        assertEquals("", viewModel.uiState.value.askConversationTitle)
    }

    @Test
    fun `onDeleteConversationConfirm removes conversation`() = runTest {
        val viewModel = createViewModel()
        val conversation = viewModel.uiState.value.askConversations.first()
        viewModel.onConversationOptionsClick(conversation)
        viewModel.onDeleteConversationClick()
        viewModel.onDeleteConversationConfirm()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.askConversations.any { it.id == conversation.id })
        assertNull(viewModel.uiState.value.deletingConversation)
        assertEquals(HomeUserMessage.CONVERSATION_DELETED, viewModel.uiState.value.userMessage)
        assertEquals(1, askRepository.deleteConversationCallCount)
        assertEquals(conversation.id, askRepository.lastDeletedConversationId)
    }

    @Test
    fun `onRenameConversationClick opens rename sheet`() = runTest {
        val viewModel = createViewModel()
        val conversation = viewModel.uiState.value.askConversations.first()
        viewModel.onConversationOptionsClick(conversation)

        viewModel.onRenameConversationClick()

        assertNull(viewModel.uiState.value.optionsConversation)
        assertEquals(conversation.id, viewModel.uiState.value.renamingConversation?.id)
    }

    @Test
    fun `onRenameConversationSave updates conversation title`() = runTest {
        val viewModel = createViewModel()
        val conversation = viewModel.uiState.value.askConversations.first()
        viewModel.onConversationOptionsClick(conversation)
        viewModel.onRenameConversationClick()

        viewModel.onRenameConversationSave("Q4 operating costs")
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.renamingConversation)
        assertFalse(viewModel.uiState.value.isRenamingConversation)
        assertEquals(
            "Q4 operating costs",
            viewModel.uiState.value.askConversations.first { it.id == conversation.id }.title,
        )
        assertEquals(HomeUserMessage.CONVERSATION_RENAMED, viewModel.uiState.value.userMessage)
        assertEquals(1, askRepository.updateConversationCallCount)
        assertEquals(conversation.id, askRepository.lastUpdatedConversationId)
        assertEquals("Q4 operating costs", askRepository.lastUpdatedConversationTitle)
        assertEquals("1", askRepository.lastConversationSpaceId)
    }

    @Test
    fun `onRenameConversationSave failure keeps rename sheet`() = runTest {
        askRepository.updateConversationResult = Result.failure(java.io.IOException("offline"))
        val viewModel = createViewModel()
        val conversation = viewModel.uiState.value.askConversations.first()
        viewModel.onConversationOptionsClick(conversation)
        viewModel.onRenameConversationClick()

        viewModel.onRenameConversationSave("Q4 operating costs")
        advanceUntilIdle()

        assertEquals(conversation.id, viewModel.uiState.value.renamingConversation?.id)
        assertFalse(viewModel.uiState.value.isRenamingConversation)
        assertEquals(conversation.title, viewModel.uiState.value.askConversations.first().title)
        assertEquals(HomeActionError.NETWORK, viewModel.uiState.value.actionError)
    }

    @Test
    fun `onAskScopeOptionSelected entire space clears source selection`() {
        val viewModel = createViewModel()
        val sourceId = viewModel.uiState.value.allSources.first().id
        viewModel.onAskScopeOptionSelected(sourceId)

        viewModel.onAskScopeOptionSelected(null)

        assertEquals(AskScope.ENTIRE_SPACE, viewModel.uiState.value.askScope)
        assertNull(viewModel.uiState.value.askSourceId)
        assertEquals(2, viewModel.uiState.value.askConversationEpoch)
    }

    @Test
    fun `onAskCitationClick opens citation preview sheet`() = runTest {
        val viewModel = createViewModel()
        val citation = AskCitation(
            index = 1,
            sourceId = "1",
            sourceTitle = "Alan Turing: Computing Machinery",
            sourceType = SourceType.FILE,
            fileExtension = "pdf",
            locationLabel = "Page 14",
            evidenceText = "imitation game",
        )

        viewModel.onAskCitationClick(citation)

        assertEquals(citation, viewModel.uiState.value.previewCitation)
    }

    @Test
    fun `onCitationOpenInSource navigates with highlight`() = runTest {
        val viewModel = createViewModel()
        val citation = AskCitation(
            index = 1,
            sourceId = "1",
            sourceTitle = "Alan Turing: Computing Machinery",
            sourceType = SourceType.FILE,
            fileExtension = "pdf",
            locationLabel = "Page 14",
            evidenceText = "imitation game",
        )
        viewModel.onAskCitationClick(citation)

        viewModel.onCitationOpenInSource()

        assertNull(viewModel.uiState.value.previewCitation)
        assertEquals("1", viewModel.uiState.value.openSourceDetailId)
        assertEquals("imitation game", viewModel.uiState.value.openSourceDetailHighlight)
    }

    @Test
    fun `onCitationPreviewDismiss clears preview`() {
        val viewModel = createViewModel()
        viewModel.onAskCitationClick(
            AskCitation(1, "1", "Title", evidenceText = "passage"),
        )

        viewModel.onCitationPreviewDismiss()

        assertNull(viewModel.uiState.value.previewCitation)
    }

    @Test
    fun `onAskSubmit streams assistant response`() = runTest {
        val viewModel = createViewModel()

        viewModel.onAskSubmit("Summarize all the evidence.")
        advanceUntilIdle()

        val messages = viewModel.uiState.value.askMessages
        assertEquals(2, messages.size)
        assertEquals(AskMessageRole.USER, messages[0].role)
        assertEquals("Summarize all the evidence.", messages[0].content)
        assertEquals(AskMessageRole.ASSISTANT, messages[1].role)
        assertFalse(messages[1].isStreaming)
        assertTrue(messages[1].content.isNotBlank())
        assertEquals(1, askRepository.streamAnswerCallCount)
        assertNull(askRepository.lastStreamConversationId)
        assertNull(viewModel.uiState.value.userMessage)
    }

    @Test
    fun `onAskSubmit SSE error marks assistant failed`() = runTest {
        askRepository.streamEvents = listOf(AskStreamEvent.Delta("Partial "))
        askRepository.streamThrow = java.io.IOException("Ask generation failed")
        val viewModel = createViewModel()

        viewModel.onAskSubmit("Question")
        advanceUntilIdle()

        val assistant = viewModel.uiState.value.askMessages.first { it.role == AskMessageRole.ASSISTANT }
        assertTrue(assistant.isFailed)
        assertFalse(assistant.isStreaming)
        assertEquals("Partial ", assistant.content)
        assertEquals(HomeActionError.NETWORK, viewModel.uiState.value.actionError)
    }

    @Test
    fun `onAskSubmit replaces streamed tokens with done content`() = runTest {
        askRepository.streamEvents = listOf(
            AskStreamEvent.Delta("draft "),
            AskStreamEvent.Delta("tokens"),
            AskStreamEvent.Completed(
                content = "Canonical answer [1].",
                citations = listOf(
                    AskCitation(
                        index = 1,
                        sourceId = "1",
                        sourceTitle = "Paper",
                        sourceType = SourceType.FILE,
                        evidenceText = "Canonical answer",
                    ),
                ),
                limitation = "Limited coverage",
            ),
        )
        val viewModel = createViewModel()

        viewModel.onAskSubmit("Question")
        advanceUntilIdle()

        val assistant = viewModel.uiState.value.askMessages.first { it.role == AskMessageRole.ASSISTANT }
        assertEquals("Canonical answer [1].", assistant.content)
        assertEquals(1, assistant.citations.size)
        assertEquals("Limited coverage", assistant.limitation)
        assertFalse(assistant.isStreaming)
        assertFalse(assistant.wasStopped)
    }

    @Test
    fun `onAskSubmit applies citations before done`() = runTest {
        askRepository.streamEvents = listOf(
            AskStreamEvent.Delta("Answer [1]."),
            AskStreamEvent.Citations(
                listOf(
                    AskCitation(
                        index = 1,
                        sourceId = "5",
                        sourceTitle = "Attention Is All You Need",
                    ),
                ),
            ),
            AskStreamEvent.Completed(content = "Answer [1]."),
        )
        val viewModel = createViewModel()

        viewModel.onAskSubmit("Question")
        advanceUntilIdle()

        val assistant = viewModel.uiState.value.askMessages.first { it.role == AskMessageRole.ASSISTANT }
        assertEquals("5", assistant.citations.single().sourceId)
    }

    @Test
    fun `onAskSubmit passes conversationId on follow-up question`() = runTest {
        val viewModel = createViewModel()
        viewModel.onAskSubmit("First question")
        advanceUntilIdle()

        viewModel.onAskSubmit("Follow-up")
        advanceUntilIdle()

        assertEquals(2, askRepository.streamAnswerCallCount)
        assertEquals("conv-1", askRepository.lastStreamConversationId)
        assertEquals("Follow-up", askRepository.lastStreamQuestion)
    }

    @Test
    fun `onNewConversation clears conversationId for next ask`() = runTest {
        val viewModel = createViewModel()
        viewModel.onAskSubmit("First question")
        advanceUntilIdle()

        viewModel.onNewConversation()
        viewModel.onAskSubmit("New thread")
        advanceUntilIdle()

        assertEquals(2, askRepository.streamAnswerCallCount)
        assertNull(askRepository.lastStreamConversationId)
        assertEquals("New thread", askRepository.lastStreamQuestion)
    }

    @Test
    fun `onAskStop retains partial assistant text`() = runTest {
        askRepository.streamEvents = listOf(
            AskStreamEvent.Delta("Partial "),
            AskStreamEvent.Delta("answer"),
        )
        askRepository.hangAfterStreamEvents = true
        val viewModel = createViewModel()
        viewModel.onAskSubmit("Question")
        advanceUntilIdle()

        viewModel.onAskStop()

        val assistant = viewModel.uiState.value.askMessages.first { it.role == AskMessageRole.ASSISTANT }
        assertFalse(assistant.isStreaming)
        assertTrue(assistant.wasStopped)
        assertEquals("Partial answer", assistant.content)
    }

    @Test
    fun `onAskStop removes empty assistant message when stopped before content generation`() = runTest {
        askRepository.streamEvents = emptyList()
        askRepository.hangAfterStreamEvents = true
        val viewModel = createViewModel()
        viewModel.onAskSubmit("Question")

        viewModel.onAskStop()

        val messages = viewModel.uiState.value.askMessages
        assertEquals(1, messages.size)
        assertEquals(AskMessageRole.USER, messages.first().role)
    }

    @Test
    fun `onAskFeedback records rating and toast`() = runTest {
        val viewModel = createViewModel()
        viewModel.onAskSubmit("Q")
        advanceUntilIdle()
        val assistantId = viewModel.uiState.value.askMessages
            .first { it.role == AskMessageRole.ASSISTANT }.id

        viewModel.onAskFeedback(assistantId, useful = true)
        advanceUntilIdle()

        val assistant = viewModel.uiState.value.askMessages.first { it.id == assistantId }
        assertEquals(AskFeedback.USEFUL, assistant.feedback)
        assertEquals(HomeUserMessage.ASK_FEEDBACK_RECORDED, viewModel.uiState.value.userMessage)
        assertEquals(1, askRepository.submitFeedbackCallCount)
        assertEquals("1", askRepository.lastFeedbackSpaceId)
        assertEquals("conv-1", askRepository.lastFeedbackConversationId)
        assertEquals("msg-1", askRepository.lastFeedbackMessageId)
        assertEquals(AskFeedbackRating.USEFUL, askRepository.lastFeedbackRating)
    }

    @Test
    fun `onAskFeedback records not useful and toast`() = runTest {
        val viewModel = createViewModel()
        viewModel.onAskSubmit("Q")
        advanceUntilIdle()
        val assistantId = viewModel.uiState.value.askMessages
            .first { it.role == AskMessageRole.ASSISTANT }.id

        viewModel.onAskFeedback(assistantId, useful = false)
        advanceUntilIdle()

        val assistant = viewModel.uiState.value.askMessages.first { it.id == assistantId }
        assertEquals(AskFeedback.NOT_USEFUL, assistant.feedback)
        assertEquals(HomeUserMessage.ASK_FEEDBACK_RECORDED, viewModel.uiState.value.userMessage)
        assertEquals(AskFeedbackRating.NOT_USEFUL, askRepository.lastFeedbackRating)
    }

    @Test
    fun `onAskFeedback can change from useful to not useful`() = runTest {
        val viewModel = createViewModel()
        viewModel.onAskSubmit("Q")
        advanceUntilIdle()
        val assistantId = viewModel.uiState.value.askMessages
            .first { it.role == AskMessageRole.ASSISTANT }.id

        viewModel.onAskFeedback(assistantId, useful = true)
        advanceUntilIdle()
        viewModel.onAskFeedback(assistantId, useful = false)
        advanceUntilIdle()

        val assistant = viewModel.uiState.value.askMessages.first { it.id == assistantId }
        assertEquals(AskFeedback.NOT_USEFUL, assistant.feedback)
        assertEquals(AskFeedbackRating.NOT_USEFUL, askRepository.lastFeedbackRating)
        assertEquals(2, askRepository.submitFeedbackCallCount)
    }

    @Test
    fun `onAskFeedback failure reverts optimistic rating and shows error`() = runTest {
        askRepository.submitFeedbackResult = Result.failure(IllegalStateException("offline"))
        val viewModel = createViewModel()
        viewModel.onAskSubmit("Q")
        advanceUntilIdle()
        val assistantId = viewModel.uiState.value.askMessages
            .first { it.role == AskMessageRole.ASSISTANT }.id

        viewModel.onAskFeedback(assistantId, useful = true)
        advanceUntilIdle()

        val assistant = viewModel.uiState.value.askMessages.first { it.id == assistantId }
        assertEquals(AskFeedback.NONE, assistant.feedback)
        assertNull(viewModel.uiState.value.userMessage)
        assertEquals(HomeActionError.GENERIC, viewModel.uiState.value.actionError)
    }

    @Test
    fun `onAskFeedback applies rating before request completes`() = runTest {
        val releaseFeedback = CompletableDeferred<Unit>()
        askRepository.submitFeedbackGate = releaseFeedback
        val viewModel = createViewModel()
        viewModel.onAskSubmit("Q")
        advanceUntilIdle()
        val assistantId = viewModel.uiState.value.askMessages
            .first { it.role == AskMessageRole.ASSISTANT }.id

        viewModel.onAskFeedback(assistantId, useful = true)
        advanceUntilIdle()

        val pending = viewModel.uiState.value.askMessages.first { it.id == assistantId }
        assertEquals(AskFeedback.USEFUL, pending.feedback)
        assertNull(viewModel.uiState.value.userMessage)

        releaseFeedback.complete(Unit)
        advanceUntilIdle()

        assertEquals(AskFeedback.USEFUL, viewModel.uiState.value.askMessages.first { it.id == assistantId }.feedback)
        assertEquals(HomeUserMessage.ASK_FEEDBACK_RECORDED, viewModel.uiState.value.userMessage)
    }

    @Test
    fun `onAskSaveAsNote opens draft with question title`() = runTest {
        val viewModel = createViewModel()
        viewModel.onAskSubmit("What is the imitation game?")
        advanceUntilIdle()
        val assistantId = viewModel.uiState.value.askMessages
            .first { it.role == AskMessageRole.ASSISTANT }.id

        viewModel.onAskSaveAsNote(assistantId)

        val draft = viewModel.uiState.value.saveAskNoteDraft
        assertEquals(assistantId, draft?.messageId)
        assertEquals("What is the imitation game?", draft?.initialTitle)
        assertTrue(draft?.content.orEmpty().contains("[1] Sample source — Page 1"))
        assertTrue(draft?.content.orEmpty().contains("Grounded answer"))
        assertEquals(0, noteRepository.createNoteCallCount)
        assertNull(viewModel.uiState.value.savingAskMessageId)
    }

    @Test
    fun `onAskSaveAsNoteConfirm creates saved answer note`() = runTest {
        val viewModel = createViewModel()
        viewModel.onAskSubmit("What is the imitation game?")
        advanceUntilIdle()
        val assistantId = viewModel.uiState.value.askMessages
            .first { it.role == AskMessageRole.ASSISTANT }.id

        viewModel.onAskSaveAsNote(assistantId)
        val draftContent = viewModel.uiState.value.saveAskNoteDraft!!.content
        viewModel.onAskSaveAsNoteConfirm("Custom title", draftContent)
        advanceUntilIdle()

        val assistant = viewModel.uiState.value.askMessages.first { it.id == assistantId }
        assertTrue(assistant.isSavedAsNote)
        assertNull(viewModel.uiState.value.savingAskMessageId)
        assertNull(viewModel.uiState.value.saveAskNoteDraft)
        assertEquals(1, noteRepository.createNoteCallCount)
        assertEquals("Custom title", noteRepository.lastCreatedRequest?.title)
        assertEquals(NoteOrigin.SAVED_ANSWER, noteRepository.lastCreatedRequest?.origin)
        assertEquals("conv-1", noteRepository.lastCreatedRequest?.conversationId)
        assertEquals("msg-1", noteRepository.lastCreatedRequest?.messageId)
        assertEquals(1, noteRepository.lastCreatedRequest?.citationCount)
        assertEquals(1, noteRepository.lastCreatedRequest?.citations?.size)
        assertTrue(noteRepository.lastCreatedRequest?.content.orEmpty().contains("[1] Sample source — Page 1"))
        assertFalse(noteRepository.lastCreatedRequest?.content.orEmpty().contains("Citations"))
        assertEquals(HomeUserMessage.NOTE_SAVED_FROM_ASK, viewModel.uiState.value.userMessage)
        assertTrue(
            viewModel.uiState.value.allNotes.any { it.origin == NoteOrigin.SAVED_ANSWER },
        )
    }

    @Test
    fun `onAskSaveAsNoteConfirm saves edited answer content`() = runTest {
        val viewModel = createViewModel()
        viewModel.onAskSubmit("What is the imitation game?")
        advanceUntilIdle()
        val assistantId = viewModel.uiState.value.askMessages
            .first { it.role == AskMessageRole.ASSISTANT }.id

        viewModel.onAskSaveAsNote(assistantId)
        viewModel.onAskSaveAsNoteConfirm("Custom title", "Edited answer body")
        advanceUntilIdle()

        assertEquals("Edited answer body", noteRepository.lastCreatedRequest?.content)
        assertEquals("Custom title", noteRepository.lastCreatedRequest?.title)
        assertEquals(1, noteRepository.createNoteCallCount)
    }

    @Test
    fun `onAskSaveAsNoteConfirm ignored when content is blank`() = runTest {
        val viewModel = createViewModel()
        viewModel.onAskSubmit("Q")
        advanceUntilIdle()
        val assistantId = viewModel.uiState.value.askMessages
            .first { it.role == AskMessageRole.ASSISTANT }.id
        viewModel.onAskSaveAsNote(assistantId)

        viewModel.onAskSaveAsNoteConfirm("Title", "   ")
        advanceUntilIdle()

        assertEquals(0, noteRepository.createNoteCallCount)
        assertNotNull(viewModel.uiState.value.saveAskNoteDraft)
    }

    @Test
    fun `onAskSaveAsNote ignored when already saved`() = runTest {
        val viewModel = createViewModel()
        viewModel.onAskSubmit("Q")
        advanceUntilIdle()
        val assistantId = viewModel.uiState.value.askMessages
            .first { it.role == AskMessageRole.ASSISTANT }.id
        viewModel.onAskSaveAsNote(assistantId)
        val draftContent = viewModel.uiState.value.saveAskNoteDraft!!.content
        viewModel.onAskSaveAsNoteConfirm("Title", draftContent)
        advanceUntilIdle()

        viewModel.onAskSaveAsNote(assistantId)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.saveAskNoteDraft)
        assertEquals(1, noteRepository.createNoteCallCount)
    }

    @Test
    fun `onAskSaveAsNoteDismiss clears draft without creating`() = runTest {
        val viewModel = createViewModel()
        viewModel.onAskSubmit("Q")
        advanceUntilIdle()
        val assistantId = viewModel.uiState.value.askMessages
            .first { it.role == AskMessageRole.ASSISTANT }.id
        viewModel.onAskSaveAsNote(assistantId)

        viewModel.onAskSaveAsNoteDismiss()

        assertNull(viewModel.uiState.value.saveAskNoteDraft)
        assertEquals(0, noteRepository.createNoteCallCount)
    }

    @Test
    fun `onAddNoteClick opens add note sheet`() {
        val viewModel = createViewModel()

        viewModel.onAddNoteClick()

        assertTrue(viewModel.uiState.value.showAddNoteSheet)
        assertFalse(viewModel.uiState.value.isCreatingNote)
    }

    @Test
    fun `onAddNoteSubmit creates user note with default title when blank`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onAddNoteClick()

        viewModel.onAddNoteSubmit(title = "  ", content = "Insight from fieldwork")
        advanceUntilIdle()

        assertEquals(1, noteRepository.createNoteCallCount)
        assertEquals("Untitled Note", noteRepository.lastCreatedRequest?.title)
        assertEquals("Insight from fieldwork", noteRepository.lastCreatedRequest?.content)
        assertEquals(NoteOrigin.USER_CREATED, noteRepository.lastCreatedRequest?.origin)
        assertEquals(HomeUserMessage.NOTE_SAVED, viewModel.uiState.value.userMessage)
        assertTrue(viewModel.uiState.value.allNotes.any { it.title == "Untitled Note" })
        assertFalse(viewModel.uiState.value.showAddNoteSheet)
        assertFalse(viewModel.uiState.value.isCreatingNote)
    }

    @Test
    fun `onAddNoteSubmit ignores empty content`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onAddNoteSubmit(title = "Draft", content = "   ")
        advanceUntilIdle()

        assertEquals(0, noteRepository.createNoteCallCount)
        assertNull(viewModel.uiState.value.userMessage)
    }

    @Test
    fun `onAddNoteSubmit ignores title over 150 characters`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onAddNoteSubmit(title = "a".repeat(151), content = "Body")
        advanceUntilIdle()

        assertEquals(0, noteRepository.createNoteCallCount)
    }

    @Test
    fun `onAskSubmit ignored when no ready sources`() = runTest {
        sourceRepository.getSourcesResult = Result.success(
            SourceLibrary(
                sources = emptyList(),
                allCount = 0,
                papersCount = 0,
                booksCount = 0,
                webCount = 0,
                textCount = 0,
            ),
        )
        val viewModel = createViewModel()
        viewModel.onRetry()
        advanceUntilIdle()

        viewModel.onAskSubmit("Question")

        assertTrue(viewModel.uiState.value.askMessages.isEmpty())
        assertEquals(0, askRepository.streamAnswerCallCount)
    }

    @Test
    fun `onAskSubmit ignores blank questions`() {
        val viewModel = createViewModel()

        viewModel.onAskSubmit("   ")

        assertTrue(viewModel.uiState.value.askMessages.isEmpty())
        assertNull(viewModel.uiState.value.userMessage)
    }

    @Test
    fun `selecting ready source with metadata loads dynamic suggestions`() = runTest {
        val viewModel = createViewModel()

        viewModel.onAskScopeOptionSelected("1")

        assertEquals(3, viewModel.uiState.value.askSuggestions.size)
        assertEquals(
            "What is Turing's main claim about machine intelligence?",
            viewModel.uiState.value.askSuggestions.first(),
        )
        assertEquals(1, askRepository.getSuggestedQuestionsCallCount)
        assertEquals("1", askRepository.lastSuggestedSpaceId)
        assertEquals("1", askRepository.lastSuggestedSourceId)
    }

    @Test
    fun `entire space clears dynamic suggestions for fallback chips`() = runTest {
        val viewModel = createViewModel()
        viewModel.onAskScopeOptionSelected("1")
        assertEquals(3, viewModel.uiState.value.askSuggestions.size)

        viewModel.onAskScopeOptionSelected(null)

        assertTrue(viewModel.uiState.value.askSuggestions.isEmpty())
        assertEquals(null, askRepository.lastSuggestedSourceId)
    }

    @Test
    fun `ready source without metadata keeps empty suggestions for fallback`() = runTest {
        val viewModel = createViewModel()

        // Source 10 is Ready in samples but has no suggestion metadata in FakeAskRepository.
        viewModel.onAskScopeOptionSelected("10")

        assertTrue(viewModel.uiState.value.askSuggestions.isEmpty())
        assertEquals(1, askRepository.getSuggestedQuestionsCallCount)
    }

    @Test
    fun `non-ready source does not fetch dynamic suggestions`() = runTest {
        val viewModel = createViewModel()

        viewModel.onAskScopeOptionSelected("4") // FAILED

        assertTrue(viewModel.uiState.value.askSuggestions.isEmpty())
        assertEquals(0, askRepository.getSuggestedQuestionsCallCount)
    }

    @Test
    fun `onAskSourceSelected auto-selects source and resets conversation`() {
        val viewModel = createViewModel()
        val sourceId = viewModel.uiState.value.allSources.first().id
        viewModel.onAskScopeOptionSelected(sourceId)
        val epochBefore = viewModel.uiState.value.askConversationEpoch

        viewModel.onAskSourceSelected(sourceId)

        assertEquals(AskScope.CURRENT_SOURCE, viewModel.uiState.value.askScope)
        assertEquals(sourceId, viewModel.uiState.value.askSourceId)
        assertEquals(epochBefore + 1, viewModel.uiState.value.askConversationEpoch)
        assertTrue(viewModel.uiState.value.askMessages.isEmpty())
    }

    @Test
    fun `onSourceProcessingOpenSource sets openSourceDetailId`() {
        val viewModel = createViewModel()
        viewModel.onAddSourceSubmit(
            AddSourceDraft.Text(
                title = "Memo",
                author = "Author",
                content = "Body",
            ),
        )
        val sourceId = viewModel.uiState.value.processingSourceId

        viewModel.onSourceProcessingOpenSource()

        assertNull(viewModel.uiState.value.processingSourceTitle)
        assertEquals(sourceId, viewModel.uiState.value.openSourceDetailId)
    }

    @Test
    fun `onEditSourceClick opens editing source`() {
        val viewModel = createViewModel()
        val source = viewModel.uiState.value.visibleSources.first()

        viewModel.onEditSourceClick(source)

        assertEquals(source, viewModel.uiState.value.editingSource)
    }

    @Test
    fun `onSourceOptionsClick shows options sheet for source`() {
        val viewModel = createViewModel()
        val source = viewModel.uiState.value.visibleSources.first()

        viewModel.onSourceOptionsClick(source)

        assertEquals(source, viewModel.uiState.value.optionsSource)
    }

    @Test
    fun `onSourceOptionsDismiss clears options source`() {
        val viewModel = createViewModel()
        val source = viewModel.uiState.value.visibleSources.first()
        viewModel.onSourceOptionsClick(source)

        viewModel.onSourceOptionsDismiss()

        assertNull(viewModel.uiState.value.optionsSource)
    }

    @Test
    fun `onEditSourceClick from options clears options sheet`() {
        val viewModel = createViewModel()
        val source = viewModel.uiState.value.visibleSources.first()
        viewModel.onSourceOptionsClick(source)

        viewModel.onEditSourceClick(source)

        assertNull(viewModel.uiState.value.optionsSource)
        assertEquals(source, viewModel.uiState.value.editingSource)
    }

    @Test
    fun `onDeleteSourceClick from options clears options sheet`() {
        val viewModel = createViewModel()
        val source = viewModel.uiState.value.visibleSources.first()
        viewModel.onSourceOptionsClick(source)

        viewModel.onDeleteSourceClick(source)

        assertNull(viewModel.uiState.value.optionsSource)
        assertEquals(source, viewModel.uiState.value.deletingSource)
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
        assertNull(sourceRepository.lastUpdatedContent)
    }

    @Test
    fun `onEditSourceClick for text loads plain content`() = runTest {
        val viewModel = createViewModel(spaceId = "3", spaceTitle = "Fieldwork")
        val source = viewModel.uiState.value.allSources.first { it.type == SourceType.TEXT }

        viewModel.onEditSourceClick(source)
        advanceUntilIdle()

        assertEquals(source, viewModel.uiState.value.editingSource)
        assertEquals(
            "Sample manual source content for editing.",
            viewModel.uiState.value.editingSourceContent,
        )
        assertEquals(1, sourceRepository.getSourceDetailCallCount)
    }

    @Test
    fun `onEditSourceSave for text saves title and author with null content`() = runTest {
        val viewModel = createViewModel(spaceId = "3", spaceTitle = "Fieldwork")
        val source = viewModel.uiState.value.allSources.first { it.type == SourceType.TEXT }
        viewModel.onEditSourceClick(source)
        advanceUntilIdle()

        viewModel.onEditSourceSave(
            title = "Updated notes",
            author = "Researcher",
        )

        assertNull(viewModel.uiState.value.editingSource)
        assertEquals(HomeUserMessage.SOURCE_UPDATED, viewModel.uiState.value.userMessage)
        assertEquals(1, sourceRepository.updateSourceCallCount)
        assertNull(sourceRepository.lastUpdatedContent)
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

        assertNull(viewModel.uiState.value.userMessage)
        assertEquals(HomeActionError.GENERIC, viewModel.uiState.value.actionError)
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

        assertNull(viewModel.uiState.value.userMessage)
        assertEquals(HomeActionError.GENERIC, viewModel.uiState.value.actionError)
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
    fun `onNoteClick shows view note sheet`() = runTest {
        val viewModel = createViewModel()
        val note = viewModel.uiState.value.visibleNotes.first()

        viewModel.onNoteClick(note)
        advanceUntilIdle()

        assertEquals(note.id, viewModel.uiState.value.viewingNote?.id)
        assertEquals(1, noteRepository.getNoteCallCount)
        assertEquals(note.id, noteRepository.lastNoteId)
        assertFalse(viewModel.uiState.value.isLoadingNoteDetail)
    }

    @Test
    fun `onViewNoteClick opens view sheet from options`() = runTest {
        val viewModel = createViewModel()
        val note = viewModel.uiState.value.visibleNotes.first()
        viewModel.onNoteOptionsClick(note)

        viewModel.onViewNoteClick()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.optionsNote)
        assertEquals(note.id, viewModel.uiState.value.viewingNote?.id)
        assertEquals(1, noteRepository.getNoteCallCount)
    }

    @Test
    fun `onEditNoteClick opens edit sheet from viewing note`() = runTest {
        val viewModel = createViewModel()
        val note = viewModel.uiState.value.visibleNotes.first()
        viewModel.onNoteClick(note)
        advanceUntilIdle()

        viewModel.onEditNoteClick()
        advanceUntilIdle()

        assertEquals(note.id, viewModel.uiState.value.viewingNote?.id)
        assertEquals(note.id, viewModel.uiState.value.editingNote?.id)
        assertTrue(noteRepository.getNoteCallCount >= 2)
    }

    @Test
    fun `onEditNoteDismiss clears editing and viewing note`() = runTest {
        val viewModel = createViewModel()
        val note = viewModel.uiState.value.visibleNotes.first()
        viewModel.onNoteClick(note)
        advanceUntilIdle()
        viewModel.onEditNoteClick()
        advanceUntilIdle()

        viewModel.onEditNoteDismiss()

        assertNull(viewModel.uiState.value.editingNote)
        assertNull(viewModel.uiState.value.viewingNote)
        assertFalse(viewModel.uiState.value.isLoadingNoteDetail)
    }

    @Test
    fun `onEditNoteSave updates note title and content`() = runTest {
        val viewModel = createViewModel()
        val note = viewModel.uiState.value.visibleNotes.first()
        viewModel.onNoteClick(note)
        advanceUntilIdle()
        viewModel.onEditNoteClick()
        advanceUntilIdle()

        viewModel.onEditNoteSave("Updated title", "Updated content")
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.editingNote)
        val updated = viewModel.uiState.value.allNotes.first { it.id == note.id }
        assertEquals("Updated title", updated.title)
        assertEquals("Updated content", updated.content)
        assertEquals("Updated title", viewModel.uiState.value.viewingNote?.title)
        assertEquals("Updated content", viewModel.uiState.value.viewingNote?.content)
        assertEquals(HomeUserMessage.NOTE_UPDATED, viewModel.uiState.value.userMessage)
        assertEquals(1, noteRepository.updateNoteCallCount)
        assertEquals(note.id, noteRepository.lastUpdatedNote?.id)
    }

    @Test
    fun `onEditNoteSave keeps list unchanged when update fails`() = runTest {
        val viewModel = createViewModel()
        val note = viewModel.uiState.value.visibleNotes.first()
        viewModel.onNoteClick(note)
        advanceUntilIdle()
        viewModel.onEditNoteClick()
        advanceUntilIdle()
        noteRepository.updateNoteResult = Result.failure(IllegalStateException("offline"))

        viewModel.onEditNoteSave("Updated title", "Updated content")
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.userMessage)
        assertEquals(HomeActionError.GENERIC, viewModel.uiState.value.actionError)
        val unchanged = viewModel.uiState.value.allNotes.first { it.id == note.id }
        assertEquals(note.title, unchanged.title)
        assertEquals(note.content, unchanged.content)
    }

    @Test
    fun `onEditNoteSave retains edited fields after loadHome reload`() {
        val viewModel = createViewModel()
        val note = viewModel.uiState.value.visibleNotes.first()
        val loadsBeforeReload = noteRepository.getNotesCallCount
        viewModel.onNoteClick(note)
        viewModel.onEditNoteClick()

        viewModel.onEditNoteSave("Persisted title", "Persisted content")
        viewModel.onUserMessageShown()
        viewModel.loadHome()

        assertEquals(loadsBeforeReload + 1, noteRepository.getNotesCallCount)
        assertEquals(1, noteRepository.updateNoteCallCount)
        val reloaded = viewModel.uiState.value.allNotes.first { it.id == note.id }
        assertEquals("Persisted title", reloaded.title)
        assertEquals("Persisted content", reloaded.content)
    }

    @Test
    fun `onDeleteNoteClick opens delete confirmation`() {
        val viewModel = createViewModel()
        val note = viewModel.uiState.value.visibleNotes.first()
        viewModel.onNoteClick(note)
        viewModel.onEditNoteClick()

        viewModel.onDeleteNoteClick()

        assertNull(viewModel.uiState.value.editingNote)
        assertEquals(note, viewModel.uiState.value.deletingNote)
        assertEquals(note, viewModel.uiState.value.viewingNote)
    }

    @Test
    fun `onDeleteNoteDismiss clears deleting and viewing note`() {
        val viewModel = createViewModel()
        val note = viewModel.uiState.value.visibleNotes.first()
        viewModel.onNoteClick(note)
        viewModel.onEditNoteClick()
        viewModel.onDeleteNoteClick()

        viewModel.onDeleteNoteDismiss()

        assertNull(viewModel.uiState.value.deletingNote)
        assertNull(viewModel.uiState.value.viewingNote)
    }

    @Test
    fun `onDeleteNoteConfirm removes note and clears sheets`() {
        val viewModel = createViewModel()
        val note = viewModel.uiState.value.visibleNotes.first()
        val beforeCount = viewModel.uiState.value.notesAllCount
        viewModel.onNoteClick(note)
        viewModel.onEditNoteClick()
        viewModel.onDeleteNoteClick()

        viewModel.onDeleteNoteConfirm()

        assertNull(viewModel.uiState.value.deletingNote)
        assertNull(viewModel.uiState.value.viewingNote)
        assertTrue(viewModel.uiState.value.allNotes.none { it.id == note.id })
        assertEquals(beforeCount - 1, viewModel.uiState.value.notesAllCount)
        assertEquals(HomeUserMessage.NOTE_DELETED, viewModel.uiState.value.userMessage)
        assertEquals(1, noteRepository.deleteNoteCallCount)
        assertEquals(note.id, noteRepository.lastDeletedNoteId)
        assertEquals(note.spaceId, noteRepository.lastDeletedSpaceId)
    }

    @Test
    fun `onDeleteNoteConfirm keeps list unchanged when delete fails`() {
        val viewModel = createViewModel()
        val note = viewModel.uiState.value.visibleNotes.first()
        val beforeCount = viewModel.uiState.value.notesAllCount
        viewModel.onNoteClick(note)
        viewModel.onEditNoteClick()
        viewModel.onDeleteNoteClick()
        noteRepository.deleteNoteResult = Result.failure(IllegalStateException("offline"))

        viewModel.onDeleteNoteConfirm()

        assertNull(viewModel.uiState.value.userMessage)
        assertEquals(HomeActionError.GENERIC, viewModel.uiState.value.actionError)
        assertTrue(viewModel.uiState.value.allNotes.any { it.id == note.id })
        assertEquals(beforeCount, viewModel.uiState.value.notesAllCount)
        assertEquals(note, viewModel.uiState.value.deletingNote)
    }

    @Test
    fun `onConvertNoteClick opens convert sheet from viewing note`() {
        val viewModel = createViewModel()
        val note = viewModel.uiState.value.visibleNotes.first()
        viewModel.onNoteClick(note)

        viewModel.onConvertNoteClick()

        assertEquals(note, viewModel.uiState.value.viewingNote)
        assertEquals(note, viewModel.uiState.value.convertingNote)
    }

    @Test
    fun `onConvertNoteDismiss clears converting and viewing note`() {
        val viewModel = createViewModel()
        val note = viewModel.uiState.value.visibleNotes.first()
        viewModel.onNoteClick(note)
        viewModel.onConvertNoteClick()

        viewModel.onConvertNoteDismiss()

        assertNull(viewModel.uiState.value.convertingNote)
        assertNull(viewModel.uiState.value.viewingNote)
    }

    @Test
    fun `onConvertNoteCreate posts convert API with title`() {
        val viewModel = createViewModel()
        val note = viewModel.uiState.value.visibleNotes.first()
        viewModel.onNoteClick(note)
        viewModel.onConvertNoteClick()

        viewModel.onConvertNoteCreate(
            title = "Converted note title",
            snapshot = "Snapshot body preserved from the note.",
        )

        assertEquals(1, noteRepository.convertNoteToSourceCallCount)
        assertEquals(0, sourceRepository.createSourceCallCount)
        assertEquals(note.spaceId, noteRepository.lastConvertSpaceId)
        assertEquals(note.id, noteRepository.lastConvertNoteId)
        assertEquals("Converted note title", noteRepository.lastConvertTitle)
        assertNull(viewModel.uiState.value.convertingNote)
        assertNull(viewModel.uiState.value.viewingNote)
        assertEquals("Converted note title", viewModel.uiState.value.processingSourceTitle)
        assertEquals(SourceProcessingState.ADDED, viewModel.uiState.value.processingState)
        assertEquals(HomeUserMessage.SOURCE_CREATED, viewModel.uiState.value.userMessage)
        assertFalse(viewModel.uiState.value.isCreatingSource)
        assertEquals(1, sourceRepository.observeSourceProcessingCallCount)
    }

    @Test
    fun `onConvertNoteCreate shows action error when convert fails`() {
        noteRepository.convertNoteToSourceResult = Result.failure(IllegalStateException("boom"))
        val viewModel = createViewModel()
        val note = viewModel.uiState.value.visibleNotes.first()
        viewModel.onNoteClick(note)
        viewModel.onConvertNoteClick()

        viewModel.onConvertNoteCreate(
            title = "Title",
            snapshot = "Enough snapshot content for create.",
        )

        assertEquals(HomeActionError.GENERIC, viewModel.uiState.value.actionError)
        assertNull(viewModel.uiState.value.processingSourceId)
        assertNull(viewModel.uiState.value.userMessage)
        assertFalse(viewModel.uiState.value.isCreatingSource)
    }

    @Test
    fun `onNotebookAddClick shows notebook actions sheet`() {
        val viewModel = createViewModel()

        viewModel.onNotebookAddClick()

        assertTrue(viewModel.uiState.value.showNotebookActions)
    }

    @Test
    fun `onCopyNotebookClick dismisses actions and sets pending copy`() {
        val viewModel = createViewModel()
        viewModel.onNotebookContentChange("Draft report")
        viewModel.onNotebookAddClick()

        viewModel.onCopyNotebookClick()

        assertFalse(viewModel.uiState.value.showNotebookActions)
        assertEquals("Draft report", viewModel.uiState.value.pendingNotebookCopy)
    }

    @Test
    fun `onPendingNotebookCopyHandled sets copied toast message`() {
        val viewModel = createViewModel()
        viewModel.onCopyNotebookClick()

        viewModel.onPendingNotebookCopyHandled()

        assertNull(viewModel.uiState.value.pendingNotebookCopy)
        assertEquals(HomeUserMessage.NOTEBOOK_COPIED, viewModel.uiState.value.userMessage)
    }

    @Test
    fun `onExportNotebookClick opens export sheet`() {
        val viewModel = createViewModel()
        viewModel.onNotebookAddClick()

        viewModel.onExportNotebookClick()

        assertFalse(viewModel.uiState.value.showNotebookActions)
        assertTrue(viewModel.uiState.value.showNotebookExport)
    }

    @Test
    fun `onNotebookExportConfirm markdown sets pending export request`() {
        val viewModel = createViewModel()
        viewModel.onNotebookContentChange("# Report")
        viewModel.onNotebookAddClick()
        viewModel.onExportNotebookClick()

        viewModel.onNotebookExportConfirm(NotebookExportFormat.MARKDOWN)

        assertFalse(viewModel.uiState.value.showNotebookExport)
        assertFalse(viewModel.uiState.value.notebookExportPickerLaunched)
        assertEquals(
            NotebookExportRequest(
                filename = "dissertation-research-notebook.md",
                markdown = "# Report\n",
            ),
            viewModel.uiState.value.pendingNotebookExport,
        )
    }

    @Test
    fun `markdown export request survives recreation until CreateDocument result is processed`() {
        val viewModel = createViewModel()
        viewModel.onNotebookContentChange("# Report")
        viewModel.onNotebookAddClick()
        viewModel.onExportNotebookClick()
        viewModel.onNotebookExportConfirm(NotebookExportFormat.MARKDOWN)

        // Picker is open; Compose clears remember-state on recreation, ViewModel must retain.
        viewModel.onNotebookExportPickerLaunched()
        val retained = viewModel.uiState.value.pendingNotebookExport
        assertNotNull(retained)
        assertTrue(viewModel.uiState.value.notebookExportPickerLaunched)

        val selectedFile = File.createTempFile("notebook-export", ".md")
        try {
            selectedFile.outputStream().use { stream ->
                NotebookExportHelper.writeMarkdown(stream, retained!!.markdown)
            }
            assertEquals("# Report\n", selectedFile.readText())

            viewModel.onNotebookExportSucceeded()
            assertNull(viewModel.uiState.value.pendingNotebookExport)
            assertFalse(viewModel.uiState.value.notebookExportPickerLaunched)
            assertEquals(HomeUserMessage.NOTEBOOK_EXPORTED, viewModel.uiState.value.userMessage)
        } finally {
            selectedFile.delete()
        }
    }

    @Test
    fun `onNotebookExportSucceeded clears pending and shows success toast`() {
        val viewModel = createViewModel()
        viewModel.onNotebookContentChange("# Report")
        viewModel.onNotebookAddClick()
        viewModel.onExportNotebookClick()
        viewModel.onNotebookExportConfirm(NotebookExportFormat.MARKDOWN)
        viewModel.onNotebookExportPickerLaunched()

        viewModel.onNotebookExportSucceeded()

        assertNull(viewModel.uiState.value.pendingNotebookExport)
        assertFalse(viewModel.uiState.value.notebookExportPickerLaunched)
        assertEquals(HomeUserMessage.NOTEBOOK_EXPORTED, viewModel.uiState.value.userMessage)
    }

    @Test
    fun `onNotebookExportFailed clears pending and reports EXPORT_FAILED`() {
        val viewModel = createViewModel()
        viewModel.onNotebookContentChange("# Report")
        viewModel.onNotebookAddClick()
        viewModel.onExportNotebookClick()
        viewModel.onNotebookExportConfirm(NotebookExportFormat.MARKDOWN)
        viewModel.onNotebookExportPickerLaunched()

        viewModel.onNotebookExportFailed()

        assertNull(viewModel.uiState.value.pendingNotebookExport)
        assertFalse(viewModel.uiState.value.notebookExportPickerLaunched)
        assertEquals(HomeActionError.EXPORT_FAILED, viewModel.uiState.value.actionError)
    }

    @Test
    fun `onNotebookExportConfirm print sets pending print request`() {
        val viewModel = createViewModel()
        viewModel.onNotebookContentChange("Print me")
        viewModel.onNotebookAddClick()
        viewModel.onExportNotebookClick()

        viewModel.onNotebookExportConfirm(NotebookExportFormat.PRINT_PDF)

        assertFalse(viewModel.uiState.value.showNotebookExport)
        assertEquals("Print me\n", viewModel.uiState.value.pendingNotebookPrint?.markdown)
    }

    @Test
    fun `print request survives recreation before PrintManager submission`() {
        val viewModel = createViewModel()
        viewModel.onNotebookContentChange("Print me")
        viewModel.onNotebookAddClick()
        viewModel.onExportNotebookClick()
        viewModel.onNotebookExportConfirm(NotebookExportFormat.PRINT_PDF)
        val firstRequest = viewModel.uiState.value.pendingNotebookPrint

        viewModel.onNotebookPrintAdapterInvalidated()

        assertEquals("Print me\n", viewModel.uiState.value.pendingNotebookPrint?.markdown)
        assertTrue(viewModel.uiState.value.pendingNotebookPrint!!.id != firstRequest!!.id)
    }

    @Test
    fun `repeat print export gets a new request id`() {
        val viewModel = createViewModel()
        viewModel.onNotebookContentChange("Print me")
        viewModel.onNotebookAddClick()
        viewModel.onExportNotebookClick()
        viewModel.onNotebookExportConfirm(NotebookExportFormat.PRINT_PDF)
        val firstRequest = viewModel.uiState.value.pendingNotebookPrint

        viewModel.onNotebookAddClick()
        viewModel.onExportNotebookClick()
        viewModel.onNotebookExportConfirm(NotebookExportFormat.PRINT_PDF)
        val secondRequest = viewModel.uiState.value.pendingNotebookPrint

        assertEquals("Print me\n", secondRequest?.markdown)
        assertTrue(secondRequest!!.id != firstRequest!!.id)

        viewModel.onPendingNotebookPrintHandled()
        assertNull(viewModel.uiState.value.pendingNotebookPrint)
    }

    @Test
    fun `loadNotebook sets loading state then clears after fetch`() = runTest {
        notebookRepository.seed("1", "Saved content")
        val viewModel = createViewModel(notebookSaveDebounceMs = 0L)
        advanceUntilIdle()

        viewModel.onTabSelected(HomeTab.NOTEBOOK)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoadingNotebook)
        assertEquals("Saved content", viewModel.uiState.value.notebookContent)
        assertTrue(notebookRepository.getNotebookCallCount >= 1)
    }

    @Test
    fun `loadNotebook shows cached content with STALE status when notebook is stale`() = runTest {
        notebookRepository.seed("1", "Cached content", isStale = true)
        val viewModel = createViewModel(notebookSaveDebounceMs = 0L)
        advanceUntilIdle()

        viewModel.onTabSelected(HomeTab.NOTEBOOK)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoadingNotebook)
        assertEquals("Cached content", viewModel.uiState.value.notebookContent)
        assertEquals(NotebookSaveStatus.STALE, viewModel.uiState.value.notebookSaveStatus)
    }

    @Test
    fun `loadNotebook shows READ_ONLY status and ignores edits when conversion is lossy`() = runTest {
        notebookRepository.seed("1", "# T\n\nSub", isReadOnly = true)
        val viewModel = createViewModel(notebookSaveDebounceMs = 0L)
        advanceUntilIdle()

        viewModel.onTabSelected(HomeTab.NOTEBOOK)
        advanceUntilIdle()

        assertEquals("# T\n\nSub", viewModel.uiState.value.notebookContent)
        assertEquals(NotebookSaveStatus.READ_ONLY, viewModel.uiState.value.notebookSaveStatus)

        viewModel.onNotebookContentChange("should not save")
        advanceUntilIdle()

        assertEquals("# T\n\nSub", viewModel.uiState.value.notebookContent)
        assertEquals(NotebookSaveStatus.READ_ONLY, viewModel.uiState.value.notebookSaveStatus)
        assertEquals(0, notebookRepository.saveNotebookCallCount)
    }

    @Test
    fun `loadNotebook keeps READ_ONLY blank content without seeding template`() = runTest {
        notebookRepository.seed("1", "", isReadOnly = true)
        val viewModel = createViewModel(
            spaceTitle = "Dissertation Research",
            researchObjective = "Primary research archive for doctoral thesis",
            notebookSaveDebounceMs = 0L,
        )
        advanceUntilIdle()

        viewModel.onTabSelected(HomeTab.NOTEBOOK)
        advanceUntilIdle()

        assertEquals("", viewModel.uiState.value.notebookContent)
        assertEquals(NotebookSaveStatus.READ_ONLY, viewModel.uiState.value.notebookSaveStatus)
    }

    @Test
    fun `loadNotebook failure leaves content unchanged and exposes error for retry`() = runTest {
        notebookRepository.getError = IllegalStateException("offline")
        val viewModel = createViewModel(notebookSaveDebounceMs = 0L)
        advanceUntilIdle()
        val loadsAfterInit = notebookRepository.getNotebookCallCount

        viewModel.onTabSelected(HomeTab.NOTEBOOK)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoadingNotebook)
        assertEquals("", viewModel.uiState.value.notebookContent)
        assertEquals("offline", viewModel.uiState.value.notebookError)
        assertEquals(0, notebookRepository.saveNotebookCallCount)

        notebookRepository.getError = null
        notebookRepository.seed("1", "Saved content")
        viewModel.onRetryNotebookLoad()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.notebookError)
        assertEquals("Saved content", viewModel.uiState.value.notebookContent)
        assertTrue(notebookRepository.getNotebookCallCount > loadsAfterInit)
    }

    @Test
    fun `loadNotebook failure can retry when Notebook tab is selected again`() = runTest {
        notebookRepository.getError = IllegalStateException("offline")
        val viewModel = createViewModel(notebookSaveDebounceMs = 0L)
        advanceUntilIdle()

        viewModel.onTabSelected(HomeTab.NOTEBOOK)
        advanceUntilIdle()
        assertEquals("offline", viewModel.uiState.value.notebookError)

        notebookRepository.getError = null
        notebookRepository.seed("1", "Recovered content")
        viewModel.onTabSelected(HomeTab.NOTES)
        viewModel.onTabSelected(HomeTab.NOTEBOOK)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.notebookError)
        assertEquals("Recovered content", viewModel.uiState.value.notebookContent)
    }

    @Test
    fun `empty notebook loads default template with space title and objective`() = runTest {
        val viewModel = createViewModel(
            spaceTitle = "Dissertation Research",
            researchObjective = "Primary research archive for doctoral thesis",
            notebookSaveDebounceMs = 0L,
        )
        advanceUntilIdle()

        viewModel.onTabSelected(HomeTab.NOTEBOOK)
        advanceUntilIdle()

        val content = viewModel.uiState.value.notebookContent
        assertTrue(content.contains("# Title"))
        assertTrue(content.contains("Dissertation Research"))
        assertTrue(content.contains("## Research Objective"))
        assertTrue(content.contains("Primary research archive for doctoral thesis"))
    }

    @Test
    fun `empty notebook default template keeps Research Objective heading when objective blank`() = runTest {
        val viewModel = createViewModel(
            spaceTitle = "Teaching Prep",
            researchObjective = "",
            notebookSaveDebounceMs = 0L,
        )
        advanceUntilIdle()

        viewModel.onTabSelected(HomeTab.NOTEBOOK)
        advanceUntilIdle()

        val content = viewModel.uiState.value.notebookContent
        assertTrue(content.contains("# Title"))
        assertTrue(content.contains("Teaching Prep"))
        assertTrue(content.contains("## Research Objective"))
    }

    @Test
    fun `pristine notebook scaffold is reseeded with research objective`() = runTest {
        notebookRepository.seed(
            "1",
            """
            # Title
            Dissertation Research

            ## Research Objective

            """.trimIndent(),
        )
        val viewModel = createViewModel(
            spaceTitle = "Dissertation Research",
            researchObjective = "Primary research archive for doctoral thesis",
            notebookSaveDebounceMs = 0L,
        )
        advanceUntilIdle()

        viewModel.onTabSelected(HomeTab.NOTEBOOK)
        advanceUntilIdle()

        val content = viewModel.uiState.value.notebookContent
        assertTrue(content.contains("Primary research archive for doctoral thesis"))
    }

    @Test
    fun `onResearchObjectiveAvailable reseeds pristine notebook`() = runTest {
        spaceRepository.spacesResult = Result.success(
            com.nus.folio.domain.model.SpacePage(
                spaces = listOf(
                    com.nus.folio.domain.model.Space(
                        id = "1",
                        title = "Dissertation Research",
                        description = "",
                        sourceCount = 0,
                        noteCount = 0,
                        updatedLabel = "Updated just now",
                    ),
                ),
                page = 1,
                limit = 20,
                hasMore = false,
            ),
        )
        val viewModel = createViewModel(
            spaceTitle = "Dissertation Research",
            researchObjective = "",
            notebookSaveDebounceMs = 0L,
        )
        advanceUntilIdle()
        viewModel.onTabSelected(HomeTab.NOTEBOOK)
        advanceUntilIdle()
        assertFalse(
            viewModel.uiState.value.notebookContent.contains(
                "Primary research archive for doctoral thesis",
            ),
        )

        viewModel.onResearchObjectiveAvailable("Primary research archive for doctoral thesis")
        advanceUntilIdle()

        assertEquals(
            "Primary research archive for doctoral thesis",
            viewModel.uiState.value.spaceResearchObjective,
        )
        assertTrue(
            viewModel.uiState.value.notebookContent.contains(
                "Primary research archive for doctoral thesis",
            ),
        )
    }

    @Test
    fun `loadHome resolves research objective from spaces when nav objective blank`() = runTest {
        val viewModel = createViewModel(
            spaceId = "1",
            spaceTitle = "Dissertation Research",
            researchObjective = "",
            notebookSaveDebounceMs = 0L,
        )
        advanceUntilIdle()

        assertEquals(
            "Primary research archive for doctoral thesis",
            viewModel.uiState.value.spaceResearchObjective,
        )
        assertTrue(spaceRepository.getSpacesCallCount >= 1)

        viewModel.onTabSelected(HomeTab.NOTEBOOK)
        advanceUntilIdle()

        assertTrue(
            viewModel.uiState.value.notebookContent.contains(
                "Primary research archive for doctoral thesis",
            ),
        )
    }

    @Test
    fun `selecting Notebook tab again does not reload stored content`() = runTest {
        notebookRepository.seed("1", "Saved content")
        val viewModel = createViewModel(notebookSaveDebounceMs = 0L)
        advanceUntilIdle()
        val loadsAfterInit = notebookRepository.getNotebookCallCount

        viewModel.onTabSelected(HomeTab.NOTEBOOK)
        advanceUntilIdle()
        viewModel.onTabSelected(HomeTab.NOTES)
        viewModel.onTabSelected(HomeTab.NOTEBOOK)
        advanceUntilIdle()

        assertEquals(loadsAfterInit, notebookRepository.getNotebookCallCount)
        assertEquals("Saved content", viewModel.uiState.value.notebookContent)
    }

    @Test
    fun `unsaved notebook edits survive tab leave before debounce save`() = runTest {
        notebookRepository.seed("1", "Saved content")
        val viewModel = createViewModel(notebookSaveDebounceMs = 1_000L)
        advanceUntilIdle()

        viewModel.onTabSelected(HomeTab.NOTEBOOK)
        advanceUntilIdle()
        viewModel.onNotebookContentChange("Draft in progress")
        assertEquals(NotebookSaveStatus.SAVING, viewModel.uiState.value.notebookSaveStatus)

        // Leave and return before the debounce fires — must not restore stored content.
        viewModel.onTabSelected(HomeTab.NOTES)
        viewModel.onTabSelected(HomeTab.NOTEBOOK)
        assertEquals("Draft in progress", viewModel.uiState.value.notebookContent)
        assertEquals(NotebookSaveStatus.SAVING, viewModel.uiState.value.notebookSaveStatus)

        advanceUntilIdle()
        assertEquals("Draft in progress", viewModel.uiState.value.notebookContent)
        assertEquals("Draft in progress", notebookRepository.lastSavedContent)
        assertEquals(NotebookSaveStatus.SAVED, viewModel.uiState.value.notebookSaveStatus)
    }

    @Test
    fun `onNotebookContentChange auto saves after debounce`() = runTest {
        val viewModel = createViewModel(notebookSaveDebounceMs = 10L)

        viewModel.onNotebookContentChange("Updated draft")
        advanceUntilIdle()

        assertEquals(NotebookSaveStatus.SAVED, viewModel.uiState.value.notebookSaveStatus)
        assertEquals("Updated draft", notebookRepository.lastSavedContent)
    }

    @Test
    fun `onNotebookContentChange clamps to max length and keeps UI in sync with saved content`() = runTest {
        val viewModel = createViewModel(notebookSaveDebounceMs = 10L)
        val oversized = "a".repeat(NotebookInputRules.MAX_CONTENT_LENGTH + 250)
        val expected = "a".repeat(NotebookInputRules.MAX_CONTENT_LENGTH)

        viewModel.onNotebookContentChange(oversized)
        assertEquals(expected, viewModel.uiState.value.notebookContent)

        advanceUntilIdle()

        assertEquals(expected, notebookRepository.lastSavedContent)
        assertEquals(expected, viewModel.uiState.value.notebookContent)
        assertEquals(NotebookSaveStatus.SAVED, viewModel.uiState.value.notebookSaveStatus)
    }

    @Test
    fun `failed notebook save exposes FAILED status and retry persists dirty content`() = runTest {
        notebookRepository.saveError = IllegalStateException("disk full")
        val viewModel = createViewModel(notebookSaveDebounceMs = 10L)

        viewModel.onNotebookContentChange("Dirty draft")
        advanceUntilIdle()

        assertEquals(NotebookSaveStatus.FAILED, viewModel.uiState.value.notebookSaveStatus)
        assertEquals("Dirty draft", viewModel.uiState.value.notebookContent)
        assertEquals(1, notebookRepository.saveNotebookCallCount)

        notebookRepository.saveError = null
        viewModel.onRetryNotebookSave()
        advanceUntilIdle()

        assertEquals(NotebookSaveStatus.SAVED, viewModel.uiState.value.notebookSaveStatus)
        assertEquals("Dirty draft", notebookRepository.lastSavedContent)
        assertEquals(2, notebookRepository.saveNotebookCallCount)
    }

    @Test
    fun `onUserMessageShown clears message`() {
        val viewModel = createViewModel()
        viewModel.onPendingNotebookCopyHandled()

        viewModel.onUserMessageShown()

        assertNull(viewModel.uiState.value.userMessage)
    }

    @Test
    fun `onSourceProcessingRetry resets progress and observes again`() = runTest {
        val viewModel = createViewModel()
        viewModel.onAddSourceSubmit(
            AddSourceDraft.Text(
                title = "Memo",
                author = "Author",
                content = "Body content",
            ),
        )
        val sourceId = viewModel.uiState.value.processingSourceId!!
        sourceRepository.emitProcessingEvent(
            SourceProcessingEvent(sourceId, SourceProcessingState.FAILED, 100),
        )

        viewModel.onSourceProcessingRetry()

        assertEquals(1, sourceRepository.retrySourceCallCount)
        assertEquals(sourceId, sourceRepository.lastRetriedSourceId)
        assertEquals(0, viewModel.uiState.value.processingProgress)
        assertEquals(SourceProcessingState.ADDED, viewModel.uiState.value.processingState)
    }
}
