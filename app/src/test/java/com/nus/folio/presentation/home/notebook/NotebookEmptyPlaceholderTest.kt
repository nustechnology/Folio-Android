package com.nus.folio.presentation.home.notebook

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotebookEmptyPlaceholderTest {
    @Test
    fun `notebookEmptyHeading uses space title when present`() {
        assertEquals(
            "Dissertation Research",
            notebookEmptyHeading("Dissertation Research", fallback = "Notebook"),
        )
    }

    @Test
    fun `notebookEmptyHeading falls back when blank`() {
        assertEquals("Notebook", notebookEmptyHeading("   ", fallback = "Notebook"))
    }

    @Test
    fun `isNotebookContentEmpty treats blank as empty`() {
        assertTrue(isNotebookContentEmpty(""))
        assertTrue(isNotebookContentEmpty("  \n  "))
        assertFalse(isNotebookContentEmpty("# Title"))
    }
}
