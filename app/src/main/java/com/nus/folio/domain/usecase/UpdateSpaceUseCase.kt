package com.nus.folio.domain.usecase

import com.nus.folio.domain.model.Space
import com.nus.folio.domain.repository.SpaceRepository

class UpdateSpaceUseCase(
    private val repository: SpaceRepository,
) {
    suspend operator fun invoke(
        spaceId: String,
        name: String,
        researchObjective: String,
    ): Result<Space> = repository.updateSpace(
        spaceId = spaceId,
        name = name,
        researchObjective = researchObjective,
    )
}
