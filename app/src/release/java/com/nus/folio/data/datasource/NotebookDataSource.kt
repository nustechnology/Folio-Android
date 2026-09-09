package com.nus.folio.data.datasource

import com.nus.folio.data.notebook.NotebookStore
import com.nus.folio.domain.model.Notebook

/**
 * Release stub — notebook GET/PUT is not wired to HTTP yet; persists locally per space.
 */
class NotebookDataSource(
    private val store: NotebookStore,
    @Suppress("UNUSED_PARAMETER")
    accessTokenProvider: () -> String? = { null },
    @Suppress("UNUSED_PARAMETER")
    refreshAccessToken: suspend () -> String? = { null },
) {
    suspend fun fetchNotebook(spaceId: String): Notebook =
        store.read(spaceId)

    suspend fun saveNotebook(spaceId: String, content: String): Notebook =
        store.write(spaceId, content)
}
