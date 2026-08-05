package com.nus.folio.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AddSourceInputRulesTest {

    @Test
    fun `supported extensions match AC formats`() {
        listOf("pdf", "docx", "txt", "md", "pptx", "xlsx", "csv", "epub").forEach {
            assertTrue(AddSourceInputRules.isSupportedExtension("file.$it"))
            assertTrue(AddSourceInputRules.isSupportedExtension("FILE.${it.uppercase()}"))
        }
        assertFalse(AddSourceInputRules.isSupportedExtension("notes.doc"))
        assertFalse(AddSourceInputRules.isSupportedExtension("image.png"))
    }

    @Test
    fun `validateFile rejects unsupported format and oversized files`() {
        assertEquals(
            AddSourceInputRules.FileValidationError.UNSUPPORTED_FORMAT,
            AddSourceInputRules.validateFile("notes.doc", 1_000),
        )
        assertEquals(
            AddSourceInputRules.FileValidationError.SIZE_EXCEEDED,
            AddSourceInputRules.validateFile(
                "paper.pdf",
                AddSourceInputRules.MAX_FILE_BYTES + 1,
            ),
        )
        assertNull(
            AddSourceInputRules.validateFile("paper.pdf", AddSourceInputRules.MAX_FILE_BYTES),
        )
    }

    @Test
    fun `isValidHttpUrl accepts http and https with host`() {
        assertTrue(AddSourceInputRules.isValidHttpUrl("https://example.com/path"))
        assertTrue(AddSourceInputRules.isValidHttpUrl("http://example.org"))
        assertFalse(AddSourceInputRules.isValidHttpUrl(""))
        assertFalse(AddSourceInputRules.isValidHttpUrl("ftp://example.com"))
        assertFalse(AddSourceInputRules.isValidHttpUrl("example.com"))
        assertFalse(AddSourceInputRules.isValidHttpUrl("https://"))
    }

    @Test
    fun `content length rules`() {
        assertFalse(AddSourceInputRules.isContentValid("short"))
        assertTrue(AddSourceInputRules.isContentValid("1234567890"))
        assertEquals(
            AddSourceInputRules.ContentValidationError.TOO_SHORT,
            AddSourceInputRules.contentValidationError("abc"),
        )
        assertEquals(
            AddSourceInputRules.ContentValidationError.TOO_SHORT,
            AddSourceInputRules.contentValidationError(""),
        )
        assertEquals(
            AddSourceInputRules.ContentValidationError.TOO_LONG,
            AddSourceInputRules.contentValidationError("x".repeat(100_001)),
        )
    }

    @Test
    fun `title and author are truncated to max lengths`() {
        assertEquals(255, AddSourceInputRules.limitTitle("t".repeat(300)).length)
        assertEquals(100, AddSourceInputRules.limitAuthor("a".repeat(150)).length)
    }

    @Test
    fun `defaultManualTitle includes date`() {
        assertEquals(
            "Untitled Source - 2026-08-04",
            AddSourceInputRules.defaultManualTitle("2026-08-04"),
        )
    }

    @Test
    fun `defaultWebAuthor uses host or Unknown Author`() {
        assertEquals("example.com", AddSourceInputRules.defaultWebAuthor("https://www.example.com/a"))
        assertEquals("Unknown Author", AddSourceInputRules.defaultWebAuthor("not-a-url"))
    }

    @Test
    fun `currentUserDisplayName prefers display name then email local part`() {
        assertEquals(
            "Ada Lovelace",
            AddSourceInputRules.currentUserDisplayName("Ada Lovelace", "ada@folio.app"),
        )
        assertEquals(
            "ada",
            AddSourceInputRules.currentUserDisplayName("  ", "ada@folio.app"),
        )
        assertEquals(
            "Unknown Author",
            AddSourceInputRules.currentUserDisplayName(null, null),
        )
    }
}
