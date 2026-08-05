package com.nus.folio.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceProcessingEventTest {

    @Test
    fun `parseState maps API pipeline values`() {
        assertEquals(SourceProcessingState.ADDED, SourceProcessingEvent.parseState("added"))
        assertEquals(
            SourceProcessingState.EXTRACTING_TEXT,
            SourceProcessingEvent.parseState("extracting_text"),
        )
        assertEquals(
            SourceProcessingState.INDEXING_EVIDENCE,
            SourceProcessingEvent.parseState("indexing_evidence"),
        )
        assertEquals(SourceProcessingState.READY, SourceProcessingEvent.parseState("ready"))
        assertEquals(SourceProcessingState.FAILED, SourceProcessingEvent.parseState("failed"))
    }

    @Test
    fun `completedStepCount matches processing sheet steps`() {
        assertEquals(0, event(SourceProcessingState.ADDED).completedStepCount())
        assertEquals(1, event(SourceProcessingState.EXTRACTING_TEXT).completedStepCount())
        assertEquals(2, event(SourceProcessingState.INDEXING_EVIDENCE).completedStepCount())
        assertEquals(4, event(SourceProcessingState.READY).completedStepCount())
        assertEquals(4, event(SourceProcessingState.FAILED).completedStepCount())
    }

    @Test
    fun `isTerminal only for ready and failed`() {
        assertFalse(event(SourceProcessingState.ADDED).isTerminal)
        assertFalse(event(SourceProcessingState.EXTRACTING_TEXT).isTerminal)
        assertTrue(event(SourceProcessingState.READY).isTerminal)
        assertTrue(event(SourceProcessingState.FAILED).isTerminal)
    }

    private fun event(state: SourceProcessingState) =
        SourceProcessingEvent(sourceId = "1", state = state, progress = 0)
}
