package com.nus.folio.presentation.home.bottomsheet

import android.content.Intent
import android.net.Uri
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nus.folio.R
import com.nus.folio.components.AnimatedModalSheet
import com.nus.folio.components.rememberSheetDiscardProtectionState
import com.nus.folio.domain.util.AddSourceInputRules
import com.nus.folio.presentation.home.HomeSheetShape
import com.nus.folio.presentation.home.HomeSheetTabShape
import com.nus.folio.ui.theme.CormorantGaramond
import com.nus.folio.ui.theme.FolioAndroidTheme
import com.nus.folio.ui.theme.HomeCardBackground
import com.nus.folio.ui.theme.HomeChipBorder
import com.nus.folio.ui.theme.HomeHeader
import com.nus.folio.ui.theme.HomeSheetBackground
import com.nus.folio.ui.theme.HomeStatusFailedText
import com.nus.folio.ui.theme.HomeTextPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val AddSourceTabEnterMillis = 280
private val AddSourceTabExitMillis = 200

/** MIME types for the file picker — aligned with [com.nus.folio.data.util.SourceMimeTypes]. */
private val AddSourceFileMimeTypes = arrayOf(
    "application/pdf",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "application/epub+zip",
    "text/markdown",
    "text/x-markdown",
    "text/plain",
    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "text/csv",
    "text/comma-separated-values",
)

private val AddSourceTabOrder = listOf(
    AddSourceTab.FILE,
    AddSourceTab.WEB,
    AddSourceTab.TEXT,
)

@Composable
internal fun AddSourceBottomSheet(
    onDismiss: () -> Unit,
    onSubmit: (AddSourceDraft) -> Unit = {},
    onFileSelected: () -> Unit = {},
    onFileSelectionFailed: (AddSourceInputRules.FileValidationError) -> Unit = {},
    isSubmitting: Boolean = false,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val discardProtection = rememberSheetDiscardProtectionState()
    var selectedFileUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedFile by remember { mutableStateOf<SelectedSourceFile?>(null) }
    var isResolvingFile by remember { mutableStateOf(false) }
    var formHasUnsavedContent by remember { mutableStateOf(false) }

    fun applySelectedUri(uri: Uri) {
        selectedFileUriString = uri.toString()
        scope.launch {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
        }
    }

    LaunchedEffect(selectedFileUriString) {
        val uriString = selectedFileUriString
        if (uriString == null) {
            selectedFile = null
            isResolvingFile = false
            return@LaunchedEffect
        }
        isResolvingFile = true
        val resolved = withContext(Dispatchers.IO) {
            resolveSelectedSourceFile(context, Uri.parse(uriString))
        }
        if (selectedFileUriString == uriString) {
            selectedFile = resolved
            isResolvingFile = false
            val error = resolved.validationError
            if (error == null) {
                onFileSelected()
            } else {
                onFileSelectionFailed(error)
            }
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        applySelectedUri(uri)
    }

    DisposableEffect(Unit) {
        val window = context.findActivityOrNull()?.window
            ?: return@DisposableEffect onDispose {}
        val previousSoftInputMode = window.attributes.softInputMode
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        onDispose {
            window.setSoftInputMode(previousSoftInputMode)
        }
    }

    SideEffect {
        discardProtection.hasUnsavedContent =
            formHasUnsavedContent || selectedFileUriString != null
    }

    AnimatedModalSheet(
        onDismiss = {
            if (!isSubmitting) onDismiss()
        },
        dismissOnScrimClick = !isSubmitting,
        confirmDismiss = {
            discardProtection.confirmDismiss(blockWhileBusy = isSubmitting)
        },
        contentWindowInsets = WindowInsets.navigationBars.union(WindowInsets.ime),
    ) { requestDismiss ->
        discardProtection.dismissHolder.requestDismiss = requestDismiss
        AddSourceDragHandle()
        AddSourceSheetContent(
            selectedFile = selectedFile,
            isResolvingFile = isResolvingFile,
            isSubmitting = isSubmitting,
            onDirtyChange = { formHasUnsavedContent = it },
            onCancelClick = {
                if (!isSubmitting) requestDismiss()
            },
            onUploadFileClick = {
                if (!isSubmitting) filePicker.launch(AddSourceFileMimeTypes)
            },
            onFileDropped = { uri ->
                if (!isSubmitting) applySelectedUri(uri)
            },
            onSubmit = onSubmit,
        )
    }

    SheetDiscardConfirmBottomSheet(
        visible = discardProtection.showDiscardConfirm,
        onKeepEditing = { discardProtection.showDiscardConfirm = false },
        onDiscard = { discardProtection.discardAndDismiss() },
    )
}

@Composable
internal fun AddSourceSheetContent(
    onUploadFileClick: () -> Unit,
    onSubmit: (AddSourceDraft) -> Unit,
    onCancelClick: () -> Unit = {},
    onFileDropped: (Uri) -> Unit = {},
    onDirtyChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
    initialTab: AddSourceTab = AddSourceTab.FILE,
    selectedFile: SelectedSourceFile? = null,
    isResolvingFile: Boolean = false,
    isSubmitting: Boolean = false,
) {
    var selectedTab by rememberSaveable { mutableStateOf(initialTab) }
    var webUrl by rememberSaveable { mutableStateOf("") }
    var webTitle by rememberSaveable { mutableStateOf("") }
    var webAuthor by rememberSaveable { mutableStateOf("") }
    var textTitle by rememberSaveable { mutableStateOf("") }
    var textAuthor by rememberSaveable { mutableStateOf("") }
    var textContent by rememberSaveable { mutableStateOf("") }
    var webUrlTouched by rememberSaveable { mutableStateOf(false) }
    var textContentTouched by rememberSaveable { mutableStateOf(false) }

    SideEffect {
        onDirtyChange(
            webUrl.isNotEmpty() ||
                webTitle.isNotEmpty() ||
                webAuthor.isNotEmpty() ||
                textTitle.isNotEmpty() ||
                textAuthor.isNotEmpty() ||
                textContent.isNotEmpty(),
        )
    }

    val webUrlValid = AddSourceInputRules.isValidHttpUrl(webUrl)
    val showWebError = selectedTab == AddSourceTab.WEB && webUrlTouched && !webUrlValid
    val contentError = when {
        !textContentTouched -> null
        textContent.length > AddSourceInputRules.MAX_CONTENT_LENGTH ->
            AddSourceInputRules.ContentValidationError.TOO_LONG
        textContent.length < AddSourceInputRules.MIN_CONTENT_LENGTH ->
            AddSourceInputRules.ContentValidationError.TOO_SHORT
        else -> null
    }
    val showContentError = contentError != null
    val fileValid = selectedFile != null && selectedFile.validationError == null
    val contentValid = AddSourceInputRules.isContentValid(textContent)

    val canSubmit = when (selectedTab) {
        AddSourceTab.FILE -> fileValid && !isResolvingFile
        AddSourceTab.WEB -> webUrlValid
        AddSourceTab.TEXT -> contentValid
    } && !isSubmitting

    Column(modifier = modifier) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.add_source_title),
            fontFamily = CormorantGaramond,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            color = HomeTextPrimary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.add_source_description),
            fontSize = 14.sp,
            color = HomeTextPrimary,
            lineHeight = 20.sp,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AddSourceTabChip(
                label = stringResource(R.string.add_source_tab_file),
                selected = selectedTab == AddSourceTab.FILE,
                onClick = { selectedTab = AddSourceTab.FILE },
                modifier = Modifier.weight(1f),
            )
            AddSourceTabChip(
                label = stringResource(R.string.add_source_tab_web),
                selected = selectedTab == AddSourceTab.WEB,
                onClick = { selectedTab = AddSourceTab.WEB },
                modifier = Modifier.weight(1f),
            )
            AddSourceTabChip(
                label = stringResource(R.string.add_source_tab_text),
                selected = selectedTab == AddSourceTab.TEXT,
                onClick = { selectedTab = AddSourceTab.TEXT },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                val fromIndex = AddSourceTabOrder.indexOf(initialState)
                val toIndex = AddSourceTabOrder.indexOf(targetState)
                val forward = toIndex >= fromIndex
                val slideOffset = { width: Int -> width / 5 }
                if (forward) {
                    (
                        slideInHorizontally(
                            animationSpec = tween(AddSourceTabEnterMillis),
                            initialOffsetX = slideOffset,
                        ) + fadeIn(animationSpec = tween(AddSourceTabEnterMillis))
                        ) togetherWith (
                        slideOutHorizontally(
                            animationSpec = tween(AddSourceTabExitMillis),
                            targetOffsetX = { -slideOffset(it) },
                        ) + fadeOut(animationSpec = tween(AddSourceTabExitMillis))
                        )
                } else {
                    (
                        slideInHorizontally(
                            animationSpec = tween(AddSourceTabEnterMillis),
                            initialOffsetX = { -slideOffset(it) },
                        ) + fadeIn(animationSpec = tween(AddSourceTabEnterMillis))
                        ) togetherWith (
                        slideOutHorizontally(
                            animationSpec = tween(AddSourceTabExitMillis),
                            targetOffsetX = slideOffset,
                        ) + fadeOut(animationSpec = tween(AddSourceTabExitMillis))
                        )
                }.using(
                    SizeTransform(
                        clip = false,
                        sizeAnimationSpec = { _, _ ->
                            tween(AddSourceTabEnterMillis)
                        },
                    ),
                )
            },
            label = "addSourceTabContent",
            modifier = Modifier.fillMaxWidth(),
        ) { tab ->
            when (tab) {
                AddSourceTab.FILE -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(AddSourceContentHeight),
                        ) {
                            FileUploadZone(
                                selectedFile = selectedFile,
                                onClick = onUploadFileClick,
                                onFileDropped = onFileDropped,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        selectedFile?.validationError?.let { error ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = when (error) {
                                    AddSourceInputRules.FileValidationError.UNSUPPORTED_FORMAT ->
                                        stringResource(R.string.add_source_file_unsupported_format)
                                    AddSourceInputRules.FileValidationError.SIZE_EXCEEDED ->
                                        stringResource(R.string.add_source_file_size_exceeded)
                                },
                                fontSize = 12.sp,
                                color = HomeStatusFailedText,
                            )
                        }
                    }
                }
                AddSourceTab.WEB -> {
                    AddSourceWebFields(
                        url = webUrl,
                        title = webTitle,
                        author = webAuthor,
                        showUrlError = showWebError,
                        onUrlChange = {
                            webUrl = it
                            webUrlTouched = true
                        },
                        onTitleChange = { webTitle = AddSourceInputRules.limitTitle(it) },
                        onAuthorChange = { webAuthor = AddSourceInputRules.limitAuthor(it) },
                    )
                }
                AddSourceTab.TEXT -> {
                    AddSourceTextFields(
                        title = textTitle,
                        author = textAuthor,
                        content = textContent,
                        showContentError = showContentError,
                        contentError = contentError,
                        onTitleChange = { textTitle = AddSourceInputRules.limitTitle(it) },
                        onAuthorChange = { textAuthor = AddSourceInputRules.limitAuthor(it) },
                        onContentChange = {
                            textContent = it
                            textContentTouched = true
                        },
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AddSourceCancelButton(
                onClick = onCancelClick,
                modifier = Modifier.weight(1f),
            )
            AddSourceSubmitButton(
                enabled = canSubmit,
                isLoading = isSubmitting,
                onClick = {
                    val draft = when (selectedTab) {
                        AddSourceTab.FILE -> AddSourceDraft.File(
                            displayName = selectedFile?.displayName.orEmpty(),
                            uri = selectedFile?.uri,
                            author = selectedFile?.author.orEmpty(),
                        )
                        AddSourceTab.WEB -> AddSourceDraft.Web(
                            url = webUrl.trim(),
                            title = webTitle.trim(),
                            author = webAuthor.trim(),
                        )
                        AddSourceTab.TEXT -> AddSourceDraft.Text(
                            title = textTitle.trim(),
                            author = textAuthor.trim(),
                            content = textContent,
                        )
                    }
                    onSubmit(draft)
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AddSourceTabChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background by animateColorAsState(
        targetValue = if (selected) HomeHeader else HomeCardBackground,
        animationSpec = tween(durationMillis = 220),
        label = "addSourceTabBackground",
    )
    val border by animateColorAsState(
        targetValue = if (selected) Color.Transparent else HomeChipBorder,
        animationSpec = tween(durationMillis = 220),
        label = "addSourceTabBorder",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) Color.White else HomeTextPrimary,
        animationSpec = tween(durationMillis = 220),
        label = "addSourceTabContent",
    )
    Box(
        modifier = modifier
            .clip(HomeSheetTabShape)
            .background(background)
            .border(1.dp, border, HomeSheetTabShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = contentColor,
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852, backgroundColor = 0xFFF7F1E6, name = "Add source — File")
@Composable
private fun AddSourceSheetContentFilePreview() {
    AddSourceSheetContentPreviewScaffold(initialTab = AddSourceTab.FILE)
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852, backgroundColor = 0xFFF7F1E6, name = "Add source — Web")
@Composable
private fun AddSourceSheetContentWebPreview() {
    AddSourceSheetContentPreviewScaffold(initialTab = AddSourceTab.WEB)
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852, backgroundColor = 0xFFF7F1E6, name = "Add source — Text")
@Composable
private fun AddSourceSheetContentTextPreview() {
    AddSourceSheetContentPreviewScaffold(initialTab = AddSourceTab.TEXT)
}

@Composable
private fun AddSourceSheetContentPreviewScaffold(initialTab: AddSourceTab) {
    FolioAndroidTheme(dynamicColor = false) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(HomeSheetBackground, HomeSheetShape)
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 20.dp),
            ) {
                AddSourceSheetContent(
                    onUploadFileClick = {},
                    onSubmit = {},
                    initialTab = initialTab,
                )
            }
        }
    }
}
