package com.nus.folio.data.repository

import com.nus.folio.data.datasource.AskDataSource
import com.nus.folio.domain.model.AskTopic
import com.nus.folio.domain.repository.AskRepository
import kotlin.coroutines.cancellation.CancellationException

class AskRepositoryImpl(
    private val dataSource: AskDataSource,
) : AskRepository {

    override suspend fun getAskTopics(): Result<List<AskTopic>> =
        try {
            Result.success(dataSource.fetchAskTopics())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
}
