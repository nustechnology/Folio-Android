package com.nus.folio.presentation.home.notebook

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

internal object NotebookMarkdownActions {
    private const val MAX_UNDO = 50

    fun toggleBold(value: TextFieldValue): TextFieldValue =
        wrapSelection(value, prefix = "**", suffix = "**")

    fun toggleItalic(value: TextFieldValue): TextFieldValue =
        wrapSelection(value, prefix = "_", suffix = "_")

    fun setHeading(value: TextFieldValue, level: Int): TextFieldValue {
        val clampedLevel = level.coerceIn(1, 3)
        val prefix = "#".repeat(clampedLevel) + " "
        return transformActiveLines(value) { line ->
            val trimmed = line.trimStart()
            val withoutHeading = trimmed.replace(Regex("^#{1,6}\\s+"), "")
            prefix + withoutHeading
        }
    }

    fun toggleBulletList(value: TextFieldValue): TextFieldValue =
        toggleLinePrefix(value, marker = "- ")

    fun toggleOrderedList(value: TextFieldValue): TextFieldValue =
        toggleNumberedList(value)

    fun toggleBlockquote(value: TextFieldValue): TextFieldValue =
        toggleLinePrefix(value, marker = "> ")

    /**
     * When the user presses Enter after inserting a single newline, continue or exit
     * ordered / bullet list lines. Returns null if the edit is not a list continuation.
     */
    fun continueListOnEnter(previous: TextFieldValue, next: TextFieldValue): TextFieldValue? {
        if (!previous.selection.collapsed || !isSingleNewlineInsert(previous, next)) return null

        val cursor = previous.selection.start
        val lineStart = previous.text.lastIndexOf('\n', cursor - 1).let { if (it == -1) 0 else it + 1 }
        val lineEnd = previous.text.indexOf('\n', cursor).let { if (it == -1) previous.text.length else it }
        val line = previous.text.substring(lineStart, lineEnd)

        NUMBERED_LINE.matchEntire(line)?.let { match ->
            val content = match.groupValues[2]
            return if (content.isEmpty()) {
                exitListItem(previous, lineStart, cursor)
            } else {
                continueNumberedList(previous, lineStart, lineEnd, cursor, match)
            }
        }

        BULLET_LINE.matchEntire(line)?.let { match ->
            val content = match.groupValues[1]
            return if (content.isEmpty()) {
                exitListItem(previous, lineStart, cursor)
            } else {
                continueBulletList(previous, lineStart, lineEnd, cursor, match)
            }
        }

        return null
    }

    internal data class UndoState(
        val undoStack: List<TextFieldValue> = emptyList(),
        val redoStack: List<TextFieldValue> = emptyList(),
    )

    fun pushUndo(state: UndoState, current: TextFieldValue): UndoState {
        val nextUndo = (state.undoStack + current).takeLast(MAX_UNDO)
        return state.copy(undoStack = nextUndo, redoStack = emptyList())
    }

    fun undo(state: UndoState, current: TextFieldValue): Pair<TextFieldValue, UndoState>? {
        val previous = state.undoStack.lastOrNull() ?: return null
        return previous to state.copy(
            undoStack = state.undoStack.dropLast(1),
            redoStack = state.redoStack + current,
        )
    }

    fun redo(state: UndoState, current: TextFieldValue): Pair<TextFieldValue, UndoState>? {
        val next = state.redoStack.lastOrNull() ?: return null
        return next to state.copy(
            redoStack = state.redoStack.dropLast(1),
            undoStack = state.undoStack + current,
        )
    }

    private fun wrapSelection(
        value: TextFieldValue,
        prefix: String,
        suffix: String,
    ): TextFieldValue {
        val text = value.text
        val selection = value.selection
        if (!selection.collapsed) {
            val selected = text.substring(selection.start, selection.end)
            val wrapped = prefix + selected + suffix
            val newText = text.replaceRange(selection.start, selection.end, wrapped)
            val cursor = selection.start + wrapped.length
            return value.copy(text = newText, selection = TextRange(cursor))
        }
        val insert = prefix + suffix
        val newText = text.replaceRange(selection.start, selection.end, insert)
        val cursor = selection.start + prefix.length
        return value.copy(text = newText, selection = TextRange(cursor))
    }

    private fun transformActiveLines(
        value: TextFieldValue,
        transform: (String) -> String,
    ): TextFieldValue {
        val text = value.text
        val span = activeLineSpan(text, value.selection)
        val lines = text.substring(span.start, span.endExclusive).split('\n')
        val transformed = lines.joinToString("\n", transform = transform)
        val newText = text.replaceRange(span.start, span.endExclusive, transformed)
        val delta = transformed.length - (span.endExclusive - span.start)
        val newCursor = (value.selection.end + delta).coerceIn(0, newText.length)
        return value.copy(text = newText, selection = TextRange(newCursor))
    }

    private fun toggleLinePrefix(value: TextFieldValue, marker: String): TextFieldValue {
        val text = value.text
        val span = activeLineSpan(text, value.selection)
        val segment = text.substring(span.start, span.endExclusive)
        val lines = segment.split('\n')
        // Blank-only content is not "already prefixed" — insert the marker.
        val hasContent = lines.any { it.isNotBlank() }
        val allPrefixed = hasContent && lines.all { it.startsWith(marker) || it.isBlank() }
        val transformed = lines.joinToString("\n") { line ->
            when {
                allPrefixed && line.startsWith(marker) -> line.removePrefix(marker)
                allPrefixed && line.isBlank() -> line
                !allPrefixed -> marker + line
                else -> line
            }
        }
        val newText = text.replaceRange(span.start, span.endExclusive, transformed)
        val delta = transformed.length - segment.length
        val newCursor = (value.selection.end + delta).coerceIn(0, newText.length)
        return value.copy(text = newText, selection = TextRange(newCursor))
    }

    private fun toggleNumberedList(value: TextFieldValue): TextFieldValue {
        val text = value.text
        val span = activeLineSpan(text, value.selection)
        val segment = text.substring(span.start, span.endExclusive)
        val lines = segment.split('\n')
        val numberedPattern = Regex("^\\d+\\.\\s+")
        val hasContent = lines.any { it.isNotBlank() }
        val allNumbered = hasContent && lines.all { numberedPattern.containsMatchIn(it) || it.isBlank() }
        val transformed = buildString {
            var index = 1
            lines.forEachIndexed { lineIndex, line ->
                if (lineIndex > 0) append('\n')
                when {
                    allNumbered && line.isBlank() -> append(line)
                    allNumbered -> append(line.replace(numberedPattern, ""))
                    else -> {
                        append("$index. $line")
                        index++
                    }
                }
            }
        }
        val newText = text.replaceRange(span.start, span.endExclusive, transformed)
        val delta = transformed.length - segment.length
        val newCursor = (value.selection.end + delta).coerceIn(0, newText.length)
        return value.copy(text = newText, selection = TextRange(newCursor))
    }

    private val NUMBERED_LINE = Regex("^(\\d+)\\.\\s(.*)$")
    private val BULLET_LINE = Regex("^- (.*)$")

    private fun isSingleNewlineInsert(previous: TextFieldValue, next: TextFieldValue): Boolean {
        val cursor = previous.selection.start
        if (next.text.length != previous.text.length + 1) return false
        if (next.text.substring(0, cursor) != previous.text.substring(0, cursor)) return false
        if (next.text[cursor] != '\n') return false
        return next.text.substring(cursor + 1) == previous.text.substring(cursor)
    }

    private fun exitListItem(previous: TextFieldValue, lineStart: Int, cursor: Int): TextFieldValue {
        val newText = buildString {
            append(previous.text.substring(0, lineStart))
            append('\n')
            append(previous.text.substring(cursor))
        }
        return TextFieldValue(newText, TextRange(lineStart + 1))
    }

    private fun continueNumberedList(
        previous: TextFieldValue,
        lineStart: Int,
        lineEnd: Int,
        cursor: Int,
        match: MatchResult,
    ): TextFieldValue {
        val num = match.groupValues[1].toInt()
        val content = match.groupValues[2]
        val contentStart = lineStart + (match.value.length - content.length)
        val beforeContent = previous.text.substring(contentStart, cursor.coerceIn(contentStart, lineEnd))
        val afterContent = previous.text.substring(cursor.coerceIn(contentStart, lineEnd), lineEnd).trimStart()

        val currentLine = "${num}. $beforeContent"
        val nextPrefix = "${num + 1}. "
        val newText = buildString {
            append(previous.text.substring(0, lineStart))
            append(currentLine)
            append('\n')
            append(nextPrefix)
            append(afterContent)
            append(previous.text.substring(lineEnd))
        }
        val newCursor = lineStart + currentLine.length + 1 + nextPrefix.length
        return TextFieldValue(newText, TextRange(newCursor))
    }

    private fun continueBulletList(
        previous: TextFieldValue,
        lineStart: Int,
        lineEnd: Int,
        cursor: Int,
        match: MatchResult,
    ): TextFieldValue {
        val content = match.groupValues[1]
        val contentStart = lineStart + 2
        val beforeContent = previous.text.substring(contentStart, cursor.coerceIn(contentStart, lineEnd))
        val afterContent = previous.text.substring(cursor.coerceIn(contentStart, lineEnd), lineEnd).trimStart()

        val currentLine = "- $beforeContent"
        val nextPrefix = "- "
        val newText = buildString {
            append(previous.text.substring(0, lineStart))
            append(currentLine)
            append('\n')
            append(nextPrefix)
            append(afterContent)
            append(previous.text.substring(lineEnd))
        }
        val newCursor = lineStart + currentLine.length + 1 + nextPrefix.length
        return TextFieldValue(newText, TextRange(newCursor))
    }

    private data class LineSpan(val start: Int, val endExclusive: Int)

    private fun activeLineSpan(text: String, selection: TextRange): LineSpan {
        val startOffset = selection.min.coerceIn(0, text.length)
        val endOffset = if (selection.collapsed) {
            startOffset
        } else {
            (selection.max - 1).coerceAtLeast(selection.min).coerceIn(0, text.length)
        }
        val start = text.lastIndexOf('\n', startOffset - 1).let { if (it == -1) 0 else it + 1 }
        val endExclusive = text.indexOf('\n', endOffset).let { if (it == -1) text.length else it }
        return LineSpan(start = start, endExclusive = endExclusive.coerceAtLeast(start))
    }
}
