package com.nus.folio.domain.usecase

import com.nus.folio.domain.model.CreateNoteRequest
import com.nus.folio.domain.model.NoteOrigin
import com.nus.folio.testing.FakeNoteRepository
import com.nus.folio.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CreateNoteUseCaseTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeNoteRepository()
    private val useCase = CreateNoteUseCase(repository)

    @Test
    fun `invoke creates note via repository`() = runTest {
        val request = CreateNoteRequest(
            spaceId = "1",
            title = "Saved answer",
            content = "Body with citations",
            origin = NoteOrigin.SAVED_ANSWER,
            citationCount = 2,
            project = "Dissertation Research",
        )

        val result = useCase(request)

        assertTrue(result.isSuccess)
        assertEquals(1, repository.createNoteCallCount)
        assertEquals(request, repository.lastCreatedRequest)
        assertEquals(NoteOrigin.SAVED_ANSWER, result.getOrThrow().origin)
        assertEquals(2, result.getOrThrow().citationCount)
    }

    @Test
    fun `invoke forwards repository failure`() = runTest {
        repository.createNoteResult = Result.failure(IllegalStateException("offline"))

        val result = useCase(
            CreateNoteRequest(
                spaceId = "1",
                title = "Title",
                content = "Content",
            ),
        )

        assertTrue(result.isFailure)
    }
}
