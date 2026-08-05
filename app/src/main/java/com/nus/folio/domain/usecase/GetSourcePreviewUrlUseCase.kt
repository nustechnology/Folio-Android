package com.nus.folio.domain.usecase

import com.nus.folio.domain.repository.SourceRepository

class GetSourcePreviewUrlUseCase(
    private val repository: SourceRepository,
) {
    suspend operator fun invoke(sourceId: String): Result<String?> =
        repository.getSourcePreviewUrl(sourceId)
}
