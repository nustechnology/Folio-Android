package com.nus.folio.data.repository

import com.nus.folio.data.datasource.AskDataSource
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
