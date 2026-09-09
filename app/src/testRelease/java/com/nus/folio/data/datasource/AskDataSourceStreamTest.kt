package com.nus.folio.data.datasource

import com.nus.folio.domain.model.AskStreamEvent
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AskDataSourceStreamTest {

    private val dataSource = AskDataSource()

    @Test
    fun `streamAnswer emits deltas then completed content`() = runTest {
        val events = dataSource.streamAnswer(
            spaceId = "1",
            question = "What is the claim?",
            sourceId = "1",
        ).toList()

        assertTrue(events.any { it is AskStreamEvent.Delta })
        val completed = events.last() as AskStreamEvent.Completed
        assertTrue(completed.content.orEmpty().isNotBlank())
        assertTrue(completed.citations.isNotEmpty())
        assertEquals(completed.content, events.filterIsInstance<AskStreamEvent.Delta>().joinToString("") { it.text })
    }

    @Test
    fun `streamAnswer trims sourceId before lookup`() = runTest {
        val events = dataSource.streamAnswer(
            spaceId = "1",
            question = "What is the claim?",
            sourceId = " 1 ",
        ).toList()

        val completed = events.last() as AskStreamEvent.Completed
        assertTrue(completed.content.orEmpty().contains("Turing"))
        assertEquals("1", completed.citations.first().sourceId)
    }
}
