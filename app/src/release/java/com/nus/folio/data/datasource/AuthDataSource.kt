package com.nus.folio.data.datasource

import com.nus.folio.domain.model.AuthSession
import com.nus.folio.domain.model.UserProfile

/**
 * Release stub — no auth backend is wired yet.
 * [com.nus.folio.data.auth.AuthCapabilities.isBackendAvailable] is false in release so the
 * login UI stays disabled instead of presenting a non-functional sign-in flow.
 *
 * Replace this class with a real backend client before shipping authentication.
 */
class AuthDataSource {

    suspend fun signUp(
        name: String,
        email: String,
        password: String,
        confirmPassword: String,
    ): AuthSession = throw authUnavailable()

    suspend fun signIn(email: String, password: String): AuthSession =
        throw authUnavailable()

    suspend fun refresh(refreshToken: String): AuthSession =
        throw authUnavailable()

    suspend fun logout(accessToken: String?): Unit =
        throw authUnavailable()

    suspend fun getUser(id: String, accessToken: String): UserProfile =
        throw authUnavailable()

    suspend fun signInWithApple(): AuthSession =
        throw authUnavailable()

    suspend fun requestPasswordReset(email: String): Unit =
        throw authUnavailable()

    private fun authUnavailable(): Nothing =
        throw UnsupportedOperationException(
            "Authentication backend is not wired in release builds yet",
        )
}
