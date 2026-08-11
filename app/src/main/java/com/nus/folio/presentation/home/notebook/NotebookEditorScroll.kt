package com.nus.folio.presentation.home.notebook

import androidx.compose.ui.text.TextLayoutResult
import kotlin.math.roundToInt

internal object NotebookEditorScroll {
    fun cursorScrollTarget(
        layout: TextLayoutResult,
        cursorOffset: Int,
        scrollOffset: Int,
        viewportHeight: Int,
        marginPx: Int,
    ): Int? {
        if (viewportHeight <= 0) return null
        val offset = cursorOffset.coerceIn(0, layout.layoutInput.text.length)
        val cursorRect = layout.getCursorRect(offset)
        return scrollTargetForCursor(
            cursorTop = cursorRect.top.roundToInt(),
            cursorBottom = cursorRect.bottom.roundToInt(),
            scrollOffset = scrollOffset,
            viewportHeight = viewportHeight,
            marginPx = marginPx,
        )
    }

    fun scrollTargetForCursor(
        cursorTop: Int,
        cursorBottom: Int,
        scrollOffset: Int,
        viewportHeight: Int,
        marginPx: Int,
    ): Int? {
        if (viewportHeight <= 0) return null
        val visibleTop = scrollOffset
        val visibleBottom = scrollOffset + viewportHeight
        return when {
            cursorBottom > visibleBottom - marginPx ->
                (cursorBottom - viewportHeight + marginPx).coerceAtLeast(0)
            cursorTop < visibleTop + marginPx ->
                (cursorTop - marginPx).coerceAtLeast(0)
            else -> null
        }
    }
}
