package com.nus.folio.testing

import com.nus.folio.domain.model.Source
import com.nus.folio.domain.model.SourceLibrary
import com.nus.folio.domain.model.SourceStatus
import com.nus.folio.domain.model.SourceType
import com.nus.folio.domain.repository.SourceRepository

class FakeSourceRepository : SourceRepository {

    var getSourcesResult: Result<SourceLibrary>? = null
    var getSourcesCallCount = 0
    var lastSpaceId: String? = null

    override suspend fun getSources(spaceId: String): Result<SourceLibrary> {
        getSourcesCallCount++
        lastSpaceId = spaceId
        getSourcesResult?.let { return it }
        val sources = sampleSources.filter { it.spaceId == spaceId }
        return Result.success(
            SourceLibrary(
                sources = sources,
                allCount = sources.size,
                papersCount = sources.count { it.type == SourceType.PDF },
                booksCount = sources.count { it.type == SourceType.BOOK },
                webCount = sources.count { it.type == SourceType.WEB },
                textCount = sources.count { it.type == SourceType.TEXT },
            ),
        )
    }

    companion object {
        val sampleSources = listOf(
            Source("1", "Alan Turing: Computing Machinery", SourceType.PDF, "Added 2d ago", SourceStatus.READY, "1"),
            Source("2", "The Origins of Totalitarianism", SourceType.PDF, "Added 2d ago", SourceStatus.READY, "2"),
            Source("3", "Weapons of Math Destruction", SourceType.BOOK, "Added 2d ago", SourceStatus.PROCESSING, "2"),
            Source("4", "The Age of Surveillance Capitalism", SourceType.PDF, "Added 2d ago", SourceStatus.FAILED, "1"),
            Source("5", "Attention Is All You Need", SourceType.PDF, "Added 2d ago", SourceStatus.READY, "1"),
            Source("6", "Interview notes: archival methods", SourceType.TEXT, "Added 2d ago", SourceStatus.READY, "3"),
            Source("7", "Lecture slides: Week 7", SourceType.PDF, "Added 3d ago", SourceStatus.READY, "4"),
            Source("8", "Course syllabus draft", SourceType.TEXT, "Added 3d ago", SourceStatus.READY, "4"),
            Source("9", "Wikipedia: Neural Networks", SourceType.WEB, "Added 2d ago", SourceStatus.READY, "1"),
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
