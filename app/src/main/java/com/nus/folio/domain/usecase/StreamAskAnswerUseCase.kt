package com.nus.folio.domain.usecase

import com.nus.folio.domain.model.AskStreamEvent
import com.nus.folio.domain.repository.AskRepository
import kotlinx.coroutines.flow.Flow

class StreamAskAnswerUseCase(
    private val repository: AskRepository,
) {
    operator fun invoke(
        spaceId: String,
        question: String,
        sourceId: String?,
        conversationId: String? = null,
    ): Flow<AskStreamEvent> =
        repository.streamAnswer(
            spaceId = spaceId,
            question = question,
            sourceId = sourceId,
            conversationId = conversationId,
        )
}
