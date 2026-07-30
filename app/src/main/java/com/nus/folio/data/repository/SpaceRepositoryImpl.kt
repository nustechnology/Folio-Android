package com.nus.folio.data.repository

import com.nus.folio.data.datasource.SpaceDataSource
import com.nus.folio.domain.model.Space
import com.nus.folio.domain.repository.SpaceRepository
import kotlin.coroutines.cancellation.CancellationException

class SpaceRepositoryImpl(
    private val dataSource: SpaceDataSource,
) : SpaceRepository {

    override suspend fun getSpaces(): Result<List<Space>> =
        try {
            Result.success(dataSource.fetchSpaces())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
}
