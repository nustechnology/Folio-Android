package com.nus.folio.presentation.sourcedetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nus.folio.components.FolioToastHost
import com.nus.folio.components.FolioToastStyle
import com.nus.folio.components.FolioToastVisuals
import com.nus.folio.components.rememberFolioToastHostState
import com.nus.folio.R
import com.nus.folio.di.LocalAppContainer
import com.nus.folio.domain.model.SourceContentFormat
import com.nus.folio.domain.model.SourceDetail
import com.nus.folio.domain.model.SourceSheetTab
import com.nus.folio.domain.model.SourceStatus
import com.nus.folio.domain.model.SourceType
import com.nus.folio.presentation.home.HomeBadgeShape
import com.nus.folio.presentation.home.HomeCardShape
import com.nus.folio.presentation.home.HomeStatusShape
import com.nus.folio.presentation.home.sourceTypeBadgeColors
import com.nus.folio.ui.theme.CormorantGaramond
import com.nus.folio.ui.theme.FolioAndroidTheme
import com.nus.folio.ui.theme.HomeBackground
import com.nus.folio.ui.theme.HomeCardBackground
import com.nus.folio.ui.theme.HomeCardBorder
import com.nus.folio.ui.theme.HomeHeader
import com.nus.folio.ui.theme.HomeStatusFailedBackground
import com.nus.folio.ui.theme.HomeStatusFailedText
import com.nus.folio.ui.theme.HomeStatusProcessingBackground
import com.nus.folio.ui.theme.HomeStatusProcessingText
import com.nus.folio.ui.theme.HomeStatusReadyBackground
import com.nus.folio.ui.theme.HomeStatusReadyText
import com.nus.folio.ui.theme.HomeTextPrimary
import com.nus.folio.ui.theme.HomeTextSecondary

@Composable
fun SourceDetailScreen(
    spaceId: String,
    sourceId: String,
    onBackClick: () -> Unit,
    onAskSourceClick: () -> Unit,
    modifier: Modifier = Modifier,
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

@Composable
private fun SourceDetailHeader(
    detail: SourceDetail?,
    showOpenOriginal: Boolean,
    onBackClick: () -> Unit,
    onAskSourceClick: () -> Unit,
    onOpenOriginalClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .border(1.dp, HomeCardBorder, CircleShape)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onBackClick,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_back),
                    contentDescription = stringResource(R.string.home_back),
                    tint = HomeTextPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(modifier = Modifier.size(12.dp))
            Text(
                text = detail?.title ?: stringResource(R.string.source_detail_title),
                fontFamily = CormorantGaramond,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                color = HomeHeader,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 32.sp,
            )
        }

        if (detail != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    SourceTypeBadge(
                        label = sourceDetailBadgeLabel(detail),
                        type = detail.type,
                    )
                    SourceStatusBadge(status = detail.status)
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (detail.status == SourceStatus.READY) {
                        AskSourceButton(onClick = onAskSourceClick)
                    }
                    if (showOpenOriginal) {
                        OpenOriginalIconButton(onClick = onOpenOriginalClick)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (detail.author.isBlank()) {
                    detail.addedLabel
                } else {
                    stringResource(R.string.home_source_meta, detail.author, detail.addedLabel)
                },
                fontSize = 13.sp,
                color = HomeTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun OpenOriginalIconButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .border(1.dp, HomeCardBorder, CircleShape)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_open_external),
            contentDescription = stringResource(R.string.source_detail_open_original),
            tint = HomeTextPrimary,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun AskSourceButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(
            horizontal = 12.dp,
            vertical = 8.dp,
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = HomeHeader,
            contentColor = Color.White,
        ),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_ask_sparkle),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.source_detail_ask_source),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun SourceDetailBody(
    detail: SourceDetail,
    selectedSheetIndex: Int,
    isContentLoading: Boolean,
    isRetrying: Boolean,
    onSheetSelected: (Int) -> Unit,
    onRetryProcessing: () -> Unit,
) {
    when (detail.status) {
        SourceStatus.PROCESSING -> {
            SourceDetailProcessingState()
        }
        SourceStatus.FAILED -> {
            SourceDetailFailedState(
                isRetrying = isRetrying,
                onRetry = onRetryProcessing,
            )
        }
        SourceStatus.READY -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
            ) {
                if (!isContentLoading &&
                    detail.contentFormat == SourceContentFormat.SHEET &&
                    detail.sheets.size > 1
                ) {
                    SheetTabSelector(
                        sheets = detail.sheets,
                        selectedIndex = selectedSheetIndex,
                        onSheetSelected = onSheetSelected,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (isContentLoading) {
                    SourceDetailHtmlLoading()
                } else {
                    val htmlBody = when (detail.contentFormat) {
                        SourceContentFormat.SHEET -> {
                            detail.sheets.getOrNull(selectedSheetIndex)?.htmlTable
                                ?: detail.htmlContent.orEmpty()
                        }
                        else -> detail.htmlContent.orEmpty()
                    }

                    SourceHtmlRenderer(
                        htmlBody = htmlBody,
                        contentFormat = detail.contentFormat,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(HomeCardShape)
                            .background(HomeCardBackground)
                            .border(1.dp, HomeCardBorder, HomeCardShape)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SourceDetailContentLoading() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = HomeHeader)
    }
}

@Composable
private fun SourceDetailHtmlLoading() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(HomeCardShape)
            .background(HomeCardBackground)
            .border(1.dp, HomeCardBorder, HomeCardShape),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = HomeHeader)
    }
}

@Composable
private fun SheetTabSelector(
    sheets: List<SourceSheetTab>,
    selectedIndex: Int,
    onSheetSelected: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedSheet = sheets.getOrNull(selectedIndex)

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(HomeCardShape)
                .background(HomeCardBackground)
                .border(1.dp, HomeCardBorder, HomeCardShape)
                .clickable { expanded = true }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.source_detail_sheet_label),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = HomeTextSecondary,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = selectedSheet?.name.orEmpty(),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = HomeTextPrimary,
            )
            Spacer(modifier = Modifier.size(8.dp))
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = HomeTextSecondary,
                modifier = Modifier.size(18.dp),
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            sheets.forEachIndexed { index, sheet ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = sheet.name,
                            fontWeight = if (index == selectedIndex) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    },
                    onClick = {
                        expanded = false
                        onSheetSelected(index)
                    },
                )
            }
        }
    }
}

@Composable
private fun SourceDetailProcessingState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(HomeStatusProcessingBackground)
                    .border(1.dp, HomeStatusProcessingText.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_toast_warning),
                    contentDescription = null,
                    tint = HomeStatusProcessingText,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.source_detail_processing_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = HomeTextPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.source_detail_processing),
                fontSize = 14.sp,
                color = HomeTextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
            )
        }
    }
}

@Composable
private fun SourceDetailFailedState(
    isRetrying: Boolean,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(HomeStatusFailedBackground)
                    .border(1.dp, HomeStatusFailedText.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_toast_error),
                    contentDescription = null,
                    tint = HomeStatusFailedText,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.source_detail_failed_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = HomeTextPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.source_detail_failed),
                fontSize = 14.sp,
                color = HomeTextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onRetry,
                enabled = !isRetrying,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HomeHeader,
                    contentColor = Color.White,
                    disabledContainerColor = HomeHeader.copy(alpha = 0.7f),
                    disabledContentColor = Color.White,
                ),
            ) {
                if (isRetrying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Text(
                    text = stringResource(R.string.source_detail_retry_processing),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun SourceDetailMessageState(
    message: String,
    actionLabel: String?,
    onAction: () -> Unit,
    isError: Boolean,
    isActionLoading: Boolean = false,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = message,
                fontSize = 15.sp,
                color = if (isError) HomeStatusFailedText else HomeTextSecondary,
                textAlign = TextAlign.Center,
            )
            if (actionLabel != null) {
                Spacer(modifier = Modifier.height(12.dp))
                if (isActionLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = HomeHeader,
                        strokeWidth = 2.5.dp,
                    )
                } else {
                    Text(
                        text = actionLabel,
                        modifier = Modifier.clickable(onClick = onAction),
                        color = HomeHeader,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun SourceTypeBadge(
    label: String,
    type: SourceType,
) {
    val colors = sourceTypeBadgeColors(type)
    Text(
        text = label,
        modifier = Modifier
            .clip(HomeBadgeShape)
            .background(colors.background)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = colors.content,
    )
}

@Composable
private fun SourceStatusBadge(status: SourceStatus) {
    val (background, textColor, labelRes) = when (status) {
        SourceStatus.READY -> Triple(
            HomeStatusReadyBackground,
            HomeStatusReadyText,
            R.string.home_status_ready,
        )
        SourceStatus.PROCESSING -> Triple(
            HomeStatusProcessingBackground,
            HomeStatusProcessingText,
            R.string.home_status_processing,
        )
        SourceStatus.FAILED -> Triple(
            HomeStatusFailedBackground,
            HomeStatusFailedText,
            R.string.home_status_failed,
        )
    }
    Text(
        text = stringResource(labelRes),
        modifier = Modifier
            .clip(HomeStatusShape)
            .background(background)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = textColor,
    )
}

@Composable
private fun sourceDetailBadgeLabel(detail: SourceDetail): String {
    when (detail.type) {
        SourceType.TEXT -> return sourceTypeLabel(SourceType.TEXT)
        SourceType.WEB -> return sourceTypeLabel(SourceType.WEB)
        SourceType.FILE, SourceType.BOOK -> Unit
    }
    val extension = detail.fileExtension.trim()
        .takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
    if (extension != null) return extension.uppercase()
    return sourceTypeLabel(detail.type)
}

@Composable
private fun sourceTypeLabel(type: SourceType): String = when (type) {
    SourceType.FILE -> stringResource(R.string.home_type_file)
    SourceType.BOOK -> stringResource(R.string.home_type_book)
    SourceType.WEB -> stringResource(R.string.home_type_web)
    SourceType.TEXT -> stringResource(R.string.home_type_text)
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
