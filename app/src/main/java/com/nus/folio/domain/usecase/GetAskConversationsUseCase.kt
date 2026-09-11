package com.nus.folio.domain.usecase

import com.nus.folio.domain.model.AskConversationLibrary
import com.nus.folio.domain.model.AskConversationPaging
import com.nus.folio.domain.repository.AskRepository

class GetAskConversationsUseCase(
    private val repository: AskRepository,
) {
    suspend operator fun invoke(
        spaceId: String,
        search: String? = null,
        page: Int = AskConversationPaging.DEFAULT_PAGE,
        limit: Int = AskConversationPaging.DEFAULT_LIMIT,
    ): Result<AskConversationLibrary> =
        repository.getConversations(
            spaceId = spaceId,
            search = search,
            page = page,
            limit = limit,
        )
}
