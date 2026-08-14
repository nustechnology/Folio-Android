package com.nus.folio.domain.usecase

import com.nus.folio.domain.model.SourceLibrary
import com.nus.folio.domain.model.SourcePaging
import com.nus.folio.domain.model.SourceSort
import com.nus.folio.domain.repository.SourceRepository

class GetSourcesUseCase(
    private val repository: SourceRepository,
) {
    suspend operator fun invoke(
        spaceId: String,
        sourceType: String? = null,
        search: String? = null,
        sort: SourceSort = SourceSort.DEFAULT,
        page: Int = SourcePaging.DEFAULT_PAGE,
        limit: Int = SourcePaging.DEFAULT_LIMIT,
    ): Result<SourceLibrary> = repository.getSources(
        spaceId = spaceId,
        sourceType = sourceType,
        search = search,
        sort = sort,
        page = page,
        limit = limit,
    )
}
