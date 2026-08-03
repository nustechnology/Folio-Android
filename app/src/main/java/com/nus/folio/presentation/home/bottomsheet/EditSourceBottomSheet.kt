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
import com.nus.folio.domain.model.Source
import com.nus.folio.domain.model.SourceStatus
import com.nus.folio.domain.model.SourceType
import com.nus.folio.components.AnimatedModalSheet
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
) {
    val context = LocalContext.current

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
        contentWindowInsets = WindowInsets.navigationBars.union(WindowInsets.ime),
    ) { requestDismiss ->
        AddSourceDragHandle()
        EditSourceSheetContent(
            source = source,
            onCancelClick = { requestDismiss() },
            onSave = { title, author ->
                requestDismiss { onSave(title, author) }
            },
        )
    }
}

@Composable
internal fun EditSourceSheetContent(
    source: Source,
    onCancelClick: () -> Unit,
    onSave: (title: String, author: String) -> Unit,
) {
    var title by rememberSaveable(source.id) { mutableStateOf(source.title) }
    var author by rememberSaveable(source.id) { mutableStateOf(source.author) }
    val canSave = title.isNotBlank()

    Column {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.edit_source_title),
            fontFamily = CormorantGaramond,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            color = HomeTextPrimary,
        )
        Spacer(modifier = Modifier.height(24.dp))
        AddSourceLabeledField(
            label = stringResource(R.string.edit_source_title_label),
            value = title,
            onValueChange = { title = it },
            placeholder = stringResource(R.string.edit_source_title_placeholder),
            singleLine = true,
        )
        Spacer(modifier = Modifier.height(16.dp))
        AddSourceLabeledField(
            label = stringResource(R.string.edit_source_author_label),
            value = author,
            onValueChange = { author = it },
            placeholder = stringResource(R.string.edit_source_author_placeholder),
            singleLine = true,
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
                onClick = { onSave(title.trim(), author.trim()) },
                labelRes = R.string.edit_source_save,
                modifier = Modifier.weight(1f),
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
                        type = SourceType.PDF,
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
