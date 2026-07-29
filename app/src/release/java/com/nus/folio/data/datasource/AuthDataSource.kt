package com.nus.folio.data.datasource

/**
 * Release stub — no auth backend is wired yet.
 * [com.nus.folio.data.auth.AuthCapabilities.isBackendAvailable] is false in release so the
 * login UI stays disabled instead of presenting a non-functional sign-in flow.
 *
 * Replace this class with a real backend client before shipping authentication.
 */
class AuthDataSource {

    suspend fun signUp(name: String, email: String, password: String): Nothing =
        throw authUnavailable()

    suspend fun signIn(email: String, password: String): Nothing =
        throw authUnavailable()

    suspend fun signInWithApple(): Nothing =
        throw authUnavailable()

    suspend fun requestPasswordReset(email: String): Nothing =
        throw authUnavailable()

    private fun authUnavailable(): Nothing =
        throw UnsupportedOperationException(
            "Authentication backend is not wired in release builds yet",
        )
}
