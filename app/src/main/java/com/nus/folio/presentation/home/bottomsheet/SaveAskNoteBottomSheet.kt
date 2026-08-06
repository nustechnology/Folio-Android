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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nus.folio.R
import com.nus.folio.components.AnimatedModalSheet
import com.nus.folio.components.ModalSheetDismiss
import com.nus.folio.domain.model.AskCitation
import com.nus.folio.domain.model.SourceType
import com.nus.folio.domain.util.NoteInputRules
import com.nus.folio.presentation.home.CitedAnswerContent
import com.nus.folio.presentation.home.HomeSheetShape
import com.nus.folio.presentation.home.HomeUploadZoneShape
import com.nus.folio.presentation.home.SaveAskNoteDraft
import com.nus.folio.ui.theme.CormorantGaramond
import com.nus.folio.ui.theme.FolioAndroidTheme
import com.nus.folio.ui.theme.HomeReadOnlyFieldBackground
import com.nus.folio.ui.theme.HomeReadOnlyFieldBorder
import com.nus.folio.ui.theme.HomeSheetBackground
import com.nus.folio.ui.theme.HomeTextPrimary
import com.nus.folio.ui.theme.LoginCopper

private val SaveAskNoteContentHeight = 200.dp

@Composable
internal fun SaveAskNoteBottomSheet(
    draft: SaveAskNoteDraft,
    onDismiss: () -> Unit,
    onSubmit: (title: String) -> Unit,
    onCitationClick: (AskCitation) -> Unit = {},
) {
    val context = LocalContext.current
    var titleModified by remember { mutableStateOf(false) }
    var bypassDiscardConfirm by remember { mutableStateOf(false) }
    var showDiscardConfirm by remember { mutableStateOf(false) }
    val dismissHolder = remember { SaveAskNoteDismissHolder() }

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
            if (bypassDiscardConfirm || !titleModified) {
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
        SaveAskNoteSheetContent(
            draft = draft,
            onTitleModifiedChange = { titleModified = it },
            onCancelClick = { requestDismiss() },
            onSubmit = { title ->
                bypassDiscardConfirm = true
                requestDismiss { onSubmit(title) }
            },
            onCitationClick = onCitationClick,
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

private class SaveAskNoteDismissHolder {
    var requestDismiss: ModalSheetDismiss? = null
}

@Composable
private fun SaveAskNoteSheetContent(
    draft: SaveAskNoteDraft,
    onCancelClick: () -> Unit,
    onSubmit: (title: String) -> Unit,
    onCitationClick: (AskCitation) -> Unit,
    onTitleModifiedChange: (Boolean) -> Unit = {},
) {
    var title by rememberSaveable(draft.messageId) { mutableStateOf(draft.initialTitle) }

    val titleChanged = title != draft.initialTitle
    SideEffect {
        onTitleModifiedChange(titleChanged)
    }

    val titleError = when (NoteInputRules.titleValidationError(title)) {
        NoteInputRules.TitleValidationError.TOO_LONG ->
            stringResource(R.string.add_note_title_too_long)
        null -> null
    }

    Column {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.save_ask_note_title),
            fontFamily = CormorantGaramond,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            color = HomeTextPrimary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.save_ask_note_description),
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
        Text(
            text = stringResource(R.string.save_ask_note_content_hint),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = LoginCopper,
        )
        Spacer(modifier = Modifier.height(8.dp))
        SaveAskNoteContentPreview(
            content = draft.content,
            citations = draft.citations,
            onCitationClick = onCitationClick,
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
                enabled = NoteInputRules.titleValidationError(title) == null,
                onClick = { onSubmit(title.trim()) },
                labelRes = R.string.add_note_submit,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SaveAskNoteContentPreview(
    content: String,
    citations: List<AskCitation>,
    onCitationClick: (AskCitation) -> Unit,
) {
    val scrollState = rememberScrollState()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(SaveAskNoteContentHeight)
            .clip(HomeUploadZoneShape)
            .border(1.dp, HomeReadOnlyFieldBorder, HomeUploadZoneShape)
            .background(HomeReadOnlyFieldBackground)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        CitedAnswerContent(
            content = content,
            citations = citations,
            onCitationClick = onCitationClick,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState),
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852, backgroundColor = 0xFFF7F1E6)
@Composable
private fun SaveAskNoteSheetContentPreview() {
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
                SaveAskNoteSheetContent(
                    draft = SaveAskNoteDraft(
                        messageId = "a1",
                        initialTitle = "What is the imitation game?",
                        content = "The imitation game reframes intelligence as observable linguistic behavior [1].",
                        citations = listOf(
                            AskCitation(
                                index = 1,
                                sourceId = "1",
                                sourceTitle = "Computing Machinery",
                                sourceType = SourceType.FILE,
                                locationLabel = "Page 14",
                            ),
                        ),
                    ),
                    onCancelClick = {},
                    onSubmit = {},
                    onCitationClick = {},
                )
            }
        }
    }
}
