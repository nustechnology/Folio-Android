package com.nus.folio.testing

import com.nus.folio.domain.model.Source
import com.nus.folio.domain.model.SourceContentFormat
import com.nus.folio.domain.model.SourceDetail
import com.nus.folio.domain.model.SourceFileLocation
import com.nus.folio.domain.model.SourceLibrary
import com.nus.folio.domain.model.SourceStatus
import com.nus.folio.domain.model.SourceType
import com.nus.folio.domain.repository.SourceRepository

class FakeSourceRepository : SourceRepository {

    var getSourcesResult: Result<SourceLibrary>? = null
    var updateSourceResult: Result<Source>? = null
    var deleteSourceResult: Result<Unit>? = null
    var getSourcesCallCount = 0
    var updateSourceCallCount = 0
    var deleteSourceCallCount = 0
    var lastSpaceId: String? = null
    var lastUpdatedSource: Source? = null
    var lastDeletedSourceId: String? = null

    private val sources: MutableList<Source> = sampleSources.toMutableList()

    var getSourceDetailResult: Result<SourceDetail>? = null
    var getSourceDetailCallCount = 0
    var lastSourceId: String? = null

    var getOriginalFileResult: Result<SourceFileLocation>? = null
    var getOriginalFileCallCount = 0

    override suspend fun getSources(spaceId: String): Result<SourceLibrary> {
        getSourcesCallCount++
        lastSpaceId = spaceId
        getSourcesResult?.let { return it }
        return Result.success(libraryFor(spaceId))
    }

    override suspend fun updateSource(source: Source): Result<Source> {
        updateSourceCallCount++
        lastUpdatedSource = source
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

    private fun libraryFor(spaceId: String): SourceLibrary {
        val scoped = sources.filter { it.spaceId == spaceId }
        return SourceLibrary(
            sources = scoped,
            allCount = scoped.size,
            papersCount = scoped.count { it.type == SourceType.PDF },
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
        val source = sampleSources.find { it.id == sourceId && it.spaceId == spaceId }
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
                fileExtension = "pdf",
                contentFormat = SourceContentFormat.DOCUMENT,
                originalFileName = "source.pdf",
                htmlContent = "<h1>${source.title}</h1><p>Preview content.</p>",
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

    companion object {
        val sampleSources = listOf(
            Source("1", "Alan Turing: Computing Machinery", SourceType.PDF, "Alan Turing", "Added 2d ago", SourceStatus.READY, "1"),
            Source("2", "The Origins of Totalitarianism", SourceType.PDF, "Hannah Arendt", "Added 2d ago", SourceStatus.READY, "2"),
            Source("3", "Weapons of Math Destruction", SourceType.BOOK, "Cathy O'Neil", "Added 2d ago", SourceStatus.PROCESSING, "2"),
            Source("4", "The Age of Surveillance Capitalism", SourceType.PDF, "Shoshana Zuboff", "Added 2d ago", SourceStatus.FAILED, "1"),
            Source("5", "Attention Is All You Need", SourceType.PDF, "Vaswani et al.", "Added 2d ago", SourceStatus.READY, "1"),
            Source("6", "Interview notes: archival methods", SourceType.TEXT, "Field notes", "Added 2d ago", SourceStatus.READY, "3"),
            Source("7", "Lecture slides: Week 7", SourceType.PDF, "Teaching staff", "Added 3d ago", SourceStatus.READY, "4"),
            Source("8", "Course syllabus draft", SourceType.TEXT, "Teaching staff", "Added 3d ago", SourceStatus.READY, "4"),
            Source("9", "Wikipedia: Neural Networks", SourceType.WEB, "Wikipedia", "Added 2d ago", SourceStatus.READY, "1"),
            Source("10", "Research metrics dashboard", SourceType.PDF, "Research team", "Added 1d ago", SourceStatus.READY, "1"),
        )

        /** Unscoped snapshot used by older assertions that override results. */
        val sampleLibrary = SourceLibrary(
            sources = sampleSources,
            allCount = sampleSources.size,
            papersCount = sampleSources.count { it.type == SourceType.PDF },
            booksCount = sampleSources.count { it.type == SourceType.BOOK },
            webCount = sampleSources.count { it.type == SourceType.WEB },
            textCount = sampleSources.count { it.type == SourceType.TEXT },
        )
    }
}
