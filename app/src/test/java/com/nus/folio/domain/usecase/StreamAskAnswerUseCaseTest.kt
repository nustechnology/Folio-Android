package com.nus.folio.domain.usecase

import com.nus.folio.domain.model.AskCitation
import com.nus.folio.domain.model.AskStreamEvent
import com.nus.folio.domain.model.SourceType
import com.nus.folio.testing.FakeAskRepository
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamAskAnswerUseCaseTest {

    private val repository = FakeAskRepository()
    private val useCase = StreamAskAnswerUseCase(repository)

    @Test
    fun `invoke forwards space question sourceId and conversationId to repository`() = runTest {
        val events = useCase(
            spaceId = "space-1",
            question = "What is the claim?",
            sourceId = "src-9",
            conversationId = "conv-2",
        ).toList()

        assertEquals(1, repository.streamAnswerCallCount)
        assertEquals("space-1", repository.lastSpaceId)
        assertEquals("What is the claim?", repository.lastStreamQuestion)
        assertEquals("src-9", repository.lastStreamSourceId)
        assertEquals("conv-2", repository.lastStreamConversationId)
        assertTrue(events.isNotEmpty())
        assertTrue(events.last() is AskStreamEvent.Completed)
    }

    @Test
    fun `invoke emits configured stream events`() = runTest {
        repository.streamEvents = listOf(
            AskStreamEvent.Delta("Hello "),
            AskStreamEvent.Delta("world"),
            AskStreamEvent.Completed(
                citations = listOf(
                    AskCitation(
                        index = 1,
                        sourceId = "1",
                        sourceTitle = "Paper",
                        sourceType = SourceType.FILE,
                        fileExtension = "pdf",
                        locationLabel = "Page 2",
                        evidenceText = "Hello world",
                    ),
                ),
                limitation = "Limited corpus",
            ),
        )

        val events = useCase(
            spaceId = "space-1",
            question = "Q",
            sourceId = null,
        ).toList()

        assertEquals(3, events.size)
        assertEquals("Hello ", (events[0] as AskStreamEvent.Delta).text)
        assertEquals("world", (events[1] as AskStreamEvent.Delta).text)
        val completed = events[2] as AskStreamEvent.Completed
        assertEquals(1, completed.citations.size)
        assertEquals("Limited corpus", completed.limitation)
        assertEquals(null, repository.lastStreamSourceId)
        assertEquals(null, repository.lastStreamConversationId)
    }
}
