package com.nus.folio.data.datasource

import com.nus.folio.data.notebook.NotebookStore
import com.nus.folio.domain.model.Notebook

class NotebookDataSource(
    private val store: NotebookStore,
) {
    suspend fun fetchNotebook(spaceId: String): Notebook =
        store.read(spaceId)

    suspend fun saveNotebook(spaceId: String, content: String): Notebook =
        store.write(spaceId, content)
}
