package com.nus.folio.domain.usecase

import com.nus.folio.domain.repository.AuthRepository

class ClearAuthSessionUseCase(
    private val repository: AuthRepository,
) {
    /**
     * Clears the local session immediately, then best-effort calls the logout API.
     */
    suspend operator fun invoke(): Result<Unit> = repository.signOut()
}
