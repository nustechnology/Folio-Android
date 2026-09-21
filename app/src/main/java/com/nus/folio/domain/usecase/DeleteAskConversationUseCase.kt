package com.nus.folio.domain.usecase

import com.nus.folio.domain.repository.AskRepository

class DeleteAskConversationUseCase(
    private val repository: AskRepository,
) {
    suspend operator fun invoke(
        spaceId: String,
        conversationId: String,
    ): Result<Unit> =
        repository.deleteConversation(spaceId, conversationId)
}
