package com.nus.folio.domain.usecase

import com.nus.folio.domain.model.SourceDetail
import com.nus.folio.domain.repository.SourceRepository

class GetSourceDetailUseCase(
    private val repository: SourceRepository,
) {
    suspend operator fun invoke(spaceId: String, sourceId: String): Result<SourceDetail> =
        repository.getSourceDetail(spaceId, sourceId)
}
