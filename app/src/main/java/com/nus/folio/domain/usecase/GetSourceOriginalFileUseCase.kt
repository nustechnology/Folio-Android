package com.nus.folio.domain.usecase

import com.nus.folio.domain.model.SourceFileLocation
import com.nus.folio.domain.repository.SourceRepository

class GetSourceOriginalFileUseCase(
    private val repository: SourceRepository,
) {
    suspend operator fun invoke(spaceId: String, sourceId: String): Result<SourceFileLocation> =
        repository.getOriginalFile(spaceId, sourceId)
}
