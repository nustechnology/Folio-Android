package com.nus.folio.domain.usecase

import com.nus.folio.domain.model.NoteLibrary
import com.nus.folio.domain.model.NotePaging
import com.nus.folio.domain.model.NoteSort
import com.nus.folio.domain.repository.NoteRepository

class GetNotesUseCase(
    private val repository: NoteRepository,
) {
    suspend operator fun invoke(
        spaceId: String,
        search: String? = null,
        sort: NoteSort = NoteSort.DEFAULT,
        page: Int = NotePaging.DEFAULT_PAGE,
        limit: Int = NotePaging.DEFAULT_LIMIT,
    ): Result<NoteLibrary> =
        repository.getNotes(
            spaceId = spaceId,
            search = search,
            sort = sort,
            page = page,
            limit = limit,
        )
}
