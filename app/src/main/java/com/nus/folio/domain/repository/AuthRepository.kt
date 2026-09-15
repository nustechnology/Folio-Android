package com.nus.folio.domain.repository

import com.nus.folio.domain.model.AuthSession

interface AuthRepository {
    suspend fun signUp(
        name: String,
        email: String,
        password: String,
        confirmPassword: String,
    ): Result<AuthSession>
    suspend fun signIn(email: String, password: String): Result<AuthSession>
    suspend fun signInWithApple(): Result<AuthSession>
    suspend fun refreshSession(): Result<AuthSession>
    suspend fun syncCurrentUser(userId: String = "me"): Result<AuthSession>
    suspend fun signOut(): Result<Unit>
    /** Loads any persisted session into memory. Safe to call more than once. */
    suspend fun restoreSession()
    fun getCurrentSession(): AuthSession?
    suspend fun clearSession()
}
