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
        assertEquals(SourceType.WEB, SourcesJsonParsers.mapSourceType("Web"))
        assertEquals(SourceType.TEXT, SourcesJsonParsers.mapSourceType("Manual"))
        assertEquals(SourceType.FILE, SourcesJsonParsers.mapSourceType("File"))
        assertEquals(SourceType.BOOK, SourcesJsonParsers.mapSourceType("Book"))
    }

    @Test
    fun `mapProcessingState maps API values`() {
        assertEquals(SourceStatus.PROCESSING, SourcesJsonParsers.mapProcessingState("added"))
        assertEquals(SourceStatus.PROCESSING, SourcesJsonParsers.mapProcessingState("processing"))
        assertEquals(SourceStatus.READY, SourcesJsonParsers.mapProcessingState("ready"))
        assertEquals(SourceStatus.FAILED, SourcesJsonParsers.mapProcessingState("failed"))
    }

    @Test
    fun `formatAddedLabel uses relative time`() {
        val now = 1_700_000_000_000L
        assertEquals(
            "Added just now",
            SourcesJsonParsers.formatAddedLabel("2023-11-14T22:13:20.000Z", now),
        )
        assertEquals(
            "Added 2d ago",
            SourcesJsonParsers.formatAddedLabel("2023-11-12T22:13:20.000Z", now),
        )
    }

    @Test
    fun `parseSseDataPayload maps state and progress`() {
        val event = SourcesSseClient.parseSseDataPayload(
            """{"sourceId":"532a3be6-cd85-48ef-aa28-8d2ba8bb5eb0","state":"extracting_text","progress":25}""",
        )

        assertEquals("532a3be6-cd85-48ef-aa28-8d2ba8bb5eb0", event?.sourceId)
        assertEquals(SourceProcessingState.EXTRACTING_TEXT, event?.state)
        assertEquals(25, event?.progress)
        assertEquals(1, event?.completedStepCount())
    }

    @Test
    fun `contentToHtml escapes and wraps paragraphs`() {
        assertEquals(
            "<p>Hello<br/>world</p><p>Next</p>",
            SourcesJsonParsers.contentToHtml("Hello\nworld\n\nNext"),
        )
        assertEquals(
            "<p>A &amp; B &lt;C&gt;</p>",
            SourcesJsonParsers.contentToHtml("A & B <C>"),
        )
        assertEquals("", SourcesJsonParsers.contentToHtml("  "))
    }

    @Test
    fun `contentToHtml sanitizes html pass through instead of trusting it`() {
        val dirty = """
            <h1 onclick="alert(1)">Title</h1>
            <p>Hello<script>alert(1)</script></p>
            <p><img src="https://cdn.example/a.png" onerror=alert(1)></p>
            <a href="javascript:alert(1)">link</a>
            <iframe src="https://evil.example"></iframe>
        """.trimIndent()

        val sanitized = SourcesJsonParsers.contentToHtml(dirty)

        assertTrue(sanitized.contains("<h1>Title</h1>"))
        assertTrue(sanitized.contains("Hello"))
        assertTrue(!sanitized.contains("<script", ignoreCase = true))
        assertTrue(!sanitized.contains("onclick", ignoreCase = true))
        assertTrue(!sanitized.contains("onerror", ignoreCase = true))
        assertTrue(!sanitized.contains("<iframe", ignoreCase = true))
        assertTrue(sanitized.contains("""<img src="https://cdn.example/a.png">"""))
        assertTrue(!sanitized.contains("javascript:", ignoreCase = true))
        assertTrue(sanitized.contains("href=\"#\""))
    }

    @Test
    fun `sanitizeHtmlFragment keeps public network images and strips local or dangerous src`() {
        assertEquals(
            """<p><img src="https://folio.nustechnology.com/media/photo.jpg" alt="Photo"></p>""",
            SourcesApiClient.sanitizeHtmlFragment(
                """<p><img src="https://folio.nustechnology.com/media/photo.jpg" alt="Photo" style="border:1px"></p>""",
            ),
        )
        assertEquals(
            """<p><img src="https://cdn.example/photo.jpg" alt="Photo"></p>""",
            SourcesApiClient.sanitizeHtmlFragment(
                """<p><img src="https://cdn.example/photo.jpg" alt="Photo" style="border:1px"></p>""",
            ),
        )
        assertEquals(
            """<img src="">""",
            SourcesApiClient.sanitizeHtmlFragment("""<img src="http://127.0.0.1/secret.png">"""),
        )
        assertEquals(
            """<img src="">""",
            SourcesApiClient.sanitizeHtmlFragment("""<img src="javascript:alert(1)">"""),
        )
        assertEquals(
            """<img src="">""",
            SourcesApiClient.sanitizeHtmlFragment("""<img src="data:text/html,<script>alert(1)</script>">"""),
        )
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
    fun `sanitizeHtmlFragment keeps figure quote and code used in PDF documents`() {
        val html = """
            <div class="pdf-page">
            <figure><img src="https://folio.nustechnology.com/media/a.png" alt="Chart"><figcaption>Fig. 1</figcaption></figure>
            <blockquote>A cited line</blockquote>
            <pre><code>x = 1</code></pre>
            <table><caption>Results</caption><tr><td>1</td></tr></table>
            <p>H<sub>2</sub>O and x<sup>2</sup></p>
            </div>
        """.trimIndent().replace("\n", "")
        assertEquals(html, SourcesApiClient.sanitizeHtmlFragment(html))
    }

    @Test
    fun `sheetContentToHtml sanitizes embedded table html`() {
        val dirty = "<table><tr><td onclick=evil()>x</td></tr></table><script>steal()</script>"
        val sanitized = SourcesJsonParsers.sheetContentToHtml(dirty)
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
        assertEquals(1_000L, SourcesSseClient.sseBackoffMillis(1))
        assertEquals(2_000L, SourcesSseClient.sseBackoffMillis(2))
        assertEquals(4_000L, SourcesSseClient.sseBackoffMillis(3))
        assertEquals(8_000L, SourcesSseClient.sseBackoffMillis(4))
        assertEquals(16_000L, SourcesSseClient.sseBackoffMillis(5))
        assertEquals(16_000L, SourcesSseClient.sseBackoffMillis(6))
        assertEquals(5, SourcesSseClient.MAX_SSE_RECONNECT_ATTEMPTS)
    }

    @Test
    fun `fileExtensionFrom prefers fileName then fileType`() {
        assertEquals("pdf", SourcesJsonParsers.fileExtensionFrom("paper.PDF", ""))
        assertEquals("pdf", SourcesJsonParsers.fileExtensionFrom("", "application/pdf"))
        assertEquals("md", SourcesJsonParsers.fileExtensionFrom("", "text/markdown"))
    }

    @Test
    fun `fileExtensionFrom ignores null fileType`() {
        assertEquals("", SourcesJsonParsers.fileExtensionFrom("", "null"))
        assertEquals("", SourcesJsonParsers.fileExtensionFrom("null", "null"))
    }

    @Test
    fun `contentFormatFrom maps spreadsheet extensions to SHEET`() {
        assertEquals(SourceContentFormat.SHEET, SourcesJsonParsers.contentFormatFrom("xlsx"))
        assertEquals(SourceContentFormat.SHEET, SourcesJsonParsers.contentFormatFrom("csv"))
        assertEquals(SourceContentFormat.SLIDES, SourcesJsonParsers.contentFormatFrom("pptx"))
        assertEquals(SourceContentFormat.DOCUMENT, SourcesJsonParsers.contentFormatFrom("pdf"))
    }

    @Test
    fun `sheetContentToHtml builds table from TSV`() {
        val html = SourcesJsonParsers.sheetContentToHtml("Metric\tQ1\tQ2\nSources\t10\t12")
        assertTrue(html.contains("<table>"))
        assertTrue(html.contains("<th>Metric</th>"))
        assertTrue(html.contains("<td>Sources</td>"))
        assertTrue(html.contains("<td>12</td>"))
    }

    @Test
    fun `parsePreviewUrl reads data previewUrl`() {
        val url = SourcesJsonParsers.parsePreviewUrl(
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
        val structured = SourcesJsonParsers.parseStructuredContent(
            """<h1 onclick="alert(1)">Neural Networks</h1><p>Article body.</p><script>evil()</script>""",
        ) as StructuredContent.Document

        assertEquals("<h1>Neural Networks</h1><p>Article body.</p>", structured.html)
        assertTrue(!structured.html.contains("<script", ignoreCase = true))
        assertTrue(!structured.html.contains("onclick", ignoreCase = true))
    }

    @Test
    fun `parseStructuredContent returns null for blank html`() {
        assertNull(SourcesJsonParsers.parseStructuredContent("   "))
        assertNull(SourcesJsonParsers.parseStructuredContent(null))
    }

    @Test
    fun `parsePreviewUrl returns null when missing`() {
        assertNull(SourcesJsonParsers.parsePreviewUrl("""{"status":"success","data":{}}"""))
        assertNull(SourcesJsonParsers.parsePreviewUrl(""))
    }

    @Test
    fun `sheetContentToHtml keeps existing html table`() {
        val table = "<table><tr><td>A</td></tr></table>"
        assertEquals(table, SourcesJsonParsers.sheetContentToHtml(table))
    }
}
