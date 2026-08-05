package com.nus.folio.domain.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthInputRulesTest {

    @Test
    fun `isValidEmail accepts typical addresses`() {
        assertTrue(AuthInputRules.isValidEmail("user@folio.app"))
        assertTrue(AuthInputRules.isValidEmail("  jordan.lee@example.com  "))
    }

    @Test
    fun `isValidEmail rejects missing at or domain`() {
        assertFalse(AuthInputRules.isValidEmail(""))
        assertFalse(AuthInputRules.isValidEmail("not-an-email"))
        assertFalse(AuthInputRules.isValidEmail("missing-domain@"))
        assertFalse(AuthInputRules.isValidEmail("@nodomain.com"))
        assertFalse(AuthInputRules.isValidEmail("a@b"))
    }

    @Test
    fun `isPasswordLongEnough requires at least 4 characters`() {
        assertFalse(AuthInputRules.isPasswordLongEnough(""))
        assertFalse(AuthInputRules.isPasswordLongEnough("abc"))
        assertTrue(AuthInputRules.isPasswordLongEnough("abcd"))
        assertTrue(AuthInputRules.isPasswordLongEnough("secret"))
    }
}
