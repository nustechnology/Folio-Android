package com.nus.folio.data.repository

import com.nus.folio.data.datasource.SourceDataSource
import com.nus.folio.domain.model.Source
import com.nus.folio.domain.model.SourceLibrary
import com.nus.folio.domain.repository.SourceRepository
import kotlin.coroutines.cancellation.CancellationException

class SourceRepositoryImpl(
    private val dataSource: SourceDataSource,
) : SourceRepository {

    override suspend fun getSources(spaceId: String): Result<SourceLibrary> =
        try {
            Result.success(dataSource.fetchSources(spaceId))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun updateSource(source: Source): Result<Source> =
        try {
            Result.success(dataSource.updateSource(source))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun deleteSource(sourceId: String): Result<Unit> =
        try {
            dataSource.deleteSource(sourceId)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
}
