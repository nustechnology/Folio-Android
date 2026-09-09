package com.nus.folio.domain.usecase

import com.nus.folio.domain.model.AskFeedbackRating
import com.nus.folio.domain.repository.AskRepository

class SubmitAskFeedbackUseCase(
    private val repository: AskRepository,
) {
    suspend operator fun invoke(
        spaceId: String,
        conversationId: String,
        messageId: String,
        rating: AskFeedbackRating,
    ): Result<Unit> =
        repository.submitFeedback(
            spaceId = spaceId,
            conversationId = conversationId,
            messageId = messageId,
            rating = rating,
        )
}
