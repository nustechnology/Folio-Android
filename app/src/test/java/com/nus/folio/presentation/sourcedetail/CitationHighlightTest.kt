package com.nus.folio.presentation.sourcedetail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CitationHighlightTest {

    @Test
    fun `apply wraps matching passage in mark tag`() {
        val html = "<p>The imitation game is introduced here.</p>"
        val result = CitationHighlight.apply(html, "imitation game")

        assertTrue(result.contains("id=\"folio-citation-highlight\""))
        assertTrue(result.contains("class=\"folio-citation-highlight\""))
        assertTrue(result.contains(">imitation game</mark>"))
    }

    @Test
    fun `apply is case insensitive`() {
        val html = "<p>Imitation Game begins.</p>"
        val result = CitationHighlight.apply(html, "imitation game")

        assertTrue(result.contains(">Imitation Game</mark>"))
    }

    @Test
    fun `apply returns original html when passage missing`() {
        val html = "<p>No match here.</p>"
        val result = CitationHighlight.apply(html, "imitation game")

        assertEquals(html, result)
        assertFalse(result.contains("folio-citation-highlight"))
    }

    @Test
    fun `apply returns original html when passage blank`() {
        val html = "<p>Content</p>"
        assertEquals(html, CitationHighlight.apply(html, "  "))
    }

    @Test
    fun `apply prefers body text over attribute values`() {
        val html = """<div class="table-scroll"><p>See the table below.</p></div>"""
        val result = CitationHighlight.apply(html, "table")

        assertTrue(result.contains("""class="table-scroll""""))
        assertTrue(result.contains(">table</mark> below."))
        assertFalse(result.contains("""class="<mark"""))
    }

    @Test
    fun `apply ignores tag names that match the passage`() {
        val html = "<table><tr><td>cell value</td></tr></table>"
        val result = CitationHighlight.apply(html, "table")

        assertEquals(html, result)
        assertFalse(result.contains("folio-citation-highlight"))
    }

    @Test
    fun `apply highlights text after a matching attribute`() {
        val html = """<p class="content">Important content here.</p>"""
        val result = CitationHighlight.apply(html, "content")

        assertTrue(result.contains("""class="content""""))
        assertTrue(result.contains(">content</mark> here."))
    }
}
