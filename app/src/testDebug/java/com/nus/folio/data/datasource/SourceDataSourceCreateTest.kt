package com.nus.folio.data.datasource

import com.nus.folio.data.network.SourcesApi
import com.nus.folio.data.network.UnauthorizedException
import com.nus.folio.domain.model.CreateSourceRequest
import com.nus.folio.domain.model.Source
import com.nus.folio.domain.model.SourceContentFormat
import com.nus.folio.domain.model.SourceDetail
import com.nus.folio.domain.model.SourceLibrary
import com.nus.folio.domain.model.SourcePaging
import com.nus.folio.domain.model.SourceProcessingEvent
import com.nus.folio.domain.model.SourceProcessingState
import com.nus.folio.domain.model.SourceStatus
import com.nus.folio.domain.model.SourceType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SourceDataSourceCreateTest {

    private class FakeSourcesApi : SourcesApi {
        var lastAccessToken: String? = null
        var lastWebUrl: String? = null
        var lastManualTitle: String? = null
        var lastManualContent: String? = null
        var lastFileName: String? = null
        var lastFileMimeType: String? = null
        var lastFileBytes: ByteArray? = null
        var lastListSpaceId: String? = null
        var lastListSourceType: String? = null
        var lastListSearch: String? = null
        var lastListSort: String? = null
        var lastListPage: Int? = null
        var lastListLimit: Int? = null
        var lastGetSourceId: String? = null
        var lastRetrySourceId: String? = null
        var lastDeleteSourceId: String? = null
        var lastUpdateSourceId: String? = null
        var lastUpdateTitle: String? = null
        var lastUpdateAuthor: String? = null
        var lastUpdateContent: String? = null
        var createCallCount = 0
        var listCallCount = 0
        var getCallCount = 0
        var retryCallCount = 0
        var deleteCallCount = 0
        var updateCallCount = 0
        var failUnauthorizedOnce = false
        var observeUnauthorizedOnce = false
        var listedSources: List<Source> = emptyList()
        var detailResult: SourceDetail = SourceDetail(
            id = "detail-1",
            title = "AI Ethics Research Paper",
            author = "Alice Johnson",
            addedLabel = "Added just now",
            type = SourceType.FILE,
            status = SourceStatus.PROCESSING,
            spaceId = "1",
            fileExtension = "pdf",
            contentFormat = SourceContentFormat.DOCUMENT,
            originalFileName = "ethics.pdf",
            htmlContent = "<p>Hello</p>",
        )
        private val statusEvents = MutableSharedFlow<SourceProcessingEvent>(extraBufferCapacity = 16)

        override suspend fun listSources(
            accessToken: String,
            spaceId: String,
            sourceType: String?,
            search: String?,
            sort: String,
            page: Int,
            limit: Int,
        ): SourceLibrary {
            listCallCount++
            lastAccessToken = accessToken
            lastListSpaceId = spaceId
            lastListSourceType = sourceType
            lastListSearch = search
            lastListSort = sort
            lastListPage = page
            lastListLimit = limit
            throwIfUnauthorized(accessToken)
            return SourceSampleData.libraryFrom(
                sources = listedSources,
                page = page,
                limit = limit,
                hasMore = false,
            )
        }

        override suspend fun getSource(accessToken: String, sourceId: String): SourceDetail {
            getCallCount++
            lastAccessToken = accessToken
            lastGetSourceId = sourceId
            throwIfUnauthorized(accessToken)
            return detailResult.copy(id = sourceId)
        }

        override suspend fun createWebSource(
            accessToken: String,
            spaceId: String,
            sourceUrl: String,
            title: String,
            author: String,
        ): Source {
            createCallCount++
            lastAccessToken = accessToken
            lastWebUrl = sourceUrl
            throwIfUnauthorized(accessToken)
            return Source(
                id = "web-1",
                title = title.ifBlank { sourceUrl },
                type = SourceType.WEB,
                author = author,
                addedLabel = "Added just now",
                status = SourceStatus.PROCESSING,
                spaceId = spaceId,
            )
        }

        override suspend fun createManualSource(
            accessToken: String,
            spaceId: String,
            title: String,
            author: String,
            content: String,
        ): Source {
            createCallCount++
            lastAccessToken = accessToken
            lastManualTitle = title
            lastManualContent = content
            throwIfUnauthorized(accessToken)
            return Source(
                id = "manual-1",
                title = title,
                type = SourceType.TEXT,
                author = author,
                addedLabel = "Added just now",
                status = SourceStatus.PROCESSING,
                spaceId = spaceId,
            )
        }

        override suspend fun createFileSource(
            accessToken: String,
            spaceId: String,
            title: String,
            author: String,
            fileName: String,
            mimeType: String,
            fileBytes: ByteArray,
        ): Source {
            createCallCount++
            lastAccessToken = accessToken
            lastFileName = fileName
            lastFileMimeType = mimeType
            lastFileBytes = fileBytes.copyOf()
            throwIfUnauthorized(accessToken)
            return Source(
                id = "file-1",
                title = title.ifBlank { fileName },
                type = SourceType.FILE,
                author = author,
                addedLabel = "Added just now",
                status = SourceStatus.PROCESSING,
                spaceId = spaceId,
            )
        }

        override suspend fun retrySource(accessToken: String, sourceId: String): SourceDetail? {
            retryCallCount++
            lastAccessToken = accessToken
            lastRetrySourceId = sourceId
            throwIfUnauthorized(accessToken)
            return detailResult.copy(id = sourceId, status = SourceStatus.PROCESSING)
        }

        override suspend fun deleteSource(accessToken: String, sourceId: String) {
            deleteCallCount++
            lastAccessToken = accessToken
            lastDeleteSourceId = sourceId
            throwIfUnauthorized(accessToken)
        }

        override suspend fun updateSource(
            accessToken: String,
            sourceId: String,
            title: String,
            author: String,
            content: String?,
        ): Source {
            updateCallCount++
            lastAccessToken = accessToken
            lastUpdateSourceId = sourceId
            lastUpdateTitle = title
            lastUpdateAuthor = author
            lastUpdateContent = content
            throwIfUnauthorized(accessToken)
            return Source(
                id = sourceId,
                title = title,
                type = if (content != null) SourceType.TEXT else SourceType.FILE,
                author = author,
                addedLabel = "Added just now",
                status = SourceStatus.READY,
                spaceId = "1",
            )
        }

        override suspend fun getSourcePreview(accessToken: String, sourceId: String): String? {
            lastAccessToken = accessToken
            throwIfUnauthorized(accessToken)
            return "https://example.org/preview/$sourceId.pdf"
        }

        override fun observeSourceStatus(accessToken: String): Flow<SourceProcessingEvent> {
            lastAccessToken = accessToken
            if (observeUnauthorizedOnce && accessToken == "expired-token") {
                observeUnauthorizedOnce = false
                throw UnauthorizedException("Source status stream failed (HTTP 401)")
            }
            return statusEvents.asSharedFlow()
        }

        suspend fun emitStatus(event: SourceProcessingEvent) {
            statusEvents.emit(event)
        }

        private fun throwIfUnauthorized(accessToken: String) {
            if (failUnauthorizedOnce && accessToken == "expired-token") {
                failUnauthorizedOnce = false
                throw UnauthorizedException("Sources API failed (HTTP 401)")
            }
        }
    }

    @Test
    fun `createSource web calls API and upserts locally`() = runTest {
        val api = FakeSourcesApi()
        val dataSource = SourceDataSource(
            sourcesApi = api,
            accessTokenProvider = { "token" },
        )

        val created = dataSource.createSource(
            CreateSourceRequest.Web(
                spaceId = "1",
                sourceUrl = "https://example.com/article",
                title = "Example Article",
                author = "",
            ),
        )

        assertEquals("web-1", created.id)
        assertEquals("https://example.com/article", api.lastWebUrl)
        assertEquals("Example Article", created.title)
        assertEquals(1, api.createCallCount)
    }

    @Test
    fun `fetchSources forwards query params and returns API list`() = runTest {
        val api = FakeSourcesApi().apply {
            listedSources = listOf(
                Source(
                    id = "api-1",
                    title = "API Source",
                    type = SourceType.FILE,
                    author = "Author",
                    addedLabel = "Added just now",
                    status = SourceStatus.READY,
                    spaceId = "space-1",
                ),
            )
        }
        val dataSource = SourceDataSource(
            sourcesApi = api,
            accessTokenProvider = { "token" },
        )

        val library = dataSource.fetchSources(
            spaceId = "space-1",
            sourceType = "File",
            search = "API",
            sort = com.nus.folio.domain.model.SourceSort.ALPHABETICAL_AZ,
        )

        assertEquals(1, library.allCount)
        assertEquals("api-1", library.sources.first().id)
        assertEquals("space-1", api.lastListSpaceId)
        assertEquals("File", api.lastListSourceType)
        assertEquals("API", api.lastListSearch)
        assertEquals("alphabetical-az", api.lastListSort)
        assertEquals(SourcePaging.DEFAULT_PAGE, api.lastListPage)
        assertEquals(SourcePaging.DEFAULT_LIMIT, api.lastListLimit)
        assertFalse(library.hasMore)
    }

    @Test
    fun `fetchSources retries after unauthorized`() = runTest {
        val api = FakeSourcesApi().apply {
            failUnauthorizedOnce = true
            listedSources = listOf(
                Source(
                    id = "api-1",
                    title = "API Source",
                    type = SourceType.WEB,
                    author = "",
                    addedLabel = "Added just now",
                    status = SourceStatus.READY,
                    spaceId = "1",
                ),
            )
        }
        var refreshCount = 0
        val dataSource = SourceDataSource(
            sourcesApi = api,
            accessTokenProvider = { "expired-token" },
            refreshAccessToken = {
                refreshCount++
                "fresh-token"
            },
        )

        val library = dataSource.fetchSources("1")

        assertEquals(1, library.sources.size)
        assertEquals(1, refreshCount)
        assertEquals("fresh-token", api.lastAccessToken)
        assertEquals(2, api.listCallCount)
    }

    @Test
    fun `updateSource calls API and upserts locally`() = runTest {
        val listed = Source(
            id = "api-1",
            title = "API Source",
            type = SourceType.FILE,
            author = "Author",
            addedLabel = "Added just now",
            status = SourceStatus.READY,
            spaceId = "1",
        )
        val api = FakeSourcesApi().apply {
            listedSources = listOf(listed)
        }
        val dataSource = SourceDataSource(
            sourcesApi = api,
            accessTokenProvider = { "token" },
        )

        dataSource.fetchSources("1")
        val updated = dataSource.updateSource(
            listed.copy(title = "Updated title", author = "Updated author"),
        )

        assertEquals(1, api.updateCallCount)
        assertEquals("api-1", api.lastUpdateSourceId)
        assertEquals("Updated title", api.lastUpdateTitle)
        assertEquals("Updated author", api.lastUpdateAuthor)
        assertNull(api.lastUpdateContent)
        assertEquals("Updated title", updated.title)
        assertEquals("Updated author", updated.author)
        assertEquals(SourceType.FILE, updated.type)
    }

    @Test
    fun `updateSource forwards content for text sources`() = runTest {
        val listed = Source(
            id = "manual-1",
            title = "Notes",
            type = SourceType.TEXT,
            author = "Alice",
            addedLabel = "Added just now",
            status = SourceStatus.READY,
            spaceId = "1",
        )
        val api = FakeSourcesApi().apply {
            listedSources = listOf(listed)
        }
        val dataSource = SourceDataSource(
            sourcesApi = api,
            accessTokenProvider = { "token" },
        )
        val content = "This is the updated manual source content text note..."

        dataSource.fetchSources("1")
        val updated = dataSource.updateSource(
            listed.copy(title = "Updated AI Ethics Research Paper", author = "Alice Johnson"),
            content = content,
        )

        assertEquals(1, api.updateCallCount)
        assertEquals(content, api.lastUpdateContent)
        assertEquals("Updated AI Ethics Research Paper", updated.title)
        assertEquals("Alice Johnson", updated.author)
        assertEquals(SourceType.TEXT, updated.type)
    }

    @Test
    fun `createSource manual forwards content`() = runTest {
        val api = FakeSourcesApi()
        val dataSource = SourceDataSource(
            sourcesApi = api,
            accessTokenProvider = { "token" },
        )
        val content = "Field notes on archival digitization and OCR quality."

        val created = dataSource.createSource(
            CreateSourceRequest.Manual(
                spaceId = "1",
                title = "Research Notes",
                author = "Alice",
                content = content,
            ),
        )

        assertEquals("manual-1", created.id)
        assertEquals("Research Notes", api.lastManualTitle)
        assertEquals(content, api.lastManualContent)
        assertEquals(1, api.createCallCount)
    }

    @Test
    fun `createSource retries after unauthorized`() = runTest {
        val api = FakeSourcesApi().apply { failUnauthorizedOnce = true }
        var refreshCount = 0
        val dataSource = SourceDataSource(
            sourcesApi = api,
            accessTokenProvider = { "expired-token" },
            refreshAccessToken = {
                refreshCount++
                "fresh-token"
            },
        )
        val content = "Enough content for validation."

        val created = dataSource.createSource(
            CreateSourceRequest.Manual(
                spaceId = "1",
                title = "Notes",
                author = "Alice",
                content = content,
            ),
        )

        assertEquals("manual-1", created.id)
        assertEquals(content, api.lastManualContent)
        assertEquals(1, refreshCount)
        assertEquals("fresh-token", api.lastAccessToken)
        assertEquals(2, api.createCallCount)
    }

    @Test
    fun `createSource file uploads bytes`() = runTest {
        val api = FakeSourcesApi()
        val dataSource = SourceDataSource(
            sourcesApi = api,
            accessTokenProvider = { "token" },
        )
        val bytes = byteArrayOf(1, 2, 3)

        val created = dataSource.createSource(
            CreateSourceRequest.File(
                spaceId = "1",
                title = "Paper",
                author = "Author",
                fileName = "paper.pdf",
                mimeType = "application/pdf",
                bytes = bytes,
            ),
        )

        assertEquals("file-1", created.id)
        assertEquals("paper.pdf", api.lastFileName)
        assertEquals("application/pdf", api.lastFileMimeType)
        assertTrue(bytes.contentEquals(api.lastFileBytes))
        assertEquals(SourceType.FILE, created.type)
    }

    @Test
    fun `observeSourceProcessing forwards API events and updates local status`() = runTest {
        val api = FakeSourcesApi()
        val dataSource = SourceDataSource(
            sourcesApi = api,
            accessTokenProvider = { "token" },
        )
        val created = dataSource.createSource(
            CreateSourceRequest.Web(
                spaceId = "1",
                sourceUrl = "https://example.com",
                title = "Article",
                author = "",
            ),
        )

        assertEquals(SourceStatus.PROCESSING, created.status)
        assertEquals(SourceStatus.PROCESSING, dataSource.cachedSource("web-1")?.status)

        val collected = mutableListOf<SourceProcessingEvent>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            dataSource.observeSourceProcessing().take(2).toList(collected)
        }

        api.emitStatus(
            SourceProcessingEvent("web-1", SourceProcessingState.EXTRACTING_TEXT, 25),
        )
        api.emitStatus(
            SourceProcessingEvent("web-1", SourceProcessingState.READY, 100),
        )
        job.join()

        assertEquals(2, collected.size)
        assertEquals(SourceProcessingState.READY, collected.last().state)
        assertEquals(SourceStatus.READY, dataSource.cachedSource("web-1")?.status)
        assertEquals(
            SourceStatus.PROCESSING,
            dataSource.fetchSourceDetail("1", "web-1").status,
        )
    }

    @Test
    fun `fetchSourceDetail calls API`() = runTest {
        val api = FakeSourcesApi()
        val dataSource = SourceDataSource(
            sourcesApi = api,
            accessTokenProvider = { "token" },
        )

        val detail = dataSource.fetchSourceDetail("1", "detail-1")

        assertEquals(1, api.getCallCount)
        assertEquals("detail-1", api.lastGetSourceId)
        assertEquals("AI Ethics Research Paper", detail.title)
        assertEquals("<p>Hello</p>", detail.htmlContent)
    }

    @Test
    fun `retrySource calls API`() = runTest {
        val api = FakeSourcesApi()
        val dataSource = SourceDataSource(
            sourcesApi = api,
            accessTokenProvider = { "token" },
        )
        dataSource.createSource(
            CreateSourceRequest.Manual(
                spaceId = "1",
                title = "Notes",
                author = "Alice",
                content = "Enough content for validation.",
            ),
        )

        dataSource.retrySource("manual-1")

        assertEquals(1, api.retryCallCount)
        assertEquals("manual-1", api.lastRetrySourceId)
    }

    @Test
    fun `deleteSource calls API and removes local source`() = runTest {
        val api = FakeSourcesApi().apply {
            listedSources = emptyList()
        }
        val dataSource = SourceDataSource(
            sourcesApi = api,
            accessTokenProvider = { "token" },
        )
        dataSource.createSource(
            CreateSourceRequest.Manual(
                spaceId = "1",
                title = "Notes",
                author = "Alice",
                content = "Enough content for validation.",
            ),
        )

        dataSource.deleteSource("manual-1")

        assertEquals(1, api.deleteCallCount)
        assertEquals("manual-1", api.lastDeleteSourceId)
        assertTrue(dataSource.fetchSources("1").sources.none { it.id == "manual-1" })
    }

    @Test
    fun `fetchSourceDetail retries after unauthorized`() = runTest {
        val api = FakeSourcesApi().apply { failUnauthorizedOnce = true }
        var refreshCount = 0
        val dataSource = SourceDataSource(
            sourcesApi = api,
            accessTokenProvider = { "expired-token" },
            refreshAccessToken = {
                refreshCount++
                "fresh-token"
            },
        )

        val detail = dataSource.fetchSourceDetail("1", "detail-1")

        assertEquals("detail-1", detail.id)
        assertEquals(1, refreshCount)
        assertEquals("fresh-token", api.lastAccessToken)
        assertEquals(2, api.getCallCount)
    }

    @Test
    fun `retrySource retries after unauthorized`() = runTest {
        val api = FakeSourcesApi().apply { failUnauthorizedOnce = true }
        var refreshCount = 0
        val dataSource = SourceDataSource(
            sourcesApi = api,
            accessTokenProvider = { "expired-token" },
            refreshAccessToken = {
                refreshCount++
                "fresh-token"
            },
        )

        dataSource.retrySource("retry-1")

        assertEquals(1, refreshCount)
        assertEquals("fresh-token", api.lastAccessToken)
        assertEquals(2, api.retryCallCount)
        assertEquals("retry-1", api.lastRetrySourceId)
    }

    @Test
    fun `deleteSource retries after unauthorized`() = runTest {
        val api = FakeSourcesApi().apply { failUnauthorizedOnce = true }
        var refreshCount = 0
        val dataSource = SourceDataSource(
            sourcesApi = api,
            accessTokenProvider = { "expired-token" },
            refreshAccessToken = {
                refreshCount++
                "fresh-token"
            },
        )

        dataSource.deleteSource("delete-1")

        assertEquals(1, refreshCount)
        assertEquals("fresh-token", api.lastAccessToken)
        assertEquals(2, api.deleteCallCount)
        assertEquals("delete-1", api.lastDeleteSourceId)
    }
}
