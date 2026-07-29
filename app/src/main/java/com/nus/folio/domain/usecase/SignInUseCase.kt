package com.nus.folio.domain.usecase

import com.nus.folio.domain.model.AuthSession
import com.nus.folio.domain.repository.AuthRepository

class SignInUseCase(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(
        email: String,
        password: String,
    ): Result<AuthSession> = repository.signIn(email, password)
}
