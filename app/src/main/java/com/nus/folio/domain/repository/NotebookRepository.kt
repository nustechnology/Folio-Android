package com.nus.folio.domain.repository

import com.nus.folio.domain.model.Notebook

interface NotebookRepository {
    suspend fun getNotebook(spaceId: String): Result<Notebook>
    suspend fun saveNotebook(spaceId: String, content: String): Result<Notebook>
}
