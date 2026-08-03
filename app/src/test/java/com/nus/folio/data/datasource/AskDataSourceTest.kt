package com.nus.folio.data.datasource

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AskDataSourceTest {

    private val dataSource = AskDataSource()

    @Test
    fun `fetchAskTopics returns space-scoped topics`() = runTest {
        val topics = dataSource.fetchAskTopics("1")

        assertEquals(2, topics.size)
        assertTrue(topics.all { it.spaceId == "1" })
        assertEquals("Core dissertation arguments", topics.first().title)
    }

    @Test
    fun `fetchAskTopics returns different content for another space`() = runTest {
        val topics = dataSource.fetchAskTopics("4")

        assertEquals(1, topics.size)
        assertEquals("Week 7 lecture prep", topics.first().title)
        assertTrue(topics.all { it.spaceId == "4" })
    }
}
