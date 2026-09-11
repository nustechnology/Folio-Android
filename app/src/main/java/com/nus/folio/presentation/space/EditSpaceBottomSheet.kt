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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nus.folio.R
import com.nus.folio.components.AnimatedModalSheet
import com.nus.folio.components.rememberSheetDiscardProtectionState
import com.nus.folio.components.rememberTextFieldCursorScroller
import com.nus.folio.domain.model.Space
import com.nus.folio.domain.util.SpaceInputRules
import com.nus.folio.presentation.home.bottomsheet.SheetDiscardConfirmBottomSheet
import com.nus.folio.presentation.home.HomeSheetInputBorder
import com.nus.folio.presentation.home.HomeSheetShape
import com.nus.folio.presentation.home.HomeUploadZoneShape
import com.nus.folio.presentation.home.bottomsheet.AddSourceCancelButton
import com.nus.folio.presentation.home.bottomsheet.AddSourceDragHandle
import com.nus.folio.presentation.home.bottomsheet.AddSourceSubmitButton
import com.nus.folio.presentation.home.bottomsheet.findActivityOrNull
import com.nus.folio.ui.theme.CormorantGaramond
import com.nus.folio.ui.theme.FolioAndroidTheme
import com.nus.folio.ui.theme.HomeCardBackground
import com.nus.folio.ui.theme.HomeSheetBackground
import com.nus.folio.ui.theme.HomeTextPrimary
import com.nus.folio.ui.theme.HomeTextSecondary
import com.nus.folio.ui.theme.LoginCopper

private val EditSpaceObjectiveHeight = 160.dp

@Composable
internal fun EditSpaceBottomSheet(
    space: Space,
    onDismiss: () -> Unit,
    onSave: (name: String, researchObjective: String) -> Unit = { _, _ -> },
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
        EditSpaceSheetContent(
            space = space,
            isSubmitting = isSubmitting,
            onDirtyChange = { discardProtection.hasUnsavedContent = it },
            onCancelClick = {
                if (!isSubmitting) requestDismiss()
            },
            onSave = { name, objective ->
                onSave(name, objective)
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
private fun EditSpaceSheetContent(
    space: Space,
    onCancelClick: () -> Unit,
    onSave: (name: String, researchObjective: String) -> Unit,
    onDirtyChange: (Boolean) -> Unit = {},
    isSubmitting: Boolean = false,
) {
    val initialName = remember(space.id) { SpaceInputRules.limitTitle(space.title) }
    val initialObjective = remember(space.id) {
        SpaceInputRules.limitObjective(space.description)
    }
    var name by rememberSaveable(space.id) { mutableStateOf(initialName) }
    var objective by rememberSaveable(space.id) { mutableStateOf(initialObjective) }
    val trimmedName = name.trim()
    val trimmedObjective = objective.trim()
    val canSave = trimmedName.isNotEmpty() &&
        !isSubmitting &&
        (trimmedName != initialName.trim() || trimmedObjective != initialObjective.trim())

    SideEffect {
        onDirtyChange(name != initialName || objective != initialObjective)
    }

    Column {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.rename_space_title),
            fontFamily = CormorantGaramond,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            color = HomeTextPrimary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.edit_space_description),
            fontSize = 14.sp,
            color = HomeTextPrimary,
            lineHeight = 20.sp,
        )
        Spacer(modifier = Modifier.height(24.dp))
        EditSpaceNameField(
            value = name,
            onValueChange = { name = SpaceInputRules.limitTitle(it) },
        )
        EditSpaceObjectiveField(
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
                enabled = canSave,
                onClick = { onSave(trimmedName, trimmedObjective) },
                labelRes = R.string.edit_source_save,
                isLoading = isSubmitting,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun EditSpaceNameField(
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
private fun EditSpaceObjectiveField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    val cursorScroller = rememberTextFieldCursorScroller()
    var fieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(value))
    }
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
                .height(EditSpaceObjectiveHeight)
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
                onValueChange = { next ->
                    val limitedText = SpaceInputRules.limitObjective(next.text)
                    fieldValue = if (limitedText == next.text) {
                        next
                    } else {
                        TextFieldValue(
                            text = limitedText,
                            selection = TextRange(
                                next.selection.start.coerceAtMost(limitedText.length),
                                next.selection.end.coerceAtMost(limitedText.length),
                            ),
                        )
                    }
                    onValueChange(limitedText)
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

@Preview(showBackground = true, widthDp = 393, heightDp = 852, backgroundColor = 0xFFF7F1E6)
@Composable
private fun EditSpaceSheetContentPreview() {
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
                EditSpaceSheetContent(
                    space = Space(
                        id = "1",
                        title = "Dissertation Research",
                        description = "PhD archive",
                        sourceCount = 12,
                        noteCount = 8,
                        updatedLabel = "Updated 2d ago",
                    ),
                    onCancelClick = {},
                    onSave = { _, _ -> },
                )
            }
        }
    }
}
