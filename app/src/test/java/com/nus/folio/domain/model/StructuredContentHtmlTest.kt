package com.nus.folio.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StructuredContentHtmlTest {

    @Test
    fun `sheetToHtml builds header and row cells`() {
        val html = StructuredContentHtml.sheetToHtml(
            StructuredSheet(
                name = "CSV Data",
                headers = listOf("A", "B"),
                rows = listOf(listOf("1", "2")),
            ),
        )

        assertTrue(html.contains("<th>A</th>"))
        assertTrue(html.contains("<th>B</th>"))
        assertTrue(html.contains("<td>1</td>"))
        assertTrue(html.contains("<td>2</td>"))
    }

    @Test
    fun `slidesToHtml renders slide cards`() {
        val html = StructuredContentHtml.slidesToHtml(
            listOf(
                StructuredSlide(
                    slideNumber = 1,
                    title = "Intro",
                    bullets = listOf("One", "Two"),
                ),
            ),
        )

        assertTrue(html.contains("Slide 1"))
        assertTrue(html.contains("Intro"))
        assertTrue(html.contains("<li>One</li>"))
        assertTrue(html.contains("<li>Two</li>"))
    }

    @Test
    fun `contentFormat maps sealed variants`() {
        assertEquals(
            SourceContentFormat.DOCUMENT,
            StructuredContentHtml.contentFormat(StructuredContent.Document("<p>Hi</p>")),
        )
        assertEquals(
            SourceContentFormat.SHEET,
            StructuredContentHtml.contentFormat(
                StructuredContent.Sheets(listOf(StructuredSheet(name = "Sheet1"))),
            ),
        )
        assertEquals(
            SourceContentFormat.SLIDES,
            StructuredContentHtml.contentFormat(
                StructuredContent.Slides(listOf(StructuredSlide(slideNumber = 1))),
            ),
        )
    }

    @Test
    fun `toSheetTabs preserves names and html tables`() {
        val tabs = StructuredContentHtml.toSheetTabs(
            listOf(
                StructuredSheet(name = "Summary", headers = listOf("X"), rows = listOf(listOf("1"))),
                StructuredSheet(name = "Raw Data", headers = listOf("Y"), rows = listOf(listOf("2"))),
            ),
        )

        assertEquals(listOf("Summary", "Raw Data"), tabs.map { it.name })
        assertTrue(tabs[0].htmlTable.contains("<th>X</th>"))
        assertTrue(tabs[1].htmlTable.contains("<td>2</td>"))
    }
}
