package com.nus.folio.presentation.home.bottomsheet

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
import com.nus.folio.domain.model.Source
import com.nus.folio.domain.model.SourceStatus
import com.nus.folio.domain.model.SourceType
import com.nus.folio.domain.util.AddSourceInputRules
import com.nus.folio.presentation.home.HomeSheetShape
import com.nus.folio.ui.theme.CormorantGaramond
import com.nus.folio.ui.theme.FolioAndroidTheme
import com.nus.folio.ui.theme.HomeSheetBackground
import com.nus.folio.ui.theme.HomeTextPrimary

@Composable
internal fun EditSourceBottomSheet(
    source: Source,
    onDismiss: () -> Unit,
    onSave: (title: String, author: String) -> Unit = { _, _ -> },
    isSubmitting: Boolean = false,
    /**
     * When true (default), save animates the sheet closed then invokes [onSave]
     * (Home edit). When false, [onSave] runs immediately and the parent closes
     * the sheet by clearing state (Source Detail with loading).
     */
    closeOnSave: Boolean = true,
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
        EditSourceSheetContent(
            source = source,
            isSubmitting = isSubmitting,
            onDirtyChange = { discardProtection.hasUnsavedContent = it },
            onCancelClick = {
                if (!isSubmitting) requestDismiss()
            },
            onSave = { title, author ->
                if (isSubmitting) return@EditSourceSheetContent
                if (closeOnSave) {
                    discardProtection.bypassDiscardConfirm = true
                    requestDismiss { onSave(title, author) }
                } else {
                    onSave(title, author)
                }
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
internal fun EditSourceSheetContent(
    source: Source,
    onCancelClick: () -> Unit,
    onSave: (title: String, author: String) -> Unit,
    onDirtyChange: (Boolean) -> Unit = {},
    isSubmitting: Boolean = false,
) {
    var title by rememberSaveable(source.id) { mutableStateOf(source.title) }
    var author by rememberSaveable(source.id) { mutableStateOf(source.author) }
    val canSave = title.isNotBlank() && !isSubmitting
    val scrollState = rememberScrollState()

    SideEffect {
        onDirtyChange(
            title != source.title || author != source.author,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState),
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.edit_source_title),
            fontFamily = CormorantGaramond,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            color = HomeTextPrimary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.edit_source_description),
            fontSize = 14.sp,
            color = HomeTextPrimary,
            lineHeight = 20.sp,
        )
        Spacer(modifier = Modifier.height(24.dp))
        AddSourceLabeledField(
            label = stringResource(R.string.edit_source_title_label),
            value = title,
            onValueChange = { title = AddSourceInputRules.limitTitle(it) },
            placeholder = stringResource(R.string.edit_source_title_placeholder),
            singleLine = true,
            characterLimit = AddSourceInputRules.MAX_TITLE_LENGTH,
        )
        AddSourceLabeledField(
            label = stringResource(R.string.edit_source_author_label),
            value = author,
            onValueChange = { author = AddSourceInputRules.limitAuthor(it) },
            placeholder = stringResource(R.string.edit_source_author_placeholder),
            singleLine = true,
            characterLimit = AddSourceInputRules.MAX_AUTHOR_LENGTH,
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
                enabled = canSave,
                onClick = {
                    onSave(
                        title.trim(),
                        author.trim(),
                    )
                },
                labelRes = R.string.edit_source_save,
                isLoading = isSubmitting,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852, backgroundColor = 0xFFF7F1E6)
@Composable
private fun EditSourceSheetContentPreview() {
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
                EditSourceSheetContent(
                    source = Source(
                        id = "1",
                        title = "Alan Turing: Computing Machinery",
                        type = SourceType.FILE,
                        author = "Alan Turing",
                        addedLabel = "Added 2d ago",
                        status = SourceStatus.READY,
                        spaceId = "1",
                    ),
                    onCancelClick = {},
                    onSave = { _, _ -> },
                )
            }
        }
    }
}
