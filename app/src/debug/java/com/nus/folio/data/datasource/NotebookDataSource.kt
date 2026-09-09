package com.nus.folio.data.datasource

import com.nus.folio.data.network.NotebookApi
import com.nus.folio.data.network.NotebookApiClient
import com.nus.folio.data.network.NotebookNotFoundException
import com.nus.folio.data.network.UnauthorizedException
import com.nus.folio.data.notebook.NotebookStore
import com.nus.folio.domain.model.Notebook
import java.io.IOException

/**
 * Debug notebook data source — GET/PUT the full document via the API, with local DataStore
 * as the source of truth for unsynced edits.
 *
 * [saveNotebook] writes markdown locally first (marked dirty), then PUTs. A failed PUT leaves
 * the dirty local copy so a later GET cannot overwrite it. A successful PUT clears dirty.
 * [fetchNotebook] falls back to that cache on transport failures (network / 5xx) and 404;
 * HTTP 401 after a failed refresh is rethrown for the session layer.
 * On HTTP 401, refreshes the access token once and retries.
 */
class NotebookDataSource(
    private val store: NotebookStore,
    private val accessTokenProvider: () -> String? = { null },
    private val refreshAccessToken: suspend () -> String? = { null },
    private val notebookApi: NotebookApi = NotebookApiClient(),
) {
    suspend fun fetchNotebook(spaceId: String): Notebook {
        require(spaceId.isNotBlank()) { "Space id is required" }
        val trimmedSpaceId = spaceId.trim()
        if (store.isDirty(trimmedSpaceId)) {
            return store.read(trimmedSpaceId)
        }
        return try {
            val remote = withAuthRetry { accessToken ->
                notebookApi.getNotebook(
                    accessToken = accessToken,
                    spaceId = trimmedSpaceId,
                )
            }
            store.writeIfNotDirty(
                trimmedSpaceId,
                remote.content,
                readOnly = remote.isReadOnly,
            )
        } catch (_: NotebookNotFoundException) {
            store.read(trimmedSpaceId)
        } catch (unauthorized: UnauthorizedException) {
            throw unauthorized
        } catch (_: IOException) {
            store.read(trimmedSpaceId).copy(isStale = true)
        }
    }

    suspend fun saveNotebook(spaceId: String, content: String): Notebook {
        require(spaceId.isNotBlank()) { "Space id is required" }
        val trimmedSpaceId = spaceId.trim()
        if (store.read(trimmedSpaceId).isReadOnly) {
            throw IllegalStateException("Notebook is read-only")
        }
        val local = store.write(trimmedSpaceId, content, dirty = true)
        withAuthRetry { accessToken ->
            notebookApi.putNotebook(
                accessToken = accessToken,
                spaceId = trimmedSpaceId,
                markdown = content,
            )
        }
        store.markClean(trimmedSpaceId, local.revision)
        return local
    }

    private suspend fun <T> withAuthRetry(block: suspend (accessToken: String) -> T): T {
        val accessToken = requireAccessToken()
        return try {
            block(accessToken)
        } catch (unauthorized: UnauthorizedException) {
            val latestToken = accessTokenProvider()?.takeIf { it.isNotBlank() }
            val tokenToRetry = if (latestToken != null && latestToken != accessToken) {
                latestToken
            } else {
                refreshAccessToken()?.takeIf { it.isNotBlank() } ?: throw unauthorized
            }
            block(tokenToRetry)
        }
    }

    private fun requireAccessToken(): String =
        accessTokenProvider()?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Not authenticated")
}
