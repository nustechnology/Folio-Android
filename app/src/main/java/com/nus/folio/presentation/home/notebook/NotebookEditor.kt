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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.nus.folio.R
import com.nus.folio.components.TextFieldCursorScroll
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
    var formatSelection by remember { mutableStateOf(TextRange.Zero) }
    var freezeSelection by remember { mutableStateOf(false) }
    var undoState by remember { mutableStateOf(NotebookMarkdownActions.UndoState()) }
    var isInternalUpdate by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    var viewportHeight by remember { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val cursorMarginPx = with(density) { 24.dp.roundToPx() }
    val focusRequester = remember { FocusRequester() }
    val markdownVisuals = remember { NotebookMarkdownVisualTransformation() }

    fun scrollToCursorIfNeeded(layout: TextLayoutResult) {
        val mapping = markdownVisuals.visualizeCached(fieldValue.text).mapping
        val cursorOffset = mapping.originalToTransformed(fieldValue.selection.end)
            .coerceIn(0, layout.layoutInput.text.length)
        val target = TextFieldCursorScroll.cursorScrollTarget(
            layout = layout,
            cursorOffset = cursorOffset,
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
            formatSelection = TextRange.Zero
            freezeSelection = false
        }
        isInternalUpdate = false
    }

    val readOnly = saveStatus == NotebookSaveStatus.READ_ONLY

    fun applyTransform(transform: (TextFieldValue) -> TextFieldValue) {
        if (readOnly) return
        val source = fieldValue.copy(selection = formatSelection)
        undoState = NotebookMarkdownActions.pushUndo(undoState, source)
        val next = transform(source)
        fieldValue = next
        formatSelection = next.selection
        freezeSelection = false
        isInternalUpdate = true
        onContentChange(next.text)
        focusRequester.requestFocus()
    }

    val activeMarks = remember(fieldValue.text, formatSelection) {
        NotebookMarkdownActions.activeMarks(fieldValue.copy(selection = formatSelection))
    }

    Column(modifier = modifier.fillMaxSize()) {
        NotebookEditorToolbar(
            saveStatus = saveStatus,
            canUndo = undoState.undoStack.isNotEmpty(),
            canRedo = undoState.redoStack.isNotEmpty(),
            activeMarks = activeMarks,
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
                    formatSelection = next.selection
                    freezeSelection = false
                    isInternalUpdate = true
                    onContentChange(next.text)
                    focusRequester.requestFocus()
                }
            },
            onRedoClick = {
                NotebookMarkdownActions.redo(undoState, fieldValue)?.let { (next, state) ->
                    undoState = state
                    fieldValue = next
                    formatSelection = next.selection
                    freezeSelection = false
                    isInternalUpdate = true
                    onContentChange(next.text)
                    focusRequester.requestFocus()
                }
            },
            onRetrySaveClick = onRetrySave,
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(1f)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (event.changes.any { it.changedToDown() }) {
                                formatSelection = fieldValue.selection
                                freezeSelection = true
                            }
                            if (event.changes.any { it.changedToUp() }) {
                                freezeSelection = false
                            }
                        }
                    }
                },
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
                visualTransformation = markdownVisuals,
                onValueChange = { next ->
                    if (readOnly) return@BasicTextField
                    val resolved = NotebookMarkdownActions.resolveMarkdownEdit(fieldValue, next)
                    val textChanged = resolved.text != fieldValue.text
                    if (textChanged) {
                        undoState = NotebookMarkdownActions.pushUndo(undoState, fieldValue)
                    }
                    fieldValue = resolved
                    if (!freezeSelection || textChanged) {
                        formatSelection = resolved.selection
                        freezeSelection = false
                    }
                    isInternalUpdate = true
                    onContentChange(resolved.text)
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
                    .focusRequester(focusRequester)
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
                                    formatSelection = next.selection
                                    freezeSelection = false
                                    isInternalUpdate = true
                                    onContentChange(next.text)
                                }
                                true
                            }
                            NotebookShortcutAction.REDO -> {
                                NotebookMarkdownActions.redo(undoState, fieldValue)?.let { (next, state) ->
                                    undoState = state
                                    fieldValue = next
                                    formatSelection = next.selection
                                    freezeSelection = false
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
