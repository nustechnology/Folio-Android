package com.nus.folio.data.repository

import com.nus.folio.data.datasource.AskDataSource
import com.nus.folio.domain.model.AskStreamEvent
import com.nus.folio.domain.repository.AskRepository
import kotlinx.coroutines.flow.Flow
import kotlin.coroutines.cancellation.CancellationException

class AskRepositoryImpl(
    private val dataSource: AskDataSource,
) : AskRepository {

    override suspend fun getSuggestedQuestions(sourceId: String): Result<List<String>> =
        try {
            Result.success(dataSource.fetchSuggestedQuestions(sourceId))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    override fun streamAnswer(
        spaceId: String,
        question: String,
        sourceId: String?,
    ): Flow<AskStreamEvent> =
        dataSource.streamAnswer(
            spaceId = spaceId,
            question = question,
            sourceId = sourceId,
        )
}
