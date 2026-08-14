package com.nus.folio.data.datasource

import com.nus.folio.domain.model.CreateSourceRequest
import com.nus.folio.domain.model.Source
import com.nus.folio.domain.model.SourceDetail
import com.nus.folio.domain.model.SourceLibrary
import com.nus.folio.domain.model.SourcePaging
import com.nus.folio.domain.model.SourceProcessingEvent
import com.nus.folio.domain.model.SourceProcessingState
import com.nus.folio.domain.model.SourceSort
import com.nus.folio.domain.model.SourceStatus
import com.nus.folio.domain.model.SourceType
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Release stub — sources list/create APIs are not wired yet; uses local sample data.
 *
 * Fake processing pipelines are owned by the data source (not by each collector):
 * entering [SourceStatus.PROCESSING] starts one pipeline that publishes through a shared flow.
 */
class SourceDataSource(
    @Suppress("UNUSED_PARAMETER")
    accessTokenProvider: () -> String? = { null },
    @Suppress("UNUSED_PARAMETER")
    refreshAccessToken: suspend () -> String? = { null },
    private val processingScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {

    private val mutex = Mutex()
    private val sources: MutableList<Source> = SourceSampleData.mutableDefaultSources()
    private val processingEvents = MutableSharedFlow<SourceProcessingEvent>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val activePipelines = ConcurrentHashMap<String, Job>()

    init {
        sources
            .asSequence()
            .filter { it.status == SourceStatus.PROCESSING }
            .map { it.id }
            .forEach(::startProcessingPipeline)
    }

    suspend fun fetchSources(
        spaceId: String,
        sourceType: String? = null,
        search: String? = null,
        sort: SourceSort = SourceSort.DEFAULT,
        page: Int = SourcePaging.DEFAULT_PAGE,
        limit: Int = SourcePaging.DEFAULT_LIMIT,
    ): SourceLibrary {
        delay(200)
        return mutex.withLock {
            var scoped = sources.filter { it.spaceId == spaceId }
            scoped = when (sourceType?.trim()?.lowercase()) {
                "file" -> scoped.filter { it.type == SourceType.FILE || it.type == SourceType.BOOK }
                "web" -> scoped.filter { it.type == SourceType.WEB }
                "manual" -> scoped.filter { it.type == SourceType.TEXT }
                else -> scoped
            }
            val query = search?.trim().orEmpty()
            if (query.isNotEmpty()) {
                scoped = scoped.filter {
                    it.title.contains(query, ignoreCase = true) ||
                        it.author.contains(query, ignoreCase = true)
                }
            }
            scoped = when (sort) {
                SourceSort.ALPHABETICAL_AZ -> scoped.sortedBy { it.title.lowercase() }
                SourceSort.ALPHABETICAL_ZA -> scoped.sortedByDescending { it.title.lowercase() }
                SourceSort.RECENTLY_ADDED -> scoped
            }
            val safePage = page.coerceAtLeast(1)
            val safeLimit = limit.coerceAtLeast(1)
            val offset = ((safePage - 1L) * safeLimit)
                .coerceAtMost(scoped.size.toLong())
                .toInt()
            val pageItems = scoped.drop(offset).take(safeLimit)
            SourceSampleData.libraryFrom(
                sources = pageItems,
                allCount = scoped.size,
                papersCount = scoped.count { it.type == SourceType.FILE },
                booksCount = scoped.count { it.type == SourceType.BOOK },
                webCount = scoped.count { it.type == SourceType.WEB },
                textCount = scoped.count { it.type == SourceType.TEXT },
                page = safePage,
                limit = safeLimit,
                hasMore = offset + pageItems.size < scoped.size,
            )
        }
    }

    suspend fun createSource(request: CreateSourceRequest): Source {
        delay(200)
        val source = when (request) {
            is CreateSourceRequest.Web -> {
                require(request.sourceUrl.isNotBlank()) { "Source URL is required" }
                Source(
                    id = UUID.randomUUID().toString(),
                    title = request.title.trim().ifBlank { request.sourceUrl.trim() },
                    type = SourceType.WEB,
                    author = request.author.trim(),
                    addedLabel = "Added just now",
                    status = SourceStatus.PROCESSING,
                    spaceId = request.spaceId,
                )
            }
            is CreateSourceRequest.Manual -> {
                require(request.title.isNotBlank()) { "Title is required" }
                require(request.content.isNotBlank()) { "Content is required" }
                Source(
                    id = UUID.randomUUID().toString(),
                    title = request.title.trim(),
                    type = SourceType.TEXT,
                    author = request.author.trim(),
                    addedLabel = "Added just now",
                    status = SourceStatus.PROCESSING,
                    spaceId = request.spaceId,
                )
            }
            is CreateSourceRequest.File -> {
                require(request.bytes.isNotEmpty()) { "File is required" }
                val fileName = request.fileName.trim().ifBlank { "source.pdf" }
                val extension = fileName.substringAfterLast('.', missingDelimiterValue = "")
                    .trim()
                    .lowercase()
                    .takeIf { it.isNotBlank() && it.length <= 8 && !it.contains(' ') }
                    .orEmpty()
                Source(
                    id = UUID.randomUUID().toString(),
                    title = request.title.trim().ifBlank { fileName },
                    type = SourceType.FILE,
                    author = request.author.trim(),
                    addedLabel = "Added just now",
                    status = SourceStatus.PROCESSING,
                    spaceId = request.spaceId,
                    fileExtension = extension.ifBlank { "pdf" },
                )
            }
        }
        mutex.withLock {
            sources.add(0, source)
            SourceSampleData.rememberCreatedDetail(source.id, request)
        }
        startProcessingPipeline(source.id)
        return source
    }

    fun observeSourceProcessing(): SharedFlow<SourceProcessingEvent> =
        processingEvents.asSharedFlow()

    private fun startProcessingPipeline(sourceId: String) {
        activePipelines[sourceId]?.cancel()
        val job = processingScope.launch {
            try {
                for (event in fakePipeline(sourceId)) {
                    ensureActive()
                    processingEvents.emit(event)
                    if (event.isTerminal) {
                        mutex.withLock {
                            val index = sources.indexOfFirst { it.id == sourceId }
                            if (index >= 0) {
                                sources[index] = sources[index].copy(
                                    status = if (event.state == SourceProcessingState.FAILED) {
                                        SourceStatus.FAILED
                                    } else {
                                        SourceStatus.READY
                                    },
                                )
                            }
                        }
                    }
                    delay(FAKE_STEP_DELAY_MS)
                }
            } finally {
                activePipelines.remove(sourceId, coroutineContext[Job])
            }
        }
        activePipelines[sourceId] = job
    }

    suspend fun updateSource(source: Source, content: String? = null): Source {
        delay(200)
        return mutex.withLock {
            val index = sources.indexOfFirst { it.id == source.id }
            if (index < 0) {
                throw NoSuchElementException("Source not found: ${source.id}")
            }
            sources[index] = source
            if (content != null) {
                SourceSampleData.rememberUpdatedManualContent(source, content)
            }
            source
        }
    }

    suspend fun deleteSource(sourceId: String) {
        delay(200)
        activePipelines.remove(sourceId)?.cancel()
        mutex.withLock {
            val removed = sources.removeAll { it.id == sourceId }
            if (!removed) {
                throw NoSuchElementException("Source not found: $sourceId")
            }
            SourceSampleData.forgetCreatedDetail(sourceId)
        }
    }

    suspend fun retrySource(sourceId: String) {
        delay(200)
        mutex.withLock {
            val index = sources.indexOfFirst { it.id == sourceId }
            if (index < 0) {
                throw NoSuchElementException("Source not found: $sourceId")
            }
            sources[index] = sources[index].copy(status = SourceStatus.PROCESSING)
        }
        startProcessingPipeline(sourceId)
    }

    /** Preview URLs are unavailable until the release backend is wired. */
    @Suppress("UNUSED_PARAMETER")
    suspend fun fetchSourcePreview(sourceId: String): String? = null

    suspend fun fetchSourceDetail(spaceId: String, sourceId: String): SourceDetail {
        delay(200)
        return mutex.withLock {
            val source = sources.find { it.id == sourceId && it.spaceId == spaceId }
                ?: throw NoSuchElementException("Source not found")
            SourceSampleData.buildSourceDetail(source)
        }
    }

    companion object {
        private const val FAKE_STEP_DELAY_MS = 700L

        private fun fakePipeline(sourceId: String): List<SourceProcessingEvent> = listOf(
            SourceProcessingEvent(sourceId, SourceProcessingState.ADDED, 0),
            SourceProcessingEvent(sourceId, SourceProcessingState.EXTRACTING_TEXT, 25),
            SourceProcessingEvent(sourceId, SourceProcessingState.INDEXING_EVIDENCE, 50),
            SourceProcessingEvent(sourceId, SourceProcessingState.READY, 100),
        )
    }
}
