package com.nus.folio.domain.usecase

import com.nus.folio.domain.repository.AskRepository

class GetAskSuggestionsUseCase(
    private val repository: AskRepository,
) {
    suspend operator fun invoke(sourceId: String): Result<List<String>> =
        repository.getSuggestedQuestions(sourceId)
}
