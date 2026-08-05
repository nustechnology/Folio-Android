package com.nus.folio.presentation.space

import com.nus.folio.domain.model.AuthApiException
import com.nus.folio.domain.model.AuthSession
import com.nus.folio.domain.model.Space
import com.nus.folio.domain.model.SpacePage
import com.nus.folio.domain.model.SpacePaging
import com.nus.folio.domain.model.SpaceSort
import com.nus.folio.domain.usecase.CreateSpaceUseCase
import com.nus.folio.domain.usecase.DeleteSpaceUseCase
import com.nus.folio.domain.usecase.GetCurrentSessionUseCase
import com.nus.folio.domain.usecase.GetSpacesUseCase
import com.nus.folio.domain.usecase.RefreshAuthSessionUseCase
import com.nus.folio.domain.usecase.SyncCurrentUserUseCase
import com.nus.folio.domain.usecase.UpdateSpaceUseCase
import com.nus.folio.testing.FakeAuthRepository
import com.nus.folio.testing.FakeSpaceRepository
import com.nus.folio.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SpaceViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val spaceRepository = FakeSpaceRepository(
        spacesResult = Result.success(
            FakeSpaceRepository.pageOf(
                spaces = listOf(
                    Space("1", "Dissertation Research", "Primary research archive for doctoral thesis", 128, 32, "Updated 2d ago"),
                    Space("2", "Public Policy Insights", "Policy papers and legislative analysis", 64, 18, "Updated 5h ago"),
                ),
                hasMore = false,
            ),
        ),
    )

    private val authRepository = FakeAuthRepository()

    private fun createViewModel(
        email: String = "jordan@folio.app",
        displayName: String = "Jordan Lee",
        userId: String? = "user-1",
        pageLimit: Int = SpacePaging.DEFAULT_LIMIT,
    ): SpaceViewModel {
        runBlocking { authRepository.clearSession() }
        authRepository.signInResult = Result.success(
            AuthSession(
                email = email,
                displayName = displayName,
                userId = userId,
                accessToken = "access",
                refreshToken = "refresh",
            ),
        )
        runBlocking { authRepository.signIn(email, "password") }
        return SpaceViewModel(
            getSpacesUseCase = GetSpacesUseCase(spaceRepository),
            createSpaceUseCase = CreateSpaceUseCase(spaceRepository),
            updateSpaceUseCase = UpdateSpaceUseCase(spaceRepository),
            deleteSpaceUseCase = DeleteSpaceUseCase(spaceRepository),
            getCurrentSessionUseCase = GetCurrentSessionUseCase(authRepository),
            syncCurrentUserUseCase = SyncCurrentUserUseCase(authRepository),
            refreshAuthSessionUseCase = RefreshAuthSessionUseCase(authRepository),
            searchDebounceMs = 0L,
            createMinDelayMs = 0L,
            loadMinDelayMs = 0L,
            pageLimit = pageLimit,
        )
    }

    @Test
    fun `init loads spaces successfully`() {
        val viewModel = createViewModel()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
        assertEquals(2, viewModel.uiState.value.visibleSpaces.size)
        assertEquals("Dissertation Research", viewModel.uiState.value.visibleSpaces.first().title)
        assertEquals(1, viewModel.uiState.value.currentPage)
        assertFalse(viewModel.uiState.value.hasMore)
        assertNull(spaceRepository.lastSearchQuery)
        assertEquals(1, spaceRepository.lastPage)
    }

    @Test
    fun `init syncs current user and loads signed in account`() {
        val viewModel = createViewModel()

        assertEquals(1, authRepository.syncCurrentUserCallCount)
        assertEquals("me", authRepository.lastSyncUserId)
        assertEquals(1, viewModel.uiState.value.accounts.size)
        assertEquals("user-1", viewModel.uiState.value.accounts.first().id)
        assertEquals("Jordan Lee", viewModel.uiState.value.accounts.first().displayName)
        assertEquals("jordan@folio.app", viewModel.uiState.value.accounts.first().email)
        assertTrue(viewModel.uiState.value.accounts.first().isSelected)
    }

    @Test
    fun `init falls back to email when user id missing`() {
        val viewModel = createViewModel(userId = null)

        assertEquals("jordan@folio.app", viewModel.uiState.value.accounts.first().id)
    }

    @Test
    fun `onAccountSelected reloads spaces`() {
        val viewModel = createViewModel()
        val callsBefore = spaceRepository.getSpacesCallCount

        viewModel.onAccountSelected("user-1")

        assertTrue(viewModel.uiState.value.accounts.first { it.id == "user-1" }.isSelected)
        assertEquals(callsBefore + 1, spaceRepository.getSpacesCallCount)
        assertEquals(2, viewModel.uiState.value.visibleSpaces.size)
        assertEquals(1, spaceRepository.lastPage)
    }

    @Test
    fun `init sets error when load fails`() {
        spaceRepository.spacesResult = Result.failure(IllegalStateException("offline"))

        val viewModel = createViewModel()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("offline", viewModel.uiState.value.error)
    }

    @Test
    fun `onRetry refreshes session then reloads spaces`() {
        spaceRepository.spacesResult = Result.failure(IllegalStateException("unauthorized"))
        val viewModel = createViewModel()
        assertEquals("unauthorized", viewModel.uiState.value.error)

        spaceRepository.spacesResult = Result.success(
            FakeSpaceRepository.pageOf(
                spaces = listOf(
                    Space("1", "Dissertation Research", "Primary research archive for doctoral thesis", 128, 32, "Updated 2d ago"),
                ),
            ),
        )
        authRepository.refreshSessionResult = Result.success(
            AuthSession(
                email = "jordan@folio.app",
                displayName = "Jordan Lee",
                userId = "user-1",
                accessToken = "access-refreshed",
                refreshToken = "refresh-refreshed",
            ),
        )

        viewModel.onRetry()

        assertEquals(1, authRepository.refreshSessionCallCount)
        assertNull(viewModel.uiState.value.error)
        assertEquals(1, viewModel.uiState.value.visibleSpaces.size)
        assertEquals(1, spaceRepository.lastPage)
        assertFalse(viewModel.uiState.value.requiresReauth)
    }

    @Test
    fun `onRefresh reloads first page without full-screen loading`() = runTest {
        val viewModel = createViewModel()
        assertFalse(viewModel.uiState.value.isLoading)
        val loadsBefore = spaceRepository.getSpacesCallCount

        val refreshStarted = CompletableDeferred<Unit>()
        val releaseRefresh = CompletableDeferred<Unit>()
        spaceRepository.getSpacesGate = {
            refreshStarted.complete(Unit)
            releaseRefresh.await()
        }

        viewModel.onRefresh()
        refreshStarted.await()

        assertEquals(loadsBefore + 1, spaceRepository.getSpacesCallCount)
        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.isRefreshing)

        releaseRefresh.complete(Unit)

        assertEquals(1, spaceRepository.lastPage)
        assertFalse(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.isRefreshing)
        assertTrue(viewModel.uiState.value.visibleSpaces.isNotEmpty())
    }

    @Test
    fun `onRetry requires reauth when refresh clears session`() {
        spaceRepository.spacesResult = Result.failure(IllegalStateException("unauthorized"))
        val viewModel = createViewModel()
        assertEquals("unauthorized", viewModel.uiState.value.error)

        authRepository.refreshSessionResult = Result.failure(
            AuthApiException("Refresh token expired"),
        )

        viewModel.onRetry()

        assertEquals(1, authRepository.refreshSessionCallCount)
        assertTrue(viewModel.uiState.value.requiresReauth)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(authRepository.getCurrentSession())
    }

    @Test
    fun `onSearchQueryChange requests spaces from api and resets page`() {
        val viewModel = createViewModel()
        spaceRepository.spacesResult = Result.success(
            FakeSpaceRepository.pageOf(
                spaces = listOf(
                    Space("2", "Public Policy Insights", "Policy papers and legislative analysis", 64, 18, "Updated 5h ago"),
                ),
            ),
        )

        viewModel.onSearchQueryChange("Policy")

        assertEquals("Policy", spaceRepository.lastSearchQuery)
        assertEquals(1, spaceRepository.lastPage)
        assertEquals(1, viewModel.uiState.value.visibleSpaces.size)
        assertEquals("Public Policy Insights", viewModel.uiState.value.visibleSpaces.first().title)
        assertEquals(1, viewModel.uiState.value.currentPage)
    }

    @Test
    fun `newer search result is not overwritten by slower older search`() = runTest {
        val firstSearchStarted = CompletableDeferred<Unit>()
        val releaseFirstSearch = CompletableDeferred<Unit>()
        spaceRepository.getSpacesGate = { query ->
            if (query == "old") {
                firstSearchStarted.complete(Unit)
                releaseFirstSearch.await()
                spaceRepository.spacesResult = Result.success(
                    FakeSpaceRepository.pageOf(
                        spaces = listOf(Space("old", "Old Query Space", "", 0, 0, "Updated 1d ago")),
                    ),
                )
            } else if (query == "new") {
                spaceRepository.spacesResult = Result.success(
                    FakeSpaceRepository.pageOf(
                        spaces = listOf(Space("new", "New Query Space", "", 0, 0, "Updated just now")),
                    ),
                )
            }
        }
        val viewModel = createViewModel()

        viewModel.onSearchQueryChange("old")
        firstSearchStarted.await()
        viewModel.onSearchQueryChange("new")
        releaseFirstSearch.complete(Unit)

        assertEquals("new", spaceRepository.lastSearchQuery)
        assertEquals(listOf("new"), viewModel.uiState.value.visibleSpaces.map { it.id })
        assertEquals("New Query Space", viewModel.uiState.value.visibleSpaces.first().title)
    }

    @Test
    fun `onLoadMore appends next page`() {
        spaceRepository.pageResults = mapOf(
            1 to Result.success(
                SpacePage(
                    spaces = listOf(
                        Space("1", "Space One", "", 0, 0, "Updated 1d ago"),
                        Space("2", "Space Two", "", 0, 0, "Updated 1d ago"),
                    ),
                    page = 1,
                    limit = 2,
                    hasMore = true,
                ),
            ),
            2 to Result.success(
                SpacePage(
                    spaces = listOf(
                        Space("3", "Space Three", "", 0, 0, "Updated 1d ago"),
                    ),
                    page = 2,
                    limit = 2,
                    hasMore = false,
                ),
            ),
        )
        val viewModel = createViewModel(pageLimit = 2)
        assertEquals(listOf("1", "2"), viewModel.uiState.value.visibleSpaces.map { it.id })
        assertTrue(viewModel.uiState.value.hasMore)

        viewModel.onLoadMore()

        assertEquals(2, spaceRepository.lastPage)
        assertEquals(listOf("1", "2", "3"), viewModel.uiState.value.visibleSpaces.map { it.id })
        assertEquals(2, viewModel.uiState.value.currentPage)
        assertFalse(viewModel.uiState.value.hasMore)
        assertFalse(viewModel.uiState.value.isLoadingMore)
    }

    @Test
    fun `onLoadMore is ignored when hasMore is false`() {
        val viewModel = createViewModel()
        val callsBefore = spaceRepository.getSpacesCallCount

        viewModel.onLoadMore()

        assertEquals(callsBefore, spaceRepository.getSpacesCallCount)
    }

    @Test
    fun `onLoadMore failure keeps existing list`() {
        spaceRepository.pageResults = mapOf(
            1 to Result.success(
                SpacePage(
                    spaces = listOf(Space("1", "Space One", "", 0, 0, "Updated 1d ago")),
                    page = 1,
                    limit = 1,
                    hasMore = true,
                ),
            ),
            2 to Result.failure(IllegalStateException("timeout")),
        )
        val viewModel = createViewModel(pageLimit = 1)
        assertEquals(1, viewModel.uiState.value.visibleSpaces.size)

        viewModel.onLoadMore()

        assertEquals(1, viewModel.uiState.value.visibleSpaces.size)
        assertEquals("1", viewModel.uiState.value.visibleSpaces.first().id)
        assertNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoadingMore)
        assertTrue(viewModel.uiState.value.hasMore)
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
    fun `onSortSelected reloads spaces with selected sort`() {
        val viewModel = createViewModel()
        assertEquals(SpaceSort.RECENTLY_UPDATED, spaceRepository.lastSort)

        viewModel.onSortSelected(SpaceSort.ALPHABETICAL_ZA)

        assertFalse(viewModel.uiState.value.showSortSheet)
        assertEquals(SpaceSort.ALPHABETICAL_ZA, viewModel.uiState.value.selectedSort)
        assertEquals(SpaceSort.ALPHABETICAL_ZA, spaceRepository.lastSort)
        assertEquals(1, spaceRepository.lastPage)
    }

    @Test
    fun `onSortSelected same sort only dismisses sheet`() {
        val viewModel = createViewModel()
        val callsBefore = spaceRepository.getSpacesCallCount
        viewModel.onFilterSortClick()

        viewModel.onSortSelected(SpaceSort.RECENTLY_UPDATED)

        assertFalse(viewModel.uiState.value.showSortSheet)
        assertEquals(callsBefore, spaceRepository.getSpacesCallCount)
    }

    @Test
    fun `onAddClick shows add sheet`() {
        val viewModel = createViewModel()

        viewModel.onAddClick()

        assertTrue(viewModel.uiState.value.showAddSheet)
    }

    @Test
    fun `onAccountClick shows account sheet`() {
        val viewModel = createViewModel()

        viewModel.onAccountClick()

        assertTrue(viewModel.uiState.value.showAccountSheet)

        viewModel.onAccountSheetDismiss()

        assertFalse(viewModel.uiState.value.showAccountSheet)
    }

    @Test
    fun `onAddSpaceSubmit creates space and dismisses sheet`() {
        spaceRepository.createSpaceResult = Result.success(
            Space(
                id = "new-1",
                title = "New Space",
                description = "Research goals",
                sourceCount = 0,
                noteCount = 0,
                updatedLabel = "Updated just now",
            ),
        )
        val viewModel = createViewModel()
        viewModel.onAddClick()

        viewModel.onAddSpaceSubmit(name = "New Space", objective = "Research goals")

        assertFalse(viewModel.uiState.value.showAddSheet)
        assertEquals("New Space", spaceRepository.lastCreateName)
        assertEquals("Research goals", spaceRepository.lastCreateObjective)
        assertEquals("new-1", viewModel.uiState.value.visibleSpaces.first().id)
        assertEquals(SpaceUserMessage.SPACE_CREATED, viewModel.uiState.value.userMessage)
    }

    @Test
    fun `onAddSpaceSubmit keeps sheet open when create fails`() {
        spaceRepository.createSpaceResult = Result.failure(IllegalStateException("quota exceeded"))
        val viewModel = createViewModel()
        viewModel.onAddClick()

        viewModel.onAddSpaceSubmit(name = "New Space", objective = "Research goals")

        assertTrue(viewModel.uiState.value.showAddSheet)
        assertEquals("quota exceeded", viewModel.uiState.value.actionError)
        assertNull(viewModel.uiState.value.userMessage)
    }

    @Test
    fun `onSpaceOptionsClick shows options sheet for space`() {
        val viewModel = createViewModel()
        val space = viewModel.uiState.value.visibleSpaces.first()

        viewModel.onSpaceOptionsClick(space)

        assertEquals(space, viewModel.uiState.value.optionsSpace)
    }

    @Test
    fun `onRenameSpaceClick opens rename sheet`() {
        val viewModel = createViewModel()
        val space = viewModel.uiState.value.visibleSpaces.first()
        viewModel.onSpaceOptionsClick(space)

        viewModel.onRenameSpaceClick()

        assertNull(viewModel.uiState.value.optionsSpace)
        assertEquals(space, viewModel.uiState.value.renamingSpace)
    }

    @Test
    fun `onRenameSpaceSave updates space title and objective`() {
        val viewModel = createViewModel()
        val space = viewModel.uiState.value.visibleSpaces.first()
        viewModel.onSpaceOptionsClick(space)
        viewModel.onRenameSpaceClick()

        viewModel.onRenameSpaceSave(
            name = "Renamed space",
            researchObjective = "Updated research objective",
        )

        assertNull(viewModel.uiState.value.renamingSpace)
        assertFalse(viewModel.uiState.value.isUpdatingSpace)
        assertEquals(1, spaceRepository.updateSpaceCallCount)
        assertEquals(space.id, spaceRepository.lastUpdateSpaceId)
        assertEquals("Renamed space", spaceRepository.lastUpdateName)
        assertEquals("Updated research objective", spaceRepository.lastUpdateObjective)
        val updated = viewModel.uiState.value.allSpaces.first { it.id == space.id }
        assertEquals("Renamed space", updated.title)
        assertEquals("Updated research objective", updated.description)
        assertEquals(SpaceUserMessage.SPACE_UPDATED, viewModel.uiState.value.userMessage)
    }

    @Test
    fun `onRenameSpaceSave keeps sheet open when update fails`() {
        spaceRepository.updateSpaceResult = Result.failure(IllegalStateException("offline"))
        val viewModel = createViewModel()
        val space = viewModel.uiState.value.visibleSpaces.first()
        viewModel.onSpaceOptionsClick(space)
        viewModel.onRenameSpaceClick()

        viewModel.onRenameSpaceSave(
            name = "Renamed space",
            researchObjective = space.description,
        )

        assertEquals(space, viewModel.uiState.value.renamingSpace)
        assertFalse(viewModel.uiState.value.isUpdatingSpace)
        assertEquals("offline", viewModel.uiState.value.actionError)
        assertNull(viewModel.uiState.value.userMessage)
    }

    @Test
    fun `onDeleteSpaceClick opens delete confirmation`() {
        val viewModel = createViewModel()
        val space = viewModel.uiState.value.visibleSpaces.first()
        viewModel.onSpaceOptionsClick(space)

        viewModel.onDeleteSpaceClick()

        assertNull(viewModel.uiState.value.optionsSpace)
        assertEquals(space, viewModel.uiState.value.deletingSpace)
    }

    @Test
    fun `onDeleteSpaceConfirm removes space and sets deleted message`() {
        val viewModel = createViewModel()
        val space = viewModel.uiState.value.visibleSpaces.first()
        viewModel.onSpaceOptionsClick(space)
        viewModel.onDeleteSpaceClick()

        viewModel.onDeleteSpaceConfirm()

        assertNull(viewModel.uiState.value.deletingSpace)
        assertFalse(viewModel.uiState.value.isDeletingSpace)
        assertEquals(1, spaceRepository.deleteSpaceCallCount)
        assertEquals(space.id, spaceRepository.lastDeletedSpaceId)
        assertTrue(viewModel.uiState.value.allSpaces.none { it.id == space.id })
        assertEquals(SpaceUserMessage.SPACE_DELETED, viewModel.uiState.value.userMessage)
    }

    @Test
    fun `onDeleteSpaceConfirm ignores repeated confirm while deleting`() = runTest {
        val gate = CompletableDeferred<Unit>()
        spaceRepository.deleteSpaceGate = { gate.await() }
        val viewModel = createViewModel()
        val space = viewModel.uiState.value.visibleSpaces.first()
        viewModel.onSpaceOptionsClick(space)
        viewModel.onDeleteSpaceClick()

        viewModel.onDeleteSpaceConfirm()
        assertTrue(viewModel.uiState.value.isDeletingSpace)
        viewModel.onDeleteSpaceConfirm()
        viewModel.onDeleteSpaceDismiss()

        assertEquals(1, spaceRepository.deleteSpaceCallCount)
        assertEquals(space, viewModel.uiState.value.deletingSpace)

        gate.complete(Unit)
        assertNull(viewModel.uiState.value.deletingSpace)
        assertFalse(viewModel.uiState.value.isDeletingSpace)
    }

    @Test
    fun `onDeleteSpaceConfirm closes sheet when delete fails`() {
        spaceRepository.deleteSpaceResult = Result.failure(IllegalStateException("offline"))
        val viewModel = createViewModel()
        val space = viewModel.uiState.value.visibleSpaces.first()
        viewModel.onSpaceOptionsClick(space)
        viewModel.onDeleteSpaceClick()

        viewModel.onDeleteSpaceConfirm()

        assertNull(viewModel.uiState.value.deletingSpace)
        assertFalse(viewModel.uiState.value.isDeletingSpace)
        assertTrue(viewModel.uiState.value.allSpaces.any { it.id == space.id })
        assertEquals("offline", viewModel.uiState.value.actionError)
        assertNull(viewModel.uiState.value.userMessage)
    }
}
