package com.nus.folio.presentation.sourcedetail

import com.nus.folio.domain.model.SourceContentFormat
import com.nus.folio.domain.model.SourceDetail
import com.nus.folio.domain.model.SourceSheetTab
import com.nus.folio.domain.model.SourceStatus
import com.nus.folio.domain.model.SourceType
import com.nus.folio.domain.usecase.GetSourceDetailUseCase
import com.nus.folio.domain.usecase.GetSourceOriginalFileUseCase
import com.nus.folio.domain.model.SourceFileLocation
import com.nus.folio.testing.FakeSourceRepository
import com.nus.folio.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SourceDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeSourceRepository()

    private fun createViewModel(
        spaceId: String = "1",
        sourceId: String = "1",
    ): SourceDetailViewModel =
        SourceDetailViewModel(
            spaceId = spaceId,
            sourceId = sourceId,
            getSourceDetailUseCase = GetSourceDetailUseCase(repository),
            getSourceOriginalFileUseCase = GetSourceOriginalFileUseCase(repository),
        )

    @Test
    fun `init loads source detail successfully`() {
        val viewModel = createViewModel()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
        assertEquals("Alan Turing: Computing Machinery", viewModel.uiState.value.detail?.title)
    }

    @Test
    fun `init sets error when load fails`() {
        repository.getSourceDetailResult = Result.failure(IllegalStateException("offline"))

        val viewModel = createViewModel()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("offline", viewModel.uiState.value.error)
    }

    @Test
    fun `onSheetSelected updates selected sheet index`() {
        repository.getSourceDetailResult = Result.success(
            SourceDetail(
                id = "10",
                title = "Research metrics dashboard",
                author = "Research team",
                addedLabel = "Added 1d ago",
                type = SourceType.PDF,
                status = SourceStatus.READY,
                spaceId = "1",
                fileExtension = "xlsx",
                contentFormat = SourceContentFormat.SHEET,
                originalFileName = "research-metrics-dashboard.xlsx",
                sheets = listOf(
                    SourceSheetTab("summary", "Summary", "<table></table>"),
                    SourceSheetTab("raw", "Raw Data", "<table></table>"),
                ),
            ),
        )

        val viewModel = createViewModel(sourceId = "10")

        viewModel.onSheetSelected(1)

        assertEquals(1, viewModel.uiState.value.selectedSheetIndex)
    }

    @Test
    fun `loadDetail retries after failure`() {
        repository.getSourceDetailResult = Result.failure(IllegalStateException("offline"))
        val viewModel = createViewModel()

        assertEquals("offline", viewModel.uiState.value.error)

        repository.getSourceDetailResult = null
        viewModel.loadDetail()

        assertNull(viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.detail != null)
    }

    @Test
    fun `onOpenOriginalClick requests original file location`() {
        val viewModel = createViewModel()

        viewModel.onOpenOriginalClick()

        assertEquals(1, repository.getOriginalFileCallCount)
        assertTrue(viewModel.uiState.value.openOriginalRequest is SourceFileLocation.Local)
    }
}
