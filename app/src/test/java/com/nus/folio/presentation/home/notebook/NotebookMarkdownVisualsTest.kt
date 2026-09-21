package com.nus.folio.presentation.home.notebook

import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotebookMarkdownVisualsTest {
    @Test
    fun `visualize hides bold markers and bolds the inner text`() {
        val visualized = NotebookMarkdownVisuals.visualize("**Hello**")
        assertEquals("Hello", visualized.text.text)
        val bold = visualized.text.spanStyles.filter { it.item.fontWeight == FontWeight.Bold }
        assertEquals(1, bold.size)
        assertEquals(0, bold.first().start)
        assertEquals(5, bold.first().end)
        assertEquals(0, visualized.mapping.originalToTransformed(2))
        assertEquals(2, visualized.mapping.transformedToOriginal(0))
        // End caret after visible text must round-trip before closing markers (not after).
        assertEquals(5, visualized.mapping.originalToTransformed(7))
        assertEquals(7, visualized.mapping.transformedToOriginal(5))
        assertEquals(
            7,
            visualized.mapping.transformedToOriginal(visualized.mapping.originalToTransformed(7)),
        )
    }

    @Test
    fun `transformedToOriginal at end of bold span sits before closing markers`() {
        val visualized = NotebookMarkdownVisuals.visualize("**bold**")
        assertEquals("bold", visualized.text.text)
        assertEquals(2, visualized.mapping.transformedToOriginal(0))
        assertEquals(5, visualized.mapping.transformedToOriginal(3))
        // Visual end (after 'd') must map to index of first closing '*', not past both.
        assertEquals(6, visualized.mapping.transformedToOriginal(4))
        assertEquals(
            6,
            visualized.mapping.transformedToOriginal(visualized.mapping.originalToTransformed(6)),
        )
    }

    @Test
    fun `visualize hides italic markers`() {
        val visualized = NotebookMarkdownVisuals.visualize("_Hello_")
        assertEquals("Hello", visualized.text.text)
        val italic = visualized.text.spanStyles.filter { it.item.fontStyle == FontStyle.Italic }
        assertEquals(1, italic.size)
        assertEquals(0, italic.first().start)
        assertEquals(5, italic.first().end)
        assertEquals(1, visualized.mapping.transformedToOriginal(0))
        assertEquals(6, visualized.mapping.transformedToOriginal(5))
        assertEquals(
            6,
            visualized.mapping.transformedToOriginal(visualized.mapping.originalToTransformed(6)),
        )
    }

    @Test
    fun `transformedToOriginal at end of italic span sits before closing marker`() {
        val visualized = NotebookMarkdownVisuals.visualize("_bold_")
        assertEquals("bold", visualized.text.text)
        assertEquals(1, visualized.mapping.transformedToOriginal(0))
        assertEquals(4, visualized.mapping.transformedToOriginal(3))
        assertEquals(5, visualized.mapping.transformedToOriginal(4))
        assertEquals(
            5,
            visualized.mapping.transformedToOriginal(visualized.mapping.originalToTransformed(5)),
        )
    }

    @Test
    fun `visualize hides heading prefix and styles the title`() {
        val visualized = NotebookMarkdownVisuals.visualize("# Title")
        assertEquals("Title", visualized.text.text)
        val heading = visualized.text.spanStyles.filter { it.item.fontWeight == FontWeight.SemiBold }
        assertTrue(heading.any { it.start == 0 && it.end == 5 })
        assertEquals(0, visualized.mapping.originalToTransformed(2))
        assertEquals(2, visualized.mapping.transformedToOriginal(0))
    }

    @Test
    fun `visualize keeps blockquote prefix visible`() {
        val visualized = NotebookMarkdownVisuals.visualize("> A wise note")
        assertEquals("> A wise note", visualized.text.text)
        assertTrue(visualized.text.spanStyles.any { it.item.fontStyle == FontStyle.Italic })
    }

    @Test
    fun `visualize keeps list markers visible`() {
        val visualized = NotebookMarkdownVisuals.visualize("- item\n1. next")
        assertEquals("- item\n1. next", visualized.text.text)
    }

    @Test
    fun `visualize keeps unclosed markers`() {
        val visualized = NotebookMarkdownVisuals.visualize("**Hello")
        assertEquals("**Hello", visualized.text.text)
        assertTrue(visualized.text.spanStyles.none { it.item.fontWeight == FontWeight.Bold })
    }

    @Test
    fun `visualize does not italicize underscores inside words`() {
        val visualized = NotebookMarkdownVisuals.visualize("space_id and _ok_")
        assertEquals("space_id and ok", visualized.text.text)
        val italic = visualized.text.spanStyles.filter { it.item.fontStyle == FontStyle.Italic }
        assertEquals(1, italic.size)
        assertEquals(visualized.text.text.indexOf("ok"), italic.first().start)
    }

    @Test
    fun `visualize styles bold inside a heading`() {
        val visualized = NotebookMarkdownVisuals.visualize("# **Title**")
        assertEquals("Title", visualized.text.text)
        assertTrue(visualized.text.spanStyles.any { it.item.fontWeight == FontWeight.Bold })
        assertTrue(visualized.text.spanStyles.any { it.item.fontFamily != null })
    }

    @Test
    fun `filter matches visualize`() {
        val markdown = "# Title\n**bold** and _italic_\n- item"
        val visualized = NotebookMarkdownVisuals.visualize(markdown)
        val filtered = NotebookMarkdownVisualTransformation().filter(
            androidx.compose.ui.text.AnnotatedString(markdown),
        )
        assertEquals(visualized.text.text, filtered.text.text)
        assertEquals("Title\nbold and italic\n- item", visualized.text.text)
    }

    @Test
    fun `visualize never throws on malformed markdown`() {
        val samples = listOf(
            "**",
            "****",
            "_",
            "[]()",
            "[a](",
            "**_mixed_",
            "# ",
            ">**bold**",
            "\n\n**a**\n_",
            "[".repeat(64) + "](x)",
            "**Hello",
            "text with **unclosed and _also",
        )
        samples.forEach { sample ->
            val visualized = NotebookMarkdownVisuals.visualize(sample)
            val transformedLen = visualized.text.text.length
            assertTrue(transformedLen >= 0)
            assertTrue(visualized.mapping.originalToTransformed(0) in 0..transformedLen)
            assertTrue(
                visualized.mapping.transformedToOriginal(0) in 0..sample.length,
            )
            assertTrue(
                visualized.mapping.originalToTransformed(sample.length) in 0..transformedLen,
            )
        }
    }

    @Test
    fun `identityVisualized maps offsets one to one`() {
        val markdown = "**raw**"
        val visualized = NotebookMarkdownVisuals.identityVisualized(markdown)
        assertEquals(markdown, visualized.text.text)
        assertEquals(0, visualized.mapping.originalToTransformed(0))
        assertEquals(markdown.length, visualized.mapping.originalToTransformed(markdown.length))
        assertEquals(3, visualized.mapping.transformedToOriginal(3))
    }
}
