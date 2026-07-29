package com.nus.folio.domain.usecase

import com.nus.folio.domain.model.AuthSession
import com.nus.folio.domain.repository.AuthRepository

class GetCurrentSessionUseCase(
    private val repository: AuthRepository,
) {
    operator fun invoke(): AuthSession? = repository.getCurrentSession()
}
