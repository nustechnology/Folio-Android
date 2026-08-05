package com.nus.folio.data.repository

import com.nus.folio.data.datasource.SpaceDataSource
import com.nus.folio.domain.model.Space
import com.nus.folio.domain.model.SpacePage
import com.nus.folio.domain.model.SpaceSort
import com.nus.folio.domain.repository.SpaceRepository
import kotlin.coroutines.cancellation.CancellationException

class SpaceRepositoryImpl(
    private val dataSource: SpaceDataSource,
) : SpaceRepository {

    override suspend fun getSpaces(
        searchQuery: String?,
        sort: SpaceSort,
        page: Int,
        limit: Int,
    ): Result<SpacePage> =
        runSuspendCatching {
            dataSource.fetchSpaces(
                searchQuery = searchQuery,
                sort = sort.apiValue,
                page = page,
                limit = limit,
            )
        }

    override suspend fun createSpace(
        name: String,
        researchObjective: String,
    ): Result<Space> = runSuspendCatching {
        dataSource.createSpace(name = name, researchObjective = researchObjective)
    }

    override suspend fun updateSpace(
        spaceId: String,
        name: String,
        researchObjective: String,
    ): Result<Space> = runSuspendCatching {
        dataSource.updateSpace(
            spaceId = spaceId,
            name = name,
            researchObjective = researchObjective,
        )
    }

    override suspend fun deleteSpace(spaceId: String): Result<Unit> =
        runSuspendCatching {
            dataSource.deleteSpace(spaceId)
        }

    private suspend fun <T> runSuspendCatching(block: suspend () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
}
