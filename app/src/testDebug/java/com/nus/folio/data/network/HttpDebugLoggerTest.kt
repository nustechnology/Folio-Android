package com.nus.folio.data.network

import com.nus.folio.BuildConfig
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HttpDebugLoggerTest {

    @After
    fun tearDown() {
        HttpDebugLogger.enabledOverride = null
    }

    @Test
    fun `isEnabled defaults to BuildConfig DEBUG`() {
        HttpDebugLogger.enabledOverride = null
        assertEquals(BuildConfig.DEBUG, HttpDebugLogger.isEnabled)
    }

    @Test
    fun `log methods are inert when disabled`() {
        HttpDebugLogger.enabledOverride = false
        assertFalse(HttpDebugLogger.isEnabled)

        // Must not throw; release builds share this logger and must stay silent.
        HttpDebugLogger.logRequest(method = "GET", url = "https://example.com", body = "secret")
        HttpDebugLogger.logResponse(
            method = "GET",
            url = "https://example.com",
            code = 200,
            body = """{"accessToken":"tok"}""",
        )
        HttpDebugLogger.logError(
            method = "GET",
            url = "https://example.com",
            error = RuntimeException("boom"),
        )
        HttpDebugLogger.logEvent("should not log")
    }

    @Test
    fun `formatBody redacts passwords and tokens in json`() {
        val raw = """
            {
              "email":"a@folio.app",
              "password":"secret",
              "confirmPassword":"secret",
              "tokens":{"accessToken":"access-1","refreshToken":"refresh-1"}
            }
        """.trimIndent()

        val formatted = HttpDebugLogger.formatBody(raw)

        assertTrue(formatted.contains("a@folio.app"))
        assertTrue(formatted.contains("***"))
        assertFalse(formatted.contains("secret"))
        assertFalse(formatted.contains("access-1"))
        assertFalse(formatted.contains("refresh-1"))
    }

    @Test
    fun `formatBody redacts bearer tokens in plain text`() {
        val formatted = HttpDebugLogger.formatBody("Authorization: Bearer super-secret-token")

        assertTrue(formatted.contains("Bearer ***"))
        assertFalse(formatted.contains("super-secret-token"))
    }
}
