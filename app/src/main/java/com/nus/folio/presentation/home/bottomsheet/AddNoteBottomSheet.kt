package com.nus.folio.presentation.home.bottomsheet

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nus.folio.R
import com.nus.folio.components.AnimatedModalSheet
import com.nus.folio.components.ModalSheetDismiss
import com.nus.folio.domain.util.NoteInputRules
import com.nus.folio.presentation.home.HomeSheetInputBorder
import com.nus.folio.presentation.home.HomeSheetShape
import com.nus.folio.presentation.home.HomeUploadZoneShape
import com.nus.folio.ui.theme.CormorantGaramond
import com.nus.folio.ui.theme.FolioAndroidTheme
import com.nus.folio.ui.theme.HomeCardBackground
import com.nus.folio.ui.theme.HomeSheetBackground
import com.nus.folio.ui.theme.HomeStatusFailedText
import com.nus.folio.ui.theme.HomeTextPrimary
import com.nus.folio.ui.theme.HomeTextSecondary
import com.nus.folio.ui.theme.LoginCopper

private val AddNoteContentHeight = 160.dp

@Composable
internal fun AddNoteBottomSheet(
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    var hasUnsavedContent by remember { mutableStateOf(false) }
    var bypassDiscardConfirm by remember { mutableStateOf(false) }
    var showDiscardConfirm by remember { mutableStateOf(false) }
    val dismissHolder = remember { DiscardDismissHolder() }

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
        onDismiss = onDismiss,
        dismissOnScrimClick = true,
        confirmDismiss = {
            if (bypassDiscardConfirm || !hasUnsavedContent) {
                true
            } else {
                showDiscardConfirm = true
                false
            }
        },
        contentWindowInsets = WindowInsets.navigationBars.union(WindowInsets.ime),
    ) { requestDismiss ->
        dismissHolder.requestDismiss = requestDismiss
        AddSourceDragHandle()
        AddNoteSheetContent(
            onDirtyChange = { hasUnsavedContent = it },
            onCancelClick = { requestDismiss() },
            onSubmit = { title, content ->
                bypassDiscardConfirm = true
                requestDismiss { onSubmit(title, content) }
            },
        )
    }

    if (showDiscardConfirm) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirm = false },
            title = { Text(text = stringResource(R.string.add_note_discard_title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardConfirm = false
                        bypassDiscardConfirm = true
                        dismissHolder.requestDismiss?.invoke()
                    },
                ) {
                    Text(text = stringResource(R.string.add_note_discard_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirm = false }) {
                    Text(text = stringResource(R.string.add_note_discard_no))
                }
            },
        )
    }
}

private class DiscardDismissHolder {
    var requestDismiss: ModalSheetDismiss? = null
}

@Composable
private fun AddNoteSheetContent(
    onCancelClick: () -> Unit,
    onSubmit: (String, String) -> Unit,
    onDirtyChange: (Boolean) -> Unit = {},
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
        Spacer(modifier = Modifier.height(24.dp))
        AddNoteLabeledField(
            label = stringResource(R.string.add_note_title_hint),
            value = title,
            onValueChange = { title = it },
            placeholder = stringResource(R.string.add_note_title_placeholder),
            singleLine = true,
            errorMessage = titleError,
        )
        Spacer(modifier = Modifier.height(16.dp))
        AddNoteLabeledField(
            label = stringResource(R.string.add_note_content_hint),
            value = content,
            onValueChange = { content = it },
            placeholder = stringResource(R.string.add_note_content_placeholder),
            singleLine = false,
            errorMessage = contentError,
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
                enabled = true,
                onClick = {
                    submitAttempted = true
                    if (NoteInputRules.canSave(title, content)) {
                        onSubmit(title.trim(), content.trim())
                    }
                },
                labelRes = R.string.add_note_submit,
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
    fieldModifier: Modifier = Modifier.fillMaxWidth(),
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = LoginCopper,
        )
        Spacer(modifier = Modifier.height(8.dp))
        AddNoteField(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            singleLine = singleLine,
            isError = errorMessage != null,
            modifier = fieldModifier,
        )
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = errorMessage,
                color = HomeStatusFailedText,
                fontSize = 12.sp,
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
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val borderColor = if (isError) HomeStatusFailedText else HomeSheetInputBorder
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(HomeUploadZoneShape)
            .border(1.dp, borderColor, HomeUploadZoneShape)
            .background(HomeCardBackground)
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
    }
}

internal val NoteSheetContentHeight = AddNoteContentHeight

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
