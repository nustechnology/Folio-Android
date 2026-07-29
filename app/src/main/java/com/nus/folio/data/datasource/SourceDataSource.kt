package com.nus.folio.data.datasource

import com.nus.folio.domain.model.Source
import com.nus.folio.domain.model.SourceLibrary
import com.nus.folio.domain.model.SourceStatus
import com.nus.folio.domain.model.SourceType
import kotlinx.coroutines.delay

class SourceDataSource {

    suspend fun fetchSources(): SourceLibrary {
        delay(200)
        return SourceLibrary(
            sources = sampleSources,
            allCount = 128,
            papersCount = 80,
            booksCount = 24,
            webCount = 18,
            textCount = 6,
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
            ),
            Source(
                id = "2",
                title = "The Origins of Totalitarianism",
                type = SourceType.PDF,
                addedLabel = "Added 2d ago",
                status = SourceStatus.READY,
            ),
            Source(
                id = "3",
                title = "Weapons of Math Destruction",
                type = SourceType.BOOK,
                addedLabel = "Added 2d ago",
                status = SourceStatus.PROCESSING,
            ),
            Source(
                id = "4",
                title = "The Age of Surveillance Capitalism",
                type = SourceType.PDF,
                addedLabel = "Added 2d ago",
                status = SourceStatus.FAILED,
            ),
            Source(
                id = "5",
                title = "Attention Is All You Need",
                type = SourceType.PDF,
                addedLabel = "Added 2d ago",
                status = SourceStatus.READY,
            ),
            Source(
                id = "6",
                title = "Interview notes: archival methods",
                type = SourceType.TEXT,
                addedLabel = "Added 2d ago",
                status = SourceStatus.READY,
            ),
        )
    }
}
