package com.nus.folio.data.datasource

import com.nus.folio.data.network.SourcesApi
import com.nus.folio.data.network.SourcesApiClient
import com.nus.folio.data.network.UnauthorizedException
import com.nus.folio.domain.model.CreateSourceRequest
import com.nus.folio.domain.model.Source
import com.nus.folio.domain.model.SourceDetail
import com.nus.folio.domain.model.SourceLibrary
import com.nus.folio.domain.model.SourceProcessingEvent
import com.nus.folio.domain.model.SourceProcessingState
import com.nus.folio.domain.model.SourceSort
import com.nus.folio.domain.model.SourceStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Debug sources data source — list/create/detail/retry/delete via the real API.
 * Listed/created/detail sources are reconciled into a local cache so update and
 * terminal processing events share one coherent view. On HTTP 401, refreshes once and retries.
 */
class SourceDataSource(
    private val sourcesApi: SourcesApi = SourcesApiClient(),
    private val accessTokenProvider: () -> String? = { null },
    private val refreshAccessToken: suspend () -> String? = { null },
) {

    private val mutex = Mutex()
    private val sources: MutableList<Source> = SourceSampleData.mutableDefaultSources()

    suspend fun fetchSources(
        spaceId: String,
        sourceType: String? = null,
        search: String? = null,
        sort: SourceSort = SourceSort.DEFAULT,
    ): SourceLibrary {
        val listed = withAuthRetry { accessToken ->
            sourcesApi.listSources(
                accessToken = accessToken,
                spaceId = spaceId,
                sourceType = sourceType?.trim()?.takeIf { it.isNotEmpty() },
                search = search?.trim()?.takeIf { it.isNotEmpty() },
                sort = sort.apiValue,
            )
        }
        mutex.withLock {
            for (source in listed) {
                upsertLocked(source)
            }
        }
        return SourceSampleData.libraryFrom(listed)
    }

    suspend fun createSource(request: CreateSourceRequest): Source {
        val created = withAuthRetry { accessToken ->
            when (request) {
                is CreateSourceRequest.Web -> {
                    require(request.sourceUrl.isNotBlank()) { "Source URL is required" }
                    sourcesApi.createWebSource(
                        accessToken = accessToken,
                        spaceId = request.spaceId,
                        sourceUrl = request.sourceUrl.trim(),
                        title = request.title.trim(),
                        author = request.author.trim(),
                    )
                }
                is CreateSourceRequest.Manual -> {
                    require(request.title.isNotBlank()) { "Title is required" }
                    require(request.content.isNotBlank()) { "Content is required" }
                    sourcesApi.createManualSource(
                        accessToken = accessToken,
                        spaceId = request.spaceId,
                        title = request.title.trim(),
                        author = request.author.trim(),
                        content = request.content.trim(),
                    )
                }
                is CreateSourceRequest.File -> {
                    require(request.bytes.isNotEmpty()) { "File is required" }
                    val fileName = request.fileName.trim().ifBlank { "source.pdf" }
                    sourcesApi.createFileSource(
                        accessToken = accessToken,
                        spaceId = request.spaceId,
                        title = request.title.trim().ifBlank { fileName },
                        author = request.author.trim(),
                        fileName = fileName,
                        mimeType = request.mimeType,
                        fileBytes = request.bytes,
                    )
                }
            }
        }
        mutex.withLock {
            upsertLocked(created, preferFront = true)
        }
        return created
    }

    fun observeSourceProcessing(): Flow<SourceProcessingEvent> = flow {
        var token = requireAccessToken()
        try {
            emitAll(sourcesApi.observeSourceStatus(token).onEach(::applyProcessingEvent))
        } catch (unauthorized: UnauthorizedException) {
            token = refreshAccessToken()?.takeIf { it.isNotBlank() }
                ?: throw unauthorized
            emitAll(sourcesApi.observeSourceStatus(token).onEach(::applyProcessingEvent))
        }
    }

    private suspend fun applyProcessingEvent(event: SourceProcessingEvent) {
        if (!event.isTerminal) return
        mutex.withLock {
            val index = sources.indexOfFirst { it.id == event.sourceId }
            if (index < 0) return
            val status = when (event.state) {
                SourceProcessingState.READY -> SourceStatus.READY
                SourceProcessingState.FAILED -> SourceStatus.FAILED
                else -> return
            }
            sources[index] = sources[index].copy(status = status)
        }
    }

    suspend fun updateSource(source: Source, content: String? = null): Source {
        require(source.id.isNotBlank()) { "Source id is required" }
        require(source.title.isNotBlank()) { "Title is required" }
        val trimmedTitle = source.title.trim()
        val trimmedAuthor = source.author.trim()
        val trimmedContent = content?.trim()
        val updated = withAuthRetry { accessToken ->
            sourcesApi.updateSource(
                accessToken = accessToken,
                sourceId = source.id.trim(),
                title = trimmedTitle,
                author = trimmedAuthor,
                content = trimmedContent,
            )
        }
        val merged = updated.copy(
            // Preserve list metadata the PATCH payload may omit.
            type = source.type,
            addedLabel = source.addedLabel.ifBlank { updated.addedLabel },
            status = source.status,
            spaceId = source.spaceId.ifBlank { updated.spaceId },
            fileExtension = source.fileExtension.ifBlank { updated.fileExtension },
            title = trimmedTitle,
            author = trimmedAuthor,
        )
        mutex.withLock {
            upsertLocked(merged)
        }
        return merged
    }

    /** Exposed for debug tests to verify local cache updates without API detail masking status. */
    internal suspend fun cachedSource(sourceId: String): Source? =
        mutex.withLock { sources.find { it.id == sourceId } }

    suspend fun deleteSource(sourceId: String) {
        withAuthRetry { accessToken ->
            sourcesApi.deleteSource(accessToken = accessToken, sourceId = sourceId)
        }
        mutex.withLock {
            sources.removeAll { it.id == sourceId }
        }
    }

    suspend fun fetchSourceDetail(spaceId: String, sourceId: String): SourceDetail {
        val detail = withAuthRetry { accessToken ->
            sourcesApi.getSource(accessToken = accessToken, sourceId = sourceId)
        }
        if (spaceId.isNotBlank() &&
            detail.spaceId.isNotBlank() &&
            detail.spaceId != spaceId
        ) {
            throw NoSuchElementException("Source not found")
        }
        mutex.withLock {
            upsertLocked(
                Source(
                    id = detail.id,
                    title = detail.title,
                    type = detail.type,
                    author = detail.author,
                    addedLabel = detail.addedLabel,
                    status = detail.status,
                    spaceId = detail.spaceId.ifBlank { spaceId },
                ),
                preferFront = true,
            )
        }
        return detail
    }

    suspend fun fetchSourcePreview(sourceId: String): String? =
        withAuthRetry { accessToken ->
            sourcesApi.getSourcePreview(accessToken = accessToken, sourceId = sourceId)
        }

    suspend fun retrySource(sourceId: String) {
        val detail = withAuthRetry { accessToken ->
            sourcesApi.retrySource(accessToken = accessToken, sourceId = sourceId)
        }
        mutex.withLock {
            val index = sources.indexOfFirst { it.id == sourceId }
            if (index >= 0) {
                sources[index] = sources[index].copy(status = SourceStatus.PROCESSING)
            } else if (detail != null) {
                upsertLocked(
                    Source(
                        id = detail.id,
                        title = detail.title,
                        type = detail.type,
                        author = detail.author,
                        addedLabel = detail.addedLabel,
                        status = SourceStatus.PROCESSING,
                        spaceId = detail.spaceId,
                    ),
                    preferFront = true,
                )
            }
        }
    }

    /** Must be called while [mutex] is held. */
    private fun upsertLocked(source: Source, preferFront: Boolean = false) {
        val index = sources.indexOfFirst { it.id == source.id }
        if (index >= 0) {
            sources[index] = source
        } else if (preferFront) {
            sources.add(0, source)
        } else {
            sources.add(source)
        }
    }

    private suspend fun <T> withAuthRetry(block: suspend (accessToken: String) -> T): T {
        val accessToken = requireAccessToken()
        return try {
            block(accessToken)
        } catch (unauthorized: UnauthorizedException) {
            val refreshedToken = refreshAccessToken()?.takeIf { it.isNotBlank() }
                ?: throw unauthorized
            block(refreshedToken)
        }
    }

    private fun requireAccessToken(): String =
        accessTokenProvider()?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Not authenticated")
}
