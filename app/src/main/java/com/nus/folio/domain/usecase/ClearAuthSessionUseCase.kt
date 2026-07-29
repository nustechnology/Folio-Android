package com.nus.folio.domain.usecase

import com.nus.folio.domain.repository.AuthRepository

class ClearAuthSessionUseCase(
    private val repository: AuthRepository,
) {
    operator fun invoke() = repository.clearSession()
}
