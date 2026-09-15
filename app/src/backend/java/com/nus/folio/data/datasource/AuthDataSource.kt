package com.nus.folio.data.datasource

import com.nus.folio.data.network.AuthApi
import com.nus.folio.data.network.AuthApiClient
import com.nus.folio.domain.model.AuthSession
import com.nus.folio.domain.model.UserProfile
import kotlinx.coroutines.delay

/**
 * Debug auth data source.
 * [signUp] / [signIn] / [refresh] / [logout] hit the real auth API; Apple stays a local mock.
 *
 * Paired with [com.nus.folio.data.auth.AuthCapabilities.isBackendAvailable] = true.
 */
class AuthDataSource(
    private val authApi: AuthApi = AuthApiClient(),
) {

    suspend fun signUp(
        name: String,
        email: String,
        password: String,
        confirmPassword: String,
    ): AuthSession {
        require(name.isNotBlank()) { "Name is required" }
        require(email.isNotBlank()) { "Email is required" }
        require(password.isNotBlank()) { "Password is required" }
        require(confirmPassword.isNotBlank()) { "Confirm password is required" }
        require(password == confirmPassword) { "Passwords do not match" }

        return authApi.signUp(
            name = name.trim(),
            email = email.trim(),
            password = password,
            confirmPassword = confirmPassword,
        )
    }

    suspend fun signIn(email: String, password: String): AuthSession {
        require(email.isNotBlank()) { "Email is required" }
        require(password.isNotBlank()) { "Password is required" }

        return authApi.login(
            email = email.trim(),
            password = password,
        )
    }

    suspend fun refresh(refreshToken: String): AuthSession {
        require(refreshToken.isNotBlank()) { "Refresh token is required" }
        return authApi.refresh(refreshToken.trim())
    }

    suspend fun logout(accessToken: String?) {
        authApi.logout(accessToken?.takeIf { it.isNotBlank() })
    }

    suspend fun getUser(id: String, accessToken: String): UserProfile {
        require(id.isNotBlank()) { "User id is required" }
        require(accessToken.isNotBlank()) { "Access token is required" }
        return authApi.getUser(id.trim(), accessToken.trim())
    }

    suspend fun signInWithApple(): AuthSession {
        delay(400)
        return AuthSession(
            email = APPLE_EMAIL,
            displayName = APPLE_DISPLAY_NAME,
            userId = APPLE_USER_ID,
        )
    }

    private companion object {
        const val APPLE_EMAIL = "apple.user@folio.app"
        const val APPLE_DISPLAY_NAME = "Apple User"
        const val APPLE_USER_ID = "apple-user"
    }
}
