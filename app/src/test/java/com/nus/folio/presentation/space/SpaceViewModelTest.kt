package com.nus.folio.presentation.space

import com.nus.folio.domain.model.AuthSession
import com.nus.folio.domain.model.Space
import com.nus.folio.domain.usecase.GetCurrentSessionUseCase
import com.nus.folio.domain.usecase.GetSpacesUseCase
import com.nus.folio.testing.FakeAuthRepository
import com.nus.folio.testing.FakeSpaceRepository
import com.nus.folio.testing.MainDispatcherRule
import kotlinx.coroutines.runBlocking
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
            listOf(
                Space("1", "Dissertation Research", "Primary research archive for doctoral thesis", 128, 32, "Updated 2d ago"),
                Space("2", "Public Policy Insights", "Policy papers and legislative analysis", 64, 18, "Updated 5h ago"),
            ),
        ),
    )

    private val authRepository = FakeAuthRepository()

    private fun createViewModel(
        email: String = "alex@folio.app",
        displayName: String = "Alex Nguyen",
    ): SpaceViewModel {
        authRepository.clearSession()
        authRepository.signInResult = Result.success(
            AuthSession(email = email, displayName = displayName),
        )
        runBlocking { authRepository.signIn(email, "password") }
        return SpaceViewModel(
            getSpacesUseCase = GetSpacesUseCase(spaceRepository),
            getCurrentSessionUseCase = GetCurrentSessionUseCase(authRepository),
        )
    }

    @Test
    fun `init loads spaces successfully`() {
        val viewModel = createViewModel()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
        assertEquals(2, viewModel.uiState.value.visibleSpaces.size)
        assertEquals("Dissertation Research", viewModel.uiState.value.visibleSpaces.first().title)
    }

    @Test
    fun `init loads current account and empty mock account`() {
        val viewModel = createViewModel()

        assertEquals(2, viewModel.uiState.value.accounts.size)
        assertEquals("Alex Nguyen", viewModel.uiState.value.accounts.first().displayName)
        assertEquals("alex@folio.app", viewModel.uiState.value.accounts.first().email)
        assertTrue(viewModel.uiState.value.accounts.first().isSelected)
        assertFalse(viewModel.uiState.value.accounts.first().isPlaceholder)
        assertEquals(SpaceViewModel.PLACEHOLDER_ACCOUNT_ID, viewModel.uiState.value.accounts[1].id)
        assertEquals(SpaceViewModel.PLACEHOLDER_ACCOUNT_EMAIL, viewModel.uiState.value.accounts[1].email)
        assertTrue(viewModel.uiState.value.accounts[1].isPlaceholder)
        assertFalse(viewModel.uiState.value.accounts[1].isSelected)
    }

    @Test
    fun `onAccountSelected empty account clears spaces`() {
        val viewModel = createViewModel()
        assertEquals(2, viewModel.uiState.value.visibleSpaces.size)

        viewModel.onAccountSelected(SpaceViewModel.PLACEHOLDER_ACCOUNT_ID)

        assertTrue(
            viewModel.uiState.value.accounts.first {
                it.id == SpaceViewModel.PLACEHOLDER_ACCOUNT_ID
            }.isSelected,
        )
        assertTrue(viewModel.uiState.value.visibleSpaces.isEmpty())
        assertFalse(viewModel.uiState.value.showAccountSheet)
    }

    @Test
    fun `onAccountSelected primary account restores spaces`() {
        val viewModel = createViewModel()
        viewModel.onAccountSelected(SpaceViewModel.PLACEHOLDER_ACCOUNT_ID)

        viewModel.onAccountSelected("alex@folio.app")

        assertTrue(viewModel.uiState.value.accounts.first { it.id == "alex@folio.app" }.isSelected)
        assertEquals(2, viewModel.uiState.value.visibleSpaces.size)
    }

    @Test
    fun `onAccountSelected keeps primary spaces when email matches placeholder email`() {
        val collidingEmail = SpaceViewModel.PLACEHOLDER_ACCOUNT_EMAIL
        val viewModel = createViewModel(
            email = collidingEmail,
            displayName = "Jordan Lee",
        )

        assertEquals(2, viewModel.uiState.value.accounts.size)
        assertEquals(1, viewModel.uiState.value.accounts.count { it.isSelected })
        assertTrue(viewModel.uiState.value.accounts.first { it.id == collidingEmail }.isSelected)
        assertFalse(
            viewModel.uiState.value.accounts.first {
                it.id == SpaceViewModel.PLACEHOLDER_ACCOUNT_ID
            }.isSelected,
        )
        assertEquals(2, viewModel.uiState.value.visibleSpaces.size)

        viewModel.onAccountSelected(SpaceViewModel.PLACEHOLDER_ACCOUNT_ID)
        assertTrue(viewModel.uiState.value.visibleSpaces.isEmpty())
        assertEquals(1, viewModel.uiState.value.accounts.count { it.isSelected })

        viewModel.onAccountSelected(collidingEmail)
        assertEquals(2, viewModel.uiState.value.visibleSpaces.size)
        assertTrue(viewModel.uiState.value.accounts.first { it.id == collidingEmail }.isSelected)
        assertFalse(
            viewModel.uiState.value.accounts.first {
                it.id == SpaceViewModel.PLACEHOLDER_ACCOUNT_ID
            }.isSelected,
        )
    }

    @Test
    fun `init sets error when load fails`() {
        spaceRepository.spacesResult = Result.failure(IllegalStateException("offline"))

        val viewModel = createViewModel()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("offline", viewModel.uiState.value.error)
    }

    @Test
    fun `onSearchQueryChange filters spaces`() {
        val viewModel = createViewModel()

        viewModel.onSearchQueryChange("Policy")

        assertEquals(1, viewModel.uiState.value.visibleSpaces.size)
        assertEquals("Public Policy Insights", viewModel.uiState.value.visibleSpaces.first().title)
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
    fun `onAddSpaceSubmit sets user message without dismissing sheet`() {
        val viewModel = createViewModel()
        viewModel.onAddClick()

        viewModel.onAddSpaceSubmit(name = "New Space", objective = "Research goals")

        assertTrue(viewModel.uiState.value.showAddSheet)
        assertEquals(SpaceUserMessage.ADD_SPACE_NOT_SUPPORTED, viewModel.uiState.value.userMessage)

        viewModel.onAddSheetDismiss()

        assertFalse(viewModel.uiState.value.showAddSheet)

        viewModel.onUserMessageShown()

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
    fun `onRenameSpaceSave updates space title`() {
        val viewModel = createViewModel()
        val space = viewModel.uiState.value.visibleSpaces.first()
        viewModel.onSpaceOptionsClick(space)
        viewModel.onRenameSpaceClick()

        viewModel.onRenameSpaceSave("Renamed space")

        assertNull(viewModel.uiState.value.renamingSpace)
        assertEquals(
            "Renamed space",
            viewModel.uiState.value.allSpaces.first { it.id == space.id }.title,
        )
        assertEquals(SpaceUserMessage.SPACE_UPDATED, viewModel.uiState.value.userMessage)
    }

    @Test
    fun `onDeleteSpaceClick dismisses options and sets message`() {
        val viewModel = createViewModel()
        val space = viewModel.uiState.value.visibleSpaces.first()
        viewModel.onSpaceOptionsClick(space)

        viewModel.onDeleteSpaceClick()

        assertNull(viewModel.uiState.value.optionsSpace)
        assertEquals(SpaceUserMessage.DELETE_SPACE_NOT_SUPPORTED, viewModel.uiState.value.userMessage)
    }
}
