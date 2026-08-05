package com.nus.folio.data.network

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HttpDebugLoggerTest {

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
