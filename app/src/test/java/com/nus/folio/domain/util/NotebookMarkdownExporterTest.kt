package com.nus.folio.domain.util

import org.junit.Assert.assertEquals
import org.junit.Test

class NotebookMarkdownExporterTest {
    @Test
    fun `normalize converts line endings and trims trailing whitespace`() {
        assertEquals(
            "Hello\n\nWorld\n",
            NotebookMarkdownExporter.normalize("Hello\r\n\r\nWorld\r\n  "),
        )
    }

    @Test
    fun `normalize keeps empty content empty`() {
        assertEquals("", NotebookMarkdownExporter.normalize("   "))
    }
}
