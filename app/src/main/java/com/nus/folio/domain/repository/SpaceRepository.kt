package com.nus.folio.domain.repository

import com.nus.folio.domain.model.Space
import com.nus.folio.domain.model.SpacePage
import com.nus.folio.domain.model.SpacePaging
import com.nus.folio.domain.model.SpaceSort

interface SpaceRepository {
    suspend fun getSpaces(
        searchQuery: String? = null,
        sort: SpaceSort = SpaceSort.DEFAULT,
        page: Int = SpacePaging.DEFAULT_PAGE,
        limit: Int = SpacePaging.DEFAULT_LIMIT,
    ): Result<SpacePage>

    suspend fun createSpace(name: String, researchObjective: String): Result<Space>

    suspend fun updateSpace(
        spaceId: String,
        name: String,
        researchObjective: String,
    ): Result<Space>

    suspend fun deleteSpace(spaceId: String): Result<Unit>
}
