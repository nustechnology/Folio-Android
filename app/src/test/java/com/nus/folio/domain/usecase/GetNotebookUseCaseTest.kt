package com.nus.folio.domain.usecase

import com.nus.folio.testing.FakeNotebookRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetNotebookUseCaseTest {

    private val repository = FakeNotebookRepository()
    private val useCase = GetNotebookUseCase(repository)

    @Test
    fun `invoke returns empty notebook when space has no saved content`() = runTest {
        val result = useCase("space-1")

        assertTrue(result.isSuccess)
        assertEquals("space-1", result.getOrThrow().spaceId)
        assertEquals("", result.getOrThrow().content)
        assertEquals(1, repository.getNotebookCallCount)
    }

    @Test
    fun `invoke returns seeded notebook for space`() = runTest {
        repository.seed("space-1", "# Notes")

        val result = useCase("space-1")

        assertTrue(result.isSuccess)
        assertEquals("# Notes", result.getOrThrow().content)
    }

    @Test
    fun `invoke returns failure when repository fails`() = runTest {
        repository.getError = IllegalStateException("datastore offline")

        val result = useCase("space-1")

        assertTrue(result.isFailure)
        assertEquals("datastore offline", result.exceptionOrNull()?.message)
    }
}
