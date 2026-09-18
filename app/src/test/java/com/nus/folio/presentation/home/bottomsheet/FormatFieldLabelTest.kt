package com.nus.folio.presentation.home.bottomsheet

import androidx.compose.ui.text.SpanStyle
import com.nus.folio.ui.theme.HomeStatusFailedText
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatFieldLabelTest {

    @Test
    fun `formatFieldLabel returns unchanged text when no asterisk present`() {
        val result = formatFieldLabel("Title")
        assertEquals("Title", result.text)
        assertEquals(0, result.spanStyles.size)
    }

    @Test
    fun `formatFieldLabel applies red color style to asterisk when present`() {
        val result = formatFieldLabel("Content *")
        assertEquals("Content *", result.text)
        assertEquals(1, result.spanStyles.size)
        val styleRange = result.spanStyles[0]
        assertEquals(8, styleRange.start)
        assertEquals(9, styleRange.end)
        assertEquals(SpanStyle(color = HomeStatusFailedText), styleRange.item)
    }
}
