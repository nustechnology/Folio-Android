package com.nus.folio.presentation.sourcedetail

import com.nus.folio.domain.model.SourceContentFormat
import com.nus.folio.domain.model.SourceDetail
import com.nus.folio.domain.model.SourceFileLocation
import com.nus.folio.domain.model.SourceSheetTab
import com.nus.folio.domain.model.SourceStatus
import com.nus.folio.domain.model.SourceType
import com.nus.folio.domain.usecase.GetSourceDetailUseCase
import com.nus.folio.domain.usecase.GetSourcePreviewUrlUseCase
import com.nus.folio.domain.usecase.ObserveSourceProcessingUseCase
import com.nus.folio.domain.usecase.RetrySourceUseCase
import com.nus.folio.domain.usecase.DeleteSourceUseCase
import com.nus.folio.domain.usecase.UpdateSourceUseCase
import com.nus.folio.testing.FakeSourceRepository
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

@OptIn(ExperimentalCoroutinesApi::class)
class SourceDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeSourceRepository()

    private fun createViewModel(
        spaceId: String = "1",
        sourceId: String = "1",
        contentRevealDelayMs: Long = 0L,
    ): SourceDetailViewModel =
        SourceDetailViewModel(
            spaceId = spaceId,
            sourceId = sourceId,
            getSourceDetailUseCase = GetSourceDetailUseCase(repository),
            getSourcePreviewUrlUseCase = GetSourcePreviewUrlUseCase(repository),
            retrySourceUseCase = RetrySourceUseCase(repository),
            observeSourceProcessingUseCase = ObserveSourceProcessingUseCase(repository),
            updateSourceUseCase = UpdateSourceUseCase(repository),
            deleteSourceUseCase = DeleteSourceUseCase(repository),
            contentRevealDelayMs = contentRevealDelayMs,
        )

    @Test
    fun `init loads source detail successfully`() {
        val viewModel = createViewModel()

        assertFalse(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.isContentLoading)
        assertNull(viewModel.uiState.value.error)
        assertEquals("Alan Turing: Computing Machinery", viewModel.uiState.value.detail?.title)
    }

    @Test
    fun `init loads preview url when available`() {
        val viewModel = createViewModel()

        assertEquals(1, repository.getSourcePreviewUrlCallCount)
        assertEquals(
            "https://example.org/preview/1.pdf",
            viewModel.uiState.value.previewUrl,
        )
    }

    @Test
    fun `init hides preview when preview url unavailable`() {
        repository.getSourcePreviewUrlResult = Result.success(null)

        val viewModel = createViewModel()

        assertNull(viewModel.uiState.value.previewUrl)
    }

    @Test
    fun `init sets error when load fails`() {
        repository.getSourceDetailResult = Result.failure(IllegalStateException("offline"))

        val viewModel = createViewModel()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("offline", viewModel.uiState.value.error)
        assertEquals(0, repository.getSourcePreviewUrlCallCount)
    }

    @Test
    fun `onSheetSelected updates selected sheet index`() {
        repository.getSourceDetailResult = Result.success(
            SourceDetail(
                id = "10",
                title = "Research metrics dashboard",
                author = "Research team",
                addedLabel = "Added 1d ago",
                type = SourceType.FILE,
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
    fun `onOpenOriginalClick opens preview url`() {
        val viewModel = createViewModel()

        viewModel.onOpenOriginalClick()

        val request = viewModel.uiState.value.openOriginalRequest
        assertTrue(request is SourceFileLocation.Remote)
        assertEquals(
            "https://example.org/preview/1.pdf",
            (request as SourceFileLocation.Remote).url,
        )
    }

    @Test
    fun `onOpenOriginalClick does nothing without preview url`() {
        repository.getSourcePreviewUrlResult = Result.success(null)
        val viewModel = createViewModel()

        viewModel.onOpenOriginalClick()

        assertNull(viewModel.uiState.value.openOriginalRequest)
    }

    @Test
    fun `onRetryProcessing marks source processing`() {
        repository.getSourceDetailResult = Result.success(
            SourceDetail(
                id = "4",
                title = "Failed source",
                author = "Author",
                addedLabel = "Added 1d ago",
                type = SourceType.FILE,
                status = SourceStatus.FAILED,
                spaceId = "1",
                fileExtension = "pdf",
                contentFormat = SourceContentFormat.DOCUMENT,
                originalFileName = "failed.pdf",
            ),
        )
        val viewModel = createViewModel(sourceId = "4")

        viewModel.onRetryProcessing()

        assertEquals(1, repository.retrySourceCallCount)
        assertEquals("4", repository.lastRetriedSourceId)
        assertEquals(SourceStatus.PROCESSING, viewModel.uiState.value.detail?.status)
        assertFalse(viewModel.uiState.value.isRetrying)
        assertNull(viewModel.uiState.value.previewUrl)
    }

    @Test
    fun `onEditSourceSave ignores repeated save while updating`() = runTest {
        val gate = CompletableDeferred<Unit>()
        repository.updateSourceGate = { gate.await() }
        val viewModel = createViewModel()
        viewModel.onEditSourceClick()

        viewModel.onEditSourceSave(title = "Updated title", author = "Author")
        assertTrue(viewModel.uiState.value.isUpdatingSource)
        viewModel.onEditSourceSave(title = "Second title", author = "Author")
        viewModel.onEditSourceDismiss()

        assertEquals(1, repository.updateSourceCallCount)
        assertEquals("Updated title", repository.lastUpdatedSource?.title)
        assertNotNull(viewModel.uiState.value.editingSource)

        gate.complete(Unit)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.editingSource)
        assertFalse(viewModel.uiState.value.isUpdatingSource)
        assertEquals("Updated title", viewModel.uiState.value.detail?.title)
        assertEquals(SourceDetailUserMessage.SOURCE_UPDATED, viewModel.uiState.value.userMessage)
    }

    @Test
    fun `onDeleteSourceConfirm ignores repeated confirm while deleting`() = runTest {
        val gate = CompletableDeferred<Unit>()
        repository.deleteSourceGate = { gate.await() }
        val viewModel = createViewModel()
        viewModel.onDeleteSourceClick()

        viewModel.onDeleteSourceConfirm()
        assertTrue(viewModel.uiState.value.isDeletingSource)
        viewModel.onDeleteSourceConfirm()
        viewModel.onDeleteSourceDismiss()

        assertEquals(1, repository.deleteSourceCallCount)
        assertNotNull(viewModel.uiState.value.deletingSource)

        gate.complete(Unit)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.deletingSource)
        assertFalse(viewModel.uiState.value.isDeletingSource)
        assertTrue(viewModel.uiState.value.sourceDeleted)
        assertEquals(SourceDetailUserMessage.SOURCE_DELETED, viewModel.uiState.value.userMessage)
    }
}
