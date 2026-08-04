package com.nus.folio.domain.usecase

import com.nus.folio.domain.model.AuthSession
import com.nus.folio.domain.repository.AuthRepository

class SyncCurrentUserUseCase(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(userId: String = "me"): Result<AuthSession> =
        repository.syncCurrentUser(userId)
}
