package com.nus.folio.presentation.home.notebook

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class NotebookMarkdownActionsTest {
    @Test
    fun `toggleBold wraps selection`() {
        val value = TextFieldValue("Hello world", TextRange(0, 5))
        val result = NotebookMarkdownActions.toggleBold(value)
        assertEquals("**Hello** world", result.text)
        assertEquals(TextRange(2, 7), result.selection)
    }

    @Test
    fun `toggleBold unwraps selected markers`() {
        val value = TextFieldValue("**Hello** world", TextRange(0, 9))
        val result = NotebookMarkdownActions.toggleBold(value)
        assertEquals("Hello world", result.text)
        assertEquals(TextRange(0, 5), result.selection)
    }

    @Test
    fun `toggleBold unwraps surrounding markers`() {
        val value = TextFieldValue("**Hello** world", TextRange(2, 7))
        val result = NotebookMarkdownActions.toggleBold(value)
        assertEquals("Hello world", result.text)
        assertEquals(TextRange(0, 5), result.selection)
    }

    @Test
    fun `toggleItalic wraps plain selection`() {
        val value = TextFieldValue("Hello", TextRange(0, 5))
        val result = NotebookMarkdownActions.toggleItalic(value)
        assertEquals("_Hello_", result.text)
        assertEquals(TextRange(1, 6), result.selection)
    }

    @Test
    fun `toggleItalic keeps inner selection`() {
        val value = TextFieldValue("Hello world", TextRange(0, 5))
        val result = NotebookMarkdownActions.toggleItalic(value)
        assertEquals("_Hello_ world", result.text)
        assertEquals(TextRange(1, 6), result.selection)
    }

    @Test
    fun `toggleItalic unwraps when selection includes the closing marker`() {
        // VisualTransformation used to map the end caret past "_", producing TextRange(1, 7).
        val value = TextFieldValue("_Hello_", TextRange(1, 7))
        val result = NotebookMarkdownActions.toggleItalic(value)
        assertEquals("Hello", result.text)
        assertEquals(TextRange(0, 5), result.selection)
    }

    @Test
    fun `setHeading preserves selection so italic can unwrap afterward`() {
        // Bold → Italic → H1 left the caret collapsed, so a later Italic inserted "__".
        var value = TextFieldValue("Hello", TextRange(0, 5))
        value = NotebookMarkdownActions.toggleBold(value)
        assertEquals("**Hello**", value.text)
        value = NotebookMarkdownActions.toggleItalic(value)
        assertEquals("**_Hello_**", value.text)
        value = NotebookMarkdownActions.setHeading(value, 1)
        assertEquals("# **_Hello_**", value.text)
        assertEquals(TextRange(5, 10), value.selection)
        value = NotebookMarkdownActions.toggleItalic(value)
        assertEquals("# **Hello**", value.text)
        assertEquals(TextRange(4, 9), value.selection)
    }

    @Test
    fun `setHeading preserves selection so bold can unwrap afterward`() {
        var value = TextFieldValue("Hello", TextRange(0, 5))
        value = NotebookMarkdownActions.toggleItalic(value)
        value = NotebookMarkdownActions.toggleBold(value)
        assertEquals("_**Hello**_", value.text)
        value = NotebookMarkdownActions.setHeading(value, 2)
        assertEquals("## _**Hello**_", value.text)
        assertEquals(TextRange(6, 11), value.selection)
        value = NotebookMarkdownActions.toggleBold(value)
        assertEquals("## _Hello_", value.text)
        assertEquals(TextRange(4, 9), value.selection)
    }

    @Test
    fun `line formats preserve selection so inline toggles still unwrap`() {
        data class Case(
            val name: String,
            val applyLine: (TextFieldValue) -> TextFieldValue,
            val expectedAfterLine: String,
            val expectedAfterItalicOff: String,
        )
        val cases = listOf(
            Case("H3", { NotebookMarkdownActions.setHeading(it, 3) }, "### **_Hello_**", "### **Hello**"),
            Case("bullet", { NotebookMarkdownActions.toggleBulletList(it) }, "- **_Hello_**", "- **Hello**"),
            Case("ordered", { NotebookMarkdownActions.toggleOrderedList(it) }, "1. **_Hello_**", "1. **Hello**"),
            Case("quote", { NotebookMarkdownActions.toggleBlockquote(it) }, "> **_Hello_**", "> **Hello**"),
        )
        for (case in cases) {
            var value = TextFieldValue("Hello", TextRange(0, 5))
            value = NotebookMarkdownActions.toggleBold(value)
            value = NotebookMarkdownActions.toggleItalic(value)
            value = case.applyLine(value)
            assertEquals(case.name, case.expectedAfterLine, value.text)
            assertEquals(case.name, false, value.selection.collapsed)
            value = NotebookMarkdownActions.toggleItalic(value)
            assertEquals(case.name, case.expectedAfterItalicOff, value.text)
            value = NotebookMarkdownActions.toggleBold(value)
            assertEquals(case.name, case.expectedAfterItalicOff.replace("**Hello**", "Hello"), value.text)
        }
    }

    @Test
    fun `line formats replace each other instead of stacking markers`() {
        var value = TextFieldValue("Hello", TextRange(0, 5))
        value = NotebookMarkdownActions.setHeading(value, 1)
        assertEquals("# Hello", value.text)
        value = NotebookMarkdownActions.toggleBulletList(value)
        assertEquals("- Hello", value.text)
        value = NotebookMarkdownActions.toggleOrderedList(value)
        assertEquals("1. Hello", value.text)
        value = NotebookMarkdownActions.toggleBlockquote(value)
        assertEquals("> Hello", value.text)
        value = NotebookMarkdownActions.setHeading(value, 3)
        assertEquals("### Hello", value.text)
    }

    @Test
    fun `toggleItalic unwraps when bold is nested outside`() {
        val value = TextFieldValue("_**Hello**_", TextRange(3, 8))
        val result = NotebookMarkdownActions.toggleItalic(value)
        assertEquals("**Hello**", result.text)
        assertEquals(TextRange(2, 7), result.selection)
    }

    @Test
    fun `toggleBold unwraps when italic is nested inside`() {
        val value = TextFieldValue("**_Hello_**", TextRange(3, 8))
        val result = NotebookMarkdownActions.toggleBold(value)
        assertEquals("_Hello_", result.text)
        assertEquals(TextRange(1, 6), result.selection)
    }

    @Test
    fun `toggleBold wraps a reversed selection`() {
        val value = TextFieldValue("Hello world", TextRange(5, 0))
        val result = NotebookMarkdownActions.toggleBold(value)
        assertEquals("**Hello** world", result.text)
        assertEquals(TextRange(2, 7), result.selection)
    }

    @Test
    fun `toggleLink wraps selection with url placeholder selected`() {
        val value = TextFieldValue("Hello world", TextRange(0, 5))
        val result = NotebookMarkdownActions.toggleLink(value)
        assertEquals("[Hello](url) world", result.text)
        assertEquals(TextRange(8, 11), result.selection)
    }

    @Test
    fun `toggleLink inserts template at caret`() {
        val value = TextFieldValue("ab", TextRange(1))
        val result = NotebookMarkdownActions.toggleLink(value)
        assertEquals("a[text](url)b", result.text)
        assertEquals(TextRange(8, 11), result.selection)
    }

    @Test
    fun `toggleLink unwraps enclosing link to label`() {
        val value = TextFieldValue("See [Hello](https://example.com) now", TextRange(6, 11))
        val result = NotebookMarkdownActions.toggleLink(value)
        assertEquals("See Hello now", result.text)
        assertEquals(TextRange(4, 9), result.selection)
    }

    @Test
    fun `activeMarks reports link when caret is inside`() {
        val value = TextFieldValue("[Hello](url)", TextRange(3))
        val marks = NotebookMarkdownActions.activeMarks(value)
        assertEquals(true, marks.link)
    }

    @Test
    fun `setHeading toggles the same level off`() {
        val value = TextFieldValue("# Title\nBody", TextRange(3))
        val result = NotebookMarkdownActions.setHeading(value, 1)
        assertEquals("Title\nBody", result.text)
    }

    @Test
    fun `setHeading replaces a different level`() {
        val value = TextFieldValue("# Title", TextRange(3))
        val result = NotebookMarkdownActions.setHeading(value, 2)
        assertEquals("## Title", result.text)
    }

    @Test
    fun `setHeading prefixes active line`() {
        val value = TextFieldValue("Title\nBody", TextRange(0, 5))
        val result = NotebookMarkdownActions.setHeading(value, 1)
        assertEquals("# Title\nBody", result.text)
    }

    @Test
    fun `undo restores previous value`() {
        val previous = TextFieldValue("Before")
        val current = TextFieldValue("After")
        val state = NotebookMarkdownActions.UndoState(undoStack = listOf(previous))
        val (next, updated) = NotebookMarkdownActions.undo(state, current)!!
        assertEquals("Before", next.text)
        assertEquals(listOf(current), updated.redoStack)
    }

    @Test
    fun `toggleBulletList on empty content inserts marker`() {
        val result = NotebookMarkdownActions.toggleBulletList(TextFieldValue(""))
        assertEquals("- ", result.text)
    }

    @Test
    fun `toggleOrderedList on empty content inserts marker`() {
        val result = NotebookMarkdownActions.toggleOrderedList(TextFieldValue(""))
        assertEquals("1. ", result.text)
    }

    @Test
    fun `toggleBlockquote on empty content inserts marker`() {
        val result = NotebookMarkdownActions.toggleBlockquote(TextFieldValue(""))
        assertEquals("> ", result.text)
    }

    @Test
    fun `setHeading on empty content inserts heading prefix`() {
        val result = NotebookMarkdownActions.setHeading(TextFieldValue(""), 1)
        assertEquals("# ", result.text)
    }

    @Test
    fun `setHeading prefixes every line in selection`() {
        val value = TextFieldValue("Title\nBody\nTail", TextRange(0, 10))
        val result = NotebookMarkdownActions.setHeading(value, 2)
        assertEquals("## Title\n## Body\nTail", result.text)
    }

    @Test
    fun `setHeading excludes next line when selection ends at its start`() {
        // "Title\nBody" — selection through Title and the newline ends at Body's start (6).
        val value = TextFieldValue("Title\nBody", TextRange(0, 6))
        val result = NotebookMarkdownActions.setHeading(value, 1)
        assertEquals("# Title\nBody", result.text)
    }

    @Test
    fun `toggleBulletList prefixes every line in selection`() {
        val value = TextFieldValue("One\nTwo\nThree", TextRange(0, 7))
        val result = NotebookMarkdownActions.toggleBulletList(value)
        assertEquals("- One\n- Two\nThree", result.text)
    }

    @Test
    fun `toggleOrderedList numbers every line in selection`() {
        val value = TextFieldValue("One\nTwo\nThree", TextRange(0, 7))
        val result = NotebookMarkdownActions.toggleOrderedList(value)
        assertEquals("1. One\n2. Two\nThree", result.text)
    }

    @Test
    fun `continueListOnEnter appends next ordered list marker`() {
        val previous = TextFieldValue("1. First item", TextRange(13))
        val next = TextFieldValue("1. First item\n", TextRange(14))
        val result = NotebookMarkdownActions.continueListOnEnter(previous, next)!!
        assertEquals("1. First item\n2. ", result.text)
        assertEquals(17, result.selection.start)
    }

    @Test
    fun `continueListOnEnter exits ordered list on empty item`() {
        val previous = TextFieldValue("1. First item\n2. ", TextRange(17))
        val next = TextFieldValue("1. First item\n2. \n", TextRange(18))
        val result = NotebookMarkdownActions.continueListOnEnter(previous, next)!!
        assertEquals("1. First item\n\n", result.text)
        assertEquals(15, result.selection.start)
    }

    @Test
    fun `continueListOnEnter appends next bullet marker`() {
        val previous = TextFieldValue("- First item", TextRange(12))
        val next = TextFieldValue("- First item\n", TextRange(13))
        val result = NotebookMarkdownActions.continueListOnEnter(previous, next)!!
        assertEquals("- First item\n- ", result.text)
        assertEquals(15, result.selection.start)
    }

    @Test
    fun `continueListOnEnter appends next blockquote marker`() {
        val previous = TextFieldValue("> A wise note", TextRange(13))
        val next = TextFieldValue("> A wise note\n", TextRange(14))
        val result = NotebookMarkdownActions.continueListOnEnter(previous, next)!!
        assertEquals("> A wise note\n> ", result.text)
        assertEquals(16, result.selection.start)
    }

    @Test
    fun `continueListOnEnter exits blockquote on empty item`() {
        val previous = TextFieldValue("> A wise note\n> ", TextRange(16))
        val next = TextFieldValue("> A wise note\n> \n", TextRange(17))
        val result = NotebookMarkdownActions.continueListOnEnter(previous, next)!!
        assertEquals("> A wise note\n\n", result.text)
        assertEquals(15, result.selection.start)
    }

    @Test
    fun `continueListOnEnter exits blockquote even when caret is on the quote marker`() {
        // Caret parked on `>` of the empty quote line (common after visual mapping).
        val previous = TextFieldValue("> A wise note\n> ", TextRange(14))
        val next = TextFieldValue("> A wise note\n\n> ", TextRange(15))
        val result = NotebookMarkdownActions.continueListOnEnter(previous, next)!!
        assertEquals("> A wise note\n\n", result.text)
        assertEquals(15, result.selection.start)
    }

    @Test
    fun `continueListOnEnter does not expose bold markers in blockquote`() {
        val previous = TextFieldValue("> **Hello**", TextRange(9))
        val next = TextFieldValue("> **Hello\n**", TextRange(10))
        val result = NotebookMarkdownActions.continueListOnEnter(previous, next)!!
        assertEquals("> **Hello**\n> ", result.text)
    }

    @Test
    fun `continueListOnEnter exits bullet list on empty item`() {
        val previous = TextFieldValue("- First item\n- ", TextRange(15))
        val next = TextFieldValue("- First item\n- \n", TextRange(16))
        val result = NotebookMarkdownActions.continueListOnEnter(previous, next)!!
        assertEquals("- First item\n\n", result.text)
        assertEquals(14, result.selection.start)
    }

    @Test
    fun `continueListOnEnter splits ordered list line at cursor`() {
        val previous = TextFieldValue("1. Hello world", TextRange(8))
        val next = TextFieldValue("1. Hello\n world", TextRange(9))
        val result = NotebookMarkdownActions.continueListOnEnter(previous, next)!!
        assertEquals("1. Hello\n2. world", result.text)
        assertEquals(12, result.selection.start)
    }

    @Test
    fun `continueListOnEnter does not expose bold markers at visual end`() {
        // Caret sits at the start of closing "**" (visual end of Hello).
        val previous = TextFieldValue("- **Hello**", TextRange(9))
        val next = TextFieldValue("- **Hello\n**", TextRange(10))
        val result = NotebookMarkdownActions.continueListOnEnter(previous, next)!!
        assertEquals("- **Hello**\n- ", result.text)
        assertEquals(false, result.text.contains("**Hello\n"))
        assertEquals(14, result.selection.start)
    }

    @Test
    fun `continueListOnEnter rebalances bold when splitting mid span`() {
        // Caret between e and l in "**Hello**" → "1. **He" + "llo**"
        val previous = TextFieldValue("1. **Hello**", TextRange(7))
        val next = TextFieldValue("1. **He\nllo**", TextRange(8))
        val result = NotebookMarkdownActions.continueListOnEnter(previous, next)!!
        assertEquals("1. **He**\n2. **llo**", result.text)
    }

    @Test
    fun `continueListOnEnter ignores plain lines without markers`() {
        val previous = TextFieldValue("Plain text", TextRange(10))
        val next = TextFieldValue("Plain text\n", TextRange(11))
        assertEquals(null, NotebookMarkdownActions.continueListOnEnter(previous, next))
    }

    @Test
    fun `continueListOnEnter does not expose bold markers at visual start of line`() {
        // Visual caret at start of "Hello" maps to original index after opening "**".
        val previous = TextFieldValue("**Hello**", TextRange(2))
        val next = TextFieldValue("**\nHello**", TextRange(3))
        val result = NotebookMarkdownActions.continueListOnEnter(previous, next)!!
        assertEquals("\n**Hello**", result.text)
        assertEquals(1, result.selection.start)
        assertEquals(false, result.text.contains("**\n"))
    }

    @Test
    fun `continueListOnEnter does not expose bold markers at visual end of plain line`() {
        val previous = TextFieldValue("**Hello**", TextRange(7))
        val next = TextFieldValue("**Hello\n**", TextRange(8))
        val result = NotebookMarkdownActions.continueListOnEnter(previous, next)!!
        assertEquals("**Hello**\n", result.text)
        assertEquals(10, result.selection.start)
    }

    @Test
    fun `continueListOnEnter rebalances bold when splitting mid span on plain line`() {
        val previous = TextFieldValue("**Hello**", TextRange(4))
        val next = TextFieldValue("**He\nllo**", TextRange(5))
        val result = NotebookMarkdownActions.continueListOnEnter(previous, next)!!
        assertEquals("**He**\n**llo**", result.text)
    }

    @Test
    fun `continueListOnEnter keeps heading prefix with body when Enter at visual start`() {
        // Visual caret at start of "Title" maps to original index after "# ".
        val previous = TextFieldValue("# Title", TextRange(2))
        val next = TextFieldValue("# \nTitle", TextRange(3))
        val result = NotebookMarkdownActions.continueListOnEnter(previous, next)!!
        assertEquals("\n# Title", result.text)
        assertEquals(1, result.selection.start)
    }

    @Test
    fun `continueListOnEnter keeps heading and bold together at visual start`() {
        val previous = TextFieldValue("# **Hello**", TextRange(2))
        val next = TextFieldValue("# \n**Hello**", TextRange(3))
        val result = NotebookMarkdownActions.continueListOnEnter(previous, next)!!
        assertEquals("\n# **Hello**", result.text)
    }

    @Test
    fun `continueListOnEnter keeps heading when Enter at start of bold body`() {
        // Visual caret at start of "Hello" maps after "# **".
        val previous = TextFieldValue("# **Hello**", TextRange(4))
        val next = TextFieldValue("# **\nHello**", TextRange(5))
        val result = NotebookMarkdownActions.continueListOnEnter(previous, next)!!
        assertEquals("\n# **Hello**", result.text)
        assertEquals(1, result.selection.start)
    }

    @Test
    fun `resolveMarkdownEdit backspace at visual start unwraps bold instead of exposing markers`() {
        val previous = TextFieldValue("**Hello**", TextRange(2))
        val next = TextFieldValue("*Hello**", TextRange(1))
        val result = NotebookMarkdownActions.resolveMarkdownEdit(previous, next)
        assertEquals("Hello", result.text)
        assertEquals(0, result.selection.start)
        assertEquals(false, result.text.contains("*"))
    }

    @Test
    fun `resolveMarkdownEdit backspace at visual start unwraps heading`() {
        val previous = TextFieldValue("# Title", TextRange(2))
        val next = TextFieldValue("#Title", TextRange(1))
        val result = NotebookMarkdownActions.resolveMarkdownEdit(previous, next)
        assertEquals("Title", result.text)
        assertEquals(0, result.selection.start)
    }

    @Test
    fun `resolveMarkdownEdit backspace before bold deletes previous visible char`() {
        val previous = TextFieldValue("x**Hello**", TextRange(3))
        val next = TextFieldValue("x*Hello**", TextRange(2))
        val result = NotebookMarkdownActions.resolveMarkdownEdit(previous, next)
        assertEquals("**Hello**", result.text)
        assertEquals(0, result.selection.start)
    }

    @Test
    fun `resolveMarkdownEdit backspace at visual start merges with previous line`() {
        val previous = TextFieldValue("A\n**Hello**", TextRange(4))
        val next = TextFieldValue("A\n*Hello**", TextRange(3))
        val result = NotebookMarkdownActions.resolveMarkdownEdit(previous, next)
        assertEquals("A**Hello**", result.text)
        assertEquals(1, result.selection.start)
    }

    @Test
    fun `resolveMarkdownEdit forward delete at visual end does not expose closers`() {
        val previous = TextFieldValue("**Hello**", TextRange(7))
        val next = TextFieldValue("**Hello*", TextRange(7))
        val result = NotebookMarkdownActions.resolveMarkdownEdit(previous, next)
        assertEquals("**Hello**", result.text)
        assertEquals(7, result.selection.start)

        // Closers at 7 and 8 must stay hidden — not treated as deletable originals.
        val visualized = NotebookMarkdownVisuals.visualize(previous.text)
        val transformedLength = visualized.text.text.length
        for (closerIndex in 7..8) {
            val t = visualized.mapping.originalToTransformed(closerIndex)
            val mapsToSelf = t < transformedLength &&
                visualized.mapping.transformedToOriginal(t) == closerIndex
            assertFalse(mapsToSelf)
        }
    }

    @Test
    fun `resolveMarkdownEdit forward delete at visual end deletes following text`() {
        val previous = TextFieldValue("**Hello** world", TextRange(7))
        val next = TextFieldValue("**Hello* world", TextRange(7))
        val result = NotebookMarkdownActions.resolveMarkdownEdit(previous, next)
        // Skips both closers and deletes the following visible space — never a '*'.
        assertEquals("**Hello**world", result.text)
        assertEquals(7, result.selection.start)
    }

    @Test
    fun `resolveMarkdownEdit forward delete before italic closer skips marker`() {
        val previous = TextFieldValue("_Hi_ x", TextRange(3))
        val next = TextFieldValue("_Hi x", TextRange(3))
        val result = NotebookMarkdownActions.resolveMarkdownEdit(previous, next)
        assertEquals("_Hi_x", result.text)
        assertEquals(3, result.selection.start)
    }

    @Test
    fun `resolveMarkdownEdit leaves normal backspace unchanged`() {
        val previous = TextFieldValue("**Hello**", TextRange(4))
        val next = TextFieldValue("**Hllo**", TextRange(3))
        val result = NotebookMarkdownActions.resolveMarkdownEdit(previous, next)
        assertEquals("**Hllo**", result.text)
        assertEquals(3, result.selection.start)
    }

    @Test
    fun `activeMarks detects bold italic and heading`() {
        val value = TextFieldValue("# **_Hello_**", TextRange(5, 10))
        val marks = NotebookMarkdownActions.activeMarks(value)
        assertEquals(true, marks.bold)
        assertEquals(true, marks.italic)
        assertEquals(1, marks.headingLevel)
        assertEquals(false, marks.bulletList)
        assertEquals(false, marks.orderedList)
        assertEquals(false, marks.blockquote)
    }

    @Test
    fun `activeMarks detects list and quote line prefixes`() {
        assertEquals(
            true,
            NotebookMarkdownActions.activeMarks(TextFieldValue("- item", TextRange(3))).bulletList,
        )
        assertEquals(
            true,
            NotebookMarkdownActions.activeMarks(TextFieldValue("1. item", TextRange(4))).orderedList,
        )
        assertEquals(
            true,
            NotebookMarkdownActions.activeMarks(TextFieldValue("> note", TextRange(3))).blockquote,
        )
    }

    @Test
    fun `activeMarks detects bold when caret is inside markers`() {
        val marks = NotebookMarkdownActions.activeMarks(TextFieldValue("**Hello**", TextRange(4)))
        assertEquals(true, marks.bold)
        assertEquals(false, marks.italic)
    }

    @Test
    fun `activeMarks does not treat mid-word underscore as italic`() {
        val marks = NotebookMarkdownActions.activeMarks(TextFieldValue("file_name", TextRange(5)))
        assertEquals(false, marks.italic)
    }

    @Test
    fun `activeMarks detects italic when caret is inside underscore markers`() {
        val marks = NotebookMarkdownActions.activeMarks(TextFieldValue("_Hello_", TextRange(3)))
        assertEquals(true, marks.italic)
    }

    @Test
    fun `activeMarks ignores underscore inside word next to real italic`() {
        val marks = NotebookMarkdownActions.activeMarks(
            TextFieldValue("space_id and _ok_", TextRange(14)),
        )
        assertEquals(true, marks.italic)
        val outside = NotebookMarkdownActions.activeMarks(
            TextFieldValue("space_id and _ok_", TextRange(5)),
        )
        assertEquals(false, outside.italic)
    }
}
