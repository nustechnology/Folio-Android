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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.nus.folio.domain.model.Source
import com.nus.folio.domain.model.SourceStatus
import com.nus.folio.domain.model.SourceType
import com.nus.folio.domain.util.AddSourceInputRules
import com.nus.folio.presentation.home.HomeSheetShape
import com.nus.folio.ui.theme.CormorantGaramond
import com.nus.folio.ui.theme.FolioAndroidTheme
import com.nus.folio.ui.theme.HomeSheetBackground
import com.nus.folio.ui.theme.HomeStatusFailedText
import com.nus.folio.ui.theme.HomeTextPrimary
import com.nus.folio.ui.theme.HomeTextSecondary
import java.text.NumberFormat
import java.util.Locale

private val EditSourceContentHeight = 160.dp

@Composable
internal fun EditSourceBottomSheet(
    source: Source,
    initialContent: String = "",
    onDismiss: () -> Unit,
    onSave: (title: String, author: String, content: String) -> Unit = { _, _, _ -> },
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
            initialContent = initialContent,
            onCancelClick = { requestDismiss() },
            onSave = { title, author, content ->
                requestDismiss { onSave(title, author, content) }
            },
        )
    }
}

@Composable
internal fun EditSourceSheetContent(
    source: Source,
    initialContent: String = "",
    onCancelClick: () -> Unit,
    onSave: (title: String, author: String, content: String) -> Unit,
) {
    val isTextSource = source.type == SourceType.TEXT
    var title by rememberSaveable(source.id) { mutableStateOf(source.title) }
    var author by rememberSaveable(source.id) { mutableStateOf(source.author) }
    var content by rememberSaveable(source.id, initialContent) { mutableStateOf(initialContent) }
    val contentError = if (isTextSource) {
        AddSourceInputRules.contentValidationError(content)
    } else {
        null
    }
    val canSave = title.isNotBlank() &&
        (!isTextSource || AddSourceInputRules.isContentValid(content))
    val numberFormat = remember { NumberFormat.getIntegerInstance(Locale.getDefault()) }
    val scrollState = rememberScrollState()

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
        Spacer(modifier = Modifier.height(24.dp))
        AddSourceLabeledField(
            label = stringResource(R.string.edit_source_title_label),
            value = title,
            onValueChange = { title = AddSourceInputRules.limitTitle(it) },
            placeholder = stringResource(R.string.edit_source_title_placeholder),
            singleLine = true,
        )
        Spacer(modifier = Modifier.height(16.dp))
        AddSourceLabeledField(
            label = stringResource(R.string.edit_source_author_label),
            value = author,
            onValueChange = { author = AddSourceInputRules.limitAuthor(it) },
            placeholder = stringResource(R.string.edit_source_author_placeholder),
            singleLine = true,
        )
        if (isTextSource) {
            Spacer(modifier = Modifier.height(16.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                AddSourceLabeledField(
                    label = stringResource(R.string.add_source_text_content_label),
                    value = content,
                    onValueChange = { content = it },
                    placeholder = stringResource(R.string.add_source_text_content_placeholder),
                    singleLine = false,
                    fieldModifier = Modifier
                        .fillMaxWidth()
                        .height(EditSourceContentHeight),
                    showResizeHint = true,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(
                        R.string.add_source_text_content_counter,
                        numberFormat.format(content.length),
                        numberFormat.format(AddSourceInputRules.MAX_CONTENT_LENGTH),
                    ),
                    fontSize = 12.sp,
                    color = if (content.length > AddSourceInputRules.MAX_CONTENT_LENGTH) {
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
                        text = when (contentError) {
                            AddSourceInputRules.ContentValidationError.TOO_SHORT ->
                                stringResource(R.string.add_source_text_content_too_short)
                            AddSourceInputRules.ContentValidationError.TOO_LONG ->
                                stringResource(
                                    R.string.add_source_text_content_too_long,
                                    numberFormat.format(AddSourceInputRules.MAX_CONTENT_LENGTH),
                                )
                        },
                        fontSize = 12.sp,
                        color = HomeStatusFailedText,
                    )
                }
            }
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
                enabled = canSave,
                onClick = {
                    onSave(
                        title.trim(),
                        author.trim(),
                        if (isTextSource) content.trim() else "",
                    )
                },
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
                        type = SourceType.FILE,
                        author = "Alan Turing",
                        addedLabel = "Added 2d ago",
                        status = SourceStatus.READY,
                        spaceId = "1",
                    ),
                    onCancelClick = {},
                    onSave = { _, _, _ -> },
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852, backgroundColor = 0xFFF7F1E6)
@Composable
private fun EditTextSourceSheetContentPreview() {
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
                        id = "6",
                        title = "Interview notes: archival methods",
                        type = SourceType.TEXT,
                        author = "Field notes",
                        addedLabel = "Added 2d ago",
                        status = SourceStatus.READY,
                        spaceId = "3",
                    ),
                    initialContent = "Frontline coordinators described repeated entry across systems.",
                    onCancelClick = {},
                    onSave = { _, _, _ -> },
                )
            }
        }
    }
}
