package com.nus.folio.domain.usecase

import com.nus.folio.domain.model.SourceProcessingEvent
import com.nus.folio.domain.model.SourceProcessingState
import com.nus.folio.testing.FakeSourceRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveSourceProcessingUseCaseTest {

    private val repository = FakeSourceRepository()
    private val useCase = ObserveSourceProcessingUseCase(repository)

    @Test
    fun `invoke returns repository processing event stream`() = runTest {
        val collected = mutableListOf<SourceProcessingEvent>()
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            useCase().collect { collected += it }
        }

        assertEquals(1, repository.observeSourceProcessingCallCount)

        val first = SourceProcessingEvent("src-1", SourceProcessingState.EXTRACTING_TEXT, 25)
        val second = SourceProcessingEvent("src-1", SourceProcessingState.READY, 100)
        repository.emitProcessingEvent(first)
        repository.emitProcessingEvent(second)

        assertEquals(listOf(first, second), collected)
        collectJob.cancel()
    }
}
