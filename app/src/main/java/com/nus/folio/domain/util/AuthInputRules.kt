package com.nus.folio.domain.util

/**
 * Shared client-side auth form rules (Sign In / Sign Up).
 */
object AuthInputRules {
    const val MIN_PASSWORD_LENGTH = 4

    private val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

    fun isValidEmail(email: String): Boolean = EMAIL_PATTERN.matches(email.trim())

    fun isPasswordLongEnough(password: String): Boolean =
        password.length >= MIN_PASSWORD_LENGTH
}
