package com.nus.folio.testing

import com.nus.folio.domain.model.CreateSourceRequest
import com.nus.folio.domain.model.Source
import com.nus.folio.domain.model.SourceContentFormat
import com.nus.folio.domain.model.SourceDetail
import com.nus.folio.domain.model.SourceFileLocation
import com.nus.folio.domain.model.SourceLibrary
import com.nus.folio.domain.model.SourceProcessingEvent
import com.nus.folio.domain.model.SourceSort
import com.nus.folio.domain.model.SourceStatus
import com.nus.folio.domain.model.SourceType
import com.nus.folio.domain.repository.SourceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.UUID

class FakeSourceRepository : SourceRepository {

    var getSourcesResult: Result<SourceLibrary>? = null
    var createSourceResult: Result<Source>? = null
    var updateSourceResult: Result<Source>? = null
    var deleteSourceResult: Result<Unit>? = null
    var retrySourceResult: Result<Unit>? = null
    var getSourcesCallCount = 0
    var createSourceCallCount = 0
    var updateSourceCallCount = 0
    var deleteSourceCallCount = 0
    var retrySourceCallCount = 0
    var lastSpaceId: String? = null
    var lastSourceType: String? = null
    var lastSearch: String? = null
    var lastSort: SourceSort? = null
    var lastCreateRequest: CreateSourceRequest? = null
    var lastUpdatedSource: Source? = null
    var lastUpdatedContent: String? = null
    var lastDeletedSourceId: String? = null
    var lastRetriedSourceId: String? = null
    var getSourcesGate: (suspend (sourceType: String?, search: String?) -> Unit)? = null

    private val sources: MutableList<Source> = sampleSources.toMutableList()
    private val processingEvents = MutableSharedFlow<SourceProcessingEvent>(extraBufferCapacity = 16)
    var observeSourceProcessingCallCount = 0

    var getSourceDetailResult: Result<SourceDetail>? = null
    var getSourceDetailCallCount = 0
    var lastSourceId: String? = null

    var getOriginalFileResult: Result<SourceFileLocation>? = null
    var getOriginalFileCallCount = 0

    var getSourcePreviewUrlResult: Result<String?>? = null
    var getSourcePreviewUrlCallCount = 0

    override suspend fun getSources(
        spaceId: String,
        sourceType: String?,
        search: String?,
        sort: SourceSort,
    ): Result<SourceLibrary> {
        getSourcesCallCount++
        lastSpaceId = spaceId
        lastSourceType = sourceType
        lastSearch = search
        lastSort = sort
        getSourcesGate?.invoke(sourceType, search)
        getSourcesResult?.let { return it }
        return Result.success(libraryFor(spaceId, sourceType, search, sort))
    }

    override suspend fun createSource(request: CreateSourceRequest): Result<Source> {
        createSourceCallCount++
        lastCreateRequest = request
        lastSpaceId = request.spaceId
        createSourceResult?.let { return it }

        val source = when (request) {
            is CreateSourceRequest.Web -> Source(
                id = UUID.randomUUID().toString(),
                title = request.title.ifBlank { request.sourceUrl },
                type = SourceType.WEB,
                author = request.author,
                addedLabel = "Added just now",
                status = SourceStatus.PROCESSING,
                spaceId = request.spaceId,
            )
            is CreateSourceRequest.Manual -> Source(
                id = UUID.randomUUID().toString(),
                title = request.title,
                type = SourceType.TEXT,
                author = request.author,
                addedLabel = "Added just now",
                status = SourceStatus.PROCESSING,
                spaceId = request.spaceId,
            )
            is CreateSourceRequest.File -> Source(
                id = UUID.randomUUID().toString(),
                title = request.title.ifBlank { request.fileName },
                type = SourceType.FILE,
                author = request.author,
                addedLabel = "Added just now",
                status = SourceStatus.PROCESSING,
                spaceId = request.spaceId,
                fileExtension = request.fileName
                    .substringAfterLast('.', missingDelimiterValue = "")
                    .trim()
                    .lowercase()
                    .ifBlank { "pdf" },
            )
        }
        sources.add(0, source)
        return Result.success(source)
    }

    suspend fun emitProcessingEvent(event: SourceProcessingEvent) {
        processingEvents.emit(event)
    }

    override fun observeSourceProcessing(): Flow<SourceProcessingEvent> {
        observeSourceProcessingCallCount++
        return processingEvents.asSharedFlow()
    }

    override suspend fun updateSource(source: Source, content: String?): Result<Source> {
        updateSourceCallCount++
        lastUpdatedSource = source
        lastUpdatedContent = content
        updateSourceResult?.let { return it }
        val index = sources.indexOfFirst { it.id == source.id }
        if (index < 0) {
            return Result.failure(NoSuchElementException("Source not found: ${source.id}"))
        }
        sources[index] = source
        return Result.success(source)
    }

    override suspend fun deleteSource(sourceId: String): Result<Unit> {
        deleteSourceCallCount++
        lastDeletedSourceId = sourceId
        deleteSourceResult?.let { return it }
        val removed = sources.removeAll { it.id == sourceId }
        if (!removed) {
            return Result.failure(NoSuchElementException("Source not found: $sourceId"))
        }
        return Result.success(Unit)
    }

    override suspend fun retrySource(sourceId: String): Result<Unit> {
        retrySourceCallCount++
        lastRetriedSourceId = sourceId
        retrySourceResult?.let { return it }
        val index = sources.indexOfFirst { it.id == sourceId }
        if (index < 0) {
            return Result.failure(NoSuchElementException("Source not found: $sourceId"))
        }
        sources[index] = sources[index].copy(status = SourceStatus.PROCESSING)
        return Result.success(Unit)
    }

    private fun libraryFor(
        spaceId: String,
        sourceType: String? = null,
        search: String? = null,
        sort: SourceSort = SourceSort.DEFAULT,
    ): SourceLibrary {
        var scoped = sources.filter { it.spaceId == spaceId }
        scoped = when (sourceType?.lowercase()) {
            "file", "pdf" -> scoped.filter { it.type == SourceType.FILE || it.type == SourceType.BOOK }
            "web" -> scoped.filter { it.type == SourceType.WEB }
            "manual", "text" -> scoped.filter { it.type == SourceType.TEXT }
            else -> scoped
        }
        val query = search?.trim().orEmpty()
        if (query.isNotEmpty()) {
            scoped = scoped.filter { it.title.contains(query, ignoreCase = true) }
        }
        scoped = when (sort) {
            SourceSort.ALPHABETICAL_AZ -> scoped.sortedBy { it.title.lowercase() }
            SourceSort.ALPHABETICAL_ZA -> scoped.sortedByDescending { it.title.lowercase() }
            SourceSort.RECENTLY_ADDED -> scoped
        }
        return SourceLibrary(
            sources = scoped,
            allCount = scoped.size,
            papersCount = scoped.count { it.type == SourceType.FILE },
            booksCount = scoped.count { it.type == SourceType.BOOK },
            webCount = scoped.count { it.type == SourceType.WEB },
            textCount = scoped.count { it.type == SourceType.TEXT },
        )
    }

    override suspend fun getSourceDetail(spaceId: String, sourceId: String): Result<SourceDetail> {
        getSourceDetailCallCount++
        lastSpaceId = spaceId
        lastSourceId = sourceId
        getSourceDetailResult?.let { return it }
        val source = sources.find { it.id == sourceId && it.spaceId == spaceId }
            ?: return Result.failure(NoSuchElementException("Source not found"))
        return Result.success(
            SourceDetail(
                id = source.id,
                title = source.title,
                author = source.author,
                addedLabel = source.addedLabel,
                type = source.type,
                status = source.status,
                spaceId = source.spaceId,
                fileExtension = source.fileExtension.ifBlank { "pdf" },
                contentFormat = SourceContentFormat.DOCUMENT,
                originalFileName = "source.${source.fileExtension.ifBlank { "pdf" }}",
                htmlContent = "<h1>${source.title}</h1><p>Preview content.</p>",
                plainContent = when (source.type) {
                    SourceType.TEXT -> "Sample manual source content for editing."
                    else -> null
                },
            ),
        )
    }

    override suspend fun getOriginalFile(spaceId: String, sourceId: String): Result<SourceFileLocation> {
        getOriginalFileCallCount++
        lastSpaceId = spaceId
        lastSourceId = sourceId
        getOriginalFileResult?.let { return it }
        return Result.success(
            SourceFileLocation.Local(
                absolutePath = "/tmp/$sourceId.pdf",
                fileName = "source.pdf",
                mimeType = "application/pdf",
            ),
        )
    }

    override suspend fun getSourcePreviewUrl(sourceId: String): Result<String?> {
        getSourcePreviewUrlCallCount++
        lastSourceId = sourceId
        getSourcePreviewUrlResult?.let { return it }
        val source = sources.find { it.id == sourceId }
        return when (source?.type) {
            SourceType.FILE, SourceType.BOOK ->
                Result.success("https://example.org/preview/$sourceId.pdf")
            SourceType.WEB ->
                Result.success("https://example.org/article")
            else -> Result.success(null)
        }
    }

    companion object {
        val sampleSources = listOf(
            Source("1", "Alan Turing: Computing Machinery", SourceType.FILE, "Alan Turing", "Added 2d ago", SourceStatus.READY, "1", "pdf"),
            Source("2", "The Origins of Totalitarianism", SourceType.FILE, "Hannah Arendt", "Added 2d ago", SourceStatus.READY, "2", "pdf"),
            Source("3", "Weapons of Math Destruction", SourceType.BOOK, "Cathy O'Neil", "Added 2d ago", SourceStatus.PROCESSING, "2", "epub"),
            Source("4", "The Age of Surveillance Capitalism", SourceType.FILE, "Shoshana Zuboff", "Added 2d ago", SourceStatus.FAILED, "1", "pdf"),
            Source("5", "Attention Is All You Need", SourceType.FILE, "Vaswani et al.", "Added 2d ago", SourceStatus.READY, "1", "pdf"),
            Source("6", "Interview notes: archival methods", SourceType.TEXT, "Field notes", "Added 2d ago", SourceStatus.READY, "3", "txt"),
            Source("7", "Lecture slides: Week 7", SourceType.FILE, "Teaching staff", "Added 3d ago", SourceStatus.READY, "4", "pptx"),
            Source("8", "Course syllabus draft", SourceType.TEXT, "Teaching staff", "Added 3d ago", SourceStatus.READY, "4", "txt"),
            Source("9", "Wikipedia: Neural Networks", SourceType.WEB, "Wikipedia", "Added 2d ago", SourceStatus.READY, "1", "md"),
            Source("10", "Research metrics dashboard", SourceType.FILE, "Research team", "Added 1d ago", SourceStatus.READY, "1", "xlsx"),
        )

        /** Unscoped snapshot used by older assertions that override results. */
        val sampleLibrary = SourceLibrary(
            sources = sampleSources,
            allCount = sampleSources.size,
            papersCount = sampleSources.count { it.type == SourceType.FILE },
            booksCount = sampleSources.count { it.type == SourceType.BOOK },
            webCount = sampleSources.count { it.type == SourceType.WEB },
            textCount = sampleSources.count { it.type == SourceType.TEXT },
        )
    }
}
