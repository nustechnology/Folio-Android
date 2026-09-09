package com.nus.folio.data.datasource

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AskDataSourceTest {

    private val dataSource = AskDataSource()

    @Test
    fun `fetchSuggestedQuestions returns context-aware questions`() = runTest {
        val suggestions = dataSource.fetchSuggestedQuestions(spaceId = "1", sourceId = "1")

        assertEquals(3, suggestions.questions.size)
        assertTrue(suggestions.isDynamic)
        assertTrue(suggestions.questions.first().contains("Turing"))
    }

    @Test
    fun `fetchSuggestedQuestions trims sourceId before lookup`() = runTest {
        val suggestions = dataSource.fetchSuggestedQuestions(spaceId = "1", sourceId = " 1 ")

        assertEquals(3, suggestions.questions.size)
        assertTrue(suggestions.isDynamic)
        assertTrue(suggestions.questions.first().contains("Turing"))
    }

    @Test
    fun `fetchSuggestedQuestions returns empty when metadata missing`() = runTest {
        val suggestions = dataSource.fetchSuggestedQuestions(spaceId = "1", sourceId = "10")

        assertTrue(suggestions.questions.isEmpty())
        assertEquals(false, suggestions.isDynamic)
    }

    @Test
    fun `fetchSuggestedQuestions for space scope is not dynamic`() = runTest {
        val suggestions = dataSource.fetchSuggestedQuestions(spaceId = "1", sourceId = null)

        assertTrue(suggestions.questions.isEmpty())
        assertEquals(false, suggestions.isDynamic)
    }
}
