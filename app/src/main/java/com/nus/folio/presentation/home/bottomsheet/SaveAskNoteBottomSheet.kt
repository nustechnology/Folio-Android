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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nus.folio.R
import com.nus.folio.components.AnimatedModalSheet
import com.nus.folio.components.rememberSheetDiscardProtectionState
import com.nus.folio.domain.model.AskCitation
import com.nus.folio.domain.model.SourceType
import com.nus.folio.domain.util.NoteInputRules
import com.nus.folio.presentation.home.CitedAnswerContent
import com.nus.folio.presentation.home.HomeSheetShape
import com.nus.folio.presentation.home.HomeUploadZoneShape
import com.nus.folio.presentation.home.SaveAskNoteDraft
import com.nus.folio.presentation.home.notebook.NotebookMarkdownVisuals
import com.nus.folio.ui.theme.CormorantGaramond
import com.nus.folio.ui.theme.FolioAndroidTheme
import com.nus.folio.ui.theme.HomeReadOnlyFieldBackground
import com.nus.folio.ui.theme.HomeReadOnlyFieldBorder
import com.nus.folio.ui.theme.HomeSheetBackground
import com.nus.folio.ui.theme.HomeStatusFailedText
import com.nus.folio.ui.theme.HomeTextPrimary
import com.nus.folio.ui.theme.HomeTextSecondary
import com.nus.folio.ui.theme.LoginCopper
import java.text.NumberFormat
import java.util.Locale

private val SaveAskNoteContentMaxHeight = 400.dp

@Composable
internal fun SaveAskNoteBottomSheet(
    draft: SaveAskNoteDraft,
    onDismiss: () -> Unit,
    onSubmit: (title: String, content: String) -> Unit,
    onCitationClick: (AskCitation) -> Unit,
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
        dismissOnScrimClick = true,
        confirmDismiss = { discardProtection.confirmDismiss() },
        contentWindowInsets = WindowInsets.navigationBars.union(WindowInsets.ime),
    ) { requestDismiss ->
        discardProtection.dismissHolder.requestDismiss = requestDismiss
        AddSourceDragHandle()
        SaveAskNoteSheetContent(
            draft = draft,
            onDirtyChange = { discardProtection.hasUnsavedContent = it },
            onCancelClick = { requestDismiss() },
            onCitationClick = onCitationClick,
            onSubmit = { title, content ->
                discardProtection.bypassDiscardConfirm = true
                requestDismiss { onSubmit(title, content) }
            },
        )
    }

    SheetDiscardConfirmBottomSheet(
        visible = discardProtection.showDiscardConfirm,
        onKeepEditing = { discardProtection.showDiscardConfirm = false },
        onDiscard = { discardProtection.discardAndDismiss() },
        titleRes = R.string.add_note_discard_title,
        messageRes = R.string.add_note_discard_message,
    )
}

@Composable
private fun SaveAskNoteSheetContent(
    draft: SaveAskNoteDraft,
    onCancelClick: () -> Unit,
    onSubmit: (title: String, content: String) -> Unit,
    onCitationClick: (AskCitation) -> Unit,
    onDirtyChange: (Boolean) -> Unit = {},
) {
    var title by rememberSaveable(draft.messageId) { mutableStateOf(draft.initialTitle) }
    val content = draft.content
    var submitAttempted by rememberSaveable(draft.messageId) { mutableStateOf(false) }
    val numberFormat = remember { NumberFormat.getIntegerInstance(Locale.getDefault()) }

    SideEffect {
        onDirtyChange(title != draft.initialTitle)
    }

    val titleError = when (NoteInputRules.titleValidationError(title)) {
        NoteInputRules.TitleValidationError.TOO_LONG ->
            stringResource(R.string.add_note_title_too_long)
        null -> null
    }
    val contentValidation = NoteInputRules.contentValidationError(content)
    val contentError = when (contentValidation) {
        NoteInputRules.ContentValidationError.TOO_LONG ->
            stringResource(R.string.add_note_content_too_long)
        NoteInputRules.ContentValidationError.EMPTY ->
            if (submitAttempted) stringResource(R.string.add_note_content_empty) else null
        null -> null
    }
    // Keep submit clickable when content is empty so submitAttempted can surface the error;
    // still block over-long title/content (those errors are already visible).
    val submitEnabled = NoteInputRules.titleValidationError(title) == null &&
        contentValidation != NoteInputRules.ContentValidationError.TOO_LONG

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
        Spacer(modifier = Modifier.height(12.dp))
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
        SaveAskNoteAnswerBox(
            content = content,
            citations = draft.citations,
            isError = contentError != null,
            onCitationClick = onCitationClick,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = SaveAskNoteContentMaxHeight),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(
                R.string.add_source_text_content_counter,
                numberFormat.format(content.length),
                numberFormat.format(NoteInputRules.MAX_CONTENT_LENGTH),
            ),
            fontSize = 12.sp,
            color = if (content.length > NoteInputRules.MAX_CONTENT_LENGTH) {
                HomeStatusFailedText
            } else {
                HomeTextSecondary
            },
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End,
        )
        if (contentError != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = contentError,
                color = HomeStatusFailedText,
                fontSize = 12.sp,
            )
        }
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
                enabled = submitEnabled,
                onClick = {
                    if (!NoteInputRules.canSave(title, content)) {
                        submitAttempted = true
                        return@AddSourceSubmitButton
                    }
                    onSubmit(title.trim(), content.trim())
                },
                labelRes = R.string.add_note_submit,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SaveAskNoteAnswerBox(
    content: String,
    citations: List<AskCitation>,
    isError: Boolean,
    onCitationClick: (AskCitation) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val markdownText = remember(content) {
        NotebookMarkdownVisuals.visualize(content).text
    }
    val borderColor = if (isError) HomeStatusFailedText else HomeReadOnlyFieldBorder
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(HomeUploadZoneShape)
            .border(1.dp, borderColor, HomeUploadZoneShape)
            .background(HomeReadOnlyFieldBackground)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        if (citations.isNotEmpty() || content.contains(Regex("""\[\d+\]"""))) {
            CitedAnswerContent(
                content = content,
                citations = citations,
                onCitationClick = onCitationClick,
                interactiveCitations = citations.isNotEmpty(),
                renderMarkdown = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
            )
        } else {
            Text(
                text = markdownText,
                color = HomeTextPrimary,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
            )
        }
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
                        content = "The imitation game reframes intelligence as observable linguistic behavior [1].\n\n" +
                            "**Limitation:** Evidence coverage is limited for this space.\n\n" +
                            "**Evidence**\n\n" +
                            "- [1] Computing Machinery — Page 14",
                        citations = listOf(
                            AskCitation(
                                index = 1,
                                sourceId = "1",
                                sourceTitle = "Computing Machinery",
                                sourceType = SourceType.FILE,
                                locationLabel = "Page 14",
                                evidenceText = "The new form of the problem can be described in terms of a game which we call the \"imitation game.\"",
                            ),
                        ),
                    ),
                    onCancelClick = {},
                    onCitationClick = {},
                    onSubmit = { _, _ -> },
                )
            }
        }
    }
}
