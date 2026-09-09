package com.nus.folio.data.network

import com.nus.folio.domain.util.NotebookHtml
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.HttpURLConnection

class NotebookApiClientMappingTest {

    @Test
    fun `spaceNotebook path encodes space id`() {
        assertEquals(
            "https://example.test/api/v1/spaces/space%2F1/notebook",
            FolioApiPaths.spaceNotebook("space/1", baseUrl = "https://example.test"),
        )
    }

    @Test
    fun `buildNotebookRequestJson wraps html content`() {
        val html = NotebookHtml.markdownToHtml("# Title")
        val json = NotebookApiClient.buildNotebookRequestJson(html)
        assertEquals("""{"content":"<h1>Title</h1>"}""", json)
    }

    @Test
    fun `buildNotebookRequestJson sends empty content for blank notebook`() {
        val json = NotebookApiClient.buildNotebookRequestJson("")
        assertEquals("""{"content":""}""", json)
    }

    @Test
    fun `buildNotebookRequestJson escapes quotes newlines and control characters`() {
        val json = NotebookApiClient.buildNotebookRequestJson("a\"b\nc\u0001")
        assertEquals("""{"content":"a\"b\nc\u0001"}""", json)
    }

    @Test
    fun `parseNotebookPayload maps nested notebook html to markdown`() {
        val notebook = NotebookApiClient.parseNotebookPayload(
            responseBody = """
                {
                  "status": "success",
                  "data": {
                    "notebook": {
                      "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                      "researchSpaceId": "space-1",
                      "content": "<h1>Aged-Care Operations</h1><p>Providers describe duplicate entry across systems.</p>",
                      "createdAt": "2026-08-17T03:36:46.135Z",
                      "updatedAt": "2026-08-17T03:36:46.135Z"
                    }
                  }
                }
            """.trimIndent(),
            fallbackSpaceId = "fallback",
        )
        assertEquals("space-1", notebook.spaceId)
        assertEquals(
            "# Aged-Care Operations\n\nProviders describe duplicate entry across systems.",
            notebook.content,
        )
        assertEquals(false, notebook.isReadOnly)
    }

    @Test
    fun `parseNotebookPayload keeps empty content`() {
        val notebook = NotebookApiClient.parseNotebookPayload(
            responseBody = """{"data":{"notebook":{"researchSpaceId":"space-1","content":""}}}""",
            fallbackSpaceId = "space-1",
        )
        assertEquals("", notebook.content)
    }

    @Test
    fun `parseNotebookPayload fails when content string is invalid`() {
        try {
            NotebookApiClient.parseNotebookPayload(
                responseBody = """{"data":{"notebook":{"researchSpaceId":"space-1","content":"hello\qworld"}}}""",
                fallbackSpaceId = "space-1",
            )
            org.junit.Assert.fail("Expected IOException")
        } catch (error: java.io.IOException) {
            assertTrue(error.message.orEmpty().contains("invalid content"))
        }
    }

    @Test
    fun `notebookFromHttpResponse maps 404 to NotebookNotFoundException`() {
        try {
            NotebookApiClient.notebookFromHttpResponse(
                code = HttpURLConnection.HTTP_NOT_FOUND,
                body = """{"message":"Notebook not found"}""",
                spaceId = "space-1",
            )
            org.junit.Assert.fail("Expected NotebookNotFoundException")
        } catch (error: NotebookNotFoundException) {
            assertEquals("Get notebook failed (HTTP 404)", error.message)
        }
    }

    @Test
    fun `notebookFromHttpResponse maps 401 to UnauthorizedException`() {
        try {
            NotebookApiClient.notebookFromHttpResponse(
                code = HttpURLConnection.HTTP_UNAUTHORIZED,
                body = """{"message":"Expired"}""",
                spaceId = "space-1",
            )
            org.junit.Assert.fail("Expected UnauthorizedException")
        } catch (error: UnauthorizedException) {
            assertEquals("Expired", error.message)
        }
    }

    @Test
    fun `parseNotebookPayload unescapes quotes inside html`() {
        val notebook = NotebookApiClient.parseNotebookPayload(
            responseBody = """{"data":{"notebook":{"researchSpaceId":"space-1","content":"<a href=\"https://example.test\">docs</a>"}}}""",
            fallbackSpaceId = "space-1",
        )
        assertTrue(notebook.content.contains("[docs](https://example.test)"))
    }

    @Test
    fun `parseNotebookPayload decodes unicode and keeps literal backslash-n`() {
        val notebook = NotebookApiClient.parseNotebookPayload(
            responseBody = """{"data":{"notebook":{"researchSpaceId":"space-1","content":"<p>caf\u00e9 a\\nb</p>"}}}""",
            fallbackSpaceId = "space-1",
        )
        assertEquals("café a\\nb", notebook.content)
    }

    @Test
    fun `parseNotebookPayload marks unsupported html as read only and keeps text`() {
        val notebook = NotebookApiClient.parseNotebookPayload(
            responseBody = """{"data":{"notebook":{"researchSpaceId":"space-1","content":"<h1>T</h1><h4>Sub</h4><pre>code()</pre><p>Body</p>"}}}""",
            fallbackSpaceId = "space-1",
        )
        assertEquals("# T\n\nSub\n\ncode()\n\nBody", notebook.content)
        assertTrue(notebook.isReadOnly)
    }
}
