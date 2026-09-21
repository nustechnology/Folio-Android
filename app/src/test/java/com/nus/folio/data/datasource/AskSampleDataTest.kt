package com.nus.folio.data.datasource

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AskSampleDataTest {

    @Test
    fun `suggestionsFor trims sourceId before lookup`() {
        val questions = AskSampleData.suggestionsFor(" 1 ")
        assertEquals(3, questions.size)
        assertTrue(questions.first().contains("Turing"))
    }

    @Test
    fun `mockAnswer trims sourceId before lookup`() {
        val answer = AskSampleData.mockAnswer(
            spaceId = "1",
            question = "What is the claim?",
            sourceId = " 1 ",
        )
        assertTrue(answer.body.contains("Turing"))
        assertEquals("1", answer.citations.first().sourceId)
    }
}
