package com.nus.folio.domain.usecase

import com.nus.folio.domain.repository.SpaceRepository

class DeleteSpaceUseCase(
    private val repository: SpaceRepository,
) {
    suspend operator fun invoke(spaceId: String): Result<Unit> =
        repository.deleteSpace(spaceId)
}
