package com.nus.folio.domain.usecase

import com.nus.folio.domain.model.Source
import com.nus.folio.domain.repository.SourceRepository

class UpdateSourceUseCase(
    private val repository: SourceRepository,
) {
    suspend operator fun invoke(source: Source): Result<Source> =
        repository.updateSource(source)
}
