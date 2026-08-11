package com.nus.folio.presentation.home.bottomsheet

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nus.folio.R
import com.nus.folio.components.AnimatedModalSheet
import com.nus.folio.presentation.home.HomeSheetShape
import com.nus.folio.ui.theme.CormorantGaramond
import com.nus.folio.ui.theme.FolioAndroidTheme
import com.nus.folio.ui.theme.HomeSheetBackground
import com.nus.folio.ui.theme.HomeTextPrimary

@Composable
internal fun DeleteConfirmationBottomSheet(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit = {},
    @StringRes titleRes: Int = R.string.source_delete_title,
    @StringRes messageRes: Int = R.string.source_delete_message,
    @StringRes confirmLabelRes: Int = R.string.source_delete_confirm,
    isSubmitting: Boolean = false,
    /**
     * When true (default), confirm animates the sheet closed then invokes [onConfirm]
     * (Home source/note delete). When false, [onConfirm] runs immediately and the
     * parent closes the sheet by clearing state (Space delete with loading).
     */
    closeOnConfirm: Boolean = true,
) {
    AnimatedModalSheet(
        onDismiss = {
            if (!isSubmitting) onDismiss()
        },
        dismissOnScrimClick = !isSubmitting,
    ) { requestDismiss ->
        AddSourceDragHandle()
        DeleteConfirmationSheetContent(
            titleRes = titleRes,
            messageRes = messageRes,
            confirmLabelRes = confirmLabelRes,
            isSubmitting = isSubmitting,
            onCancelClick = {
                if (!isSubmitting) requestDismiss()
            },
            onConfirm = {
                if (isSubmitting) return@DeleteConfirmationSheetContent
                if (closeOnConfirm) {
                    requestDismiss { onConfirm() }
                } else {
                    onConfirm()
                }
            },
        )
    }
}

@Composable
internal fun DeleteConfirmationSheetContent(
    onCancelClick: () -> Unit,
    onConfirm: () -> Unit,
    @StringRes titleRes: Int = R.string.source_delete_title,
    @StringRes messageRes: Int = R.string.source_delete_message,
    @StringRes confirmLabelRes: Int = R.string.source_delete_confirm,
    isSubmitting: Boolean = false,
) {
    Column {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(titleRes),
            fontFamily = CormorantGaramond,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            color = HomeTextPrimary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(messageRes),
            fontSize = 14.sp,
            color = HomeTextPrimary,
            lineHeight = 20.sp,
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
            AddSourceDestructiveButton(
                onClick = onConfirm,
                modifier = Modifier.weight(1f),
                labelRes = confirmLabelRes,
                isLoading = isSubmitting,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852, backgroundColor = 0xFFF7F1E6)
@Composable
private fun DeleteSourceConfirmationPreview() {
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
                DeleteConfirmationSheetContent(
                    onCancelClick = {},
                    onConfirm = {},
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852, backgroundColor = 0xFFF7F1E6)
@Composable
private fun DeleteNoteConfirmationPreview() {
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
                DeleteConfirmationSheetContent(
                    titleRes = R.string.note_delete_title,
                    messageRes = R.string.note_delete_message,
                    onCancelClick = {},
                    onConfirm = {},
                )
            }
        }
    }
}

@Composable
internal fun SheetDiscardConfirmBottomSheet(
    visible: Boolean,
    onKeepEditing: () -> Unit,
    onDiscard: () -> Unit,
    @StringRes titleRes: Int = R.string.sheet_discard_unsaved_title,
    @StringRes messageRes: Int = R.string.sheet_discard_unsaved_message,
    @StringRes confirmLabelRes: Int = R.string.sheet_discard_confirm,
) {
    if (!visible) return
    DeleteConfirmationBottomSheet(
        onDismiss = onKeepEditing,
        onConfirm = onDiscard,
        titleRes = titleRes,
        messageRes = messageRes,
        confirmLabelRes = confirmLabelRes,
    )
}
