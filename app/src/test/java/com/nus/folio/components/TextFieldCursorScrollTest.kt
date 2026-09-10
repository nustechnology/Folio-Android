package com.nus.folio.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TextFieldCursorScrollTest {
    @Test
    fun `scrollTargetForCursor scrolls down when cursor is below viewport`() {
        val target = TextFieldCursorScroll.scrollTargetForCursor(
            cursorTop = 780,
            cursorBottom = 804,
            scrollOffset = 0,
            viewportHeight = 800,
            marginPx = 24,
        )
        assertEquals(28, target)
    }

    @Test
    fun `scrollTargetForCursor scrolls up when cursor is above viewport`() {
        val target = TextFieldCursorScroll.scrollTargetForCursor(
            cursorTop = 12,
            cursorBottom = 36,
            scrollOffset = 200,
            viewportHeight = 800,
            marginPx = 24,
        )
        assertEquals(0, target)
    }

    @Test
    fun `scrollTargetForCursor returns null when cursor stays visible`() {
        assertNull(
            TextFieldCursorScroll.scrollTargetForCursor(
                cursorTop = 400,
                cursorBottom = 424,
                scrollOffset = 100,
                viewportHeight = 800,
                marginPx = 24,
            ),
        )
    }
}
