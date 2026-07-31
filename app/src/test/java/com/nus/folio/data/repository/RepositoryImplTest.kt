package com.nus.folio.data.repository

import com.nus.folio.data.datasource.AskDataSource
import com.nus.folio.data.datasource.GreetingDataSource
import com.nus.folio.data.datasource.SourceDataSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GreetingRepositoryImplTest {

    private val repository = GreetingRepositoryImpl(GreetingDataSource())

    @Test
    fun `getGreeting returns success with greeting message`() = runTest {
        val result = repository.getGreeting()

        assertTrue(result.isSuccess)
        assertEquals("Hello Folio!", result.getOrNull()?.message)
    }
}

class SourceRepositoryImplTest {

    private val repository = SourceRepositoryImpl(SourceDataSource())

    @Test
    fun `getSources returns success library for space`() = runTest {
        val result = repository.getSources("1")

        assertTrue(result.isSuccess)
        assertEquals(4, result.getOrNull()?.allCount)
        assertEquals(4, result.getOrNull()?.sources?.size)
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
        assertEquals(3, repository.getSources("1").getOrNull()?.allCount)
        assertTrue(repository.getSources("1").getOrNull()!!.sources.none { it.id == "1" })
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
