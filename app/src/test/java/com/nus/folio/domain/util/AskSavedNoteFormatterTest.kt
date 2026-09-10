package com.nus.folio.domain.util

import com.nus.folio.domain.model.AskCitation
import com.nus.folio.domain.model.SourceType
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

    @Test
    fun `body appends evidence excerpts from citations`() {
        val body = AskSavedNoteFormatter.body(
            content = "  Answer text [1].  ",
            citations = listOf(
                AskCitation(
                    index = 1,
                    sourceId = "1",
                    sourceTitle = "Computing Machinery",
                    sourceType = SourceType.FILE,
                    locationLabel = "Page 14",
                    evidenceText = "  The imitation game.  ",
                ),
                AskCitation(
                    index = 2,
                    sourceId = "2",
                    sourceTitle = "Empty evidence",
                    evidenceText = "  ",
                ),
            ),
        )
        assertEquals(
            "Answer text [1].\n\nEvidence\n\n[1] Computing Machinery — Page 14\nThe imitation game.",
            body,
        )
    }

    @Test
    fun `body appends limitation and evidence heading`() {
        val body = AskSavedNoteFormatter.body(
            content = "Answer text [1].",
            citations = listOf(
                AskCitation(
                    index = 1,
                    sourceId = "1",
                    sourceTitle = "Computing Machinery",
                    locationLabel = "Page 14",
                    evidenceText = "The imitation game.",
                ),
            ),
            limitation = "Limitation: Sparse coverage.",
        )
        assertEquals(
            "Answer text [1].\n\nLimitation: Sparse coverage.\n\nEvidence\n\n[1] Computing Machinery — Page 14\nThe imitation game.",
            body,
        )
    }

    @Test
    fun `body skips blank limitation`() {
        assertEquals(
            "Answer only.",
            AskSavedNoteFormatter.body(
                content = "Answer only.",
                limitation = "  ",
            ),
        )
    }

    @Test
    fun `body skips citations without evidence`() {
        assertEquals(
            "Answer only.",
            AskSavedNoteFormatter.body(
                content = "Answer only.",
                citations = listOf(
                    AskCitation(index = 1, sourceId = "1", sourceTitle = "Paper"),
                ),
            ),
        )
    }
}
