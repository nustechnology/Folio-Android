package com.nus.folio.data.datasource

import com.nus.folio.domain.model.CreateSourceRequest
import com.nus.folio.domain.model.SourceContentFormat
import com.nus.folio.domain.model.SourceProcessingEvent
import com.nus.folio.domain.model.SourceProcessingState
import com.nus.folio.domain.model.SourceStatus
import com.nus.folio.domain.model.SourceType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SourceDataSourceCreateTest {

    @Test
    fun `fetchSources filters by type search and sort`() = runTest {
        val dataSource = SourceDataSource()

        val webOnly = dataSource.fetchSources(spaceId = "1", sourceType = "Web")
        assertEquals(1, webOnly.sources.size)
        assertEquals(SourceType.WEB, webOnly.sources.first().type)

        val search = dataSource.fetchSources(spaceId = "1", search = "Turing")
        assertEquals(1, search.sources.size)
        assertTrue(search.sources.first().title.contains("Turing"))

        val az = dataSource.fetchSources(
            spaceId = "1",
            sort = com.nus.folio.domain.model.SourceSort.ALPHABETICAL_AZ,
        )
        assertEquals(
            az.sources.map { it.title.lowercase() },
            az.sources.map { it.title.lowercase() }.sorted(),
        )
    }

    @Test
    fun `fetchSources does not throw for large page and limit`() = runTest {
        val dataSource = SourceDataSource()

        val library = dataSource.fetchSources(
            spaceId = "1",
            page = Int.MAX_VALUE,
            limit = Int.MAX_VALUE,
        )

        assertTrue(library.sources.isEmpty())
        assertFalse(library.hasMore)
    }

    @Test
    fun `createSource web adds local processing source`() = runTest {
        val dataSource = SourceDataSource()

        val created = dataSource.createSource(
            CreateSourceRequest.Web(
                spaceId = "1",
                sourceUrl = "https://example.com/article",
                title = "Example Article",
                author = "",
            ),
        )
        val library = dataSource.fetchSources("1")

        assertEquals("Example Article", created.title)
        assertEquals(SourceType.WEB, created.type)
        assertEquals(SourceStatus.PROCESSING, created.status)
        assertTrue(library.sources.any { it.id == created.id })
        assertEquals(6, library.allCount)
    }

    @Test
    fun `createSource manual adds text source`() = runTest {
        val dataSource = SourceDataSource()

        val created = dataSource.createSource(
            CreateSourceRequest.Manual(
                spaceId = "1",
                title = "My Research Notes",
                author = "Alice Johnson",
                content = "This is a manual source with enough content to satisfy validation.",
            ),
        )

        assertEquals(SourceType.TEXT, created.type)
        assertEquals("My Research Notes", created.title)
        assertEquals(1, dataSource.fetchSources("1").textCount)
    }

    @Test
    fun `createSource file adds pdf source`() = runTest {
        val dataSource = SourceDataSource()

        val created = dataSource.createSource(
            CreateSourceRequest.File(
                spaceId = "1",
                title = "AI Ethics Research Paper",
                author = "Alice Johnson",
                fileName = "paper.pdf",
                mimeType = "application/pdf",
                bytes = byteArrayOf(1, 2, 3, 4),
            ),
        )

        assertEquals(SourceType.FILE, created.type)
        assertEquals(SourceStatus.PROCESSING, created.status)
        assertEquals(5, dataSource.fetchSources("1").papersCount)
    }

    @Test
    fun `observeSourceProcessing emits timed ready pipeline for processing sources`() = runTest {
        val dataSource = SourceDataSource(processingScope = backgroundScope)
        val created = dataSource.createSource(
            CreateSourceRequest.Manual(
                spaceId = "1",
                title = "Notes",
                author = "Alice",
                content = "Enough content for validation.",
            ),
        )

        val events = dataSource.observeSourceProcessing()
            .filter { it.sourceId == created.id }
            .take(4)
            .toList()

        assertEquals(4, events.size)
        assertTrue(events.all { it.sourceId == created.id })
        assertEquals(SourceProcessingState.ADDED, events.first().state)
        assertEquals(SourceProcessingState.READY, events.last().state)
        assertEquals(
            SourceStatus.READY,
            dataSource.fetchSources("1").sources.first { it.id == created.id }.status,
        )
    }

    @Test
    fun `observeSourceProcessing receives pipeline for source created while collecting`() = runTest {
        val dataSource = SourceDataSource(processingScope = backgroundScope)
        val collected = mutableListOf<SourceProcessingEvent>()
        val subscribed = CompletableDeferred<Unit>()
        val collectJob = launch {
            dataSource.observeSourceProcessing()
                .onStart { subscribed.complete(Unit) }
                .collect { collected += it }
        }
        subscribed.await()

        val created = dataSource.createSource(
            CreateSourceRequest.Manual(
                spaceId = "1",
                title = "Notes while collecting",
                author = "Alice",
                content = "Enough content for validation.",
            ),
        )

        val events = collectProcessingEventsForSource(collected, created.id)

        assertEquals(SourceProcessingState.ADDED, events.first().state)
        assertEquals(SourceProcessingState.READY, events.last().state)
        assertEquals(
            SourceStatus.READY,
            dataSource.fetchSources("1").sources.first { it.id == created.id }.status,
        )
        collectJob.cancel()
    }

    @Test
    fun `observeSourceProcessing receives pipeline for retry while collecting`() = runTest {
        val dataSource = SourceDataSource(processingScope = backgroundScope)
        val collected = mutableListOf<SourceProcessingEvent>()
        val subscribed = CompletableDeferred<Unit>()
        val collectJob = launch {
            dataSource.observeSourceProcessing()
                .onStart { subscribed.complete(Unit) }
                .collect { collected += it }
        }
        subscribed.await()

        dataSource.retrySource("4")

        val events = collectProcessingEventsForSource(collected, "4")

        assertEquals(SourceProcessingState.READY, events.last().state)
        assertEquals(
            SourceStatus.READY,
            dataSource.fetchSources("1").sources.first { it.id == "4" }.status,
        )
        collectJob.cancel()
    }

    @Test
    fun `fetchSourceDetail for created manual source uses request content`() = runTest {
        val dataSource = SourceDataSource()
        val content = "Unique manual notes about archival methods."
        val created = dataSource.createSource(
            CreateSourceRequest.Manual(
                spaceId = "1",
                title = "My Research Notes",
                author = "Alice Johnson",
                content = content,
            ),
        )

        val detail = dataSource.fetchSourceDetail("1", created.id)

        assertEquals(created.id, detail.id)
        assertEquals(SourceType.TEXT, detail.type)
        assertEquals(SourceContentFormat.DOCUMENT, detail.contentFormat)
        assertEquals("txt", detail.fileExtension)
        assertTrue(detail.htmlContent.orEmpty().contains(content))
        assertTrue(detail.htmlContent.orEmpty().contains("My Research Notes"))
        assertFalse(detail.htmlContent.orEmpty().contains("Imitation Game"))
    }

    @Test
    fun `fetchSourceDetail for created xlsx source uses sheet format`() = runTest {
        val dataSource = SourceDataSource()
        val created = dataSource.createSource(
            CreateSourceRequest.File(
                spaceId = "1",
                title = "Budget Tracker",
                author = "Finance",
                fileName = "budget.xlsx",
                mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                bytes = byteArrayOf(1, 2, 3, 4),
            ),
        )

        val detail = dataSource.fetchSourceDetail("1", created.id)

        assertEquals(created.id, detail.id)
        assertEquals(SourceContentFormat.SHEET, detail.contentFormat)
        assertEquals("xlsx", detail.fileExtension)
        assertEquals("budget.xlsx", detail.originalFileName)
        assertTrue(detail.sheets.isNotEmpty())
        assertNull(detail.htmlContent)
    }

    @Test
    fun `fetchSourceDetail returns local sample detail`() = runTest {
        val dataSource = SourceDataSource()

        val detail = dataSource.fetchSourceDetail("1", "1")

        assertEquals("1", detail.id)
        assertEquals("Alan Turing: Computing Machinery", detail.title)
    }

    @Test
    fun `retrySource marks source processing`() = runTest {
        val dataSource = SourceDataSource()

        dataSource.retrySource("4")

        assertEquals(
            SourceStatus.PROCESSING,
            dataSource.fetchSources("1").sources.first { it.id == "4" }.status,
        )
    }

    @Test
    fun `deleteSource removes local source`() = runTest {
        val dataSource = SourceDataSource()

        dataSource.deleteSource("1")

        assertTrue(dataSource.fetchSources("1").sources.none { it.id == "1" })
    }

    /**
     * Advances virtual time in [stepDelayMs] steps until [expectedCount] events arrive or the
     * budget is exhausted, then asserts the event count so missing events fail fast in CI.
     */
    private fun TestScope.collectProcessingEventsForSource(
        collected: List<SourceProcessingEvent>,
        sourceId: String,
        expectedCount: Int = 4,
        stepDelayMs: Long = 700L,
    ): List<SourceProcessingEvent> {
        val filtered = mutableListOf<SourceProcessingEvent>()
        var elapsedVirtualMs = 0L
        val maxVirtualMs = stepDelayMs * expectedCount
        while (filtered.size < expectedCount && elapsedVirtualMs < maxVirtualMs) {
            advanceTimeBy(stepDelayMs)
            elapsedVirtualMs += stepDelayMs
            runCurrent()
            filtered.clear()
            filtered += collected.filter { it.sourceId == sourceId }
        }
        assertEquals(
            "Expected $expectedCount processing events for source $sourceId " +
                "within ${maxVirtualMs}ms virtual time",
            expectedCount,
            filtered.size,
        )
        return filtered.toList()
    }
}
