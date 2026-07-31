package com.nus.folio.data.datasource

import com.nus.folio.domain.model.Source
import com.nus.folio.domain.model.SourceLibrary
import com.nus.folio.domain.model.SourceStatus
import com.nus.folio.domain.model.SourceType
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SourceDataSource {

    private val mutex = Mutex()
    private val sources: MutableList<Source> = defaultSources.toMutableList()

    suspend fun fetchSources(spaceId: String): SourceLibrary {
        delay(200)
        return libraryFor(spaceId)
    }

    suspend fun updateSource(source: Source): Source {
        delay(200)
        return mutex.withLock {
            val index = sources.indexOfFirst { it.id == source.id }
            if (index < 0) {
                throw NoSuchElementException("Source not found: ${source.id}")
            }
            sources[index] = source
            source
        }
    }

    suspend fun deleteSource(sourceId: String) {
        delay(200)
        mutex.withLock {
            val removed = sources.removeAll { it.id == sourceId }
            if (!removed) {
                throw NoSuchElementException("Source not found: $sourceId")
            }
        }
    }

    private suspend fun libraryFor(spaceId: String): SourceLibrary =
        mutex.withLock {
            val scoped = sources.filter { it.spaceId == spaceId }
            SourceLibrary(
                sources = scoped,
                allCount = scoped.size,
                papersCount = scoped.count { it.type == SourceType.PDF },
                booksCount = scoped.count { it.type == SourceType.BOOK },
                webCount = scoped.count { it.type == SourceType.WEB },
                textCount = scoped.count { it.type == SourceType.TEXT },
            )
        }

    companion object {
        private val defaultSources = listOf(
            Source(
                id = "1",
                title = "Alan Turing: Computing Machinery",
                type = SourceType.PDF,
                author = "Alan Turing",
                addedLabel = "Added 2d ago",
                status = SourceStatus.READY,
                spaceId = "1",
            ),
            Source(
                id = "2",
                title = "The Origins of Totalitarianism",
                type = SourceType.PDF,
                author = "Hannah Arendt",
                addedLabel = "Added 2d ago",
                status = SourceStatus.READY,
                spaceId = "2",
            ),
            Source(
                id = "3",
                title = "Weapons of Math Destruction",
                type = SourceType.BOOK,
                author = "Cathy O'Neil",
                addedLabel = "Added 2d ago",
                status = SourceStatus.PROCESSING,
                spaceId = "2",
            ),
            Source(
                id = "4",
                title = "The Age of Surveillance Capitalism",
                type = SourceType.PDF,
                author = "Shoshana Zuboff",
                addedLabel = "Added 2d ago",
                status = SourceStatus.FAILED,
                spaceId = "1",
            ),
            Source(
                id = "5",
                title = "Attention Is All You Need",
                type = SourceType.PDF,
                author = "Vaswani et al.",
                addedLabel = "Added 2d ago",
                status = SourceStatus.READY,
                spaceId = "1",
            ),
            Source(
                id = "6",
                title = "Interview notes: archival methods",
                type = SourceType.TEXT,
                author = "Field notes",
                addedLabel = "Added 2d ago",
                status = SourceStatus.READY,
                spaceId = "3",
            ),
            Source(
                id = "7",
                title = "Lecture slides: Week 7",
                type = SourceType.PDF,
                author = "Teaching staff",
                addedLabel = "Added 3d ago",
                status = SourceStatus.READY,
                spaceId = "4",
            ),
            Source(
                id = "8",
                title = "Course syllabus draft",
                type = SourceType.TEXT,
                author = "Teaching staff",
                addedLabel = "Added 3d ago",
                status = SourceStatus.READY,
                spaceId = "4",
            ),
            Source(
                id = "9",
                title = "Wikipedia: Neural Networks",
                type = SourceType.WEB,
                author = "Wikipedia",
                addedLabel = "Added 2d ago",
                status = SourceStatus.READY,
                spaceId = "1",
            ),
        )
    }
}
