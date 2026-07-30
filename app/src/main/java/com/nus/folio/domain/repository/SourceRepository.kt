package com.nus.folio.domain.repository

import com.nus.folio.domain.model.SourceLibrary

interface SourceRepository {
    suspend fun getSources(spaceId: String): Result<SourceLibrary>
}
