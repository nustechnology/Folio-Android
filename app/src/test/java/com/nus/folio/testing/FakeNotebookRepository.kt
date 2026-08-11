package com.nus.folio.testing

import com.nus.folio.domain.model.Notebook
import com.nus.folio.domain.repository.NotebookRepository

class FakeNotebookRepository : NotebookRepository {
    private val notebooks = mutableMapOf<String, Notebook>()

    var getNotebookCallCount: Int = 0
        private set
    var saveNotebookCallCount: Int = 0
        private set
    var lastSavedSpaceId: String? = null
        private set
    var lastSavedContent: String? = null
        private set
    var saveError: Throwable? = null
    var getError: Throwable? = null

    fun seed(spaceId: String, content: String) {
        notebooks[spaceId] = Notebook(
            spaceId = spaceId,
            content = content,
            updatedAtMillis = 1L,
        )
    }

    override suspend fun getNotebook(spaceId: String): Result<Notebook> {
        getNotebookCallCount++
        getError?.let { return Result.failure(it) }
        return Result.success(
            notebooks[spaceId] ?: Notebook(spaceId = spaceId, content = ""),
        )
    }

    override suspend fun saveNotebook(spaceId: String, content: String): Result<Notebook> {
        saveNotebookCallCount++
        lastSavedSpaceId = spaceId
        lastSavedContent = content
        saveError?.let { return Result.failure(it) }
        val notebook = Notebook(
            spaceId = spaceId,
            content = content,
            updatedAtMillis = System.currentTimeMillis(),
        )
        notebooks[spaceId] = notebook
        return Result.success(notebook)
    }
}
