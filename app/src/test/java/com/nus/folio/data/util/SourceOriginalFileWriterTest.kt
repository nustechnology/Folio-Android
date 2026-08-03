package com.nus.folio.data.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.zip.ZipFile
import kotlin.text.Charsets
class SourceOriginalFileWriterTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `writeOriginalFile writes pdf bytes`() {
        val target = temporaryFolder.newFile("sample.pdf")

        SourceOriginalFileWriter.writeOriginalFile(
            target = target,
            sourceId = "1",
            title = "Sample PDF",
            extension = "pdf",
        )

        val bytes = target.readBytes()
        assertTrue(bytes.isNotEmpty())
        assertTrue(bytes.decodeToString().contains("%PDF"))
        assertTrue(bytes.decodeToString().contains("Sample PDF"))
    }

    @Test
    fun `writeOriginalFile writes pdf with complete xref table`() {
        val target = temporaryFolder.newFile("xref.pdf")

        SourceOriginalFileWriter.writeOriginalFile(
            target = target,
            sourceId = "1",
            title = "Xref PDF",
            extension = "pdf",
        )

        val text = target.readBytes().toString(Charsets.US_ASCII)
        val xrefIndex = text.indexOf("xref\n")
        assertTrue(xrefIndex > 0)
        assertTrue(text.contains("xref\n0 6\n"))

        val xrefBody = text.substring(xrefIndex)
        val entryLines = xrefBody
            .lineSequence()
            .drop(2) // "xref" and "0 6"
            .take(6)
            .toList()
        assertEquals(6, entryLines.size)
        assertTrue(entryLines[0].matches(Regex("""0000000000 65535 f ?""")))
        for (i in 1..5) {
            assertTrue(entryLines[i].matches(Regex("""\d{10} 00000 n ?""")))
            val offset = entryLines[i].substring(0, 10).toInt()
            assertTrue(offset > 0)
            assertTrue(text.startsWith("${i} 0 obj", startIndex = offset))
        }

        val startxrefIndex = text.lastIndexOf("startxref\n")
        assertTrue(startxrefIndex > xrefIndex)
        val startxrefValue = text
            .substring(startxrefIndex + "startxref\n".length)
            .lineSequence()
            .first()
            .toInt()
        assertEquals(xrefIndex, startxrefValue)
    }

    @Test
    fun `writeOriginalFile writes text content for known source`() {
        val target = temporaryFolder.newFile("notes.txt")

        SourceOriginalFileWriter.writeOriginalFile(
            target = target,
            sourceId = "6",
            title = "Interview notes",
            extension = "txt",
        )

        val text = target.readText()
        assertTrue(text.contains("Interview notes: archival methods"))
        assertTrue(text.contains("Archivists described"))
    }

    @Test
    fun `writeOriginalFile writes csv content`() {
        val target = temporaryFolder.newFile("metrics.csv")

        SourceOriginalFileWriter.writeOriginalFile(
            target = target,
            sourceId = "10",
            title = "Research metrics",
            extension = "csv",
        )

        val text = target.readText()
        assertTrue(text.contains("Metric,Q1,Q2,Q3,Q4"))
        assertTrue(text.contains("Research metrics"))
    }

    @Test
    fun `writeOriginalFile writes zip-based office documents`() {
        val target = temporaryFolder.newFile("deck.pptx")

        SourceOriginalFileWriter.writeOriginalFile(
            target = target,
            sourceId = "7",
            title = "Lecture slides",
            extension = "pptx",
        )

        ZipFile(target).use { zip ->
            assertTrue(zip.getEntry("[Content_Types].xml") != null)
            assertTrue(zip.getEntry("ppt/slides/slide1.xml") != null)
        }
    }

    @Test
    fun `writeOriginalFile writes fallback text for unknown extension`() {
        val target = temporaryFolder.newFile("file.bin")

        SourceOriginalFileWriter.writeOriginalFile(
            target = target,
            sourceId = "x",
            title = "Unknown type",
            extension = "bin",
        )

        assertTrue(target.readText().contains("Folio source: Unknown type"))
    }
}
