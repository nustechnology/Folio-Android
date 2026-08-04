package com.nus.folio.domain.usecase

import com.nus.folio.domain.model.AuthSession
import com.nus.folio.domain.repository.AuthRepository

class SignUpUseCase(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(
        name: String,
        email: String,
        password: String,
        confirmPassword: String,
    ): Result<AuthSession> = repository.signUp(name, email, password, confirmPassword)
}
