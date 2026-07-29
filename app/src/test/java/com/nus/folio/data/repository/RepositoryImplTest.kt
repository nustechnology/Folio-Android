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
    fun `getSources returns success library`() = runTest {
        val result = repository.getSources()

        assertTrue(result.isSuccess)
        assertEquals(128, result.getOrNull()?.allCount)
        assertEquals(6, result.getOrNull()?.sources?.size)
        assertEquals(6, result.getOrNull()?.textCount)
    }
}

class AskRepositoryImplTest {

    private val repository = AskRepositoryImpl(AskDataSource())

    @Test
    fun `getAskTopics returns success list`() = runTest {
        val result = repository.getAskTopics()

        assertTrue(result.isSuccess)
        assertEquals(4, result.getOrNull()?.size)
        assertEquals("Dissertation Research", result.getOrNull()?.first()?.title)
    }
}
