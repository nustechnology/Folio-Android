package com.nus.folio.presentation.home.bottomsheet

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.draganddrop.dragAndDropTarget
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nus.folio.R
import com.nus.folio.components.AnimatedModalSheet
import com.nus.folio.domain.util.AddSourceInputRules
import com.nus.folio.presentation.home.HomeSheetInputBorder
import com.nus.folio.presentation.home.HomeSheetShape
import com.nus.folio.presentation.home.HomeSheetTabShape
import com.nus.folio.presentation.home.HomeUploadZoneShape
import com.nus.folio.ui.theme.CormorantGaramond
import com.nus.folio.ui.theme.FolioAndroidTheme
import com.nus.folio.ui.theme.HomeCardBackground
import com.nus.folio.ui.theme.HomeChipBorder
import com.nus.folio.ui.theme.HomeHeader
import com.nus.folio.ui.theme.HomeSheetBackground
import com.nus.folio.ui.theme.HomeStatusFailedText
import com.nus.folio.ui.theme.HomeTextPrimary
import com.nus.folio.ui.theme.HomeTextSecondary
import com.nus.folio.ui.theme.HomeUploadIcon
import com.nus.folio.ui.theme.LoginCopper
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val AddSourceContentHeight = 160.dp
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

enum class AddSourceTab {
    FILE,
    WEB,
    TEXT,
}

sealed interface AddSourceDraft {
    data class File(
        val displayName: String,
        val uri: Uri?,
        val author: String = "",
    ) : AddSourceDraft

    data class Web(
        val url: String,
        val title: String,
        val author: String,
    ) : AddSourceDraft

    data class Text(
        val title: String,
        val author: String,
        val content: String,
    ) : AddSourceDraft
}

@Composable
internal fun AddSourceBottomSheet(
    onDismiss: () -> Unit,
    onSubmit: (AddSourceDraft) -> Unit = {},
    isSubmitting: Boolean = false,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedFileUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedFile by remember { mutableStateOf<SelectedSourceFile?>(null) }
    var isResolvingFile by remember { mutableStateOf(false) }

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

    AnimatedModalSheet(
        onDismiss = {
            if (!isSubmitting) onDismiss()
        },
        contentWindowInsets = WindowInsets.navigationBars.union(WindowInsets.ime),
    ) { requestDismiss ->
        AddSourceDragHandle()
        AddSourceSheetContent(
            selectedFile = selectedFile,
            isResolvingFile = isResolvingFile,
            isSubmitting = isSubmitting,
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
}

private fun Context.findActivityOrNull(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return current as? Activity
}

@Composable
internal fun AddSourceSheetContent(
    onUploadFileClick: () -> Unit,
    onSubmit: (AddSourceDraft) -> Unit,
    onCancelClick: () -> Unit = {},
    onFileDropped: (Uri) -> Unit = {},
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

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun FileUploadZone(
    selectedFile: SelectedSourceFile?,
    onClick: () -> Unit,
    onFileDropped: (Uri) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val dropTarget = remember(onFileDropped, context) {
        object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                val androidEvent = event.toAndroidDragEvent()
                val clipData = androidEvent.clipData ?: return false
                // Cross-app drops need a temporary grant; persistable URI permission
                // cannot acquire this access.
                context.findActivityOrNull()?.requestDragAndDropPermissions(androidEvent)
                for (index in 0 until clipData.itemCount) {
                    val uri = clipData.getItemAt(index).uri
                    if (uri != null) {
                        onFileDropped(uri)
                        return true
                    }
                }
                return false
            }
        }
    }
    val dashWidth = 1.5.dp
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(HomeUploadZoneShape)
            .dragAndDropTarget(
                shouldStartDragAndDrop = { event ->
                    event.mimeTypes().any { mime ->
                        mime.startsWith("application/") ||
                            mime.startsWith("text/") ||
                            mime == "*/*"
                    }
                },
                target = dropTarget,
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .drawBehind {
                val stroke = Stroke(
                    width = dashWidth.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(12f, 10f),
                        phase = 0f,
                    ),
                )
                drawRoundRect(
                    color = HomeSheetInputBorder,
                    style = stroke,
                    cornerRadius = CornerRadius(16.dp.toPx()),
                )
            }
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_document),
            contentDescription = null,
            tint = HomeUploadIcon,
            modifier = Modifier.size(28.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (selectedFile != null) {
            Text(
                text = stringResource(R.string.add_source_file_selected, selectedFile.displayName),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = HomeTextPrimary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val detailLine = buildFileMetaLine(selectedFile)
            if (detailLine.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = detailLine,
                    fontSize = 13.sp,
                    color = HomeTextSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            selectedFile.author?.takeIf { it.isNotBlank() }?.let { author ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.add_source_file_meta_author, author),
                    fontSize = 13.sp,
                    color = HomeTextSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.add_source_change_file),
                fontSize = 13.sp,
                color = HomeTextSecondary,
                textAlign = TextAlign.Center,
            )
        } else {
            Text(
                text = stringResource(R.string.add_source_upload_hint),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = HomeTextPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.add_source_upload_types),
                fontSize = 13.sp,
                color = HomeTextSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.add_source_upload_limit),
                fontSize = 13.sp,
                color = HomeTextSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun buildFileMetaLine(selectedFile: SelectedSourceFile): String {
    val parts = mutableListOf<String>()
    selectedFile.sizeBytes?.let { parts += formatFileSize(it) }
    selectedFile.pageCount?.let { pages ->
        parts += pluralStringResource(R.plurals.add_source_file_meta_pages, pages, pages)
    }
    selectedFile.characterCount?.let { chars ->
        val formatted = NumberFormat.getIntegerInstance(Locale.getDefault()).format(chars)
        parts += pluralStringResource(
            R.plurals.add_source_file_meta_characters,
            chars,
            formatted,
        )
    }
    return parts.joinToString(" · ")
}

@Composable
private fun AddSourceWebFields(
    url: String,
    title: String,
    author: String,
    showUrlError: Boolean,
    onUrlChange: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    onAuthorChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            AddSourceLabeledField(
                label = stringResource(R.string.add_source_web_url_label),
                value = url,
                onValueChange = onUrlChange,
                placeholder = stringResource(R.string.add_source_web_url_placeholder),
            )
            if (showUrlError) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.add_source_web_invalid_url),
                    fontSize = 12.sp,
                    color = HomeStatusFailedText,
                )
            }
        }
        AddSourceLabeledField(
            label = stringResource(R.string.add_source_web_title_label),
            value = title,
            onValueChange = onTitleChange,
            placeholder = stringResource(R.string.add_source_web_title_placeholder),
        )
        AddSourceLabeledField(
            label = stringResource(R.string.add_source_web_author_label),
            value = author,
            onValueChange = onAuthorChange,
            placeholder = stringResource(R.string.add_source_web_author_placeholder),
        )
    }
}

@Composable
private fun AddSourceTextFields(
    title: String,
    author: String,
    content: String,
    showContentError: Boolean,
    contentError: AddSourceInputRules.ContentValidationError?,
    onTitleChange: (String) -> Unit,
    onAuthorChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
) {
    val numberFormat = remember { NumberFormat.getIntegerInstance(Locale.getDefault()) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AddSourceLabeledField(
            label = stringResource(R.string.add_source_text_title_label),
            value = title,
            onValueChange = onTitleChange,
            placeholder = stringResource(R.string.add_source_text_title_placeholder),
        )
        AddSourceLabeledField(
            label = stringResource(R.string.add_source_text_author_label),
            value = author,
            onValueChange = onAuthorChange,
            placeholder = stringResource(R.string.add_source_text_author_placeholder),
        )
        Column(modifier = Modifier.fillMaxWidth()) {
            AddSourceLabeledField(
                label = stringResource(R.string.add_source_text_content_label),
                value = content,
                onValueChange = onContentChange,
                placeholder = stringResource(R.string.add_source_text_content_placeholder),
                singleLine = false,
                fieldModifier = Modifier
                    .fillMaxWidth()
                    .height(AddSourceContentHeight),
                showResizeHint = true,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(
                    R.string.add_source_text_content_counter,
                    numberFormat.format(content.length),
                    numberFormat.format(AddSourceInputRules.MAX_CONTENT_LENGTH),
                ),
                fontSize = 12.sp,
                color = if (content.length > AddSourceInputRules.MAX_CONTENT_LENGTH) {
                    HomeStatusFailedText
                } else {
                    HomeTextSecondary
                },
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End,
            )
            if (showContentError && contentError != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when (contentError) {
                        AddSourceInputRules.ContentValidationError.TOO_SHORT ->
                            stringResource(R.string.add_source_text_content_too_short)
                        AddSourceInputRules.ContentValidationError.TOO_LONG ->
                            stringResource(
                                R.string.add_source_text_content_too_long,
                                numberFormat.format(AddSourceInputRules.MAX_CONTENT_LENGTH),
                            )
                    },
                    fontSize = 12.sp,
                    color = HomeStatusFailedText,
                )
            }
        }
    }
}

@Composable
internal fun AddSourceLabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean = true,
    fieldModifier: Modifier = Modifier.fillMaxWidth(),
    showResizeHint: Boolean = false,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = LoginCopper,
        )
        Spacer(modifier = Modifier.height(8.dp))
        AddSourceTextField(
            value = value,
            onValueChange = onValueChange,
            hint = placeholder,
            singleLine = singleLine,
            showResizeHint = showResizeHint,
            modifier = fieldModifier,
        )
    }
}

@Composable
private fun AddSourceTextField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    singleLine: Boolean,
    modifier: Modifier = Modifier,
    showResizeHint: Boolean = false,
) {
    val scrollState = rememberScrollState()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(HomeUploadZoneShape)
            .border(1.dp, HomeSheetInputBorder, HomeUploadZoneShape)
            .background(HomeCardBackground)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        if (value.isEmpty()) {
            Text(
                text = hint,
                color = HomeTextSecondary,
                fontSize = 15.sp,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = TextStyle(color = HomeTextPrimary, fontSize = 15.sp),
            cursorBrush = SolidColor(HomeTextPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (singleLine) {
                        Modifier
                    } else {
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                    },
                ),
        )
        if (showResizeHint) {
            AddSourceResizeHint(modifier = Modifier.align(Alignment.BottomEnd))
        }
    }
}

@Composable
private fun AddSourceResizeHint(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(2.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .height(1.dp)
                    .background(HomeChipBorder),
            )
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .height(1.dp)
                    .background(HomeChipBorder),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .height(1.dp)
                    .background(HomeChipBorder),
            )
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .height(1.dp)
                    .background(HomeChipBorder),
            )
        }
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
