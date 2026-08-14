package com.nus.folio.data.repository

import com.nus.folio.data.datasource.SourceDataSource
import com.nus.folio.domain.model.CreateSourceRequest
import com.nus.folio.domain.model.Source
import com.nus.folio.domain.model.SourceDetail
import com.nus.folio.domain.model.SourceFileLocation
import com.nus.folio.domain.model.SourceLibrary
import com.nus.folio.domain.model.SourcePaging
import com.nus.folio.domain.model.SourceProcessingEvent
import com.nus.folio.domain.model.SourceSort
import com.nus.folio.domain.repository.SourceOriginalFileResolver
import com.nus.folio.domain.repository.SourceRepository
import kotlinx.coroutines.flow.Flow
import kotlin.coroutines.cancellation.CancellationException

class SourceRepositoryImpl(
    private val dataSource: SourceDataSource,
    private val originalFileResolver: SourceOriginalFileResolver,
) : SourceRepository {

    override suspend fun getSources(
        spaceId: String,
        sourceType: String?,
        search: String?,
        sort: SourceSort,
        page: Int,
        limit: Int,
    ): Result<SourceLibrary> =
        try {
            Result.success(
                dataSource.fetchSources(
                    spaceId = spaceId,
                    sourceType = sourceType,
                    search = search,
                    sort = sort,
                    page = page,
                    limit = limit,
                ),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun createSource(request: CreateSourceRequest): Result<Source> =
        try {
            Result.success(dataSource.createSource(request))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun updateSource(source: Source, content: String?): Result<Source> =
        try {
            Result.success(dataSource.updateSource(source, content))
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

    override suspend fun retrySource(sourceId: String): Result<Unit> =
        try {
            dataSource.retrySource(sourceId)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun getSourceDetail(spaceId: String, sourceId: String): Result<SourceDetail> =
        try {
            Result.success(dataSource.fetchSourceDetail(spaceId, sourceId))
        } catch (e: NoSuchElementException) {
            Result.failure(e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun getSourcePreviewUrl(sourceId: String): Result<String?> =
        try {
            Result.success(dataSource.fetchSourcePreview(sourceId))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun getOriginalFile(spaceId: String, sourceId: String): Result<SourceFileLocation> =
        try {
            Result.success(originalFileResolver.resolveOriginalFile(spaceId, sourceId))
        } catch (e: NoSuchElementException) {
            Result.failure(e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    override fun observeSourceProcessing(): Flow<SourceProcessingEvent> =
        dataSource.observeSourceProcessing()
}
