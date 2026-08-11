package com.nus.folio.presentation.home.notebook

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotebookPrintTemplateTest {
    @Test
    fun `markdownToHtml renders standalone blockquote`() {
        val html = NotebookPrintTemplate.markdownToHtml("> A wise note")
        assertTrue(html.contains("<blockquote>A wise note</blockquote>"))
    }

    @Test
    fun `markdownToHtml renders blockquote after heading in same block`() {
        val html = NotebookPrintTemplate.markdownToHtml("## Notes\n> A wise note")
        assertTrue(html.contains("<h2>Notes</h2>"))
        assertTrue(html.contains("<blockquote>A wise note</blockquote>"))
        assertFalse(html.contains("&gt; A wise note"))
    }

    @Test
    fun `markdownToHtml renders multiline blockquote`() {
        val html = NotebookPrintTemplate.markdownToHtml("> Line one\n> Line two")
        assertTrue(html.contains("<blockquote>Line one<br/>Line two</blockquote>"))
    }
}
