package com.nus.folio.presentation.space

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
import com.nus.folio.presentation.home.bottomsheet.SheetDiscardConfirmBottomSheet
import com.nus.folio.presentation.home.bottomsheet.AddSourceCancelButton
import com.nus.folio.presentation.home.bottomsheet.AddSourceDragHandle
import com.nus.folio.presentation.home.bottomsheet.AddSourceSubmitButton
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
            onValueChange = { name = it },
        )
        Spacer(modifier = Modifier.height(20.dp))
        AddSpaceObjectiveField(
            value = objective,
            onValueChange = { objective = it },
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
    }
}

@Composable
private fun AddSpaceObjectiveField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    val scrollState = rememberScrollState()
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
                value = value,
                onValueChange = onValueChange,
                singleLine = false,
                textStyle = TextStyle(color = HomeTextPrimary, fontSize = 15.sp),
                cursorBrush = SolidColor(HomeTextPrimary),
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
            )
        }
    }
}

private fun Context.findActivityOrNull(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return current as? Activity
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
