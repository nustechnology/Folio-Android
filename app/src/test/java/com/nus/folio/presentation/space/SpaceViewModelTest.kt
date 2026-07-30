package com.nus.folio.presentation.space

import com.nus.folio.domain.model.Space
import com.nus.folio.domain.usecase.GetSpacesUseCase
import com.nus.folio.testing.FakeSpaceRepository
import com.nus.folio.testing.MainDispatcherRule
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

    private fun createViewModel(): SpaceViewModel =
        SpaceViewModel(getSpacesUseCase = GetSpacesUseCase(spaceRepository))

    @Test
    fun `init loads spaces successfully`() {
        val viewModel = createViewModel()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
        assertEquals(2, viewModel.uiState.value.visibleSpaces.size)
        assertEquals("Dissertation Research", viewModel.uiState.value.visibleSpaces.first().title)
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
    fun `onSearchClick toggles search visibility and clears query when hidden`() {
        val viewModel = createViewModel()

        viewModel.onSearchQueryChange("Policy")
        viewModel.onSearchClick()

        assertTrue(viewModel.uiState.value.isSearchVisible)
        assertEquals(1, viewModel.uiState.value.visibleSpaces.size)

        viewModel.onSearchClick()

        assertFalse(viewModel.uiState.value.isSearchVisible)
        assertEquals("", viewModel.uiState.value.searchQuery)
        assertEquals(2, viewModel.uiState.value.visibleSpaces.size)
    }

    @Test
    fun `onAddClick shows add sheet`() {
        val viewModel = createViewModel()

        viewModel.onAddClick()

        assertTrue(viewModel.uiState.value.showAddSheet)
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
    fun `onRenameSpaceClick dismisses options and sets message`() {
        val viewModel = createViewModel()
        val space = viewModel.uiState.value.visibleSpaces.first()
        viewModel.onSpaceOptionsClick(space)

        viewModel.onRenameSpaceClick()

        assertNull(viewModel.uiState.value.optionsSpace)
        assertEquals(SpaceUserMessage.RENAME_SPACE_NOT_SUPPORTED, viewModel.uiState.value.userMessage)
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
