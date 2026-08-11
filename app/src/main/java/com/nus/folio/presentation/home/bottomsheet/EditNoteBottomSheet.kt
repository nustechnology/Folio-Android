package com.nus.folio.presentation.home.bottomsheet

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.foundation.background
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nus.folio.R
import com.nus.folio.components.AnimatedModalSheet
import com.nus.folio.components.rememberSheetDiscardProtectionState
import com.nus.folio.domain.model.Note
import com.nus.folio.domain.model.NoteOrigin
import com.nus.folio.presentation.home.HomeSheetShape
import com.nus.folio.ui.theme.CormorantGaramond
import com.nus.folio.ui.theme.FolioAndroidTheme
import com.nus.folio.ui.theme.HomeSheetBackground
import com.nus.folio.ui.theme.HomeTextPrimary

@Composable
internal fun EditNoteBottomSheet(
    note: Note,
    onDismiss: () -> Unit,
    onSave: (title: String, content: String) -> Unit = { _, _ -> },
    onDelete: () -> Unit = {},
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
        onDismiss = onDismiss,
        confirmDismiss = { discardProtection.confirmDismiss() },
        contentWindowInsets = WindowInsets.navigationBars.union(WindowInsets.ime),
    ) { requestDismiss ->
        discardProtection.dismissHolder.requestDismiss = requestDismiss
        AddSourceDragHandle()
        EditNoteSheetContent(
            note = note,
            onDirtyChange = { discardProtection.hasUnsavedContent = it },
            onDeleteClick = {
                discardProtection.bypassDiscardConfirm = true
                requestDismiss { onDelete() }
            },
            onCloseClick = { requestDismiss() },
            onSave = { title, content ->
                discardProtection.bypassDiscardConfirm = true
                requestDismiss { onSave(title, content) }
            },
        )
    }

    SheetDiscardConfirmBottomSheet(
        visible = discardProtection.showDiscardConfirm,
        onKeepEditing = { discardProtection.showDiscardConfirm = false },
        onDiscard = { discardProtection.discardAndDismiss() },
    )
}

@Composable
private fun EditNoteSheetContent(
    note: Note,
    onDeleteClick: () -> Unit,
    onCloseClick: () -> Unit,
    onSave: (title: String, content: String) -> Unit,
    onDirtyChange: (Boolean) -> Unit = {},
) {
    var title by rememberSaveable(note.id) { mutableStateOf(note.title) }
    var content by rememberSaveable(note.id) { mutableStateOf(note.content) }
    val canSave = title.isNotBlank() && content.isNotBlank()

    SideEffect {
        onDirtyChange(title != note.title || content != note.content)
    }

    Column {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = stringResource(R.string.edit_note_title),
                fontFamily = CormorantGaramond,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                color = HomeTextPrimary,
                modifier = Modifier.weight(1f),
            )
            SheetCloseIconButton(onClick = onCloseClick)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.edit_note_description),
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
        )
        Spacer(modifier = Modifier.height(16.dp))
        AddNoteLabeledField(
            label = stringResource(R.string.add_note_content_hint),
            value = content,
            onValueChange = { content = it },
            placeholder = stringResource(R.string.add_note_content_placeholder),
            singleLine = false,
            fieldModifier = Modifier
                .fillMaxWidth()
                .height(NoteSheetContentHeight),
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AddSourceDestructiveButton(
                onClick = onDeleteClick,
                labelRes = R.string.note_options_delete,
                modifier = Modifier.weight(1f),
            )
            AddSourceSubmitButton(
                enabled = canSave,
                onClick = { onSave(title.trim(), content.trim()) },
                labelRes = R.string.add_note_submit,
                modifier = Modifier.weight(2f),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852, backgroundColor = 0xFFF7F1E6)
@Composable
private fun EditNoteSheetContentPreview() {
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
                EditNoteSheetContent(
                    note = Note(
                        id = "1",
                        title = "Research Question Draft",
                        content = "How do informal transit networks reshape access in mid-sized cities?",
                        project = "Urban Mobility",
                        updatedLabel = "Updated 1d ago",
                        isPinned = true,
                        spaceId = "1",
                        origin = NoteOrigin.USER_CREATED,
                    ),
                    onDeleteClick = {},
                    onCloseClick = {},
                    onSave = { _, _ -> },
                )
            }
        }
    }
}
