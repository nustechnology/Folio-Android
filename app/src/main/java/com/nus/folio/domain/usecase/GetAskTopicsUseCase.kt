package com.nus.folio.domain.usecase

import com.nus.folio.domain.model.AskTopic
import com.nus.folio.domain.repository.AskRepository

class GetAskTopicsUseCase(
    private val repository: AskRepository,
) {
    suspend operator fun invoke(spaceId: String): Result<List<AskTopic>> =
        repository.getAskTopics(spaceId)
}
