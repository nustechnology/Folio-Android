package com.nus.folio.domain.repository

import com.nus.folio.domain.model.Source
import com.nus.folio.domain.model.SourceDetail
import com.nus.folio.domain.model.SourceFileLocation
import com.nus.folio.domain.model.SourceLibrary

interface SourceRepository {
    suspend fun getSources(spaceId: String): Result<SourceLibrary>
    suspend fun updateSource(source: Source): Result<Source>
    suspend fun deleteSource(sourceId: String): Result<Unit>

    suspend fun getSourceDetail(spaceId: String, sourceId: String): Result<SourceDetail>

    suspend fun getOriginalFile(spaceId: String, sourceId: String): Result<SourceFileLocation>
}
