package com.nus.folio.domain.usecase

import com.nus.folio.domain.model.AuthSession
import com.nus.folio.domain.repository.AuthRepository

class SignInWithAppleUseCase(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(): Result<AuthSession> = repository.signInWithApple()
}
