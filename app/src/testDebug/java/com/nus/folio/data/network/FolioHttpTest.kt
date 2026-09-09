package com.nus.folio.data.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.HttpURLConnection

class FolioHttpTest {

    @Test
    fun `requireHttpsForCredentialedRequest rejects http with bearer token`() {
        val error = assertThrows(IOException::class.java) {
            FolioHttp.requireHttpsForCredentialedRequest(
                url = "http://example.com/api/v1/sources",
                headers = FolioHttp.jsonAcceptHeaders("token-123"),
            )
        }
        assertEquals("Credentialed requests must use HTTPS", error.message)
    }

    @Test
    fun `requireHttpsForCredentialedRequest allows https with bearer token`() {
        FolioHttp.requireHttpsForCredentialedRequest(
            url = "https://example.com/api/v1/sources",
            headers = FolioHttp.jsonAcceptHeaders("token-123"),
        )
    }

    @Test
    fun `requireHttpsForCredentialedRequest allows http without credentials`() {
        FolioHttp.requireHttpsForCredentialedRequest(
            url = "http://example.com/api/v1/sources",
            headers = FolioHttp.jsonAcceptHeaders(null),
        )
    }

    @Test
    fun `requireHttps rejects password auth over http`() {
        val error = assertThrows(IOException::class.java) {
            FolioHttp.requireHttps("http://example.com/api/v1/auth/login")
        }
        assertEquals("Credentialed requests must use HTTPS", error.message)
    }

    @Test
    fun `requireHttps allows https auth endpoints`() {
        FolioHttp.requireHttps("https://example.com/api/v1/auth/login")
        FolioHttp.requireHttps("https://example.com/api/v1/auth/sign-up")
        FolioHttp.requireHttps("https://example.com/api/v1/auth/refresh")
    }

    @Test
    fun `open rejects credentialed http before opening connection`() {
        val error = assertThrows(IOException::class.java) {
            FolioHttp.open(
                method = "GET",
                url = "http://example.com/api/v1/spaces",
                headers = FolioHttp.jsonAcceptHeaders("token-123"),
            )
        }
        assertEquals("Credentialed requests must use HTTPS", error.message)
    }

    @Test
    fun `parseErrorMessage prefers message field`() {
        val message = FolioHttp.parseErrorMessage(
            responseBody = """{"message":"Invalid credentials","error":"ignored"}""",
            code = 401,
            failureLabel = "Login",
        )
        assertEquals("Invalid credentials", message)
    }

    @Test
    fun `parseErrorMessage falls back to label and status`() {
        val message = FolioHttp.parseErrorMessage(
            responseBody = "",
            code = 500,
            failureLabel = "Get spaces",
        )
        assertEquals("Get spaces failed (HTTP 500)", message)
    }

    @Test
    fun `unauthorizedOrIo maps 401 to UnauthorizedException`() {
        val error = FolioHttp.unauthorizedOrIo(
            responseBody = """{"message":"Expired"}""",
            code = HttpURLConnection.HTTP_UNAUTHORIZED,
            failureLabel = "Get sources",
        )
        assertTrue(error is UnauthorizedException)
        assertEquals("Expired", error.message)
    }

    @Test
    fun `unauthorizedOrIo maps other codes to IOException`() {
        val error = FolioHttp.unauthorizedOrIo(
            responseBody = """{"detail":"Not found"}""",
            code = 404,
            failureLabel = "Get source",
        )
        assertEquals(IOException::class.java, error::class.java)
        assertEquals("Not found", error.message)
    }

    @Test
    fun `unauthorizedOrIo maps 413 to IOException`() {
        val error = FolioHttp.unauthorizedOrIo(
            responseBody = "",
            code = HttpURLConnection.HTTP_ENTITY_TOO_LARGE,
            failureLabel = "Save notebook",
        )
        assertEquals(IOException::class.java, error::class.java)
        assertEquals("Save notebook failed (HTTP 413)", error.message)
    }

    @Test
    fun `jsonAcceptHeaders includes bearer when token present`() {
        val headers = FolioHttp.jsonAcceptHeaders("token-123")
        assertEquals("application/json", headers["Accept"])
        assertEquals("true", headers["ngrok-skip-browser-warning"])
        assertEquals("Bearer token-123", headers["Authorization"])
    }

    @Test
    fun `jsonAcceptHeaders omits bearer when token blank`() {
        val headers = FolioHttp.jsonAcceptHeaders(null)
        assertTrue(!headers.containsKey("Authorization"))
    }

    @Test
    fun `jsonContentHeaders includes content type and bearer`() {
        val headers = FolioHttp.jsonContentHeaders("token-123")
        assertEquals("application/json", headers["Content-Type"])
        assertEquals("application/json", headers["Accept"])
        assertEquals("Bearer token-123", headers["Authorization"])
        assertTrue(!headers.containsKey("X-HTTP-Method-Override"))
    }

    @Test
    fun `open accepts PATCH connection method`() {
        val connection = FolioHttp.open(
            method = "PATCH",
            url = "https://example.com/api/v1/spaces/1",
            headers = FolioHttp.jsonContentHeaders("token-123"),
            doOutput = true,
        )
        try {
            assertEquals("PATCH", connection.requestMethod)
        } finally {
            connection.disconnect()
        }
    }
}
