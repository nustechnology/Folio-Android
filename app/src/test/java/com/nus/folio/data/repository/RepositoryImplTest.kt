package com.nus.folio.data.repository

import com.nus.folio.data.datasource.AskDataSource
import com.nus.folio.data.datasource.NoteDataSource
import com.nus.folio.data.datasource.SourceDataSource
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
    fun `deleteSource returns success and persists`() = runTest {
        val result = repository.deleteSource("1")

        assertTrue(result.isSuccess)
        assertEquals(4, repository.getSources("1").getOrNull()?.allCount)
        assertTrue(repository.getSources("1").getOrNull()!!.sources.none { it.id == "1" })
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
}

class AskRepositoryImplTest {

    private val repository = AskRepositoryImpl(AskDataSource())

    @Test
    fun `getAskTopics returns success list for space`() = runTest {
        val result = repository.getAskTopics("1")

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.size)
        assertEquals("Core dissertation arguments", result.getOrNull()?.first()?.title)
    }
}

class NoteRepositoryImplTest {

    private val repository = NoteRepositoryImpl(NoteDataSource())

    @Test
    fun `getNotes returns success library for space`() = runTest {
        val result = repository.getNotes("1")

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.allCount)
        assertEquals(2, result.getOrNull()?.notes?.size)
        assertEquals(1, result.getOrNull()?.pinnedCount)
    }

    @Test
    fun `updateNote returns success and persists`() = runTest {
        val original = repository.getNotes("1").getOrNull()!!.notes.first { it.id == "2" }
        val updated = original.copy(title = "Updated title", content = "Updated content")

        val result = repository.updateNote(updated)

        assertTrue(result.isSuccess)
        assertEquals(updated, result.getOrNull())
        assertEquals(
            updated,
            repository.getNotes("1").getOrNull()!!.notes.first { it.id == "2" },
        )
    }

    @Test
    fun `deleteNote returns success and persists`() = runTest {
        val result = repository.deleteNote("2")

        assertTrue(result.isSuccess)
        assertEquals(1, repository.getNotes("1").getOrNull()?.allCount)
        assertTrue(repository.getNotes("1").getOrNull()!!.notes.none { it.id == "2" })
    }
}
