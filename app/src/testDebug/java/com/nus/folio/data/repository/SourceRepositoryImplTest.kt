package com.nus.folio.data.repository

import com.nus.folio.data.datasource.SourceDataSource
import com.nus.folio.data.datasource.SourceSampleData
import com.nus.folio.data.network.SourcesApi
import com.nus.folio.domain.model.Source
import com.nus.folio.domain.model.SourceContentFormat
import com.nus.folio.domain.model.SourceDetail
import com.nus.folio.domain.model.SourceLibrary
import com.nus.folio.domain.model.SourceProcessingEvent
import com.nus.folio.domain.model.SourceStatus
import com.nus.folio.domain.model.SourceType
import com.nus.folio.domain.repository.SourceOriginalFileResolver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceRepositoryImplTest {

    private class FakeSourcesApi : SourcesApi {
        var listedSources: List<Source> = listOf(
            Source(
                id = "1",
                title = "API Source",
                type = SourceType.FILE,
                author = "Author",
                addedLabel = "Added just now",
                status = SourceStatus.READY,
                spaceId = "1",
            ),
        )
        var detailResult: SourceDetail = SourceDetail(
            id = "10",
            title = "Research metrics dashboard",
            author = "Author",
            addedLabel = "Added just now",
            type = SourceType.FILE,
            status = SourceStatus.READY,
            spaceId = "1",
            fileExtension = "xlsx",
            contentFormat = SourceContentFormat.DOCUMENT,
            originalFileName = "metrics.xlsx",
        )
        var deleteCallCount = 0
        var retryCallCount = 0
        var lastDeletedId: String? = null
        var lastRetriedId: String? = null
        var throwOnGet = false

        override suspend fun listSources(
            accessToken: String,
            spaceId: String,
            sourceType: String?,
            search: String?,
            sort: String,
            page: Int,
            limit: Int,
        ): SourceLibrary = SourceSampleData.libraryFrom(
            sources = listedSources.filter { it.spaceId == spaceId },
            page = page,
            limit = limit,
            hasMore = false,
        )

        override suspend fun getSource(accessToken: String, sourceId: String): SourceDetail {
            if (throwOnGet || sourceId == "missing") {
                throw NoSuchElementException("Source not found")
            }
            return detailResult.copy(id = sourceId)
        }

        override suspend fun createWebSource(
            accessToken: String,
            spaceId: String,
            sourceUrl: String,
            title: String,
            author: String,
        ): Source = error("unused")

        override suspend fun createManualSource(
            accessToken: String,
            spaceId: String,
            title: String,
            author: String,
            content: String,
        ): Source = error("unused")

        override suspend fun createFileSource(
            accessToken: String,
            spaceId: String,
            title: String,
            author: String,
            fileName: String,
            mimeType: String,
            fileBytes: ByteArray,
        ): Source = error("unused")

        override suspend fun retrySource(accessToken: String, sourceId: String): SourceDetail? {
            retryCallCount++
            lastRetriedId = sourceId
            return detailResult.copy(id = sourceId, status = SourceStatus.PROCESSING)
        }

        override suspend fun deleteSource(accessToken: String, sourceId: String) {
            deleteCallCount++
            lastDeletedId = sourceId
            listedSources = listedSources.filterNot { it.id == sourceId }
        }

        override suspend fun updateSource(
            accessToken: String,
            sourceId: String,
            title: String,
            author: String,
            content: String?,
        ): Source {
            val existing = listedSources.first { it.id == sourceId }
            val updated = existing.copy(title = title, author = author)
            listedSources = listedSources.map { if (it.id == sourceId) updated else it }
            return updated
        }

        override suspend fun getSourcePreview(accessToken: String, sourceId: String): String? =
            "https://example.org/preview/$sourceId.pdf"

        override fun observeSourceStatus(accessToken: String): Flow<SourceProcessingEvent> =
            emptyFlow()
    }

    private fun createRepository(api: FakeSourcesApi = FakeSourcesApi()) =
        SourceRepositoryImpl(
            dataSource = SourceDataSource(
                sourcesApi = api,
                accessTokenProvider = { "token" },
            ),
            originalFileResolver = object : SourceOriginalFileResolver {
                override suspend fun resolveOriginalFile(spaceId: String, sourceId: String) =
                    com.nus.folio.domain.model.SourceFileLocation.Local(
                        absolutePath = "/tmp/$sourceId.pdf",
                        fileName = "source.pdf",
                        mimeType = "application/pdf",
                    )
            },
        ) to api

    @Test
    fun `getSources returns success library for space`() = runTest {
        val (repository, _) = createRepository()

        val result = repository.getSources("1")

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.allCount)
        assertEquals("API Source", result.getOrNull()?.sources?.first()?.title)
    }

    @Test
    fun `updateSource returns success`() = runTest {
        val (repository, _) = createRepository()
        val original = repository.getSources("1").getOrNull()!!.sources.first()
        val updated = original.copy(title = "Updated title", author = "Updated author")

        val result = repository.updateSource(updated)

        assertTrue(result.isSuccess)
        assertEquals(updated, result.getOrNull())
    }

    @Test
    fun `deleteSource returns success and calls API`() = runTest {
        val (repository, api) = createRepository()

        val result = repository.deleteSource("1")

        assertTrue(result.isSuccess)
        assertEquals(1, api.deleteCallCount)
        assertEquals("1", api.lastDeletedId)
        assertTrue(repository.getSources("1").getOrNull()!!.sources.none { it.id == "1" })
    }

    @Test
    fun `retrySource returns success and calls API`() = runTest {
        val (repository, api) = createRepository()

        val result = repository.retrySource("1")

        assertTrue(result.isSuccess)
        assertEquals(1, api.retryCallCount)
        assertEquals("1", api.lastRetriedId)
    }

    @Test
    fun `getSourceDetail returns success for known source`() = runTest {
        val (repository, _) = createRepository()

        val result = repository.getSourceDetail("1", "10")

        assertTrue(result.isSuccess)
        assertEquals("Research metrics dashboard", result.getOrNull()?.title)
        assertEquals("xlsx", result.getOrNull()?.fileExtension)
    }

    @Test
    fun `getSourceDetail returns failure for unknown source`() = runTest {
        val (repository, _) = createRepository()

        val result = repository.getSourceDetail("1", "missing")

        assertTrue(result.isFailure)
    }

    @Test
    fun `getOriginalFile delegates to resolver`() = runTest {
        val (repository, _) = createRepository()

        val result = repository.getOriginalFile("1", "1")

        assertTrue(result.isSuccess)
        val local = result.getOrNull() as com.nus.folio.domain.model.SourceFileLocation.Local
        assertEquals("/tmp/1.pdf", local.absolutePath)
        assertEquals("application/pdf", local.mimeType)
    }

    @Test
    fun `getSourcePreviewUrl returns preview url from API`() = runTest {
        val (repository, _) = createRepository()

        val result = repository.getSourcePreviewUrl("1")

        assertTrue(result.isSuccess)
        assertEquals("https://example.org/preview/1.pdf", result.getOrNull())
    }
}
