package com.nus.folio.domain.usecase

import com.nus.folio.domain.model.Notebook
import com.nus.folio.domain.repository.NotebookRepository
import com.nus.folio.domain.util.NotebookInputRules

class SaveNotebookUseCase(
    private val repository: NotebookRepository,
) {
    suspend operator fun invoke(spaceId: String, content: String): Result<Notebook> =
        repository.saveNotebook(
            spaceId = spaceId,
            content = NotebookInputRules.clampContent(content),
        )
}
