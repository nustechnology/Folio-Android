package com.nus.folio.data.util

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

internal object SourceOriginalFileWriter {

    fun writeOriginalFile(target: File, sourceId: String, title: String, extension: String) {
        when (extension.lowercase()) {
            "pdf" -> target.writeBytes(minimalPdf(title))
            "txt", "md" -> target.writeText(textContent(sourceId, title))
            "csv" -> target.writeText(csvContent(title))
            "docx" -> writeDocx(target, title)
            "pptx" -> writePptx(target, title)
            "xlsx" -> writeXlsx(target, title)
            "epub" -> writeEpub(target, title)
            else -> target.writeText("Folio source: $title")
        }
    }

    private fun textContent(sourceId: String, title: String): String = when (sourceId) {
        "6" -> """
            Interview notes: archival methods
            Field notes · March 2026

            Archivists described a shift from item-level description to collection-level discovery.
            Students still need close reading skills, but they also need to understand how
            digitization changes what survives in the record.
        """.trimIndent()
        "8" -> """
            Course syllabus draft
            Teaching staff

            Week 7: History of Science in the Digital Age
            - Archival methods and born-digital evidence
            - Close reading and computational discovery
            - Discussion: what counts as a primary source?
        """.trimIndent()
        "9" -> """
            # Artificial neural network

            An artificial neural network is a computational model inspired by biological neural networks.
            Modern language models rely on attention mechanisms to weigh relationships between tokens.
        """.trimIndent()
        else -> "Folio source: $title"
    }

    private fun csvContent(title: String): String = """
        Metric,Q1,Q2,Q3,Q4
        Sources indexed,128,142,156,171
        Notes created,32,41,38,47
        Ask sessions,18,24,29,35
        Report,$title
    """.trimIndent()

    private fun minimalPdf(title: String): ByteArray {
        val safeTitle = title
            .replace("\\", "\\\\")
            .replace("(", "\\(")
            .replace(")", "\\)")
        val streamContent = "BT /F1 18 Tf 72 720 Td ($safeTitle) Tj ET"
        val objects = listOf(
            "1 0 obj<< /Type /Catalog /Pages 2 0 R >>endobj\n",
            "2 0 obj<< /Type /Pages /Kids [3 0 R] /Count 1 >>endobj\n",
            "3 0 obj<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] " +
                "/Contents 4 0 R /Resources<< /Font<< /F1 5 0 R >>>>> >>endobj\n",
            "4 0 obj<< /Length ${streamContent.length} >>stream\n${streamContent}endstream\nendobj\n",
            "5 0 obj<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>endobj\n",
        )

        val pdf = StringBuilder("%PDF-1.4\n")
        val objectOffsets = IntArray(objects.size + 1)
        objects.forEachIndexed { index, obj ->
            objectOffsets[index + 1] = pdf.length
            pdf.append(obj)
        }

        val xrefOffset = pdf.length
        pdf.append("xref\n")
        pdf.append("0 ${objectOffsets.size}\n")
        // Each xref entry is exactly 20 bytes (nnnnnnnnnn ggggg n eol).
        pdf.append("0000000000 65535 f \n")
        for (i in 1 until objectOffsets.size) {
            pdf.append("%010d 00000 n \n".format(objectOffsets[i]))
        }
        pdf.append("trailer<< /Size ${objectOffsets.size} /Root 1 0 R >>\n")
        pdf.append("startxref\n")
        pdf.append("$xrefOffset\n")
        pdf.append("%%EOF\n")
        return pdf.toString().toByteArray(Charsets.US_ASCII)
    }

    private fun writeDocx(target: File, title: String) {
        writeZip(target) { zip ->
            zip.writeEntry(
                "[Content_Types].xml",
                """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                  <Default Extension="xml" ContentType="application/xml"/>
                  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
                </Types>
                """.trimIndent(),
            )
            zip.writeEntry(
                "_rels/.rels",
                """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
                </Relationships>
                """.trimIndent(),
            )
            zip.writeEntry(
                "word/document.xml",
                """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                  <w:body>
                    <w:p><w:r><w:t>${escapeXml(title)}</w:t></w:r></w:p>
                    <w:p><w:r><w:t>Original uploaded document preview.</w:t></w:r></w:p>
                  </w:body>
                </w:document>
                """.trimIndent(),
            )
        }
    }

    private fun writePptx(target: File, title: String) {
        writeZip(target) { zip ->
            zip.writeEntry(
                "[Content_Types].xml",
                """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                  <Default Extension="xml" ContentType="application/xml"/>
                  <Override PartName="/ppt/presentation.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml"/>
                  <Override PartName="/ppt/slides/slide1.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slide+xml"/>
                </Types>
                """.trimIndent(),
            )
            zip.writeEntry(
                "_rels/.rels",
                """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="ppt/presentation.xml"/>
                </Relationships>
                """.trimIndent(),
            )
            zip.writeEntry(
                "ppt/presentation.xml",
                """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <p:presentation xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main"
                  xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                  <p:sldIdLst><p:sldId id="256" r:id="rId2"/></p:sldIdLst>
                </p:presentation>
                """.trimIndent(),
            )
            zip.writeEntry(
                "ppt/_rels/presentation.xml.rels",
                """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide" Target="slides/slide1.xml"/>
                </Relationships>
                """.trimIndent(),
            )
            zip.writeEntry(
                "ppt/slides/slide1.xml",
                """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <p:sld xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main"
                  xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main">
                  <p:cSld><p:spTree>
                    <p:sp><p:txBody><a:p><a:r><a:t>Slide 1: ${escapeXml(title)}</a:t></a:r></a:p></p:txBody></p:sp>
                  </p:spTree></p:cSld>
                </p:sld>
                """.trimIndent(),
            )
        }
    }

    private fun writeXlsx(target: File, title: String) {
        writeZip(target) { zip ->
            zip.writeEntry(
                "[Content_Types].xml",
                """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                  <Default Extension="xml" ContentType="application/xml"/>
                  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                </Types>
                """.trimIndent(),
            )
            zip.writeEntry(
                "_rels/.rels",
                """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                </Relationships>
                """.trimIndent(),
            )
            zip.writeEntry(
                "xl/workbook.xml",
                """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
                  xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                  <sheets><sheet name="Summary" sheetId="1" r:id="rId1"/></sheets>
                </workbook>
                """.trimIndent(),
            )
            zip.writeEntry(
                "xl/_rels/workbook.xml.rels",
                """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
                </Relationships>
                """.trimIndent(),
            )
            zip.writeEntry(
                "xl/worksheets/sheet1.xml",
                """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                  <sheetData>
                    <row r="1"><c r="A1" t="inlineStr"><is><t>${escapeXml(title)}</t></is></c></row>
                    <row r="2"><c r="A2" t="inlineStr"><is><t>Metric</t></is></c><c r="B2" t="inlineStr"><is><t>Q1</t></is></c></row>
                    <row r="3"><c r="A3" t="inlineStr"><is><t>Sources indexed</t></is></c><c r="B3" t="inlineStr"><is><t>128</t></is></c></row>
                  </sheetData>
                </worksheet>
                """.trimIndent(),
            )
        }
    }

    private fun writeEpub(target: File, title: String) {
        writeZip(target) { zip ->
            zip.writeEntry(
                "mimetype",
                "application/epub+zip",
            )
            zip.writeEntry(
                "META-INF/container.xml",
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
                  <rootfiles>
                    <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
                  </rootfiles>
                </container>
                """.trimIndent(),
            )
            zip.writeEntry(
                "OEBPS/content.opf",
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <package xmlns="http://www.idpf.org/2007/opf" version="2.0" unique-identifier="bookid">
                  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
                    <dc:title>${escapeXml(title)}</dc:title>
                  </metadata>
                  <manifest>
                    <item id="chapter1" href="chapter1.xhtml" media-type="application/xhtml+xml"/>
                  </manifest>
                  <spine>
                    <itemref idref="chapter1"/>
                  </spine>
                </package>
                """.trimIndent(),
            )
            zip.writeEntry(
                "OEBPS/chapter1.xhtml",
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <html xmlns="http://www.w3.org/1999/xhtml">
                  <head><title>${escapeXml(title)}</title></head>
                  <body><h1>${escapeXml(title)}</h1><p>Original uploaded book preview.</p></body>
                </html>
                """.trimIndent(),
            )
        }
    }

    private inline fun writeZip(target: File, block: (ZipOutputStream) -> Unit) {
        ZipOutputStream(target.outputStream()).use(block)
    }

    private fun ZipOutputStream.writeEntry(path: String, content: String) {
        putNextEntry(ZipEntry(path))
        write(content.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun escapeXml(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}
