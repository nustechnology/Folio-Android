package com.nus.folio.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Keeps the caret visible in a vertically scrollable multiline text field
 * when soft-wrapping or new lines push content past the viewport.
 */
internal object TextFieldCursorScroll {
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

internal class TextFieldCursorScroller(
    private val scrollState: ScrollState,
    private val viewportHeight: () -> Int,
    private val setViewportHeight: (Int) -> Unit,
    private val marginPx: Int,
    private val scope: CoroutineScope,
) {
    val scrollModifier: Modifier
        get() = Modifier
            .onSizeChanged { setViewportHeight(it.height) }
            .verticalScroll(scrollState)

    fun onTextLayout(cursorOffset: Int): (TextLayoutResult) -> Unit = { layout ->
        val target = TextFieldCursorScroll.cursorScrollTarget(
            layout = layout,
            cursorOffset = cursorOffset,
            scrollOffset = scrollState.value,
            viewportHeight = viewportHeight(),
            marginPx = marginPx,
        )
        if (target != null && target != scrollState.value) {
            scope.launch {
                scrollState.animateScrollTo(target)
            }
        }
    }
}

@Composable
internal fun rememberTextFieldCursorScroller(
    scrollState: ScrollState = rememberScrollState(),
    margin: Dp = 8.dp,
): TextFieldCursorScroller {
    val viewportHeightState = remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val marginPx = with(LocalDensity.current) { margin.roundToPx() }
    return remember(scrollState, marginPx, scope) {
        TextFieldCursorScroller(
            scrollState = scrollState,
            viewportHeight = { viewportHeightState.intValue },
            setViewportHeight = { viewportHeightState.intValue = it },
            marginPx = marginPx,
            scope = scope,
        )
    }
}
