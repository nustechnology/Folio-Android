package com.nus.folio.presentation.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.nus.folio.R
import com.nus.folio.components.ItemOptionAction
import com.nus.folio.components.ItemOptionStyle
import com.nus.folio.components.ItemOptionsBottomSheet
import com.nus.folio.domain.model.AskCitation
import com.nus.folio.domain.model.NoteSort
import com.nus.folio.domain.model.Source
import com.nus.folio.domain.model.SourceSort
import com.nus.folio.presentation.home.bottomsheet.AddNoteBottomSheet
import com.nus.folio.presentation.home.bottomsheet.AddSourceBottomSheet
import com.nus.folio.presentation.home.bottomsheet.AddSourceDraft
import com.nus.folio.domain.util.AddSourceInputRules
import com.nus.folio.presentation.home.bottomsheet.AnswerScopeBottomSheet
import com.nus.folio.presentation.home.bottomsheet.CitationPreviewBottomSheet
import com.nus.folio.presentation.home.bottomsheet.ConversationBottomSheet
import com.nus.folio.presentation.home.bottomsheet.ConvertNoteBottomSheet
import com.nus.folio.presentation.home.bottomsheet.DeleteConfirmationBottomSheet
import com.nus.folio.presentation.home.bottomsheet.EditNoteBottomSheet
import com.nus.folio.presentation.home.bottomsheet.EditSourceBottomSheet
import com.nus.folio.presentation.home.bottomsheet.ExportNotebookBottomSheet
import com.nus.folio.presentation.home.bottomsheet.SaveAskNoteBottomSheet
import com.nus.folio.presentation.home.bottomsheet.SortNotesBottomSheet
import com.nus.folio.presentation.home.bottomsheet.SortSourcesBottomSheet
import com.nus.folio.presentation.home.bottomsheet.SourceProcessingBottomSheet
import com.nus.folio.presentation.home.bottomsheet.ViewNoteBottomSheet

@Composable
internal fun HomeOverlaySheets(
    uiState: HomeUiState,
    showConversationSheet: Boolean,
    showAnswerScopeSheet: Boolean,
    showSignOutConfirm: Boolean,
    onAddSourceSheetDismiss: () -> Unit,
    onAddSourceSubmit: (AddSourceDraft) -> Unit,
    onAddSourceFileSelected: () -> Unit,
    onAddSourceFileSelectionFailed: (AddSourceInputRules.FileValidationError) -> Unit,
    onSortSelected: (SourceSort) -> Unit,
    onNoteSortSelected: (NoteSort) -> Unit,
    onSortSheetDismiss: () -> Unit,
    onSourceProcessingDismiss: () -> Unit,
    onSourceProcessingOpenSource: () -> Unit,
    onSourceProcessingAsk: () -> Unit,
    onSourceProcessingRetry: () -> Unit,
    onAddNoteSheetDismiss: () -> Unit,
    onAddNoteSubmit: (String, String) -> Unit,
    onAskSaveAsNoteDismiss: () -> Unit,
    onAskSaveAsNoteConfirm: (String) -> Unit,
    onAskCitationClick: (AskCitation) -> Unit,
    onConversationSheetDismiss: () -> Unit,
    onNewConversation: () -> Unit,
    onAskScopeOptionSelected: (String?) -> Unit,
    onAnswerScopeSheetDismiss: () -> Unit,
    onCitationPreviewDismiss: () -> Unit,
    onCitationOpenInSource: () -> Unit,
    onEditSourceDismiss: () -> Unit,
    onEditSourceSave: (String, String, String) -> Unit,
    onDeleteSourceDismiss: () -> Unit,
    onDeleteSourceConfirm: () -> Unit,
    onViewNoteDismiss: () -> Unit,
    onConvertNoteClick: () -> Unit,
    onEditNoteClick: () -> Unit,
    onEditNoteDismiss: () -> Unit,
    onEditNoteSave: (String, String) -> Unit,
    onDeleteNoteClick: () -> Unit,
    onConvertNoteDismiss: () -> Unit,
    onConvertNoteCreate: (String, String) -> Unit,
    onDeleteNoteDismiss: () -> Unit,
    onDeleteNoteConfirm: () -> Unit,
    onSignOutConfirmDismiss: () -> Unit,
    onSignOutConfirm: () -> Unit,
    onEditSourceClick: (Source) -> Unit,
    onDeleteSourceClick: (Source) -> Unit,
    onSourceOptionsDismiss: () -> Unit,
    onViewNoteClick: () -> Unit,
    onNoteOptionsDismiss: () -> Unit,
    onCopyNotebookClick: () -> Unit,
    onExportNotebookClick: () -> Unit,
    onNotebookActionsDismiss: () -> Unit,
    onNotebookExportDismiss: () -> Unit,
    onNotebookExportConfirm: (NotebookExportFormat) -> Unit,
) {
    if (uiState.isOpeningSource) {
        SourceOpeningScreen()
    }

    if (uiState.showAddSourceSheet) {
        AddSourceBottomSheet(
            isSubmitting = uiState.isCreatingSource,
            onDismiss = onAddSourceSheetDismiss,
            onSubmit = onAddSourceSubmit,
            onFileSelected = onAddSourceFileSelected,
            onFileSelectionFailed = onAddSourceFileSelectionFailed,
        )
    }

    if (uiState.showSortSheet) {
        SortSourcesBottomSheet(
            selectedSort = uiState.selectedSort,
            onSortSelected = onSortSelected,
            onDismiss = onSortSheetDismiss,
        )
    }

    if (uiState.showNoteSortSheet) {
        SortNotesBottomSheet(
            selectedSort = uiState.selectedNoteSort,
            onSortSelected = onNoteSortSelected,
            onDismiss = onSortSheetDismiss,
        )
    }

    uiState.processingSourceTitle?.let { title ->
        SourceProcessingBottomSheet(
            sourceTitle = title,
            progress = uiState.processingProgress,
            state = uiState.processingState,
            onDismiss = onSourceProcessingDismiss,
            onOpenSource = onSourceProcessingOpenSource,
            onAsk = onSourceProcessingAsk,
            onRetry = onSourceProcessingRetry,
        )
    }

    if (uiState.showAddNoteSheet) {
        AddNoteBottomSheet(
            isSubmitting = uiState.isCreatingNote,
            onDismiss = onAddNoteSheetDismiss,
            onSubmit = onAddNoteSubmit,
        )
    }

    uiState.saveAskNoteDraft?.let { draft ->
        SaveAskNoteBottomSheet(
            draft = draft,
            onDismiss = onAskSaveAsNoteDismiss,
            onSubmit = onAskSaveAsNoteConfirm,
            onCitationClick = onAskCitationClick,
        )
    }

    if (showConversationSheet) {
        ConversationBottomSheet(
            onDismiss = onConversationSheetDismiss,
            onNewConversation = onNewConversation,
        )
    }

    if (showAnswerScopeSheet) {
        AnswerScopeBottomSheet(
            selectedScope = uiState.askScope,
            selectedSourceId = uiState.askSourceId,
            sources = uiState.askScopeSources(),
            readySourceCount = uiState.askReadySourceCount(),
            onScopeOptionSelected = onAskScopeOptionSelected,
            onDismiss = onAnswerScopeSheetDismiss,
        )
    }

    uiState.previewCitation?.let { citation ->
        CitationPreviewBottomSheet(
            citation = citation,
            onDismiss = onCitationPreviewDismiss,
            onOpenInSource = onCitationOpenInSource,
        )
    }

    uiState.editingSource?.let { source ->
        EditSourceBottomSheet(
            source = source,
            initialContent = uiState.editingSourceContent,
            onDismiss = onEditSourceDismiss,
            onSave = onEditSourceSave,
        )
    }

    uiState.deletingSource?.let {
        DeleteConfirmationBottomSheet(
            onDismiss = onDeleteSourceDismiss,
            onConfirm = onDeleteSourceConfirm,
        )
    }

    if (uiState.editingNote == null &&
        uiState.deletingNote == null &&
        uiState.convertingNote == null
    ) {
        uiState.viewingNote?.let { note ->
            ViewNoteBottomSheet(
                note = note,
                onDismiss = onViewNoteDismiss,
                onConvertClick = onConvertNoteClick,
                onEditClick = onEditNoteClick,
                onCitationClick = onAskCitationClick,
            )
        }
    }

    uiState.editingNote?.let { note ->
        EditNoteBottomSheet(
            note = note,
            onDismiss = onEditNoteDismiss,
            onSave = onEditNoteSave,
            onDelete = onDeleteNoteClick,
        )
    }

    uiState.convertingNote?.let { note ->
        ConvertNoteBottomSheet(
            note = note,
            onDismiss = onConvertNoteDismiss,
            onCreateSource = onConvertNoteCreate,
        )
    }

    uiState.deletingNote?.let {
        DeleteConfirmationBottomSheet(
            titleRes = R.string.note_delete_title,
            messageRes = R.string.note_delete_message,
            onDismiss = onDeleteNoteDismiss,
            onConfirm = onDeleteNoteConfirm,
        )
    }

    if (showSignOutConfirm) {
        DeleteConfirmationBottomSheet(
            titleRes = R.string.account_sign_out_title,
            messageRes = R.string.account_sign_out_message,
            confirmLabelRes = R.string.account_sign_out,
            onDismiss = onSignOutConfirmDismiss,
            onConfirm = onSignOutConfirm,
        )
    }

    uiState.optionsSource?.let { source ->
        ItemOptionsBottomSheet(
            title = source.title,
            actions = listOf(
                ItemOptionAction(
                    label = stringResource(R.string.source_options_edit),
                    onClick = { onEditSourceClick(source) },
                ),
                ItemOptionAction(
                    label = stringResource(R.string.source_options_delete),
                    style = ItemOptionStyle.Destructive,
                    onClick = { onDeleteSourceClick(source) },
                ),
            ),
            onDismiss = onSourceOptionsDismiss,
        )
    }

    uiState.optionsNote?.let { note ->
        ItemOptionsBottomSheet(
            title = note.title,
            actions = listOf(
                ItemOptionAction(
                    label = stringResource(R.string.note_options_view),
                    onClick = onViewNoteClick,
                ),
                ItemOptionAction(
                    label = stringResource(R.string.note_options_edit),
                    onClick = onEditNoteClick,
                ),
                ItemOptionAction(
                    label = stringResource(R.string.note_options_convert),
                    onClick = onConvertNoteClick,
                ),
                ItemOptionAction(
                    label = stringResource(R.string.note_options_delete),
                    style = ItemOptionStyle.Destructive,
                    onClick = onDeleteNoteClick,
                ),
            ),
            onDismiss = onNoteOptionsDismiss,
        )
    }

    if (uiState.showNotebookActions) {
        ItemOptionsBottomSheet(
            title = stringResource(R.string.notebook_actions_title),
            actions = listOf(
                ItemOptionAction(
                    label = stringResource(R.string.notebook_actions_copy),
                    onClick = onCopyNotebookClick,
                ),
                ItemOptionAction(
                    label = stringResource(R.string.notebook_actions_export),
                    onClick = onExportNotebookClick,
                ),
            ),
            onDismiss = onNotebookActionsDismiss,
        )
    }

    if (uiState.showNotebookExport) {
        ExportNotebookBottomSheet(
            onDismiss = onNotebookExportDismiss,
            onExport = onNotebookExportConfirm,
        )
    }
}
