package com.nus.folio.domain.usecase

import com.nus.folio.domain.repository.AuthRepository

class RequestPasswordResetUseCase(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(email: String): Result<Unit> = repository.requestPasswordReset(email)
}
