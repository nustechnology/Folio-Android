package com.nus.folio.presentation.sourcedetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nus.folio.R
import com.nus.folio.components.FolioToastHost
import com.nus.folio.components.FolioToastStyle
import com.nus.folio.components.FolioToastVisuals
import com.nus.folio.components.ItemOptionAction
import com.nus.folio.components.ItemOptionStyle
import com.nus.folio.components.ItemOptionsBottomSheet
import com.nus.folio.components.rememberFolioToastHostState
import com.nus.folio.di.LocalAppContainer
import com.nus.folio.domain.model.SourceContentFormat
import com.nus.folio.domain.model.SourceDetail
import com.nus.folio.domain.model.Source
import com.nus.folio.domain.model.SourceStatus
import com.nus.folio.domain.model.SourceType
import com.nus.folio.domain.model.StructuredContent
import com.nus.folio.presentation.home.bottomsheet.DeleteConfirmationBottomSheet
import com.nus.folio.presentation.home.bottomsheet.EditSourceBottomSheet
import com.nus.folio.ui.theme.FolioAndroidTheme
import com.nus.folio.ui.theme.HomeBackground

@Composable
fun SourceDetailScreen(
    spaceId: String,
    sourceId: String,
    onBackClick: () -> Unit,
    onSourceDeleted: () -> Unit = onBackClick,
    onAskSourceClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlightText: String? = null,
    viewModel: SourceDetailViewModel = viewModel(
        factory = SourceDetailViewModel.Factory(
            spaceId = spaceId,
            sourceId = sourceId,
            getSourceDetailUseCase = LocalAppContainer.current.getSourceDetailUseCase,
            getSourcePreviewUrlUseCase = LocalAppContainer.current.getSourcePreviewUrlUseCase,
            retrySourceUseCase = LocalAppContainer.current.retrySourceUseCase,
            observeSourceProcessingUseCase = LocalAppContainer.current.observeSourceProcessingUseCase,
            updateSourceUseCase = LocalAppContainer.current.updateSourceUseCase,
            deleteSourceUseCase = LocalAppContainer.current.deleteSourceUseCase,
        ),
    ),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val toastHostState = rememberFolioToastHostState()
    var showOpenOriginalSheet by remember { mutableStateOf(false) }
    var showSourceOptionsSheet by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.openOriginalRequest) {
        val request = uiState.openOriginalRequest ?: return@LaunchedEffect
        val result = SourceOriginalFileOpener.open(context, request)
        viewModel.onOpenOriginalResult(result)
    }

    LaunchedEffect(uiState.userMessage) {
        val message = uiState.userMessage ?: return@LaunchedEffect
        toastHostState.showToast(message.toToastVisuals(context))
        viewModel.onUserMessageShown()
    }

    LaunchedEffect(uiState.sourceDeleted) {
        if (!uiState.sourceDeleted) return@LaunchedEffect
        kotlinx.coroutines.delay(350)
        viewModel.onSourceDeletedHandled()
        onSourceDeleted()
    }

    LaunchedEffect(uiState.actionError) {
        val message = uiState.actionError ?: return@LaunchedEffect
        toastHostState.showToast(
            FolioToastVisuals(
                title = message,
                style = FolioToastStyle.Error,
            ),
        )
        viewModel.onActionErrorShown()
    }

    Box(modifier = modifier.fillMaxSize()) {
        SourceDetailContent(
            uiState = uiState,
            highlightText = highlightText,
            onBackClick = onBackClick,
            onMoreClick = { showSourceOptionsSheet = true },
            onAskSourceClick = onAskSourceClick,
            onOpenOriginalClick = { showOpenOriginalSheet = true },
            onRetryLoad = viewModel::loadDetail,
            onRetryProcessing = viewModel::onRetryProcessing,
            onSheetSelected = viewModel::onSheetSelected,
        )

        if (showOpenOriginalSheet) {
            val detail = uiState.detail
            if (detail != null && detail.type != SourceType.TEXT) {
                val displayValue = when (detail.type) {
                    SourceType.WEB -> uiState.previewUrl.orEmpty()
                    SourceType.FILE, SourceType.BOOK, SourceType.TEXT ->
                        detail.originalFileName.takeIf { it.isNotBlank() } ?: detail.title
                }
                OpenOriginalBottomSheet(
                    sourceType = detail.type,
                    displayValue = displayValue.ifBlank {
                        stringResource(R.string.source_detail_title)
                    },
                    onDismiss = { showOpenOriginalSheet = false },
                    onConfirm = viewModel::onOpenOriginalClick,
                )
            }
        }

        if (showSourceOptionsSheet) {
            val source = uiState.detail?.toSource()
            if (source != null) {
                ItemOptionsBottomSheet(
                    title = source.title,
                    actions = listOf(
                        ItemOptionAction(
                            label = stringResource(R.string.source_options_edit),
                            onClick = {
                                showSourceOptionsSheet = false
                                viewModel.onEditSourceClick()
                            },
                        ),
                        ItemOptionAction(
                            label = stringResource(R.string.source_options_delete),
                            style = ItemOptionStyle.Destructive,
                            onClick = {
                                showSourceOptionsSheet = false
                                viewModel.onDeleteSourceClick()
                            },
                        ),
                    ),
                    onDismiss = { showSourceOptionsSheet = false },
                )
            } else {
                showSourceOptionsSheet = false
            }
        }

        uiState.editingSource?.let { source ->
            EditSourceBottomSheet(
                source = source,
                initialContent = uiState.editingSourceContent,
                onDismiss = viewModel::onEditSourceDismiss,
                onSave = viewModel::onEditSourceSave,
                isSubmitting = uiState.isUpdatingSource,
                closeOnSave = false,
            )
        }

        uiState.deletingSource?.let {
            DeleteConfirmationBottomSheet(
                onDismiss = viewModel::onDeleteSourceDismiss,
                onConfirm = viewModel::onDeleteSourceConfirm,
                isSubmitting = uiState.isDeletingSource,
                closeOnConfirm = false,
            )
        }

        FolioToastHost(
            hostState = toastHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 12.dp),
        )
    }
}

@Composable
private fun SourceDetailContent(
    uiState: SourceDetailUiState,
    highlightText: String?,
    onBackClick: () -> Unit,
    onMoreClick: () -> Unit,
    onAskSourceClick: () -> Unit,
    onOpenOriginalClick: () -> Unit,
    onRetryLoad: () -> Unit,
    onRetryProcessing: () -> Unit,
    onSheetSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HomeBackground)
            .statusBarsPadding(),
    ) {
        SourceDetailHeader(
            detail = uiState.detail,
            showOpenOriginal = uiState.detail?.let { detail ->
                detail.status == SourceStatus.READY &&
                    detail.type != SourceType.TEXT &&
                    !uiState.previewUrl.isNullOrBlank()
            } == true,
            onBackClick = onBackClick,
            onMoreClick = onMoreClick,
            onAskSourceClick = onAskSourceClick,
            onOpenOriginalClick = onOpenOriginalClick,
        )

        when {
            uiState.error != null && uiState.detail == null -> {
                SourceDetailMessageState(
                    message = uiState.error,
                    actionLabel = stringResource(R.string.home_retry),
                    onAction = onRetryLoad,
                    isError = true,
                )
            }
            uiState.detail != null -> {
                SourceDetailBody(
                    detail = uiState.detail,
                    selectedSheetIndex = uiState.selectedSheetIndex,
                    isContentLoading = uiState.isContentLoading,
                    isRetrying = uiState.isRetrying,
                    highlightText = highlightText,
                    onSheetSelected = onSheetSelected,
                    onRetryProcessing = onRetryProcessing,
                )
            }
            uiState.isLoading || uiState.isContentLoading -> {
                SourceDetailContentLoading()
            }
        }
    }
}

private fun SourceDetailUserMessage.toToastVisuals(context: android.content.Context): FolioToastVisuals {
    val messageRes = when (this) {
        SourceDetailUserMessage.OPEN_ORIGINAL_NO_APP -> R.string.source_detail_open_original_no_app
        SourceDetailUserMessage.OPEN_ORIGINAL_FAILED -> R.string.source_detail_open_original_failed
        SourceDetailUserMessage.SOURCE_UPDATED -> R.string.toast_source_updated
        SourceDetailUserMessage.SOURCE_DELETED -> R.string.toast_source_deleted
    }
    val style = when (this) {
        SourceDetailUserMessage.OPEN_ORIGINAL_NO_APP -> FolioToastStyle.Warning
        SourceDetailUserMessage.OPEN_ORIGINAL_FAILED -> FolioToastStyle.Error
        SourceDetailUserMessage.SOURCE_UPDATED -> FolioToastStyle.Success
        SourceDetailUserMessage.SOURCE_DELETED -> FolioToastStyle.Success
    }
    return FolioToastVisuals(
        title = context.getString(messageRes),
        style = style,
    )
}

private fun SourceDetail.toSource(): Source =
    Source(
        id = id,
        title = title,
        type = type,
        author = author,
        addedLabel = addedLabel,
        status = status,
        spaceId = spaceId,
        fileExtension = fileExtension,
    )

@Preview(showBackground = true, widthDp = 393, heightDp = 852, name = "Source detail — web")
@Composable
private fun SourceDetailWebPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        SourceDetailContent(
            highlightText = null,
            uiState = SourceDetailUiState(
                detail = SourceDetail(
                    id = "9",
                    title = "Wikipedia: Neural Networks",
                    author = "Wikipedia",
                    addedLabel = "Added 2d ago",
                    type = SourceType.WEB,
                    status = SourceStatus.READY,
                    spaceId = "1",
                    fileExtension = "md",
                    contentFormat = SourceContentFormat.DOCUMENT,
                    originalFileName = "wikipedia-neural-networks.md",
                    htmlContent = "<p>Fallback markdown HTML.</p>",
                    structuredContent = StructuredContent.Document(
                        "<h1>Artificial neural network</h1><p>Structured web HTML preview.</p>",
                    ),
                ),
                previewUrl = "https://en.wikipedia.org/wiki/Artificial_neural_network",
            ),
            onBackClick = {},
            onMoreClick = {},
            onAskSourceClick = {},
            onOpenOriginalClick = {},
            onRetryLoad = {},
            onRetryProcessing = {},
            onSheetSelected = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun SourceDetailDocumentPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        SourceDetailContent(
            highlightText = null,
            uiState = SourceDetailUiState(
                detail = SourceDetail(
                    id = "1",
                    title = "Alan Turing: Computing Machinery",
                    author = "Alan Turing",
                    addedLabel = "Added 2d ago",
                    type = SourceType.FILE,
                    status = SourceStatus.READY,
                    spaceId = "1",
                    fileExtension = "pdf",
                    contentFormat = SourceContentFormat.DOCUMENT,
                    originalFileName = "alan-turing-computing-machinery.pdf",
                    htmlContent = "<h1>Computing Machinery and Intelligence</h1><p>Preview content.</p>",
                ),
                previewUrl = "https://example.org/preview/paper.pdf",
            ),
            onBackClick = {},
            onMoreClick = {},
            onAskSourceClick = {},
            onOpenOriginalClick = {},
            onRetryLoad = {},
            onRetryProcessing = {},
            onSheetSelected = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun SourceDetailProcessingPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        SourceDetailContent(
            highlightText = null,
            uiState = SourceDetailUiState(
                detail = SourceDetail(
                    id = "3",
                    title = "Weapons of Math Destruction",
                    author = "Cathy O'Neil",
                    addedLabel = "Added 2d ago",
                    type = SourceType.BOOK,
                    status = SourceStatus.PROCESSING,
                    spaceId = "2",
                    fileExtension = "epub",
                    contentFormat = SourceContentFormat.DOCUMENT,
                    originalFileName = "weapons-of-math-destruction.epub",
                ),
                previewUrl = "https://example.org/preview/book.epub",
            ),
            onBackClick = {},
            onMoreClick = {},
            onAskSourceClick = {},
            onOpenOriginalClick = {},
            onRetryLoad = {},
            onRetryProcessing = {},
            onSheetSelected = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852, name = "Source detail — failed")
@Composable
private fun SourceDetailFailedPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        SourceDetailContent(
            highlightText = null,
            uiState = SourceDetailUiState(
                detail = SourceDetail(
                    id = "4",
                    title = "The Age of Surveillance Capitalism",
                    author = "Shoshana Zuboff",
                    addedLabel = "Added 2d ago",
                    type = SourceType.FILE,
                    status = SourceStatus.FAILED,
                    spaceId = "1",
                    fileExtension = "pdf",
                    contentFormat = SourceContentFormat.DOCUMENT,
                    originalFileName = "surveillance-capitalism.pdf",
                ),
            ),
            onBackClick = {},
            onMoreClick = {},
            onAskSourceClick = {},
            onOpenOriginalClick = {},
            onRetryLoad = {},
            onRetryProcessing = {},
            onSheetSelected = {},
        )
    }
}
