package com.nus.folio.domain.usecase

import com.nus.folio.domain.model.Space
import com.nus.folio.domain.repository.SpaceRepository

class GetSpacesUseCase(
    private val repository: SpaceRepository,
) {
    suspend operator fun invoke(): Result<List<Space>> = repository.getSpaces()
}
