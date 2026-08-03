package com.nus.folio.data.util

import org.junit.Assert.assertEquals
import org.junit.Test

class SourceMimeTypesTest {

    @Test
    fun `forExtension maps known extensions`() {
        assertEquals("application/pdf", SourceMimeTypes.forExtension("pdf"))
        assertEquals("application/pdf", SourceMimeTypes.forExtension("PDF"))
        assertEquals(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            SourceMimeTypes.forExtension("docx"),
        )
        assertEquals("application/epub+zip", SourceMimeTypes.forExtension("epub"))
        assertEquals("text/markdown", SourceMimeTypes.forExtension("md"))
        assertEquals("text/plain", SourceMimeTypes.forExtension("txt"))
        assertEquals(
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            SourceMimeTypes.forExtension("pptx"),
        )
        assertEquals(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            SourceMimeTypes.forExtension("xlsx"),
        )
        assertEquals("text/csv", SourceMimeTypes.forExtension("csv"))
    }

    @Test
    fun `forExtension falls back for unknown extension`() {
        assertEquals("application/octet-stream", SourceMimeTypes.forExtension("bin"))
        assertEquals("application/octet-stream", SourceMimeTypes.forExtension(""))
    }
}
