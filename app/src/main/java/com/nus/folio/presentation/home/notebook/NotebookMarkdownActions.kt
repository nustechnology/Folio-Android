package com.nus.folio.presentation.home.notebook

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/** Which markdown toolbar options apply at the current caret / selection. */
internal data class NotebookToolbarMarks(
    val bold: Boolean = false,
    val italic: Boolean = false,
    val headingLevel: Int? = null,
    val bulletList: Boolean = false,
    val orderedList: Boolean = false,
    val blockquote: Boolean = false,
    val link: Boolean = false,
)

internal object NotebookMarkdownActions {
    private const val MAX_UNDO = 50
    private val LINK_PATTERN = Regex("\\[([^\\]]+)]\\(([^)]+)\\)")
    private const val LINK_URL_PLACEHOLDER = "url"

    fun toggleBold(value: TextFieldValue): TextFieldValue =
        wrapSelection(value, prefix = "**", suffix = "**")

    fun toggleItalic(value: TextFieldValue): TextFieldValue =
        wrapSelection(value, prefix = "_", suffix = "_")

    /**
     * Inserts or removes a markdown link around the caret / selection.
     * With a selection: `[selection](url)` and selects the URL placeholder.
     * Inside an existing link: unwraps to the label only.
     */
    fun toggleLink(value: TextFieldValue): TextFieldValue {
        val text = value.text
        val start = value.selection.min.coerceIn(0, text.length)
        val end = value.selection.max.coerceIn(0, text.length)
        findEnclosingLink(text, start, end)?.let { link ->
            val label = link.groups[1]?.value.orEmpty()
            val newText = text.replaceRange(link.range.first, link.range.last + 1, label)
            val labelStart = link.range.first
            return value.copy(
                text = newText,
                selection = TextRange(labelStart, labelStart + label.length),
            )
        }
        val label = if (start != end) {
            text.substring(start, end)
        } else {
            "text"
        }
        val inserted = "[$label]($LINK_URL_PLACEHOLDER)"
        val newText = text.replaceRange(start, end, inserted)
        val urlStart = start + 1 + label.length + 2
        return value.copy(
            text = newText,
            selection = TextRange(urlStart, urlStart + LINK_URL_PLACEHOLDER.length),
        )
    }

    /**
     * Resolves which toolbar options are active for [value]'s selection so the toolbar
     * can highlight matching controls.
     */
    fun activeMarks(value: TextFieldValue): NotebookToolbarMarks {
        val text = value.text
        if (text.isEmpty()) return NotebookToolbarMarks()
        val start = value.selection.min.coerceIn(0, text.length)
        val end = value.selection.max.coerceIn(0, text.length)

        val span = activeLineSpan(text, value.selection)
        val lines = text.substring(span.start, span.endExclusive).split('\n')
        val contentLines = lines.filter { it.isNotBlank() }.ifEmpty { lines }

        val headingLevels = contentLines.map { line ->
            HEADING_PREFIX.find(line.trimStart())?.groupValues?.get(1)?.length
        }
        val headingLevel = headingLevels.firstOrNull()?.takeIf { level ->
            headingLevels.all { it == level }
        }

        val bulletList = contentLines.isNotEmpty() && contentLines.all { it.startsWith("- ") }
        val orderedList = contentLines.isNotEmpty() &&
            contentLines.all { NUMBERED_PREFIX.containsMatchIn(it) }
        val blockquote = contentLines.isNotEmpty() && contentLines.all { it.startsWith(">") }

        return NotebookToolbarMarks(
            bold = isInlineMarkActive(text, start, end, prefix = "**", suffix = "**"),
            italic = isInlineMarkActive(text, start, end, prefix = "_", suffix = "_"),
            headingLevel = headingLevel,
            bulletList = bulletList,
            orderedList = orderedList,
            blockquote = blockquote,
            link = findEnclosingLink(text, start, end) != null,
        )
    }

    private fun findEnclosingLink(text: String, start: Int, end: Int): MatchResult? {
        LINK_PATTERN.findAll(text).forEach { match ->
            val linkStart = match.range.first
            val linkEndExclusive = match.range.last + 1
            if (start >= linkStart && end <= linkEndExclusive) return match
        }
        return null
    }

    fun setHeading(value: TextFieldValue, level: Int): TextFieldValue {
        val clampedLevel = level.coerceIn(1, 3)
        val prefix = "#".repeat(clampedLevel) + " "
        return transformActiveLines(value) { line ->
            val trimmed = line.trimStart()
            val currentLevel = HEADING_PREFIX.find(trimmed)?.groupValues?.get(1)?.length
            val content = stripLineMarkers(line)
            if (currentLevel == clampedLevel) content else prefix + content
        }
    }

    fun toggleBulletList(value: TextFieldValue): TextFieldValue =
        toggleLinePrefix(value, marker = "- ")

    fun toggleOrderedList(value: TextFieldValue): TextFieldValue =
        toggleNumberedList(value)

    fun toggleBlockquote(value: TextFieldValue): TextFieldValue =
        toggleLinePrefix(value, marker = "> ")

    /**
     * Rewrites editor edits that would expose hidden markdown markers (Enter / Delete /
     * Backspace at visual boundaries). Returns [next] unchanged when no rewrite is needed.
     */
    fun resolveMarkdownEdit(previous: TextFieldValue, next: TextFieldValue): TextFieldValue =
        continueListOnEnter(previous, next)
            ?: handleMarkerAwareDelete(previous, next)
            ?: next

    /**
     * When the user presses Enter after inserting a single newline, continue or exit
     * ordered / bullet list / blockquote lines, or rebalance inline markers / headings so
     * hidden markdown does not become visible. Returns null if the edit needs no rewrite.
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
                exitPrefixLine(previous, lineStart, lineEnd)
            } else {
                continueNumberedList(previous, lineStart, lineEnd, cursor, match)
            }
        }

        BULLET_LINE.matchEntire(line)?.let { match ->
            val content = match.groupValues[1]
            return if (content.isEmpty()) {
                exitPrefixLine(previous, lineStart, lineEnd)
            } else {
                continueBulletList(previous, lineStart, lineEnd, cursor, match)
            }
        }

        QUOTE_LINE.matchEntire(line)?.let { match ->
            val content = match.groupValues[1]
            return if (content.isEmpty()) {
                // Second Enter on an empty `>` / `> ` line leaves the quote.
                exitPrefixLine(previous, lineStart, lineEnd)
            } else {
                // First Enter continues the quote onto the next line.
                continueBlockquote(previous, lineStart, lineEnd, cursor, match)
            }
        }

        return continueInlineMarksOnEnter(previous, lineStart, lineEnd, cursor)
    }

    /**
     * Rebalances heading prefixes and `**` / `_` when Enter splits a normal line.
     * VisualTransformation parks the caret after hidden openers / before closers, so a
     * raw newline turns `**Hello**` into `**` + `Hello**` and the markers become visible.
     */
    private fun continueInlineMarksOnEnter(
        previous: TextFieldValue,
        lineStart: Int,
        lineEnd: Int,
        cursor: Int,
    ): TextFieldValue? {
        val before = previous.text.substring(lineStart, cursor)
        val after = previous.text.substring(cursor, lineEnd)
        val (left, right) = splitKeepingInlineMarkers(before, after)
        if (left == before && right == after) return null

        val newText = buildString {
            append(previous.text.substring(0, lineStart))
            append(left)
            append('\n')
            append(right)
            append(previous.text.substring(lineEnd))
        }
        val newCursor = lineStart + left.length + 1
        return TextFieldValue(newText, TextRange(newCursor))
    }

    /**
     * When Backspace / Delete would remove a hidden marker char (caret parked after openers
     * or before closers by VisualTransformation), delete the adjacent visible character
     * instead — or merge / unwrap so markers stay balanced and stay hidden.
     */
    private fun handleMarkerAwareDelete(
        previous: TextFieldValue,
        next: TextFieldValue,
    ): TextFieldValue? {
        if (!previous.selection.collapsed || !next.selection.collapsed) return null
        if (previous.text.length - next.text.length != 1) return null

        val cursor = previous.selection.start
        val isBackspace = cursor > 0 &&
            next.selection.start == cursor - 1 &&
            next.text == previous.text.removeRange(cursor - 1, cursor)
        val isForwardDelete = cursor < previous.text.length &&
            next.selection.start == cursor &&
            next.text == previous.text.removeRange(cursor, cursor + 1)
        if (!isBackspace && !isForwardDelete) return null

        val deleteIndex = if (isBackspace) cursor - 1 else cursor
        val visualized = NotebookMarkdownVisuals.visualize(previous.text)
        if (isOriginalCharVisible(visualized, previous.text.length, deleteIndex)) return null

        val mapping = visualized.mapping
        val visualCursor = mapping.originalToTransformed(cursor)

        if (isBackspace) {
            if (visualCursor > 0) {
                val origToDelete = mapping.transformedToOriginal(visualCursor - 1)
                return TextFieldValue(
                    text = previous.text.removeRange(origToDelete, origToDelete + 1),
                    selection = TextRange(origToDelete),
                )
            }
            val lineStart = previous.text.lastIndexOf('\n', cursor - 1).let { if (it == -1) 0 else it + 1 }
            if (lineStart > 0) {
                return TextFieldValue(
                    text = previous.text.removeRange(lineStart - 1, lineStart),
                    selection = TextRange(lineStart - 1),
                )
            }
            return unwrapLeadingHiddenMarks(previous, cursor) ?: previous
        }

        // Forward delete into a hidden marker: skip trailing/leading hidden markers
        // and delete the next visible character. Do not trust transformedToOriginal
        // at the visual-end boundary — that offset maps to the first closer itself.
        var index = cursor
        while (
            index < previous.text.length &&
            !isOriginalCharVisible(visualized, previous.text.length, index)
        ) {
            index++
        }
        if (index < previous.text.length) {
            return TextFieldValue(
                text = previous.text.removeRange(index, index + 1),
                selection = TextRange(cursor),
            )
        }
        // At visual end with only trailing closers left — keep markers intact.
        return previous
    }

    private fun isOriginalCharVisible(
        visualized: VisualizedMarkdown,
        originalLength: Int,
        index: Int,
    ): Boolean {
        if (index < 0 || index >= originalLength) return false
        val transformedLength = visualized.text.text.length
        val t = visualized.mapping.originalToTransformed(index)
        return t < transformedLength && visualized.mapping.transformedToOriginal(t) == index
    }

    private fun unwrapLeadingHiddenMarks(previous: TextFieldValue, cursor: Int): TextFieldValue? {
        val text = previous.text
        val lineStart = text.lastIndexOf('\n', cursor - 1).let { if (it == -1) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', cursor).let { if (it == -1) text.length else it }
        val line = text.substring(lineStart, lineEnd)
        val heading = HEADING_PREFIX.find(line)
        val contentStart = if (heading != null) lineStart + heading.value.length else lineStart
        if (cursor < contentStart) return null

        val openers = text.substring(contentStart, cursor)
        if (openers.isNotEmpty() && !isOnlyInlineOpeners(openers)) return null
        if (openers.isEmpty() && heading == null) return null

        val after = text.substring(cursor, lineEnd)
        val closerStr = openInlineMarkerStack(openers).asReversed().joinToString("")
        if (openers.isNotEmpty() && !after.endsWith(closerStr)) return null
        if (openers.isNotEmpty() && openInlineMarkerStack(openers + after).isNotEmpty()) return null

        val inner = if (openers.isEmpty()) after else after.removeSuffix(closerStr)
        val newText = text.substring(0, lineStart) + inner + text.substring(lineEnd)
        return TextFieldValue(newText, TextRange(lineStart))
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
        val start = value.selection.min.coerceIn(0, text.length)
        val end = value.selection.max.coerceIn(0, text.length)
        if (start != end) {
            unwrapSelection(text, start, end, prefix, suffix)?.let { (newText, range) ->
                return value.copy(text = newText, selection = range)
            }
            val selected = text.substring(start, end)
            val wrapped = prefix + selected + suffix
            val newText = text.replaceRange(start, end, wrapped)
            val innerStart = start + prefix.length
            return value.copy(text = newText, selection = TextRange(innerStart, innerStart + selected.length))
        }
        val insert = prefix + suffix
        val newText = text.replaceRange(start, end, insert)
        val cursor = start + prefix.length
        return value.copy(text = newText, selection = TextRange(cursor))
    }

    private fun unwrapSelection(
        text: String,
        start: Int,
        end: Int,
        prefix: String,
        suffix: String,
    ): Pair<String, TextRange>? {
        val selected = text.substring(start, end)
        if (selected.startsWith(prefix) && selected.endsWith(suffix) &&
            selected.length >= prefix.length + suffix.length
        ) {
            val inner = selected.removePrefix(prefix).removeSuffix(suffix)
            val newText = text.replaceRange(start, end, inner)
            return newText to TextRange(start, start + inner.length)
        }
        findEnclosingWrap(text, start, end, prefix, suffix)?.let { (wrapStart, wrapEnd) ->
            val inner = text.substring(wrapStart + prefix.length, wrapEnd - suffix.length)
            val newText = text.replaceRange(wrapStart, wrapEnd, inner)
            val newStart = (start - prefix.length).coerceAtLeast(wrapStart)
            val newEnd = (end - prefix.length).coerceAtMost(wrapStart + inner.length)
            return newText to TextRange(newStart, newEnd)
        }
        return null
    }

    /**
     * Finds [prefix]…[suffix] wrapping [start, end], either immediately adjacent, with the
     * closing/opening marker already inside the selection (common after VisualTransformation
     * round-trips), or with one intervening layer of the other inline marker (`**` ↔ `_`).
     */
    private fun findEnclosingWrap(
        text: String,
        start: Int,
        end: Int,
        prefix: String,
        suffix: String,
    ): Pair<Int, Int>? {
        // Selection already includes both markers: "_Hello_" / "**Hello**"
        if (end - start >= prefix.length + suffix.length &&
            text.startsWith(prefix, start) &&
            text.startsWith(suffix, end - suffix.length)
        ) {
            return start to end
        }
        // Selection includes only the closing marker: "Hello_" with "_" before it
        if (end - start >= suffix.length &&
            text.startsWith(suffix, end - suffix.length) &&
            start >= prefix.length &&
            text.startsWith(prefix, start - prefix.length)
        ) {
            return (start - prefix.length) to end
        }
        // Selection includes only the opening marker: "_Hello" with "_" after it
        if (end - start >= prefix.length &&
            text.startsWith(prefix, start) &&
            end + suffix.length <= text.length &&
            text.startsWith(suffix, end)
        ) {
            return start to (end + suffix.length)
        }
        if (start >= prefix.length && end + suffix.length <= text.length &&
            text.startsWith(prefix, start - prefix.length) &&
            text.startsWith(suffix, end)
        ) {
            return (start - prefix.length) to (end + suffix.length)
        }
        val otherPrefix = when (prefix) {
            "_" -> "**"
            "**" -> "_"
            else -> return null
        }
        val otherSuffix = otherPrefix
        val nested = prefix.length + otherPrefix.length
        if (start >= nested && end + otherSuffix.length + suffix.length <= text.length) {
            val wrapStart = start - nested
            val afterInner = end + otherSuffix.length
            if (text.startsWith(prefix, wrapStart) &&
                text.startsWith(otherPrefix, wrapStart + prefix.length) &&
                text.startsWith(otherSuffix, end) &&
                text.startsWith(suffix, afterInner)
            ) {
                return wrapStart to (afterInner + suffix.length)
            }
        }
        return null
    }

    private fun isInlineMarkActive(
        text: String,
        start: Int,
        end: Int,
        prefix: String,
        suffix: String,
    ): Boolean {
        if (findEnclosingWrap(text, start, end, prefix, suffix) != null) return true
        // Caret or selection fully inside a mark on the same line.
        return if (prefix == "_") {
            areOffsetsInsideItalic(text, start, end)
        } else {
            isInsideBoldMark(text, start) &&
                (start == end || isInsideBoldMark(text, end))
        }
    }

    /**
     * Italic detection walks each active line once (O(n)) instead of calling
     * [findItalicCloseMarker] from every underscore (O(n²) on dense `_` lines).
     */
    private fun areOffsetsInsideItalic(text: String, start: Int, end: Int): Boolean {
        if (text.isEmpty()) return false
        val startProbe = start.coerceIn(0, text.length)
        val endProbe = end.coerceIn(0, text.length)
        val startLineStart = lineStartOf(text, startProbe)
        val startLineEnd = lineEndOf(text, startProbe)
        if (startProbe == endProbe) {
            return isOffsetInsideItalic(
                italicInsideRanges(text, startLineStart, startLineEnd),
                startProbe,
            )
        }
        val endLineStart = lineStartOf(text, endProbe)
        val endLineEnd = lineEndOf(text, endProbe)
        if (startLineStart == endLineStart) {
            val ranges = italicInsideRanges(text, startLineStart, startLineEnd)
            return isOffsetInsideItalic(ranges, startProbe) &&
                isOffsetInsideItalic(ranges, endProbe)
        }
        return isOffsetInsideItalic(
            italicInsideRanges(text, startLineStart, startLineEnd),
            startProbe,
        ) && isOffsetInsideItalic(
            italicInsideRanges(text, endLineStart, endLineEnd),
            endProbe,
        )
    }

    private fun isOffsetInsideItalic(ranges: List<IntRange>, probe: Int): Boolean =
        ranges.any { probe in it }

    /**
     * Builds inclusive ranges of offsets that count as "inside" italic on [lineStart, lineEnd),
     * matching [isItalicOpenMarker] / [findItalicCloseMarker] (open < offset ≤ close).
     */
    private fun italicInsideRanges(text: String, lineStart: Int, lineEnd: Int): List<IntRange> {
        if (lineStart >= lineEnd) return emptyList()
        val closerPositions = ArrayList<Int>()
        for (i in lineStart until lineEnd) {
            if (text[i] != '_') continue
            val next = i + 1
            if (next >= lineEnd || !text[next].isMdWord()) {
                closerPositions.add(i)
            }
        }
        if (closerPositions.isEmpty()) return emptyList()

        val ranges = ArrayList<IntRange>()
        var closerIdx = 0
        var index = lineStart
        while (index < lineEnd) {
            val canOpen = text[index] == '_' && (index == 0 || !text[index - 1].isMdWord())
            if (canOpen) {
                while (closerIdx < closerPositions.size && closerPositions[closerIdx] <= index) {
                    closerIdx++
                }
                if (closerIdx < closerPositions.size) {
                    val close = closerPositions[closerIdx]
                    if (close > index) {
                        ranges.add((index + 1)..close)
                    }
                    index = close + 1
                    closerIdx++
                    continue
                }
            }
            index++
        }
        return ranges
    }

    private fun lineStartOf(text: String, probe: Int): Int =
        text.lastIndexOf('\n', (probe - 1).coerceAtLeast(-1)).let { if (it == -1) 0 else it + 1 }

    private fun lineEndOf(text: String, probe: Int): Int =
        text.indexOf('\n', probe).let { if (it == -1) text.length else it }

    private fun isInsideBoldMark(text: String, offset: Int): Boolean {
        if (text.isEmpty()) return false
        val probe = offset.coerceIn(0, text.length)
        val lineStart = lineStartOf(text, probe)
        var open = false
        var index = lineStart
        while (index < probe) {
            if (text.startsWith("**", index)) {
                open = !open
                index += 2
            } else {
                index++
            }
        }
        return open
    }

    private fun transformActiveLines(
        value: TextFieldValue,
        transform: (String) -> String,
    ): TextFieldValue {
        val text = value.text
        val span = activeLineSpan(text, value.selection)
        val segment = text.substring(span.start, span.endExclusive)
        val lines = segment.split('\n')
        val transformedLines = lines.map(transform)
        val transformed = transformedLines.joinToString("\n")
        val newText = text.replaceRange(span.start, span.endExclusive, transformed)
        val newSelection = mapSelectionThroughLineTransform(
            spanStart = span.start,
            oldLines = lines,
            newLines = transformedLines,
            selection = value.selection,
            newTextLength = newText.length,
        )
        return value.copy(text = newText, selection = newSelection)
    }

    private fun toggleLinePrefix(value: TextFieldValue, marker: String): TextFieldValue {
        val text = value.text
        val span = activeLineSpan(text, value.selection)
        val segment = text.substring(span.start, span.endExclusive)
        val lines = segment.split('\n')
        // Blank-only content is not "already prefixed" — insert the marker.
        val hasContent = lines.any { it.isNotBlank() }
        val allPrefixed = hasContent && lines.all { it.startsWith(marker) || it.isBlank() }
        val transformedLines = lines.map { line ->
            when {
                allPrefixed && line.startsWith(marker) -> line.removePrefix(marker)
                allPrefixed && line.isBlank() -> line
                !allPrefixed -> marker + stripLineMarkers(line)
                else -> line
            }
        }
        val transformed = transformedLines.joinToString("\n")
        val newText = text.replaceRange(span.start, span.endExclusive, transformed)
        val newSelection = mapSelectionThroughLineTransform(
            spanStart = span.start,
            oldLines = lines,
            newLines = transformedLines,
            selection = value.selection,
            newTextLength = newText.length,
        )
        return value.copy(text = newText, selection = newSelection)
    }

    private fun toggleNumberedList(value: TextFieldValue): TextFieldValue {
        val text = value.text
        val span = activeLineSpan(text, value.selection)
        val segment = text.substring(span.start, span.endExclusive)
        val lines = segment.split('\n')
        val hasContent = lines.any { it.isNotBlank() }
        val allNumbered = hasContent && lines.all { NUMBERED_PREFIX.containsMatchIn(it) || it.isBlank() }
        val transformedLines = buildList {
            var index = 1
            for (line in lines) {
                add(
                    when {
                        allNumbered && line.isBlank() -> line
                        allNumbered -> line.replace(NUMBERED_PREFIX, "")
                        else -> {
                            val numbered = "$index. ${stripLineMarkers(line)}"
                            index++
                            numbered
                        }
                    },
                )
            }
        }
        val transformed = transformedLines.joinToString("\n")
        val newText = text.replaceRange(span.start, span.endExclusive, transformed)
        val newSelection = mapSelectionThroughLineTransform(
            spanStart = span.start,
            oldLines = lines,
            newLines = transformedLines,
            selection = value.selection,
            newTextLength = newText.length,
        )
        return value.copy(text = newText, selection = newSelection)
    }

    /** Removes heading / list / quote markers so line formats replace each other instead of stacking. */
    private fun stripLineMarkers(line: String): String {
        var result = line.trimStart()
        result = result.replace(HEADING_PREFIX, "")
        result = when {
            result.startsWith("> ") -> result.removePrefix("> ")
            result.startsWith(">") -> result.removePrefix(">").trimStart()
            result.startsWith("- ") -> result.removePrefix("- ")
            else -> result.replace(NUMBERED_PREFIX, "")
        }
        return result
    }

    /**
     * Keeps a non-collapsed selection usable for inline toggles after line-prefix edits
     * (heading / list / quote). Without this, H1 collapses the caret and the next Italic
     * click inserts `__` instead of unwrapping.
     */
    private fun mapSelectionThroughLineTransform(
        spanStart: Int,
        oldLines: List<String>,
        newLines: List<String>,
        selection: TextRange,
        newTextLength: Int,
    ): TextRange {
        fun mapOffset(absolute: Int): Int {
            val relative = (absolute - spanStart).coerceAtLeast(0)
            var oldPos = 0
            var newPos = 0
            for (i in oldLines.indices) {
                val oldLine = oldLines[i]
                val newLine = newLines.getOrElse(i) { oldLine }
                val oldLineEnd = oldPos + oldLine.length
                if (relative <= oldLineEnd) {
                    val inLine = mapLineOffset(oldLine, newLine, relative - oldPos)
                    return (spanStart + newPos + inLine).coerceIn(0, newTextLength)
                }
                oldPos = oldLineEnd + 1
                newPos += newLine.length + 1
            }
            return (spanStart + newPos).coerceIn(0, newTextLength)
        }
        val start = mapOffset(selection.min)
        val end = mapOffset(selection.max)
        return TextRange(start, end)
    }

    /** Maps an offset through a prefix/suffix line edit by anchoring on the shared content suffix. */
    private fun mapLineOffset(oldLine: String, newLine: String, offset: Int): Int {
        var shared = 0
        val maxShared = minOf(oldLine.length, newLine.length)
        while (shared < maxShared &&
            oldLine[oldLine.length - 1 - shared] == newLine[newLine.length - 1 - shared]
        ) {
            shared++
        }
        val oldContentStart = oldLine.length - shared
        val newContentStart = newLine.length - shared
        val clamped = offset.coerceIn(0, oldLine.length)
        return if (clamped < oldContentStart) {
            clamped.coerceAtMost(newContentStart)
        } else {
            newContentStart + (clamped - oldContentStart)
        }
    }

    private val NUMBERED_LINE = Regex("^(\\d+)\\.\\s(.*)$")
    private val NUMBERED_PREFIX = Regex("^\\d+\\.\\s+")
    private val BULLET_LINE = Regex("^- (.*)$")
    /** Blockquote line: `>`, `> `, or `> text` / `>text`. */
    private val QUOTE_LINE = Regex("^>\\s*(.*)$")
    private val HEADING_PREFIX = Regex("^(#{1,6})\\s+")

    private fun isSingleNewlineInsert(previous: TextFieldValue, next: TextFieldValue): Boolean {
        val cursor = previous.selection.start
        if (next.text.length != previous.text.length + 1) return false
        if (next.text.substring(0, cursor) != previous.text.substring(0, cursor)) return false
        if (next.text[cursor] != '\n') return false
        return next.text.substring(cursor + 1) == previous.text.substring(cursor)
    }

    /**
     * Removes an empty list/quote line and leaves a blank line in its place.
     * Uses [lineEnd] (not caret) so a caret parked on `>` still fully exits.
     */
    private fun exitPrefixLine(
        previous: TextFieldValue,
        lineStart: Int,
        lineEnd: Int,
    ): TextFieldValue {
        val afterLine = if (lineEnd < previous.text.length && previous.text[lineEnd] == '\n') {
            lineEnd + 1
        } else {
            lineEnd
        }
        val newText = buildString {
            append(previous.text.substring(0, lineStart))
            append('\n')
            append(previous.text.substring(afterLine))
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
        val splitAt = cursor.coerceIn(contentStart, lineEnd)
        val rawBefore = previous.text.substring(contentStart, splitAt)
        val rawAfter = previous.text.substring(splitAt, lineEnd).trimStart()
        val (beforeContent, afterContent) = splitKeepingInlineMarkers(rawBefore, rawAfter)

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
        val contentStart = lineStart + 2
        val splitAt = cursor.coerceIn(contentStart, lineEnd)
        val rawBefore = previous.text.substring(contentStart, splitAt)
        val rawAfter = previous.text.substring(splitAt, lineEnd).trimStart()
        val (beforeContent, afterContent) = splitKeepingInlineMarkers(rawBefore, rawAfter)

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

    private fun continueBlockquote(
        previous: TextFieldValue,
        lineStart: Int,
        lineEnd: Int,
        cursor: Int,
        match: MatchResult,
    ): TextFieldValue {
        val content = match.groupValues[1]
        val contentStart = lineStart + (match.value.length - content.length)
        val splitAt = cursor.coerceIn(contentStart, lineEnd)
        val rawBefore = previous.text.substring(contentStart, splitAt)
        val rawAfter = previous.text.substring(splitAt, lineEnd).trimStart()
        val (beforeContent, afterContent) = splitKeepingInlineMarkers(rawBefore, rawAfter)

        val currentLine = "> $beforeContent"
        val nextPrefix = "> "
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

    /**
     * Keeps `**` / `_` balanced across an Enter split. VisualTransformation parks the caret
     * at the start of trailing closers / after leading openers, so a naive split turns
     * `**Hello**` into `**` / `Hello**` and the markers become visible.
     */
    private fun splitKeepingInlineMarkers(before: String, after: String): Pair<String, String> {
        if (after.isEmpty()) return before to after
        if (TRAILING_CLOSE_MARKERS.matches(after)) {
            return (before + after) to ""
        }
        // Enter at the visual start of a heading / marked span: before is only a heading
        // prefix and/or openers, and after already closes them — move the whole span down.
        val headingPrefix = HEADING_PREFIX.find(before)?.value.orEmpty()
        val beforeWithoutHeading = if (headingPrefix.isNotEmpty()) {
            before.removePrefix(headingPrefix)
        } else {
            before
        }
        if (
            (headingPrefix.isNotEmpty() || beforeWithoutHeading.isNotEmpty()) &&
            (beforeWithoutHeading.isEmpty() || isOnlyInlineOpeners(beforeWithoutHeading)) &&
            openInlineMarkerStack(beforeWithoutHeading + after).isEmpty()
        ) {
            return "" to (before + after)
        }
        val open = openInlineMarkerStack(before)
        if (open.isEmpty()) return before to after
        val closers = open.asReversed().joinToString("")
        val openers = open.joinToString("")
        return (before + closers) to (openers + after)
    }

    private fun isOnlyInlineOpeners(text: String): Boolean {
        if (text.isEmpty()) return false
        var index = 0
        while (index < text.length) {
            when {
                text.startsWith("**", index) -> index += 2
                text[index] == '_' -> index++
                else -> return false
            }
        }
        return openInlineMarkerStack(text).isNotEmpty()
    }

    private fun openInlineMarkerStack(text: String): List<String> {
        val stack = mutableListOf<String>()
        var index = 0
        while (index < text.length) {
            when {
                text.startsWith("**", index) -> {
                    if (stack.lastOrNull() == "**") stack.removeAt(stack.lastIndex) else stack += "**"
                    index += 2
                }
                text[index] == '_' -> {
                    if (stack.lastOrNull() == "_") stack.removeAt(stack.lastIndex) else stack += "_"
                    index++
                }
                else -> index++
            }
        }
        return stack
    }

    private val TRAILING_CLOSE_MARKERS = Regex("^(?:\\*\\*|_)+\\s*$")

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
