package com.nus.folio.data.datasource

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AskDataSourceTest {

    private val dataSource = AskDataSource()

    @Test
    fun `fetchSuggestedQuestions returns context-aware questions`() = runTest {
        val suggestions = dataSource.fetchSuggestedQuestions("1")

        assertEquals(3, suggestions.size)
        assertTrue(suggestions.first().contains("Turing"))
    }

    @Test
    fun `fetchSuggestedQuestions returns empty when metadata missing`() = runTest {
        val suggestions = dataSource.fetchSuggestedQuestions("10")

        assertTrue(suggestions.isEmpty())
    }
}
