package com.nus.folio.domain.usecase

import com.nus.folio.domain.model.SourceProcessingEvent
import com.nus.folio.domain.repository.SourceRepository
import kotlinx.coroutines.flow.Flow

class ObserveSourceProcessingUseCase(
    private val repository: SourceRepository,
) {
    operator fun invoke(): Flow<SourceProcessingEvent> =
        repository.observeSourceProcessing()
}
