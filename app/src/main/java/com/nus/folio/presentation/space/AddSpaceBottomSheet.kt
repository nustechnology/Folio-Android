package com.nus.folio.presentation.space

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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nus.folio.R
import com.nus.folio.components.AnimatedModalSheet
import com.nus.folio.components.rememberSheetDiscardProtectionState
import com.nus.folio.components.rememberTextFieldCursorScroller
import com.nus.folio.domain.util.SpaceInputRules
import com.nus.folio.presentation.home.bottomsheet.SheetDiscardConfirmBottomSheet
import com.nus.folio.presentation.home.bottomsheet.AddSourceCancelButton
import com.nus.folio.presentation.home.bottomsheet.AddSourceDragHandle
import com.nus.folio.presentation.home.bottomsheet.AddSourceSubmitButton
import com.nus.folio.presentation.home.bottomsheet.findActivityOrNull
import com.nus.folio.presentation.home.HomeSheetInputBorder
import com.nus.folio.presentation.home.HomeUploadZoneShape
import com.nus.folio.ui.theme.CormorantGaramond
import com.nus.folio.ui.theme.FolioAndroidTheme
import com.nus.folio.ui.theme.FolioSheetShape
import com.nus.folio.ui.theme.HomeCardBackground
import com.nus.folio.ui.theme.HomeSheetBackground
import com.nus.folio.ui.theme.HomeTextPrimary
import com.nus.folio.ui.theme.HomeTextSecondary
import com.nus.folio.ui.theme.LoginCopper
import java.text.NumberFormat
import java.util.Locale

private val AddSpaceObjectiveHeight = 160.dp

@Composable
internal fun AddSpaceBottomSheet(
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
        AddSpaceSheetContent(
            isSubmitting = isSubmitting,
            onDirtyChange = { discardProtection.hasUnsavedContent = it },
            onCancelClick = {
                if (!isSubmitting) requestDismiss()
            },
            onSubmit = { name, objective ->
                onSubmit(name, objective)
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
private fun AddSpaceSheetContent(
    onCancelClick: () -> Unit,
    onSubmit: (String, String) -> Unit,
    onDirtyChange: (Boolean) -> Unit = {},
    isSubmitting: Boolean = false,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var objective by rememberSaveable { mutableStateOf("") }
    val canSubmit = name.isNotBlank() && !isSubmitting

    SideEffect {
        onDirtyChange(name.isNotEmpty() || objective.isNotEmpty())
    }

    Column {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.add_space_title),
            fontFamily = CormorantGaramond,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            color = HomeTextPrimary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.add_space_description),
            fontSize = 14.sp,
            color = HomeTextPrimary,
            lineHeight = 20.sp,
        )
        Spacer(modifier = Modifier.height(24.dp))
        AddSpaceNameField(
            value = name,
            onValueChange = { name = SpaceInputRules.limitTitle(it) },
        )
        AddSpaceObjectiveField(
            value = objective,
            onValueChange = { objective = SpaceInputRules.limitObjective(it) },
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
                enabled = canSubmit,
                onClick = { onSubmit(name.trim(), objective.trim()) },
                labelRes = R.string.add_space_submit,
                isLoading = isSubmitting,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AddSpaceNameField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.add_space_name_hint),
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
                    text = stringResource(R.string.add_space_name_placeholder),
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
        SpaceFieldCharacterCounter(
            length = value.length,
            limit = SpaceInputRules.MAX_TITLE_LENGTH,
        )
    }
}

@Composable
private fun AddSpaceObjectiveField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    val cursorScroller = rememberTextFieldCursorScroller()
    var fieldValue by remember { mutableStateOf(TextFieldValue(value)) }
    LaunchedEffect(value) {
        if (value != fieldValue.text) {
            fieldValue = TextFieldValue(
                text = value,
                selection = TextRange(
                    fieldValue.selection.start.coerceAtMost(value.length),
                    fieldValue.selection.end.coerceAtMost(value.length),
                ),
            )
        }
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.add_space_objective_hint),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = LoginCopper,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(AddSpaceObjectiveHeight)
                .clip(HomeUploadZoneShape)
                .border(1.dp, HomeSheetInputBorder, HomeUploadZoneShape)
                .background(HomeCardBackground)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            if (value.isEmpty()) {
                Text(
                    text = stringResource(R.string.add_space_objective_placeholder),
                    color = HomeTextSecondary,
                    fontSize = 15.sp,
                )
            }
            BasicTextField(
                value = fieldValue,
                onValueChange = {
                    fieldValue = it
                    onValueChange(it.text)
                },
                singleLine = false,
                textStyle = TextStyle(color = HomeTextPrimary, fontSize = 15.sp),
                cursorBrush = SolidColor(HomeTextPrimary),
                onTextLayout = cursorScroller.onTextLayout(fieldValue.selection.end),
                modifier = Modifier
                    .fillMaxSize()
                    .then(cursorScroller.scrollModifier),
            )
        }
        SpaceFieldCharacterCounter(
            length = value.length,
            limit = SpaceInputRules.MAX_OBJECTIVE_LENGTH,
        )
    }
}

@Composable
internal fun SpaceFieldCharacterCounter(
    length: Int,
    limit: Int,
) {
    val numberFormat = remember { NumberFormat.getIntegerInstance(Locale.getDefault()) }
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = stringResource(
            R.string.add_source_text_content_counter,
            numberFormat.format(length),
            numberFormat.format(limit),
        ),
        fontSize = 12.sp,
        color = HomeTextSecondary,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.End,
    )
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852, backgroundColor = 0xFFF7F1E6)
@Composable
private fun AddSpaceSheetContentPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(HomeSheetBackground, FolioSheetShape)
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 20.dp),
            ) {
                AddSourceDragHandle()
                AddSpaceSheetContent(
                    onCancelClick = {},
                    onSubmit = { _, _ -> },
                )
            }
        }
    }
}
