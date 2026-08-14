package com.nus.folio.data.network

import com.nus.folio.domain.model.SourceContentFormat
import com.nus.folio.domain.model.SourceProcessingState
import com.nus.folio.domain.model.SourceStatus
import com.nus.folio.domain.model.SourceType
import com.nus.folio.domain.model.StructuredContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class SourcesApiClientMappingTest {

    @Test
    fun `mapSourceType maps API values`() {
        assertEquals(SourceType.WEB, SourcesApiClient.mapSourceType("Web"))
        assertEquals(SourceType.TEXT, SourcesApiClient.mapSourceType("Manual"))
        assertEquals(SourceType.FILE, SourcesApiClient.mapSourceType("File"))
        assertEquals(SourceType.BOOK, SourcesApiClient.mapSourceType("Book"))
    }

    @Test
    fun `mapProcessingState maps API values`() {
        assertEquals(SourceStatus.PROCESSING, SourcesApiClient.mapProcessingState("added"))
        assertEquals(SourceStatus.PROCESSING, SourcesApiClient.mapProcessingState("processing"))
        assertEquals(SourceStatus.READY, SourcesApiClient.mapProcessingState("ready"))
        assertEquals(SourceStatus.FAILED, SourcesApiClient.mapProcessingState("failed"))
    }

    @Test
    fun `formatAddedLabel uses relative time`() {
        val now = 1_700_000_000_000L
        assertEquals(
            "Added just now",
            SourcesApiClient.formatAddedLabel("2023-11-14T22:13:20.000Z", now),
        )
        assertEquals(
            "Added 2d ago",
            SourcesApiClient.formatAddedLabel("2023-11-12T22:13:20.000Z", now),
        )
    }

    @Test
    fun `parseSseDataPayload maps state and progress`() {
        val event = SourcesApiClient.parseSseDataPayload(
            """{"sourceId":"532a3be6-cd85-48ef-aa28-8d2ba8bb5eb0","state":"extracting_text","progress":25}""",
        )

        assertEquals("532a3be6-cd85-48ef-aa28-8d2ba8bb5eb0", event?.sourceId)
        assertEquals(SourceProcessingState.EXTRACTING_TEXT, event?.state)
        assertEquals(25, event?.progress)
        assertEquals(1, event?.completedStepCount())
    }

    @Test
    fun `parseSseIdFieldValue updates last event id buffer`() {
        assertEquals("evt-42", SourcesApiClient.parseSseIdFieldValue("id: evt-42"))
        assertEquals("evt-42", SourcesApiClient.parseSseIdFieldValue("id:evt-42"))
        assertEquals("", SourcesApiClient.parseSseIdFieldValue("id:"))
        assertEquals("", SourcesApiClient.parseSseIdFieldValue("id: "))
        assertEquals(null, SourcesApiClient.parseSseIdFieldValue("id: bad\u0000id"))
    }

    @Test
    fun `contentToHtml escapes and wraps paragraphs`() {
        assertEquals(
            "<p>Hello<br/>world</p><p>Next</p>",
            SourcesApiClient.contentToHtml("Hello\nworld\n\nNext"),
        )
        assertEquals(
            "<p>A &amp; B &lt;C&gt;</p>",
            SourcesApiClient.contentToHtml("A & B <C>"),
        )
        assertEquals("", SourcesApiClient.contentToHtml("  "))
    }

    @Test
    fun `contentToHtml sanitizes html pass through instead of trusting it`() {
        val dirty = """
            <h1 onclick="alert(1)">Title</h1>
            <p>Hello<script>alert(1)</script></p>
            <p><img src=x onerror=alert(1)></p>
            <a href="javascript:alert(1)">link</a>
            <iframe src="https://evil.example"></iframe>
        """.trimIndent()

        val sanitized = SourcesApiClient.contentToHtml(dirty)

        assertTrue(sanitized.contains("<h1>Title</h1>"))
        assertTrue(sanitized.contains("Hello"))
        assertTrue(!sanitized.contains("<script", ignoreCase = true))
        assertTrue(!sanitized.contains("onclick", ignoreCase = true))
        assertTrue(!sanitized.contains("onerror", ignoreCase = true))
        assertTrue(!sanitized.contains("<iframe", ignoreCase = true))
        assertTrue(!sanitized.contains("<img", ignoreCase = true))
        assertTrue(!sanitized.contains("javascript:", ignoreCase = true))
        assertTrue(sanitized.contains("href=\"#\""))
    }

    @Test
    fun `sanitizeHtmlFragment keeps prose that looks like attributes`() {
        assertEquals(
            "<p>hello world review</p>",
            SourcesApiClient.sanitizeHtmlFragment("<p>hello world review</p>"),
        )
        assertEquals(
            "<p class=\"lead\">hello world review</p>",
            SourcesApiClient.sanitizeHtmlFragment(
                """<p class="lead" style="color:red" onclick="evil()">hello world review</p>""",
            ),
        )
        assertEquals(
            """<a href="#">click here now</a>""",
            SourcesApiClient.sanitizeHtmlFragment(
                """<a href="javascript:alert(1)" target="_blank">click here now</a>""",
            ),
        )
    }

    @Test
    fun `sanitizeHtmlFragment keeps safe table markup`() {
        val html = "<table><thead><tr><th>A</th></tr></thead><tbody><tr><td>1</td></tr></tbody></table>"
        assertEquals(html, SourcesApiClient.sanitizeHtmlFragment(html))
    }

    @Test
    fun `sheetContentToHtml sanitizes embedded table html`() {
        val dirty = "<table><tr><td onclick=evil()>x</td></tr></table><script>steal()</script>"
        val sanitized = SourcesApiClient.sheetContentToHtml(dirty)
        assertTrue(sanitized.contains("<table>"))
        assertTrue(sanitized.contains("<td>x</td>") || sanitized.contains(">x</td>"))
        assertTrue(!sanitized.contains("onclick", ignoreCase = true))
        assertTrue(!sanitized.contains("<script", ignoreCase = true))
    }

    @Test
    fun `sanitizeMultipartFileName strips header breakers and caps length`() {
        assertEquals(
            "report.pdf",
            SourcesApiClient.sanitizeMultipartFileName("report\r\n\".pdf"),
        )
        assertEquals(
            "foldername.pdf",
            SourcesApiClient.sanitizeMultipartFileName("folder\\name.pdf"),
        )
        assertEquals(
            "a".repeat(255),
            SourcesApiClient.sanitizeMultipartFileName("a".repeat(300)),
        )
        assertEquals("file", SourcesApiClient.sanitizeMultipartFileName("\"\\\r\n\""))
        assertEquals("paper.pdf", SourcesApiClient.sanitizeMultipartFileName("paper.pdf"))
    }

    @Test
    fun `sanitizeMultipartMimeType keeps type subtype and rejects injection`() {
        assertEquals("application/pdf", SourcesApiClient.sanitizeMultipartMimeType("application/pdf"))
        assertEquals(
            "application/pdf",
            SourcesApiClient.sanitizeMultipartMimeType("  application/pdf; charset=utf-8  "),
        )
        assertEquals(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            SourcesApiClient.sanitizeMultipartMimeType(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            ),
        )
        assertEquals(
            "application/octet-stream",
            SourcesApiClient.sanitizeMultipartMimeType("application/pdf\r\nspaceId: hijacked"),
        )
        assertEquals(
            "application/octet-stream",
            SourcesApiClient.sanitizeMultipartMimeType("text/plain\nContent-Disposition: form-data"),
        )
        assertEquals(
            "application/octet-stream",
            SourcesApiClient.sanitizeMultipartMimeType(""),
        )
        assertEquals(
            "application/octet-stream",
            SourcesApiClient.sanitizeMultipartMimeType("not-a-mime"),
        )
        assertEquals(
            "application/octet-stream",
            SourcesApiClient.sanitizeMultipartMimeType("application/pdf script=alert(1)"),
        )
    }

    @Test
    fun `encodeMultipartFileName percent encodes non ascii without filename star`() {
        assertEquals("paper.pdf", SourcesApiClient.encodeMultipartFileName("paper.pdf"))
        assertEquals(
            "r%C3%A9sum%C3%A9.pdf",
            SourcesApiClient.encodeMultipartFileName("résumé.pdf"),
        )
        assertEquals(
            "notes%E2%80%94final.txt",
            SourcesApiClient.encodeMultipartFileName("notes—final.txt"),
        )
        val encoded = SourcesApiClient.encodeMultipartFileName("résumé.pdf")
        assertTrue(!encoded.contains("filename*"))
        assertTrue(encoded.all { it.code in 0x20..0x7E })
    }

    @Test
    fun `multipartBodyContentLength matches emitted multipart bytes`() {
        val boundary = "Boundary-test"
        val spaceId = "space-1"
        val title = "Résumé notes"
        val author = "Ada"
        val fileName = "résumé.pdf"
        val mimeType = "application/pdf"
        val fileBytes = byteArrayOf(1, 2, 3, 4, 5)
        val safeName = SourcesApiClient.encodeMultipartFileName(fileName)
        val safeMimeType = SourcesApiClient.sanitizeMultipartMimeType(mimeType)

        fun part(text: String) = text.toByteArray(Charsets.UTF_8)
        val expected = buildList {
            fun addForm(name: String, value: String) {
                add(part("--$boundary\r\n"))
                add(part("Content-Disposition: form-data; name=\"$name\"\r\n\r\n"))
                add(part(value))
                add(part("\r\n"))
            }
            addForm("spaceId", spaceId)
            addForm("sourceType", SourcesApiClient.SOURCE_TYPE_FILE)
            addForm("title", title)
            addForm("author", author)
            add(part("--$boundary\r\n"))
            add(part("Content-Disposition: form-data; name=\"file\"; filename=\"$safeName\"\r\n"))
            add(part("Content-Type: $safeMimeType\r\n\r\n"))
            add(fileBytes)
            add(part("\r\n"))
            add(part("--$boundary--\r\n"))
        }.sumOf { it.size.toLong() }

        assertEquals(
            expected,
            SourcesApiClient.multipartBodyContentLength(
                boundary = boundary,
                spaceId = spaceId,
                title = title,
                author = author,
                fileName = fileName,
                mimeType = mimeType,
                fileBytesSize = fileBytes.size,
            ),
        )
    }

    @Test
    fun `randomMultipartBoundary uses uuid suffix without wall clock`() {
        val first = SourcesApiClient.randomMultipartBoundary()
        val second = SourcesApiClient.randomMultipartBoundary()
        assertTrue(first.startsWith("Boundary-"))
        assertTrue(second.startsWith("Boundary-"))
        assertEquals(32, first.removePrefix("Boundary-").length)
        assertTrue(first.removePrefix("Boundary-").all { it.isDigit() || it in 'a'..'f' })
        assertTrue(first != second)
    }

    @Test
    fun `chooseMultipartBoundary retries when candidate appears in file bytes`() {
        val colliding = "Boundary-deadbeef"
        val safe = "Boundary-cafebabe"
        var calls = 0
        val boundary = SourcesApiClient.chooseMultipartBoundary(
            fileBytes = "prefix $colliding suffix".toByteArray(Charsets.US_ASCII),
            boundaryFactory = {
                calls++
                if (calls == 1) colliding else safe
            },
        )
        assertEquals(safe, boundary)
        assertEquals(2, calls)
    }

    @Test
    fun `chooseMultipartBoundary throws when every candidate collides`() {
        val colliding = "Boundary-always"
        assertThrows(IOException::class.java) {
            SourcesApiClient.chooseMultipartBoundary(
                fileBytes = colliding.toByteArray(Charsets.US_ASCII),
                boundaryFactory = { colliding },
                maxAttempts = 3,
            )
        }
    }

    @Test
    fun `sseBackoffMillis grows exponentially and caps`() {
        assertEquals(1_000L, SourcesApiClient.sseBackoffMillis(1))
        assertEquals(2_000L, SourcesApiClient.sseBackoffMillis(2))
        assertEquals(4_000L, SourcesApiClient.sseBackoffMillis(3))
        assertEquals(8_000L, SourcesApiClient.sseBackoffMillis(4))
        assertEquals(16_000L, SourcesApiClient.sseBackoffMillis(5))
        assertEquals(16_000L, SourcesApiClient.sseBackoffMillis(6))
        assertEquals(5, SourcesApiClient.MAX_SSE_RECONNECT_ATTEMPTS)
    }

    @Test
    fun `nextSseReconnectAttempt keeps counting for short flaps`() {
        val openedAt = 1_000L
        assertEquals(
            3,
            SourcesApiClient.nextSseReconnectAttempt(
                currentAttempt = 2,
                openedAtMs = openedAt,
                nowMs = openedAt + 500L,
            ),
        )
        assertEquals(
            1,
            SourcesApiClient.nextSseReconnectAttempt(
                currentAttempt = 4,
                openedAtMs = openedAt,
                nowMs = openedAt + SourcesApiClient.MIN_SSE_STABLE_OPEN_MS,
            ),
        )
        assertEquals(
            5,
            SourcesApiClient.nextSseReconnectAttempt(
                currentAttempt = 4,
                openedAtMs = null,
                nowMs = openedAt,
            ),
        )
    }

    @Test
    fun `fileExtensionFrom prefers fileName then fileType`() {
        assertEquals("pdf", SourcesApiClient.fileExtensionFrom("paper.PDF", ""))
        assertEquals("pdf", SourcesApiClient.fileExtensionFrom("", "application/pdf"))
        assertEquals("md", SourcesApiClient.fileExtensionFrom("", "text/markdown"))
    }

    @Test
    fun `fileExtensionFrom ignores null fileType`() {
        assertEquals("", SourcesApiClient.fileExtensionFrom("", "null"))
        assertEquals("", SourcesApiClient.fileExtensionFrom("null", "null"))
    }

    @Test
    fun `contentFormatFrom maps spreadsheet extensions to SHEET`() {
        assertEquals(SourceContentFormat.SHEET, SourcesApiClient.contentFormatFrom("xlsx"))
        assertEquals(SourceContentFormat.SHEET, SourcesApiClient.contentFormatFrom("csv"))
        assertEquals(SourceContentFormat.SLIDES, SourcesApiClient.contentFormatFrom("pptx"))
        assertEquals(SourceContentFormat.DOCUMENT, SourcesApiClient.contentFormatFrom("pdf"))
    }

    @Test
    fun `sheetContentToHtml builds table from TSV`() {
        val html = SourcesApiClient.sheetContentToHtml("Metric\tQ1\tQ2\nSources\t10\t12")
        assertTrue(html.contains("<table>"))
        assertTrue(html.contains("<th>Metric</th>"))
        assertTrue(html.contains("<td>Sources</td>"))
        assertTrue(html.contains("<td>12</td>"))
    }

    @Test
    fun `parsePreviewUrl reads data previewUrl`() {
        val url = SourcesApiClient.parsePreviewUrl(
            """
            {
              "status": "success",
              "data": {
                "previewUrl": "http://localhost:9000/folio-sources/sources/a24bc98e/paper.pdf"
              }
            }
            """.trimIndent(),
        )

        assertEquals(
            "http://localhost:9000/folio-sources/sources/a24bc98e/paper.pdf",
            url,
        )
    }

    @Test
    fun `parseStructuredContent accepts legacy html fragment`() {
        val structured = SourcesApiClient.parseStructuredContent(
            """<h1 onclick="alert(1)">Neural Networks</h1><p>Article body.</p><script>evil()</script>""",
        ) as StructuredContent.Document

        assertEquals("<h1>Neural Networks</h1><p>Article body.</p>", structured.html)
        assertTrue(!structured.html.contains("<script", ignoreCase = true))
        assertTrue(!structured.html.contains("onclick", ignoreCase = true))
    }

    @Test
    fun `parseStructuredContent returns null for blank html`() {
        assertNull(SourcesApiClient.parseStructuredContent("   "))
        assertNull(SourcesApiClient.parseStructuredContent(null))
    }

    @Test
    fun `parsePreviewUrl returns null when missing`() {
        assertNull(SourcesApiClient.parsePreviewUrl("""{"status":"success","data":{}}"""))
        assertNull(SourcesApiClient.parsePreviewUrl(""))
    }

    @Test
    fun `sheetContentToHtml keeps existing html table`() {
        val table = "<table><tr><td>A</td></tr></table>"
        assertEquals(table, SourcesApiClient.sheetContentToHtml(table))
    }
}
