package com.nus.folio.data.repository

import com.nus.folio.data.datasource.AskDataSource
import com.nus.folio.data.datasource.NoteDataSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AskRepositoryImplTest {

    private val repository = AskRepositoryImpl(AskDataSource())

    @Test
    fun `getAskTopics returns success list for space`() = runTest {
        val result = repository.getAskTopics("1")

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.size)
        assertEquals("Core dissertation arguments", result.getOrNull()?.first()?.title)
    }

    @Test
    fun `getSuggestedQuestions returns success list for source`() = runTest {
        val result = repository.getSuggestedQuestions("1")

        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrNull()?.size)
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
