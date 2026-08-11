package com.nus.folio.domain.util

import org.junit.Assert.assertEquals
import org.junit.Test

class NotebookFilenameTest {
    @Test
    fun `forSpace slugifies title and appends notebook md suffix`() {
        assertEquals(
            "dissertation-research-notebook.md",
            NotebookFilename.forSpace("Dissertation Research"),
        )
    }

    @Test
    fun `forSpace uses notebook when title is blank after slugify`() {
        assertEquals(
            "notebook-notebook.md",
            NotebookFilename.forSpace("!!!"),
        )
    }
}
