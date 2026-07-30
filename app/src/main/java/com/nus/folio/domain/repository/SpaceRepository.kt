package com.nus.folio.domain.repository

import com.nus.folio.domain.model.Space

interface SpaceRepository {
    suspend fun getSpaces(): Result<List<Space>>
}
