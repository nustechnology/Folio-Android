package com.nus.folio.domain.util

import org.junit.Assert.assertEquals
import org.junit.Test

class SpaceInputRulesTest {

    @Test
    fun `limitTitle keeps short values`() {
        assertEquals("Dissertation", SpaceInputRules.limitTitle("Dissertation"))
    }

    @Test
    fun `limitTitle clamps to max length`() {
        val input = "a".repeat(SpaceInputRules.MAX_TITLE_LENGTH + 25)
        val limited = SpaceInputRules.limitTitle(input)
        assertEquals(SpaceInputRules.MAX_TITLE_LENGTH, limited.length)
        assertEquals("a".repeat(SpaceInputRules.MAX_TITLE_LENGTH), limited)
    }

    @Test
    fun `limitObjective clamps to max length`() {
        val input = "b".repeat(SpaceInputRules.MAX_OBJECTIVE_LENGTH + 10)
        val limited = SpaceInputRules.limitObjective(input)
        assertEquals(SpaceInputRules.MAX_OBJECTIVE_LENGTH, limited.length)
        assertEquals("b".repeat(SpaceInputRules.MAX_OBJECTIVE_LENGTH), limited)
    }
}
