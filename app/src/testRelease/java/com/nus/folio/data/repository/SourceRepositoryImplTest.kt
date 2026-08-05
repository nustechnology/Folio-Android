package com.nus.folio.data.repository

import com.nus.folio.data.datasource.SourceDataSource
import com.nus.folio.domain.model.SourceStatus
import com.nus.folio.domain.repository.SourceOriginalFileResolver
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceRepositoryImplTest {

    private val repository = SourceRepositoryImpl(
        dataSource = SourceDataSource(),
        originalFileResolver = object : SourceOriginalFileResolver {
            override suspend fun resolveOriginalFile(spaceId: String, sourceId: String) =
                com.nus.folio.domain.model.SourceFileLocation.Local(
                    absolutePath = "/tmp/$sourceId.pdf",
                    fileName = "source.pdf",
                    mimeType = "application/pdf",
                )
        },
    )

    @Test
    fun `getSources returns success library for space`() = runTest {
        val result = repository.getSources("1")

        assertTrue(result.isSuccess)
        assertEquals(5, result.getOrNull()?.allCount)
        assertEquals(5, result.getOrNull()?.sources?.size)
        assertEquals(0, result.getOrNull()?.textCount)
    }

    @Test
    fun `updateSource returns success and persists`() = runTest {
        val original = repository.getSources("1").getOrNull()!!.sources.first { it.id == "1" }
        val updated = original.copy(title = "Updated title", author = "Updated author")

        val result = repository.updateSource(updated)

        assertTrue(result.isSuccess)
        assertEquals(updated, result.getOrNull())
        assertEquals(
            updated,
            repository.getSources("1").getOrNull()!!.sources.first { it.id == "1" },
        )
    }

    @Test
    fun `updateSource with content persists plain content on detail`() = runTest {
        val created = repository.createSource(
            com.nus.folio.domain.model.CreateSourceRequest.Manual(
                spaceId = "1",
                title = "Notes",
                author = "Alice",
                content = "Original manual source content for the archive.",
            ),
        ).getOrNull()!!
        val updated = created.copy(title = "Updated notes", author = "Alice Johnson")
        val content = "This is the updated manual source content text note..."

        val result = repository.updateSource(updated, content)

        assertTrue(result.isSuccess)
        val detail = repository.getSourceDetail(created.spaceId, created.id).getOrNull()!!
        assertEquals(content, detail.plainContent)
        assertEquals("Updated notes", detail.title)
        assertEquals("Alice Johnson", detail.author)

        repository.deleteSource(created.id)
    }

    @Test
    fun `updateSource without content leaves existing plain content`() = runTest {
        val created = repository.createSource(
            com.nus.folio.domain.model.CreateSourceRequest.Manual(
                spaceId = "1",
                title = "Notes",
                author = "Alice",
                content = "Original manual source content for the archive.",
            ),
        ).getOrNull()!!

        val result = repository.updateSource(
            created.copy(title = "Renamed only"),
            content = null,
        )

        assertTrue(result.isSuccess)
        val after = repository.getSourceDetail(created.spaceId, created.id).getOrNull()!!
        assertEquals("Renamed only", after.title)
        assertEquals("Original manual source content for the archive.", after.plainContent)

        repository.deleteSource(created.id)
    }

    @Test
    fun `updateSource with empty content clears stored plain content`() = runTest {
        val created = repository.createSource(
            com.nus.folio.domain.model.CreateSourceRequest.Manual(
                spaceId = "1",
                title = "Notes",
                author = "Alice",
                content = "Original manual source content for the archive.",
            ),
        ).getOrNull()!!

        val result = repository.updateSource(created, content = "")

        assertTrue(result.isSuccess)
        assertEquals(
            "",
            repository.getSourceDetail(created.spaceId, created.id).getOrNull()!!.plainContent,
        )

        repository.deleteSource(created.id)
    }

    @Test
    fun `deleteSource returns success and persists`() = runTest {
        val result = repository.deleteSource("1")

        assertTrue(result.isSuccess)
        assertEquals(4, repository.getSources("1").getOrNull()?.allCount)
        assertTrue(repository.getSources("1").getOrNull()!!.sources.none { it.id == "1" })
    }

    @Test
    fun `retrySource marks source processing`() = runTest {
        val result = repository.retrySource("4")

        assertTrue(result.isSuccess)
        assertEquals(
            SourceStatus.PROCESSING,
            repository.getSources("1").getOrNull()!!.sources.first { it.id == "4" }.status,
        )
    }

    @Test
    fun `getSourceDetail returns success for known source`() = runTest {
        val result = repository.getSourceDetail("1", "10")

        assertTrue(result.isSuccess)
        assertEquals("Research metrics dashboard", result.getOrNull()?.title)
        assertEquals("xlsx", result.getOrNull()?.fileExtension)
    }

    @Test
    fun `getSourceDetail returns failure for unknown source`() = runTest {
        val result = repository.getSourceDetail("1", "missing")

        assertTrue(result.isFailure)
    }

    @Test
    fun `getOriginalFile delegates to resolver`() = runTest {
        val result = repository.getOriginalFile("1", "1")

        assertTrue(result.isSuccess)
        val local = result.getOrNull() as com.nus.folio.domain.model.SourceFileLocation.Local
        assertEquals("/tmp/1.pdf", local.absolutePath)
        assertEquals("application/pdf", local.mimeType)
    }

    @Test
    fun `getSourcePreviewUrl returns null until backend is wired`() = runTest {
        val result = repository.getSourcePreviewUrl("1")

        assertTrue(result.isSuccess)
        assertEquals(null, result.getOrNull())
    }
}
