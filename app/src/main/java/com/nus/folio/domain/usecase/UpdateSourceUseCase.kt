package com.nus.folio.domain.usecase

import com.nus.folio.domain.model.Source
import com.nus.folio.domain.repository.SourceRepository

class UpdateSourceUseCase(
    private val repository: SourceRepository,
) {
    /**
     * @param content Manual/TEXT body to PATCH; omit (null) for Web/File edits.
     */
    suspend operator fun invoke(source: Source, content: String? = null): Result<Source> =
        repository.updateSource(source, content)
}
