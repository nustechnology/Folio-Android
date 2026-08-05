package com.nus.folio.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthApiExceptionTest {

    @Test
    fun `indicatesEmailAlreadyExists is true for conflict status`() {
        assertTrue(AuthApiException("Conflict", statusCode = 409).indicatesEmailAlreadyExists())
    }

    @Test
    fun `indicatesEmailAlreadyExists is true for known message patterns`() {
        assertTrue(AuthApiException("Email already registered").indicatesEmailAlreadyExists())
        assertTrue(
            AuthApiException("An account with this email already exists")
                .indicatesEmailAlreadyExists(),
        )
        assertTrue(AuthApiException("This email already exists").indicatesEmailAlreadyExists())
    }

    @Test
    fun `indicatesEmailAlreadyExists is false for unrelated errors`() {
        assertFalse(AuthApiException("Rate limited").indicatesEmailAlreadyExists())
        assertFalse(AuthApiException("Bad request", statusCode = 400).indicatesEmailAlreadyExists())
    }
}
