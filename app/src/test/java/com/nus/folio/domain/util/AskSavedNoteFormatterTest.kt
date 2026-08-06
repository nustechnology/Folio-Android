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
    fun `bodyWithCitationFootnotes appends citation list`() {
        val body = AskSavedNoteFormatter.bodyWithCitationFootnotes(
            content = "Answer text [1].",
            citations = listOf(
                AskCitation(
                    index = 1,
                    sourceId = "1",
                    sourceTitle = "Alan Turing: Computing Machinery",
                    sourceType = SourceType.FILE,
                    fileExtension = "pdf",
                    locationLabel = "Page 14",
                    evidenceText = "imitation game",
                ),
            ),
        )

        assertEquals(
            """
            Answer text [1].

            Citations
            [1] Alan Turing: Computing Machinery · Page 14
            """.trimIndent(),
            body,
        )
    }
}
