package com.nus.folio.data.datasource

import com.nus.folio.domain.model.Source
import com.nus.folio.domain.model.SourceLibrary
import com.nus.folio.domain.model.SourceStatus
import com.nus.folio.domain.model.SourceType
import kotlinx.coroutines.delay

class SourceDataSource {

    suspend fun fetchSources(spaceId: String): SourceLibrary {
        delay(200)
        val sources = sampleSources.filter { it.spaceId == spaceId }
        return SourceLibrary(
            sources = sources,
            allCount = sources.size,
            papersCount = sources.count { it.type == SourceType.PDF },
            booksCount = sources.count { it.type == SourceType.BOOK },
            webCount = sources.count { it.type == SourceType.WEB },
            textCount = sources.count { it.type == SourceType.TEXT },
        )
    }

    companion object {
        private val sampleSources = listOf(
            Source(
                id = "1",
                title = "Alan Turing: Computing Machinery",
                type = SourceType.PDF,
                addedLabel = "Added 2d ago",
                status = SourceStatus.READY,
                spaceId = "1",
            ),
            Source(
                id = "2",
                title = "The Origins of Totalitarianism",
                type = SourceType.PDF,
                addedLabel = "Added 2d ago",
                status = SourceStatus.READY,
                spaceId = "2",
            ),
            Source(
                id = "3",
                title = "Weapons of Math Destruction",
                type = SourceType.BOOK,
                addedLabel = "Added 2d ago",
                status = SourceStatus.PROCESSING,
                spaceId = "2",
            ),
            Source(
                id = "4",
                title = "The Age of Surveillance Capitalism",
                type = SourceType.PDF,
                addedLabel = "Added 2d ago",
                status = SourceStatus.FAILED,
                spaceId = "1",
            ),
            Source(
                id = "5",
                title = "Attention Is All You Need",
                type = SourceType.PDF,
                addedLabel = "Added 2d ago",
                status = SourceStatus.READY,
                spaceId = "1",
            ),
            Source(
                id = "6",
                title = "Interview notes: archival methods",
                type = SourceType.TEXT,
                addedLabel = "Added 2d ago",
                status = SourceStatus.READY,
                spaceId = "3",
            ),
            Source(
                id = "7",
                title = "Lecture slides: Week 7",
                type = SourceType.PDF,
                addedLabel = "Added 3d ago",
                status = SourceStatus.READY,
                spaceId = "4",
            ),
            Source(
                id = "8",
                title = "Course syllabus draft",
                type = SourceType.TEXT,
                addedLabel = "Added 3d ago",
                status = SourceStatus.READY,
                spaceId = "4",
            ),
            Source(
                id = "9",
                title = "Wikipedia: Neural Networks",
                type = SourceType.WEB,
                addedLabel = "Added 2d ago",
                status = SourceStatus.READY,
                spaceId = "1",
            ),
        )
    }
}
