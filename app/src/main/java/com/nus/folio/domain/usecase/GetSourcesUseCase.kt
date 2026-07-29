package com.nus.folio.domain.usecase

import com.nus.folio.domain.model.SourceLibrary
import com.nus.folio.domain.repository.SourceRepository

class GetSourcesUseCase(
    private val repository: SourceRepository,
) {
    suspend operator fun invoke(): Result<SourceLibrary> = repository.getSources()
}
