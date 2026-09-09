package com.nus.folio.domain.usecase

import com.nus.folio.domain.model.AskSuggestions
import com.nus.folio.domain.repository.AskRepository

class GetAskSuggestionsUseCase(
    private val repository: AskRepository,
) {
    suspend operator fun invoke(
        spaceId: String,
        sourceId: String?,
    ): Result<AskSuggestions> =
        repository.getSuggestedQuestions(
            spaceId = spaceId,
            sourceId = sourceId,
        )
}
