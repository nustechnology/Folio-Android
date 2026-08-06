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
import com.nus.folio.components.rememberFolioToastHostState
import com.nus.folio.di.LocalAppContainer
import com.nus.folio.domain.model.SourceContentFormat
import com.nus.folio.domain.model.SourceDetail
import com.nus.folio.domain.model.SourceStatus
import com.nus.folio.domain.model.SourceType
import com.nus.folio.ui.theme.FolioAndroidTheme
import com.nus.folio.ui.theme.HomeBackground

@Composable
fun SourceDetailScreen(
    spaceId: String,
    sourceId: String,
    onBackClick: () -> Unit,
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
        ),
    ),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val toastHostState = rememberFolioToastHostState()
    var showOpenOriginalSheet by remember { mutableStateOf(false) }

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
            onAskSourceClick = onAskSourceClick,
            onOpenOriginalClick = { showOpenOriginalSheet = true },
            onRetryLoad = viewModel::loadDetail,
            onRetryProcessing = viewModel::onRetryProcessing,
            onSheetSelected = viewModel::onSheetSelected,
        )

        FolioToastHost(
            hostState = toastHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 12.dp),
        )

        if (showOpenOriginalSheet) {
            val detail = uiState.detail
            OpenOriginalBottomSheet(
                fileName = detail?.originalFileName
                    ?.takeIf { it.isNotBlank() }
                    ?: detail?.title
                    ?: stringResource(R.string.source_detail_title),
                onDismiss = { showOpenOriginalSheet = false },
                onConfirm = viewModel::onOpenOriginalClick,
            )
        }
    }
}

@Composable
private fun SourceDetailContent(
    uiState: SourceDetailUiState,
    highlightText: String?,
    onBackClick: () -> Unit,
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
            showOpenOriginal = uiState.detail?.status == SourceStatus.READY &&
                !uiState.previewUrl.isNullOrBlank(),
            onBackClick = onBackClick,
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
    }
    val style = when (this) {
        SourceDetailUserMessage.OPEN_ORIGINAL_NO_APP -> FolioToastStyle.Warning
        SourceDetailUserMessage.OPEN_ORIGINAL_FAILED -> FolioToastStyle.Error
    }
    return FolioToastVisuals(
        title = context.getString(messageRes),
        style = style,
    )
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
            onAskSourceClick = {},
            onOpenOriginalClick = {},
            onRetryLoad = {},
            onRetryProcessing = {},
            onSheetSelected = {},
        )
    }
}
