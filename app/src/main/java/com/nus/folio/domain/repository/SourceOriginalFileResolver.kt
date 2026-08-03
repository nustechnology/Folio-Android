package com.nus.folio.domain.repository

import com.nus.folio.domain.model.SourceFileLocation

interface SourceOriginalFileResolver {
    suspend fun resolveOriginalFile(spaceId: String, sourceId: String): SourceFileLocation
}
