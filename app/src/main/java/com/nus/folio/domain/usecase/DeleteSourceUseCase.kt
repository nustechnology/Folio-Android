package com.nus.folio.domain.usecase

import com.nus.folio.domain.repository.SourceRepository

class DeleteSourceUseCase(
    private val repository: SourceRepository,
) {
    suspend operator fun invoke(sourceId: String): Result<Unit> =
        repository.deleteSource(sourceId)
}
