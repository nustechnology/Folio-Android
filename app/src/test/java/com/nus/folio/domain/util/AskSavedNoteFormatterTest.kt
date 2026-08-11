package com.nus.folio.domain.util

import org.junit.Assert.assertEquals
import org.junit.Test

class AskSavedNoteFormatterTest {

    @Test
    fun `titleFromQuestion uses trimmed question truncated to 150`() {
        assertEquals(
            "What is the imitation game?",
            AskSavedNoteFormatter.titleFromQuestion("  What is the imitation game?  "),
        )
        val long = "Q".repeat(160)
        assertEquals(
            "Q".repeat(150),
            AskSavedNoteFormatter.titleFromQuestion(long),
        )
    }

    @Test
    fun `titleFromQuestion falls back when blank`() {
        assertEquals(
            AskSavedNoteFormatter.DEFAULT_TITLE,
            AskSavedNoteFormatter.titleFromQuestion("   \n  "),
        )
    }

    @Test
    fun `body keeps inline citations and trims`() {
        assertEquals(
            "Answer text [1].",
            AskSavedNoteFormatter.body(content = "  Answer text [1].  "),
        )
    }
}
