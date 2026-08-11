package com.nus.folio.domain.usecase

import com.nus.folio.domain.util.NotebookInputRules
import com.nus.folio.testing.FakeNotebookRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveNotebookUseCaseTest {

    private val repository = FakeNotebookRepository()
    private val useCase = SaveNotebookUseCase(repository)

    @Test
    fun `invoke clamps content then saves`() = runTest {
        val oversized = "a".repeat(NotebookInputRules.MAX_CONTENT_LENGTH + 25)

        val result = useCase("space-1", oversized)

        assertTrue(result.isSuccess)
        assertEquals("space-1", repository.lastSavedSpaceId)
        assertEquals(NotebookInputRules.MAX_CONTENT_LENGTH, repository.lastSavedContent?.length)
        assertEquals(NotebookInputRules.MAX_CONTENT_LENGTH, result.getOrThrow().content.length)
        assertEquals(1, repository.saveNotebookCallCount)
    }

    @Test
    fun `invoke preserves short content`() = runTest {
        val result = useCase("space-2", "## Research\n")

        assertTrue(result.isSuccess)
        assertEquals("## Research\n", result.getOrThrow().content)
        assertEquals("## Research\n", repository.lastSavedContent)
    }

    @Test
    fun `invoke returns failure when repository fails`() = runTest {
        repository.saveError = IllegalStateException("write failed")

        val result = useCase("space-1", "draft")

        assertTrue(result.isFailure)
        assertEquals("write failed", result.exceptionOrNull()?.message)
    }
}
