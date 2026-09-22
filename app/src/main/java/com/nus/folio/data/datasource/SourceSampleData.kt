package com.nus.folio.data.datasource

import com.nus.folio.domain.model.Source
import com.nus.folio.domain.model.SourceLibrary
import com.nus.folio.domain.model.SourcePaging
import com.nus.folio.domain.model.SourceStatus
import com.nus.folio.domain.model.SourceType

/**
 * Shared sample catalog used to seed the local [SourceDataSource] cache.
 */
internal object SourceSampleData {

    fun mutableDefaultSources(): MutableList<Source> = samples.toMutableList()

    fun libraryFrom(
        sources: List<Source>,
        allCount: Int = sources.size,
        papersCount: Int = sources.count { it.type == SourceType.FILE },
        booksCount: Int = sources.count { it.type == SourceType.BOOK },
        webCount: Int = sources.count { it.type == SourceType.WEB },
        textCount: Int = sources.count { it.type == SourceType.TEXT },
        page: Int = SourcePaging.DEFAULT_PAGE,
        limit: Int = SourcePaging.DEFAULT_LIMIT,
        hasMore: Boolean = false,
    ): SourceLibrary =
        SourceLibrary(
            sources = sources,
            allCount = allCount,
            papersCount = papersCount,
            booksCount = booksCount,
            webCount = webCount,
            textCount = textCount,
            page = page,
            limit = limit,
            hasMore = hasMore,
        )

    private val samples = listOf(
        Source(
            id = "1",
            title = "Alan Turing: Computing Machinery",
            type = SourceType.FILE,
            author = "Alan Turing",
            addedLabel = "Added 2d ago",
            status = SourceStatus.READY,
            spaceId = "1",
            fileExtension = "pdf",
        ),
        Source(
            id = "2",
            title = "The Origins of Totalitarianism",
            type = SourceType.FILE,
            author = "Hannah Arendt",
            addedLabel = "Added 2d ago",
            status = SourceStatus.READY,
            spaceId = "2",
            fileExtension = "pdf",
        ),
        Source(
            id = "3",
            title = "Weapons of Math Destruction",
            type = SourceType.BOOK,
            author = "Cathy O'Neil",
            addedLabel = "Added 2d ago",
            status = SourceStatus.PROCESSING,
            spaceId = "2",
            fileExtension = "epub",
        ),
        Source(
            id = "4",
            title = "The Age of Surveillance Capitalism",
            type = SourceType.FILE,
            author = "Shoshana Zuboff",
            addedLabel = "Added 2d ago",
            status = SourceStatus.FAILED,
            spaceId = "1",
            fileExtension = "pdf",
        ),
        Source(
            id = "5",
            title = "Attention Is All You Need",
            type = SourceType.FILE,
            author = "Vaswani et al.",
            addedLabel = "Added 2d ago",
            status = SourceStatus.READY,
            spaceId = "1",
            fileExtension = "pdf",
        ),
        Source(
            id = "6",
            title = "Interview notes: archival methods",
            type = SourceType.TEXT,
            author = "Field notes",
            addedLabel = "Added 2d ago",
            status = SourceStatus.READY,
            spaceId = "3",
            fileExtension = "txt",
        ),
        Source(
            id = "7",
            title = "Lecture slides: Week 7",
            type = SourceType.FILE,
            author = "Teaching staff",
            addedLabel = "Added 3d ago",
            status = SourceStatus.READY,
            spaceId = "4",
            fileExtension = "pptx",
        ),
        Source(
            id = "8",
            title = "Course syllabus draft",
            type = SourceType.TEXT,
            author = "Teaching staff",
            addedLabel = "Added 3d ago",
            status = SourceStatus.READY,
            spaceId = "4",
            fileExtension = "txt",
        ),
        Source(
            id = "9",
            title = "Wikipedia: Neural Networks",
            type = SourceType.WEB,
            author = "Wikipedia",
            addedLabel = "Added 2d ago",
            status = SourceStatus.READY,
            spaceId = "1",
            fileExtension = "md",
        ),
        Source(
            id = "10",
            title = "Research metrics dashboard",
            type = SourceType.FILE,
            author = "Research team",
            addedLabel = "Added 1d ago",
            status = SourceStatus.READY,
            spaceId = "1",
            fileExtension = "xlsx",
        ),
    )
}
