package com.nus.folio.presentation.home.notebook

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Test

class NotebookMarkdownActionsTest {
    @Test
    fun `toggleBold wraps selection`() {
        val value = TextFieldValue("Hello world", TextRange(0, 5))
        val result = NotebookMarkdownActions.toggleBold(value)
        assertEquals("**Hello** world", result.text)
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
    fun `continueListOnEnter ignores non-list lines`() {
        val previous = TextFieldValue("Plain text", TextRange(10))
        val next = TextFieldValue("Plain text\n", TextRange(11))
        assertEquals(null, NotebookMarkdownActions.continueListOnEnter(previous, next))
    }
}
