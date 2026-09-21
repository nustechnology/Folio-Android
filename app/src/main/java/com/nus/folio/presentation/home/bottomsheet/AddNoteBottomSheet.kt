package com.nus.folio.presentation.home.bottomsheet

import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.nus.folio.R
import com.nus.folio.components.AnimatedModalSheet
import com.nus.folio.components.rememberSheetDiscardProtectionState
import com.nus.folio.components.rememberTextFieldCursorScroller
import com.nus.folio.domain.util.NoteInputRules
import com.nus.folio.presentation.home.HomeSheetInputBorder
import com.nus.folio.presentation.home.HomeSheetShape
import com.nus.folio.presentation.home.HomeUploadZoneShape
import com.nus.folio.presentation.home.notebook.NotebookMarkdownActions
import com.nus.folio.presentation.home.notebook.NotebookMarkdownVisualTransformation
import com.nus.folio.ui.theme.CormorantGaramond
import com.nus.folio.ui.theme.FolioAndroidTheme
import com.nus.folio.ui.theme.HomeCardBackground
import com.nus.folio.ui.theme.HomeReadOnlyFieldBackground
import com.nus.folio.ui.theme.HomeReadOnlyFieldBorder
import com.nus.folio.ui.theme.HomeSheetBackground
import com.nus.folio.ui.theme.HomeStatusFailedText
import com.nus.folio.ui.theme.HomeTextPrimary
import com.nus.folio.ui.theme.HomeTextSecondary
import com.nus.folio.ui.theme.LoginCopper
import java.text.NumberFormat
import java.util.Locale

private val AddNoteContentHeight = 160.dp

@Composable
internal fun AddNoteBottomSheet(
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit = { _, _ -> },
    isSubmitting: Boolean = false,
) {
    val context = LocalContext.current
    val discardProtection = rememberSheetDiscardProtectionState()

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
        dismissOnScrimClick = !isSubmitting,
        confirmDismiss = {
            discardProtection.confirmDismiss(blockWhileBusy = isSubmitting)
        },
        contentWindowInsets = WindowInsets.navigationBars.union(WindowInsets.ime),
    ) { requestDismiss ->
        discardProtection.dismissHolder.requestDismiss = requestDismiss
        AddSourceDragHandle()
        AddNoteSheetContent(
            isSubmitting = isSubmitting,
            onDirtyChange = { discardProtection.hasUnsavedContent = it },
            onCancelClick = {
                if (!isSubmitting) requestDismiss()
            },
            onSubmit = { title, content ->
                onSubmit(title, content)
            },
        )
    }

    SheetDiscardConfirmBottomSheet(
        visible = discardProtection.showDiscardConfirm,
        onKeepEditing = { discardProtection.showDiscardConfirm = false },
        onDiscard = { discardProtection.discardAndDismiss() },
        titleRes = R.string.add_note_discard_title,
        messageRes = R.string.add_note_discard_message,
        cancelLabelRes = R.string.add_note_discard_keep_editing,
        confirmLabelRes = R.string.add_note_discard_confirm,
    )
}

@Composable
private fun AddNoteSheetContent(
    onCancelClick: () -> Unit,
    onSubmit: (String, String) -> Unit,
    onDirtyChange: (Boolean) -> Unit = {},
    isSubmitting: Boolean = false,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var content by rememberSaveable { mutableStateOf("") }
    var submitAttempted by rememberSaveable { mutableStateOf(false) }

    val isDirty = title.isNotEmpty() || content.isNotEmpty()
    SideEffect {
        onDirtyChange(isDirty)
    }

    val titleError = when (NoteInputRules.titleValidationError(title)) {
        NoteInputRules.TitleValidationError.TOO_LONG ->
            stringResource(R.string.add_note_title_too_long)
        null -> null
    }
    val contentError = when (NoteInputRules.contentValidationError(content)) {
        NoteInputRules.ContentValidationError.TOO_LONG ->
            stringResource(R.string.add_note_content_too_long)
        NoteInputRules.ContentValidationError.EMPTY ->
            if (submitAttempted) stringResource(R.string.add_note_content_empty) else null
        null -> null
    }

    Column {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.add_note_title),
            fontFamily = CormorantGaramond,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            color = HomeTextPrimary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.add_note_description),
            fontSize = 14.sp,
            color = HomeTextPrimary,
            lineHeight = 20.sp,
        )
        Spacer(modifier = Modifier.height(12.dp))
        AddNoteLabeledField(
            label = stringResource(R.string.add_note_title_hint),
            value = title,
            onValueChange = {
                if (!isSubmitting) title = NoteInputRules.limitTitle(it)
            },
            placeholder = stringResource(R.string.add_note_title_placeholder),
            singleLine = true,
            readOnly = isSubmitting,
            errorMessage = titleError,
            characterLimit = NoteInputRules.MAX_TITLE_LENGTH,
        )
        AddNoteLabeledField(
            label = stringResource(R.string.add_note_content_hint),
            value = content,
            onValueChange = { if (!isSubmitting) content = it },
            placeholder = stringResource(R.string.add_note_content_placeholder),
            singleLine = false,
            readOnly = isSubmitting,
            showFormatToolbar = true,
            formatToolbarEnabled = !isSubmitting,
            errorMessage = contentError,
            characterLimit = NoteInputRules.MAX_CONTENT_LENGTH,
            fieldModifier = Modifier
                .fillMaxWidth()
                .height(AddNoteContentHeight),
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AddSourceCancelButton(
                onClick = onCancelClick,
                modifier = Modifier.weight(1f),
            )
            AddSourceSubmitButton(
                enabled = !isSubmitting && NoteInputRules.canSave(title, content),
                onClick = {
                    if (isSubmitting || !NoteInputRules.canSave(title, content)) {
                        return@AddSourceSubmitButton
                    }
                    submitAttempted = true
                    onSubmit(title.trim(), content.trim())
                },
                labelRes = R.string.add_note_submit,
                isLoading = isSubmitting,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
internal fun AddNoteLabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean,
    errorMessage: String? = null,
    characterLimit: Int? = null,
    readOnly: Boolean = false,
    fillHeight: Boolean = true,
    showFormatToolbar: Boolean = false,
    formatToolbarEnabled: Boolean = true,
    fieldModifier: Modifier = Modifier.fillMaxWidth(),
) {
    val numberFormat = remember { NumberFormat.getIntegerInstance(Locale.getDefault()) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = LoginCopper,
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (showFormatToolbar && !singleLine) {
            AddNoteFormattedField(
                value = value,
                onValueChange = onValueChange,
                placeholder = placeholder,
                isError = errorMessage != null,
                readOnly = readOnly,
                fillHeight = fillHeight,
                formatToolbarEnabled = formatToolbarEnabled && !readOnly,
                modifier = fieldModifier,
            )
        } else {
            AddNoteField(
                value = value,
                onValueChange = onValueChange,
                placeholder = placeholder,
                singleLine = singleLine,
                isError = errorMessage != null,
                readOnly = readOnly,
                fillHeight = fillHeight,
                modifier = fieldModifier,
            )
        }
        if (characterLimit != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(
                    R.string.add_source_text_content_counter,
                    numberFormat.format(value.length),
                    numberFormat.format(characterLimit),
                ),
                fontSize = 12.sp,
                color = if (value.length > characterLimit) {
                    HomeStatusFailedText
                } else {
                    HomeTextSecondary
                },
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End,
            )
        }
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(if (characterLimit != null) 4.dp else 6.dp))
            Text(
                text = errorMessage,
                color = HomeStatusFailedText,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun AddNoteFormattedField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isError: Boolean = false,
    readOnly: Boolean = false,
    fillHeight: Boolean = true,
    formatToolbarEnabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val cursorScroller = rememberTextFieldCursorScroller()
    val focusRequester = remember { FocusRequester() }
    val markdownVisuals = remember { NotebookMarkdownVisualTransformation() }
    var fieldValue by remember { mutableStateOf(TextFieldValue(value)) }
    var formatSelection by remember { mutableStateOf(TextRange.Zero) }
    var freezeSelection by remember { mutableStateOf(false) }
    var reconcileGeneration by remember { mutableIntStateOf(0) }

    fun reconcileToParent() {
        if (fieldValue.text == value) return
        fieldValue = TextFieldValue(
            text = value,
            selection = TextRange(
                fieldValue.selection.start.coerceIn(0, value.length),
                fieldValue.selection.end.coerceIn(0, value.length),
            ),
        )
        formatSelection = TextRange(
            formatSelection.start.coerceIn(0, value.length),
            formatSelection.end.coerceIn(0, value.length),
        )
        freezeSelection = false
    }

    // Re-run after every edit callback, including when [value] is unchanged
    // (truncate / ignore), so local TextFieldValue cannot drift from the parent.
    LaunchedEffect(value, reconcileGeneration) {
        reconcileToParent()
    }

    val displayedValue = if (fieldValue.text == value) {
        fieldValue
    } else {
        TextFieldValue(
            text = value,
            selection = TextRange(
                fieldValue.selection.start.coerceIn(0, value.length),
                fieldValue.selection.end.coerceIn(0, value.length),
            ),
        )
    }

    fun applyTransform(transform: (TextFieldValue) -> TextFieldValue) {
        if (readOnly || !formatToolbarEnabled) return
        val source = displayedValue.copy(selection = formatSelection)
        val next = transform(source)
        fieldValue = next
        formatSelection = next.selection
        freezeSelection = false
        onValueChange(next.text)
        reconcileGeneration++
        focusRequester.requestFocus()
    }

    val activeMarks = remember(displayedValue.text, formatSelection) {
        NotebookMarkdownActions.activeMarks(displayedValue.copy(selection = formatSelection))
    }

    val borderColor = when {
        isError -> HomeStatusFailedText
        readOnly -> HomeReadOnlyFieldBorder
        else -> HomeSheetInputBorder
    }
    val backgroundColor = if (readOnly) HomeReadOnlyFieldBackground else HomeCardBackground

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(HomeUploadZoneShape)
            .border(1.dp, borderColor, HomeUploadZoneShape)
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        NoteContentFormatToolbar(
            activeMarks = activeMarks,
            enabled = formatToolbarEnabled && !readOnly,
            onBoldClick = { applyTransform(NotebookMarkdownActions::toggleBold) },
            onItalicClick = { applyTransform(NotebookMarkdownActions::toggleItalic) },
            onBulletListClick = { applyTransform(NotebookMarkdownActions::toggleBulletList) },
            onOrderedListClick = { applyTransform(NotebookMarkdownActions::toggleOrderedList) },
            onLinkClick = { applyTransform(NotebookMarkdownActions::toggleLink) },
            modifier = Modifier
                .align(Alignment.Start)
                .zIndex(1f)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (event.changes.any { it.changedToDown() }) {
                                formatSelection = displayedValue.selection
                                freezeSelection = true
                            }
                        }
                    }
                },
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (fillHeight) Modifier.weight(1f) else Modifier),
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    color = HomeTextSecondary,
                    fontSize = 15.sp,
                )
            }
            BasicTextField(
                value = displayedValue,
                onValueChange = { next ->
                    val resolved = NotebookMarkdownActions.resolveMarkdownEdit(displayedValue, next)
                    val textChanged = resolved.text != displayedValue.text
                    fieldValue = resolved
                    if (!freezeSelection || textChanged) {
                        formatSelection = resolved.selection
                        freezeSelection = false
                    }
                    onValueChange(resolved.text)
                    reconcileGeneration++
                },
                readOnly = readOnly,
                singleLine = false,
                visualTransformation = markdownVisuals,
                textStyle = TextStyle(color = HomeTextPrimary, fontSize = 15.sp),
                cursorBrush = SolidColor(HomeTextPrimary),
                onTextLayout = { layout ->
                    val mappedOffset = markdownVisuals.visualizeCached(displayedValue.text)
                        .mapping
                        .originalToTransformed(displayedValue.selection.end)
                        .coerceIn(0, layout.layoutInput.text.length)
                    cursorScroller.onTextLayout(mappedOffset)(layout)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .then(
                        if (fillHeight) {
                            Modifier
                                .fillMaxSize()
                                .then(cursorScroller.scrollModifier)
                        } else {
                            cursorScroller.scrollModifier
                        },
                    ),
            )
        }
    }
}

@Composable
internal fun AddNoteField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean,
    isError: Boolean = false,
    readOnly: Boolean = false,
    fillHeight: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val cursorScroller = rememberTextFieldCursorScroller()
    var fieldValue by remember { mutableStateOf(TextFieldValue(value)) }
    var reconcileGeneration by remember { mutableIntStateOf(0) }

    fun reconcileToParent() {
        if (fieldValue.text == value) return
        fieldValue = TextFieldValue(
            text = value,
            selection = TextRange(
                fieldValue.selection.start.coerceIn(0, value.length),
                fieldValue.selection.end.coerceIn(0, value.length),
            ),
        )
    }

    // Re-run after every edit callback, including when [value] is unchanged
    // (truncate / ignore), so local TextFieldValue cannot drift from the parent.
    LaunchedEffect(value, reconcileGeneration) {
        reconcileToParent()
    }

    val displayedValue = if (fieldValue.text == value) {
        fieldValue
    } else {
        TextFieldValue(
            text = value,
            selection = TextRange(
                fieldValue.selection.start.coerceIn(0, value.length),
                fieldValue.selection.end.coerceIn(0, value.length),
            ),
        )
    }

    val borderColor = when {
        isError -> HomeStatusFailedText
        readOnly -> HomeReadOnlyFieldBorder
        else -> HomeSheetInputBorder
    }
    val backgroundColor = if (readOnly) HomeReadOnlyFieldBackground else HomeCardBackground
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(HomeUploadZoneShape)
            .border(1.dp, borderColor, HomeUploadZoneShape)
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                color = HomeTextSecondary,
                fontSize = 15.sp,
            )
        }
        BasicTextField(
            value = displayedValue,
            onValueChange = {
                fieldValue = it
                onValueChange(it.text)
                reconcileGeneration++
            },
            readOnly = readOnly,
            singleLine = singleLine,
            textStyle = TextStyle(color = HomeTextPrimary, fontSize = 15.sp),
            cursorBrush = SolidColor(HomeTextPrimary),
            onTextLayout = if (singleLine) {
                {}
            } else {
                cursorScroller.onTextLayout(displayedValue.selection.end)
            },
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    when {
                        singleLine -> Modifier
                        fillHeight -> Modifier
                            .fillMaxSize()
                            .then(cursorScroller.scrollModifier)
                        else -> cursorScroller.scrollModifier
                    },
                ),
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852, backgroundColor = 0xFFF7F1E6)
@Composable
private fun AddNoteSheetContentPreview() {
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
                AddSourceDragHandle()
                AddNoteSheetContent(
                    onCancelClick = {},
                    onSubmit = { _, _ -> },
                )
            }
        }
    }
}
