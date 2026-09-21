package com.nus.folio.domain.usecase

import com.nus.folio.domain.model.AskConversation
import com.nus.folio.domain.repository.AskRepository

class UpdateAskConversationUseCase(
    private val repository: AskRepository,
) {
    suspend operator fun invoke(
        spaceId: String,
        conversationId: String,
        title: String,
    ): Result<AskConversation> =
        repository.updateConversation(spaceId, conversationId, title)
}
