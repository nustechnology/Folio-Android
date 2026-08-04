package com.nus.folio.domain.usecase

import com.nus.folio.domain.model.SpacePage
import com.nus.folio.domain.model.SpacePaging
import com.nus.folio.domain.model.SpaceSort
import com.nus.folio.domain.repository.SpaceRepository

class GetSpacesUseCase(
    private val repository: SpaceRepository,
) {
    suspend operator fun invoke(
        searchQuery: String? = null,
        sort: SpaceSort = SpaceSort.DEFAULT,
        page: Int = SpacePaging.DEFAULT_PAGE,
        limit: Int = SpacePaging.DEFAULT_LIMIT,
    ): Result<SpacePage> =
        repository.getSpaces(
            searchQuery = searchQuery,
            sort = sort,
            page = page,
            limit = limit,
        )
}
