package com.nus.folio.domain.repository

import com.nus.folio.domain.model.CreateSourceRequest
import com.nus.folio.domain.model.Source
import com.nus.folio.domain.model.SourceDetail
import com.nus.folio.domain.model.SourceFileLocation
import com.nus.folio.domain.model.SourceLibrary
import com.nus.folio.domain.model.SourcePaging
import com.nus.folio.domain.model.SourceProcessingEvent
import com.nus.folio.domain.model.SourceSort
import kotlinx.coroutines.flow.Flow

interface SourceRepository {
    suspend fun getSources(
        spaceId: String,
        sourceType: String? = null,
        search: String? = null,
        sort: SourceSort = SourceSort.DEFAULT,
        page: Int = SourcePaging.DEFAULT_PAGE,
        limit: Int = SourcePaging.DEFAULT_LIMIT,
    ): Result<SourceLibrary>
    suspend fun createSource(request: CreateSourceRequest): Result<Source>
    suspend fun updateSource(source: Source, content: String? = null): Result<Source>
    suspend fun deleteSource(sourceId: String): Result<Unit>
    suspend fun retrySource(sourceId: String): Result<Unit>

    suspend fun getSourceDetail(spaceId: String, sourceId: String): Result<SourceDetail>

    /** Public/anonymous preview URL for the original file, when the backend provides one. */
    suspend fun getSourcePreviewUrl(sourceId: String): Result<String?>

    suspend fun getOriginalFile(spaceId: String, sourceId: String): Result<SourceFileLocation>

    fun observeSourceProcessing(): Flow<SourceProcessingEvent>
}
