package com.nus.folio.data.datasource

import com.nus.folio.data.auth.AuthCapabilities
import com.nus.folio.domain.model.AuthSession
import kotlinx.coroutines.delay

/**
 * Debug-only mock auth for local UI development.
 * Seeds the demo account and stores credentials registered via [signUp] for the process lifetime.
 * Not compiled into release builds.
 *
 * Demo: [AuthCapabilities.defaultLoginEmail] / [AuthCapabilities.defaultLoginPassword]
 *
 * Paired with [AuthCapabilities.isBackendAvailable] = true.
 */
class AuthDataSource {

    private data class Account(
        val displayName: String,
        val password: String,
    )

    private val accounts = mutableMapOf(
        AuthCapabilities.defaultLoginEmail.lowercase() to Account(
            DEMO_DISPLAY_NAME,
            AuthCapabilities.defaultLoginPassword,
        ),
    )

    suspend fun signUp(name: String, email: String, password: String): AuthSession {
        delay(400)
        require(name.isNotBlank()) { "Name is required" }
        require(email.isNotBlank()) { "Email is required" }
        require(password.isNotBlank()) { "Password is required" }

        val normalizedEmail = email.trim()
        val displayName = name.trim()
        val key = normalizedEmail.lowercase()
        require(key !in accounts) { "Account already exists" }

        accounts[key] = Account(displayName, password)
        return AuthSession(email = normalizedEmail, displayName = displayName)
    }

    suspend fun signIn(email: String, password: String): AuthSession {
        delay(400)
        require(email.isNotBlank()) { "Email is required" }
        require(password.isNotBlank()) { "Password is required" }

        val normalizedEmail = email.trim()
        val account = accounts[normalizedEmail.lowercase()]
        if (account == null || account.password != password) {
            throw IllegalArgumentException("Invalid credentials")
        }

        return AuthSession(email = normalizedEmail, displayName = account.displayName)
    }

    suspend fun signInWithApple(): AuthSession {
        delay(400)
        return AuthSession(
            email = APPLE_EMAIL,
            displayName = APPLE_DISPLAY_NAME,
        )
    }

    suspend fun requestPasswordReset(email: String) {
        delay(300)
        require(email.isNotBlank()) { "Email is required" }
    }

    private companion object {
        const val DEMO_DISPLAY_NAME = "Folio Researcher"
        const val APPLE_EMAIL = "apple.user@folio.app"
        const val APPLE_DISPLAY_NAME = "Apple User"
    }
}
