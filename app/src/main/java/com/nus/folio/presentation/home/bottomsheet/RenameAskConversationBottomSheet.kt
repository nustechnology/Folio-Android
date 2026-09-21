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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.nus.folio.components.rememberSheetDiscardProtectionState
import com.nus.folio.domain.model.AskConversation
import com.nus.folio.presentation.home.HomeSheetInputBorder
import com.nus.folio.presentation.home.HomeSheetShape
import com.nus.folio.presentation.home.HomeUploadZoneShape
import com.nus.folio.ui.theme.CormorantGaramond
import com.nus.folio.ui.theme.FolioAndroidTheme
import com.nus.folio.ui.theme.HomeCardBackground
import com.nus.folio.ui.theme.HomeSheetBackground
import com.nus.folio.ui.theme.HomeTextPrimary
import com.nus.folio.ui.theme.HomeTextSecondary
import com.nus.folio.ui.theme.LoginCopper

private const val MaxConversationTitleLength = 255

@Composable
internal fun RenameAskConversationBottomSheet(
    conversation: AskConversation,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit = {},
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
        RenameAskConversationSheetContent(
            conversation = conversation,
            isSubmitting = isSubmitting,
            onDirtyChange = { discardProtection.hasUnsavedContent = it },
            onCancelClick = {
                if (!isSubmitting) requestDismiss()
            },
            onSave = onSave,
        )
    }

    SheetDiscardConfirmBottomSheet(
        visible = discardProtection.showDiscardConfirm,
        onKeepEditing = { discardProtection.showDiscardConfirm = false },
        onDiscard = { discardProtection.discardAndDismiss() },
    )
}

@Composable
private fun RenameAskConversationSheetContent(
    conversation: AskConversation,
    onCancelClick: () -> Unit,
    onSave: (String) -> Unit,
    onDirtyChange: (Boolean) -> Unit = {},
    isSubmitting: Boolean = false,
) {
    var title by rememberSaveable(conversation.id) { mutableStateOf(conversation.title) }
    val trimmedTitle = title.trim()
    val canSave = trimmedTitle.isNotEmpty() &&
        !isSubmitting &&
        trimmedTitle != conversation.title

    SideEffect {
        onDirtyChange(title != conversation.title)
    }

    Column {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.rename_conversation_title),
            fontFamily = CormorantGaramond,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            color = HomeTextPrimary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.rename_conversation_description),
            fontSize = 14.sp,
            color = HomeTextPrimary,
            lineHeight = 20.sp,
        )
        Spacer(modifier = Modifier.height(24.dp))
        RenameConversationTitleField(
            value = title,
            onValueChange = { value ->
                if (!isSubmitting) title = value.take(MaxConversationTitleLength)
            },
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
                onClick = { onSave(trimmedTitle) },
                labelRes = R.string.edit_source_save,
                isLoading = isSubmitting,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun RenameConversationTitleField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.rename_conversation_name_label),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = LoginCopper,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(HomeUploadZoneShape)
                .border(1.dp, HomeSheetInputBorder, HomeUploadZoneShape)
                .background(HomeCardBackground)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            if (value.isEmpty()) {
                Text(
                    text = stringResource(R.string.rename_conversation_name_placeholder),
                    color = HomeTextSecondary,
                    fontSize = 15.sp,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = HomeTextPrimary, fontSize = 15.sp),
                cursorBrush = SolidColor(HomeTextPrimary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852, backgroundColor = 0xFFF7F1E6)
@Composable
private fun RenameAskConversationSheetContentPreview() {
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
                RenameAskConversationSheetContent(
                    conversation = AskConversation(
                        id = "conv-1",
                        title = "What were the operating costs in Q4?",
                        dateLabel = "Aug 20, 07:54",
                        spaceId = "1",
                    ),
                    onCancelClick = {},
                    onSave = {},
                )
            }
        }
    }
}
