package com.nus.folio.presentation.home

import com.nus.folio.domain.usecase.GetNotebookUseCase
import com.nus.folio.domain.usecase.SaveNotebookUseCase
import com.nus.folio.domain.util.NotebookDefaults
import com.nus.folio.testing.FakeNotebookRepository
import com.nus.folio.testing.MainDispatcherRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeNotebookDelegateTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val notebookRepository = FakeNotebookRepository()
    private val state = MutableStateFlow(HomeUiState())

    private val spaceTitle = "Dissertation Research"
    private val researchObjective = "Primary research archive for doctoral thesis"

    private fun stockScaffold(): String = NotebookDefaults.template(
        spaceTitle = spaceTitle,
        researchObjective = researchObjective,
    )

    private fun createDelegate(
        scope: CoroutineScope,
        spaceId: String = "1",
        saveDebounceMs: Long = 0L,
    ): HomeNotebookDelegate {
        state.value = HomeUiState(
            spaceId = spaceId,
            spaceTitle = spaceTitle,
            spaceResearchObjective = researchObjective,
        )
        return HomeNotebookDelegate(
            spaceId = spaceId,
            state = state,
            scope = scope,
            getNotebookUseCase = GetNotebookUseCase(notebookRepository),
            saveNotebookUseCase = SaveNotebookUseCase(notebookRepository),
            saveDebounceMs = saveDebounceMs,
        )
    }

    @Test
    fun `loadNotebook sanitizes stock scaffold when no pending edits`() = runTest {
        notebookRepository.seed(spaceId = "1", content = stockScaffold())
        val delegate = createDelegate(scope = this)

        delegate.loadNotebook()
        advanceUntilIdle()

        assertEquals("", state.value.notebookContent)
        assertEquals(NotebookSaveStatus.SAVED, state.value.notebookSaveStatus)
        assertEquals(1, notebookRepository.saveNotebookCallCount)
        assertEquals("", notebookRepository.lastSavedContent)
    }

    @Test
    fun `loadNotebook keeps custom content without sanitation save`() = runTest {
        notebookRepository.seed(spaceId = "1", content = "custom notes")
        val delegate = createDelegate(scope = this)

        delegate.loadNotebook()
        advanceUntilIdle()

        assertEquals("custom notes", state.value.notebookContent)
        assertEquals(NotebookSaveStatus.IDLE, state.value.notebookSaveStatus)
        assertEquals(0, notebookRepository.saveNotebookCallCount)
    }

    @Test
    fun `loadNotebook skips sanitation save when status marks pending edits`() = runTest {
        notebookRepository.seed(spaceId = "1", content = stockScaffold())
        val delegate = createDelegate(scope = this)
        state.update {
            it.copy(
                notebookContent = "user draft in progress",
                notebookSaveStatus = NotebookSaveStatus.SAVING,
            )
        }

        delegate.loadNotebook()
        advanceUntilIdle()

        assertEquals("user draft in progress", state.value.notebookContent)
        assertEquals(NotebookSaveStatus.SAVING, state.value.notebookSaveStatus)
        assertEquals(0, notebookRepository.saveNotebookCallCount)
    }

    @Test
    fun `loadNotebook does not cancel pending debounced edit save with sanitation save`() = runTest {
        notebookRepository.seed(spaceId = "1", content = stockScaffold())
        val delegate = createDelegate(scope = this, saveDebounceMs = 1_000)

        delegate.onNotebookContentChange("user draft")
        assertEquals(NotebookSaveStatus.SAVING, state.value.notebookSaveStatus)

        delegate.loadNotebook()
        advanceUntilIdle()

        assertEquals("user draft", state.value.notebookContent)
        assertEquals(NotebookSaveStatus.SAVED, state.value.notebookSaveStatus)
        assertEquals(1, notebookRepository.saveNotebookCallCount)
        assertEquals("user draft", notebookRepository.lastSavedContent)
    }
}
