package com.nus.folio.data.datasource

import com.nus.folio.domain.model.SourceContentFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceSampleDataTest {

    @Test
    fun `seeded sample details match source titles and formats`() {
        val sources = SourceSampleData.mutableDefaultSources()

        val turing = SourceSampleData.buildSourceDetail(sources.first { it.id == "1" })
        assertEquals("Alan Turing: Computing Machinery", turing.title)
        assertEquals(SourceContentFormat.DOCUMENT, turing.contentFormat)
        assertEquals("pdf", turing.fileExtension)
        assertTrue(turing.htmlContent.orEmpty().contains("Computing Machinery and Intelligence"))
        assertFalse(turing.htmlContent.orEmpty().contains("Weapons of Math Destruction"))

        val arendt = SourceSampleData.buildSourceDetail(sources.first { it.id == "2" })
        assertEquals("The Origins of Totalitarianism", arendt.title)
        assertEquals(SourceContentFormat.DOCUMENT, arendt.contentFormat)
        assertTrue(arendt.htmlContent.orEmpty().contains("Hannah Arendt"))

        val wmd = SourceSampleData.buildSourceDetail(sources.first { it.id == "3" })
        assertEquals("Weapons of Math Destruction", wmd.title)
        assertEquals("epub", wmd.fileExtension)
        assertTrue(wmd.htmlContent.orEmpty().contains("Weapons of Math Destruction"))
        assertTrue(wmd.htmlContent.orEmpty().contains("Cathy O'Neil"))
        assertFalse(wmd.htmlContent.orEmpty().contains("Imitation Game"))

        val zuboff = SourceSampleData.buildSourceDetail(sources.first { it.id == "4" })
        assertEquals("The Age of Surveillance Capitalism", zuboff.title)
        assertTrue(zuboff.htmlContent.orEmpty().contains("Surveillance Capitalism"))

        val attention = SourceSampleData.buildSourceDetail(sources.first { it.id == "5" })
        assertEquals("Attention Is All You Need", attention.title)
        assertTrue(attention.htmlContent.orEmpty().contains("Transformer"))

        val notes = SourceSampleData.buildSourceDetail(sources.first { it.id == "6" })
        assertEquals("Interview notes: archival methods", notes.title)
        assertTrue(notes.htmlContent.orEmpty().contains("Interview notes"))

        val slides = SourceSampleData.buildSourceDetail(sources.first { it.id == "7" })
        assertEquals(SourceContentFormat.SLIDES, slides.contentFormat)
        assertEquals("pptx", slides.fileExtension)
        assertTrue(slides.htmlContent.orEmpty().contains("Slide 1"))

        val syllabus = SourceSampleData.buildSourceDetail(sources.first { it.id == "8" })
        assertEquals("Course syllabus draft", syllabus.title)
        assertTrue(syllabus.htmlContent.orEmpty().contains("Course syllabus draft"))
        assertFalse(syllabus.htmlContent.orEmpty().contains("Interview notes: archival methods"))

        val web = SourceSampleData.buildSourceDetail(sources.first { it.id == "9" })
        assertEquals("Wikipedia: Neural Networks", web.title)
        assertTrue(web.htmlContent.orEmpty().contains("Artificial neural network"))
    }

    @Test
    fun `seeded spreadsheet detail uses sheet tabs and xlsx metadata`() {
        val source = SourceSampleData.mutableDefaultSources().first { it.id == "10" }

        val detail = SourceSampleData.buildSourceDetail(source)

        assertEquals("Research metrics dashboard", detail.title)
        assertEquals(SourceContentFormat.SHEET, detail.contentFormat)
        assertEquals("xlsx", detail.fileExtension)
        assertEquals("research-metrics-dashboard.xlsx", detail.originalFileName)
        assertNull(detail.htmlContent)
        assertEquals(listOf("Summary", "Raw Data"), detail.sheets.map { it.name })
        val rawTable = detail.sheets.last().htmlTable
        assertTrue(rawTable.contains("Research metrics dashboard"))
        assertTrue(rawTable.contains("Spreadsheet"))
        assertFalse(
            rawTable.contains(
                "<td>10</td>\n                            <td>Research metrics dashboard</td>\n                            <td>PDF</td>",
            ),
        )
    }

    @Test
    fun `rememberUpdatedManualContent persists plain content for later detail reads`() {
        val source = SourceSampleData.mutableDefaultSources().first { it.id == "6" }
            .copy(title = "Updated notes", author = "Alice")
        val content = "This is the updated manual source content text note..."

        SourceSampleData.rememberUpdatedManualContent(source, content)

        val detail = SourceSampleData.buildSourceDetail(source)
        assertEquals(content, detail.plainContent)
        assertTrue(detail.htmlContent.orEmpty().contains(content))
        assertEquals("Updated notes", detail.title)

        SourceSampleData.forgetCreatedDetail(source.id)
    }

    @Test
    fun `rememberUpdatedManualContent accepts empty string as a content patch`() {
        val source = SourceSampleData.mutableDefaultSources().first { it.id == "6" }

        SourceSampleData.rememberUpdatedManualContent(source, "")

        assertEquals("", SourceSampleData.buildSourceDetail(source).plainContent)

        SourceSampleData.forgetCreatedDetail(source.id)
    }
}
