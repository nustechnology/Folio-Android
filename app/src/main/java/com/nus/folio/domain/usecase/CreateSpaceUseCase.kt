package com.nus.folio.domain.usecase

import com.nus.folio.domain.model.Space
import com.nus.folio.domain.repository.SpaceRepository

class CreateSpaceUseCase(
    private val repository: SpaceRepository,
) {
    suspend operator fun invoke(
        name: String,
        researchObjective: String,
    ): Result<Space> = repository.createSpace(
        name = name,
        researchObjective = researchObjective,
    )
}
