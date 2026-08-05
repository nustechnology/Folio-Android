package com.nus.folio.domain.usecase

import com.nus.folio.domain.model.CreateSourceRequest
import com.nus.folio.domain.model.Source
import com.nus.folio.domain.repository.SourceRepository

class CreateSourceUseCase(
    private val repository: SourceRepository,
) {
    suspend operator fun invoke(request: CreateSourceRequest): Result<Source> =
        repository.createSource(request)
}
