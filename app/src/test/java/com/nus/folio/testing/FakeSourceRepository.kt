package com.nus.folio.testing

import com.nus.folio.domain.model.Source
import com.nus.folio.domain.model.SourceLibrary
import com.nus.folio.domain.model.SourceStatus
import com.nus.folio.domain.model.SourceType
import com.nus.folio.domain.repository.SourceRepository

class FakeSourceRepository : SourceRepository {

    var getSourcesResult: Result<SourceLibrary> = Result.success(sampleLibrary)
    var getSourcesCallCount = 0

    override suspend fun getSources(): Result<SourceLibrary> {
        getSourcesCallCount++
        return getSourcesResult
    }

    companion object {
        val sampleLibrary = SourceLibrary(
            sources = listOf(
                Source("1", "Alan Turing: Computing Machinery", SourceType.PDF, "Added 2d ago", SourceStatus.READY),
                Source("2", "The Origins of Totalitarianism", SourceType.PDF, "Added 2d ago", SourceStatus.READY),
                Source("3", "Weapons of Math Destruction", SourceType.BOOK, "Added 2d ago", SourceStatus.PROCESSING),
                Source("4", "The Age of Surveillance Capitalism", SourceType.PDF, "Added 2d ago", SourceStatus.FAILED),
                Source("5", "Attention Is All You Need", SourceType.PDF, "Added 2d ago", SourceStatus.READY),
                Source("6", "Wikipedia: Neural Networks", SourceType.WEB, "Added 2d ago", SourceStatus.READY),
                Source("7", "Interview notes: archival methods", SourceType.TEXT, "Added 2d ago", SourceStatus.READY),
            ),
            allCount = 128,
            papersCount = 80,
            booksCount = 24,
            webCount = 18,
            textCount = 6,
        )
    }
}
