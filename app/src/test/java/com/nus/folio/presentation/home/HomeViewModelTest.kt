package com.nus.folio.presentation.home

import com.nus.folio.domain.model.AuthApiException
import com.nus.folio.domain.model.AuthSession
import com.nus.folio.domain.model.NoteFilter
import com.nus.folio.domain.model.SourceFilter
import com.nus.folio.domain.model.SourceProcessingEvent
import com.nus.folio.domain.model.SourceProcessingState
import com.nus.folio.domain.model.SourceSort
import com.nus.folio.domain.model.SourceType
import com.nus.folio.domain.repository.SourceFileBytes
import com.nus.folio.domain.repository.SourceFileBytesReader
import com.nus.folio.domain.usecase.CreateSourceUseCase
import com.nus.folio.domain.usecase.DeleteNoteUseCase
import com.nus.folio.domain.usecase.DeleteSourceUseCase
import com.nus.folio.domain.usecase.GetAskTopicsUseCase
import com.nus.folio.domain.usecase.GetCurrentSessionUseCase
import com.nus.folio.domain.usecase.GetNotesUseCase
import com.nus.folio.domain.usecase.GetSourceDetailUseCase
import com.nus.folio.domain.usecase.GetSourcesUseCase
import com.nus.folio.domain.usecase.ObserveSourceProcessingUseCase
import com.nus.folio.domain.usecase.RefreshAuthSessionUseCase
import com.nus.folio.domain.usecase.RetrySourceUseCase
import com.nus.folio.domain.usecase.UpdateNoteUseCase
import com.nus.folio.domain.usecase.UpdateSourceUseCase
import com.nus.folio.domain.model.CreateSourceRequest
import com.nus.folio.presentation.home.bottomsheet.AddSourceDraft
import com.nus.folio.testing.FakeAskRepository
import com.nus.folio.testing.FakeAuthRepository
import com.nus.folio.testing.FakeNoteRepository
import com.nus.folio.testing.FakeSourceRepository
import com.nus.folio.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val sourceRepository = FakeSourceRepository()
    private val askRepository = FakeAskRepository()
    private val noteRepository = FakeNoteRepository()
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
        openSourceDelayMs: Long = 0L,
        searchDebounceMs: Long = 0L,
    ): HomeViewModel =
        HomeViewModel(
            spaceId = spaceId,
            spaceTitle = spaceTitle,
            getSourcesUseCase = GetSourcesUseCase(sourceRepository),
            createSourceUseCase = CreateSourceUseCase(sourceRepository),
            observeSourceProcessingUseCase = ObserveSourceProcessingUseCase(sourceRepository),
            updateSourceUseCase = UpdateSourceUseCase(sourceRepository),
            deleteSourceUseCase = DeleteSourceUseCase(sourceRepository),
            getSourceDetailUseCase = GetSourceDetailUseCase(sourceRepository),
            getAskTopicsUseCase = GetAskTopicsUseCase(askRepository),
            getNotesUseCase = GetNotesUseCase(noteRepository),
            updateNoteUseCase = UpdateNoteUseCase(noteRepository),
            deleteNoteUseCase = DeleteNoteUseCase(noteRepository),
            sourceFileBytesReader = sourceFileBytesReader,
            refreshAuthSessionUseCase = RefreshAuthSessionUseCase(authRepository),
            getCurrentSessionUseCase = GetCurrentSessionUseCase(authRepository),
            retrySourceUseCase = RetrySourceUseCase(sourceRepository),
            openSourceDelayMs = openSourceDelayMs,
            searchDebounceMs = searchDebounceMs,
            createMinDelayMs = 0L,
            loadMinDelayMs = 0L,
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
        assertEquals(5, viewModel.uiState.value.visibleSources.size)
        assertEquals(5, viewModel.uiState.value.allCount)
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

        assertEquals(5, dissertation.uiState.value.visibleSources.size)
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
    fun `onFilterSelected FILE shows only file`() {
        val viewModel = createViewModel()

        viewModel.onFilterSelected(SourceFilter.FILE)

        assertEquals(SourceFilter.FILE, viewModel.uiState.value.selectedFilter)
        assertEquals("File", sourceRepository.lastSourceType)
        assertEquals(4, viewModel.uiState.value.visibleSources.size)
        assertTrue(viewModel.uiState.value.visibleSources.all { it.type == SourceType.FILE })
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
    }

    @Test
    fun `onSortSelected same sort only dismisses sheet`() {
        val viewModel = createViewModel()
        val callsBefore = sourceRepository.getSourcesCallCount
        viewModel.onFilterSortClick()

        viewModel.onSortSelected(SourceSort.RECENTLY_ADDED)

        assertFalse(viewModel.uiState.value.showSortSheet)
        assertEquals(callsBefore, sourceRepository.getSourcesCallCount)
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

        assertEquals("Turing", sourceRepository.lastSearch)
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
    fun `onSourceClick opens source detail after delay`() {
        val viewModel = createViewModel(openSourceDelayMs = 0L)
        val source = viewModel.uiState.value.visibleSources.first()

        viewModel.onSourceClick(source)

        assertFalse(viewModel.uiState.value.isOpeningSource)
        assertEquals(source.id, viewModel.uiState.value.openSourceDetailId)

        viewModel.onOpenSourceDetailHandled()

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
        assertEquals(2, viewModel.uiState.value.visibleAskTopics.size)
    }

    @Test
    fun `loadSources reloads library`() {
        val viewModel = createViewModel()

        viewModel.loadSources()

        assertTrue(sourceRepository.getSourcesCallCount >= 2)
        assertEquals(5, viewModel.uiState.value.visibleSources.size)
        assertEquals(2, viewModel.uiState.value.visibleAskTopics.size)
        assertEquals(2, viewModel.uiState.value.visibleNotes.size)
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

        viewModel.onSourceProcessingAsk()

        assertNull(viewModel.uiState.value.processingSourceTitle)
        assertNull(viewModel.uiState.value.processingSourceId)
        assertEquals(HomeTab.ASK, viewModel.uiState.value.selectedTab)
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
    fun `onEditSourceSave for text includes content`() = runTest {
        val viewModel = createViewModel(spaceId = "3", spaceTitle = "Fieldwork")
        val source = viewModel.uiState.value.allSources.first { it.type == SourceType.TEXT }
        viewModel.onEditSourceClick(source)
        advanceUntilIdle()

        viewModel.onEditSourceSave(
            title = "Updated notes",
            author = "Researcher",
            content = "Updated manual source content for the archive.",
        )

        assertNull(viewModel.uiState.value.editingSource)
        assertEquals(HomeUserMessage.SOURCE_UPDATED, viewModel.uiState.value.userMessage)
        assertEquals(1, sourceRepository.updateSourceCallCount)
        assertEquals(
            "Updated manual source content for the archive.",
            sourceRepository.lastUpdatedContent,
        )
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
    fun `onNoteClick shows view note sheet`() {
        val viewModel = createViewModel()
        val note = viewModel.uiState.value.visibleNotes.first()

        viewModel.onNoteClick(note)

        assertEquals(note, viewModel.uiState.value.viewingNote)
    }

    @Test
    fun `onViewNoteClick opens view sheet from options`() {
        val viewModel = createViewModel()
        val note = viewModel.uiState.value.visibleNotes.first()
        viewModel.onNoteOptionsClick(note)

        viewModel.onViewNoteClick()

        assertNull(viewModel.uiState.value.optionsNote)
        assertEquals(note, viewModel.uiState.value.viewingNote)
    }

    @Test
    fun `onEditNoteClick opens edit sheet from viewing note`() {
        val viewModel = createViewModel()
        val note = viewModel.uiState.value.visibleNotes.first()
        viewModel.onNoteClick(note)

        viewModel.onEditNoteClick()

        assertEquals(note, viewModel.uiState.value.viewingNote)
        assertEquals(note, viewModel.uiState.value.editingNote)
    }

    @Test
    fun `onEditNoteDismiss clears editing and viewing note`() {
        val viewModel = createViewModel()
        val note = viewModel.uiState.value.visibleNotes.first()
        viewModel.onNoteClick(note)
        viewModel.onEditNoteClick()

        viewModel.onEditNoteDismiss()

        assertNull(viewModel.uiState.value.editingNote)
        assertNull(viewModel.uiState.value.viewingNote)
    }

    @Test
    fun `onEditNoteSave updates note title and content`() {
        val viewModel = createViewModel()
        val note = viewModel.uiState.value.visibleNotes.first()
        viewModel.onNoteClick(note)
        viewModel.onEditNoteClick()

        viewModel.onEditNoteSave("Updated title", "Updated content")

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
    fun `onEditNoteSave keeps list unchanged when update fails`() {
        val viewModel = createViewModel()
        val note = viewModel.uiState.value.visibleNotes.first()
        viewModel.onNoteClick(note)
        viewModel.onEditNoteClick()
        noteRepository.updateNoteResult = Result.failure(IllegalStateException("offline"))

        viewModel.onEditNoteSave("Updated title", "Updated content")

        assertEquals(HomeUserMessage.EDIT_NOTE_NOT_SUPPORTED, viewModel.uiState.value.userMessage)
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

        assertEquals(HomeUserMessage.DELETE_NOTE_NOT_SUPPORTED, viewModel.uiState.value.userMessage)
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
    fun `onNotebookAddClick shows notebook actions sheet`() {
        val viewModel = createViewModel()

        viewModel.onNotebookAddClick()

        assertTrue(viewModel.uiState.value.showNotebookActions)
    }

    @Test
    fun `onCopyNotebookClick dismisses actions and sets message`() {
        val viewModel = createViewModel()
        viewModel.onNotebookAddClick()

        viewModel.onCopyNotebookClick()

        assertFalse(viewModel.uiState.value.showNotebookActions)
        assertEquals(HomeUserMessage.COPY_NOTEBOOK_NOT_SUPPORTED, viewModel.uiState.value.userMessage)
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
    fun `onNotebookExportConfirm dismisses export and sets message`() {
        val viewModel = createViewModel()
        viewModel.onNotebookAddClick()
        viewModel.onExportNotebookClick()

        viewModel.onNotebookExportConfirm(NotebookExportFormat.MARKDOWN)

        assertFalse(viewModel.uiState.value.showNotebookExport)
        assertEquals(HomeUserMessage.EXPORT_NOTEBOOK_NOT_SUPPORTED, viewModel.uiState.value.userMessage)
    }

    @Test
    fun `onUserMessageShown clears message`() {
        val viewModel = createViewModel()
        viewModel.onCopyNotebookClick()

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
