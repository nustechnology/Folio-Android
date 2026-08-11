package com.nus.folio.domain.repository

import com.nus.folio.domain.model.AskStreamEvent
import kotlinx.coroutines.flow.Flow

interface AskRepository {
    /**
     * Context-aware suggested questions for a Ready source with metadata/summary.
     * Returns an empty list when suggestions are unavailable (caller should use fallbacks).
     */
    suspend fun getSuggestedQuestions(sourceId: String): Result<List<String>>

    /**
     * Streams a grounded answer for [question] within [spaceId].
     * When [sourceId] is non-null, grounds on that source; otherwise the entire space.
     */
    fun streamAnswer(
        spaceId: String,
        question: String,
        sourceId: String?,
    ): Flow<AskStreamEvent>
}
