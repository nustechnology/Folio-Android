package com.nus.folio.data.repository

import com.nus.folio.data.datasource.NotebookDataSource
import com.nus.folio.domain.model.Notebook
import com.nus.folio.domain.repository.NotebookRepository
import kotlin.coroutines.cancellation.CancellationException

class NotebookRepositoryImpl(
    private val dataSource: NotebookDataSource,
) : NotebookRepository {

    override suspend fun getNotebook(spaceId: String): Result<Notebook> =
        try {
            Result.success(dataSource.fetchNotebook(spaceId))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun saveNotebook(spaceId: String, content: String): Result<Notebook> =
        try {
            Result.success(dataSource.saveNotebook(spaceId, content))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
}
