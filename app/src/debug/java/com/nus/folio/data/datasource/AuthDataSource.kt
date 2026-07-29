package com.nus.folio.data.datasource

import com.nus.folio.domain.model.AuthSession
import kotlinx.coroutines.delay

/**
 * Debug-only mock auth for local UI development.
 * Only the documented demo credentials succeed; not compiled into release builds.
 *
 * Demo: [DEMO_EMAIL] / [DEMO_PASSWORD]
 *
 * Paired with [com.nus.folio.data.auth.AuthCapabilities.isBackendAvailable] = true.
 */
class AuthDataSource {

    suspend fun signIn(email: String, password: String): AuthSession {
        delay(400)
        require(email.isNotBlank()) { "Email is required" }
        require(password.isNotBlank()) { "Password is required" }

        val normalizedEmail = email.trim()
        if (
            !normalizedEmail.equals(DEMO_EMAIL, ignoreCase = true) ||
            password != DEMO_PASSWORD
        ) {
            throw IllegalArgumentException("Invalid credentials")
        }

        return AuthSession(email = normalizedEmail)
    }

    suspend fun requestPasswordReset(email: String) {
        delay(300)
        require(email.isNotBlank()) { "Email is required" }
    }

    private companion object {
        const val DEMO_EMAIL = "researcher@folio.app"
        const val DEMO_PASSWORD = "folio-debug"
    }
}
