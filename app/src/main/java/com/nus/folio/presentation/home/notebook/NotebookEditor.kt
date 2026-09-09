package com.nus.folio.presentation.home.notebook

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nus.folio.R
import com.nus.folio.presentation.home.NotebookSaveStatus
import com.nus.folio.ui.theme.HomeTextPrimary
import com.nus.folio.ui.theme.LoginCopper
import kotlinx.coroutines.launch

@Composable
internal fun NotebookEditor(
    content: String,
    spaceTitle: String,
    saveStatus: NotebookSaveStatus,
    onContentChange: (String) -> Unit,
    onRetrySave: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var fieldValue by remember { mutableStateOf(TextFieldValue(content)) }
    var undoState by remember { mutableStateOf(NotebookMarkdownActions.UndoState()) }
    var isInternalUpdate by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    var viewportHeight by remember { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val cursorMarginPx = with(density) { 24.dp.roundToPx() }

    fun scrollToCursorIfNeeded(layout: TextLayoutResult) {
        val target = NotebookEditorScroll.cursorScrollTarget(
            layout = layout,
            cursorOffset = fieldValue.selection.end,
            scrollOffset = scrollState.value,
            viewportHeight = viewportHeight,
            marginPx = cursorMarginPx,
        ) ?: return
        if (target == scrollState.value) return
        coroutineScope.launch {
            scrollState.animateScrollTo(target)
        }
    }

    LaunchedEffect(content) {
        if (!isInternalUpdate && fieldValue.text != content) {
            fieldValue = TextFieldValue(content)
        }
        isInternalUpdate = false
    }

    val readOnly = saveStatus == NotebookSaveStatus.READ_ONLY

    fun applyTransform(transform: (TextFieldValue) -> TextFieldValue) {
        if (readOnly) return
        undoState = NotebookMarkdownActions.pushUndo(undoState, fieldValue)
        val next = transform(fieldValue)
        fieldValue = next
        isInternalUpdate = true
        onContentChange(next.text)
    }

    Column(modifier = modifier.fillMaxSize()) {
        NotebookEditorToolbar(
            saveStatus = saveStatus,
            canUndo = undoState.undoStack.isNotEmpty(),
            canRedo = undoState.redoStack.isNotEmpty(),
            onBoldClick = { applyTransform(NotebookMarkdownActions::toggleBold) },
            onItalicClick = { applyTransform(NotebookMarkdownActions::toggleItalic) },
            onHeading1Click = { applyTransform { NotebookMarkdownActions.setHeading(it, 1) } },
            onHeading2Click = { applyTransform { NotebookMarkdownActions.setHeading(it, 2) } },
            onHeading3Click = { applyTransform { NotebookMarkdownActions.setHeading(it, 3) } },
            onBulletListClick = { applyTransform(NotebookMarkdownActions::toggleBulletList) },
            onOrderedListClick = { applyTransform(NotebookMarkdownActions::toggleOrderedList) },
            onBlockquoteClick = { applyTransform(NotebookMarkdownActions::toggleBlockquote) },
            onUndoClick = {
                NotebookMarkdownActions.undo(undoState, fieldValue)?.let { (next, state) ->
                    undoState = state
                    fieldValue = next
                    isInternalUpdate = true
                    onContentChange(next.text)
                }
            },
            onRedoClick = {
                NotebookMarkdownActions.redo(undoState, fieldValue)?.let { (next, state) ->
                    undoState = state
                    fieldValue = next
                    isInternalUpdate = true
                    onContentChange(next.text)
                }
            },
            onRetrySaveClick = onRetrySave,
            modifier = Modifier.fillMaxWidth(),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .onSizeChanged { viewportHeight = it.height }
                .verticalScroll(scrollState),
        ) {
            BasicTextField(
                value = fieldValue,
                enabled = !readOnly,
                onValueChange = { next ->
                    if (!readOnly) {
                        val resolved = NotebookMarkdownActions.continueListOnEnter(fieldValue, next) ?: next
                        if (resolved.text != fieldValue.text) {
                            undoState = NotebookMarkdownActions.pushUndo(undoState, fieldValue)
                        }
                        fieldValue = resolved
                        isInternalUpdate = true
                        onContentChange(resolved.text)
                    }
                },
                onTextLayout = ::scrollToCursorIfNeeded,
                textStyle = TextStyle(
                    color = HomeTextPrimary,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                ),
                cursorBrush = SolidColor(LoginCopper),
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (isNotebookContentEmpty(fieldValue.text)) {
                            NotebookEmptyPlaceholder(
                                heading = notebookEmptyHeading(
                                    spaceTitle = spaceTitle,
                                    fallback = stringResource(R.string.home_notebook_title),
                                ),
                            )
                        }
                        innerTextField()
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .onPreviewKeyEvent { event ->
                        when (notebookShortcutAction(event)) {
                            NotebookShortcutAction.BOLD -> {
                                applyTransform(NotebookMarkdownActions::toggleBold)
                                true
                            }
                            NotebookShortcutAction.ITALIC -> {
                                applyTransform(NotebookMarkdownActions::toggleItalic)
                                true
                            }
                            NotebookShortcutAction.HEADING_1 -> {
                                applyTransform { NotebookMarkdownActions.setHeading(it, 1) }
                                true
                            }
                            NotebookShortcutAction.HEADING_2 -> {
                                applyTransform { NotebookMarkdownActions.setHeading(it, 2) }
                                true
                            }
                            NotebookShortcutAction.HEADING_3 -> {
                                applyTransform { NotebookMarkdownActions.setHeading(it, 3) }
                                true
                            }
                            NotebookShortcutAction.BULLET_LIST -> {
                                applyTransform(NotebookMarkdownActions::toggleBulletList)
                                true
                            }
                            NotebookShortcutAction.ORDERED_LIST -> {
                                applyTransform(NotebookMarkdownActions::toggleOrderedList)
                                true
                            }
                            NotebookShortcutAction.BLOCKQUOTE -> {
                                applyTransform(NotebookMarkdownActions::toggleBlockquote)
                                true
                            }
                            NotebookShortcutAction.UNDO -> {
                                NotebookMarkdownActions.undo(undoState, fieldValue)?.let { (next, state) ->
                                    undoState = state
                                    fieldValue = next
                                    isInternalUpdate = true
                                    onContentChange(next.text)
                                }
                                true
                            }
                            NotebookShortcutAction.REDO -> {
                                NotebookMarkdownActions.redo(undoState, fieldValue)?.let { (next, state) ->
                                    undoState = state
                                    fieldValue = next
                                    isInternalUpdate = true
                                    onContentChange(next.text)
                                }
                                true
                            }
                            null -> false
                        }
                    },
            )
        }
    }
}
