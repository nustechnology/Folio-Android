package com.nus.folio.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteInputRulesTest {

    @Test
    fun `resolveTitle defaults blank or whitespace to Untitled Note`() {
        assertEquals(NoteInputRules.DEFAULT_TITLE, NoteInputRules.resolveTitle(""))
        assertEquals(NoteInputRules.DEFAULT_TITLE, NoteInputRules.resolveTitle("   "))
        assertEquals("My note", NoteInputRules.resolveTitle("  My note  "))
    }

    @Test
    fun `titleValidationError when over 150 characters`() {
        assertNull(NoteInputRules.titleValidationError("a".repeat(150)))
        assertEquals(
            NoteInputRules.TitleValidationError.TOO_LONG,
            NoteInputRules.titleValidationError("a".repeat(151)),
        )
    }

    @Test
    fun `contentValidationError for empty and too long`() {
        assertEquals(
            NoteInputRules.ContentValidationError.EMPTY,
            NoteInputRules.contentValidationError(""),
        )
        assertEquals(
            NoteInputRules.ContentValidationError.EMPTY,
            NoteInputRules.contentValidationError("   "),
        )
        assertNull(NoteInputRules.contentValidationError("ok"))
        assertEquals(
            NoteInputRules.ContentValidationError.TOO_LONG,
            NoteInputRules.contentValidationError("a".repeat(20_001)),
        )
        assertNull(NoteInputRules.contentValidationError("a".repeat(20_000)))
    }

    @Test
    fun `canSave requires valid content and title length`() {
        assertTrue(NoteInputRules.canSave("", "body"))
        assertFalse(NoteInputRules.canSave("", ""))
        assertFalse(NoteInputRules.canSave("a".repeat(151), "body"))
        assertFalse(NoteInputRules.canSave("title", "a".repeat(20_001)))
    }
}
