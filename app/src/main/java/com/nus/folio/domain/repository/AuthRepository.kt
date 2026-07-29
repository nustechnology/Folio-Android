package com.nus.folio.domain.repository

import com.nus.folio.domain.model.AuthSession

interface AuthRepository {
    suspend fun signIn(email: String, password: String): Result<AuthSession>
    suspend fun requestPasswordReset(email: String): Result<Unit>
}
