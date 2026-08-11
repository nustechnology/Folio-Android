package com.nus.folio.domain.usecase

import com.nus.folio.domain.model.Notebook
import com.nus.folio.domain.repository.NotebookRepository

class GetNotebookUseCase(
    private val repository: NotebookRepository,
) {
    suspend operator fun invoke(spaceId: String): Result<Notebook> =
        repository.getNotebook(spaceId)
}
