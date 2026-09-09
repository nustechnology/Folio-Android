package com.nus.folio.domain.util

import org.junit.Assert.assertEquals
import org.junit.Test

class NotebookInputRulesTest {

    @Test
    fun `clampContent keeps content within max length`() {
        assertEquals("short", NotebookInputRules.clampContent("short"))
        assertEquals(
            "a".repeat(NotebookInputRules.MAX_CONTENT_LENGTH),
            NotebookInputRules.clampContent("a".repeat(NotebookInputRules.MAX_CONTENT_LENGTH)),
        )
    }

    @Test
    fun `clampContent truncates content over max length`() {
        val input = "b".repeat(NotebookInputRules.MAX_CONTENT_LENGTH + 10)

        val clamped = NotebookInputRules.clampContent(input)

        assertEquals(NotebookInputRules.MAX_CONTENT_LENGTH, clamped.length)
        assertEquals("b".repeat(NotebookInputRules.MAX_CONTENT_LENGTH), clamped)
    }

    @Test
    fun `clampContent keeps empty string`() {
        assertEquals("", NotebookInputRules.clampContent(""))
    }

    @Test
    fun `html and json caps match API limits`() {
        assertEquals(1_000_000, NotebookInputRules.MAX_HTML_LENGTH)
        assertEquals(2 * 1024 * 1024, NotebookInputRules.MAX_JSON_BODY_BYTES)
    }
}
