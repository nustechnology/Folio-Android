package com.nus.folio.domain.usecase

import com.nus.folio.domain.model.AskConversationDetail
import com.nus.folio.domain.repository.AskRepository

class GetAskConversationUseCase(
    private val repository: AskRepository,
) {
    suspend operator fun invoke(
        spaceId: String,
        conversationId: String,
    ): Result<AskConversationDetail> =
        repository.getConversation(spaceId, conversationId)
}
