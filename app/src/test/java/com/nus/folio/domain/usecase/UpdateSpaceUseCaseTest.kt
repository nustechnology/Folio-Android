package com.nus.folio.domain.usecase

import com.nus.folio.domain.model.Space
import com.nus.folio.testing.FakeSpaceRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateSpaceUseCaseTest {

    private val repository = FakeSpaceRepository()
    private val useCase = UpdateSpaceUseCase(repository)

    @Test
    fun `invoke forwards fields and returns space`() = runTest {
        repository.updateSpaceResult = Result.success(
            Space(
                id = "space-1",
                title = "AI Ethics Research",
                description = "Explore ethical frameworks for AI decision-making.",
                sourceCount = 2,
                noteCount = 1,
                updatedLabel = "Updated just now",
            ),
        )

        val result = useCase(
            spaceId = "space-1",
            name = "AI Ethics Research",
            researchObjective = "Explore ethical frameworks for AI decision-making.",
        )

        assertTrue(result.isSuccess)
        assertEquals("space-1", repository.lastUpdateSpaceId)
        assertEquals("AI Ethics Research", repository.lastUpdateName)
        assertEquals(
            "Explore ethical frameworks for AI decision-making.",
            repository.lastUpdateObjective,
        )
        assertEquals("space-1", result.getOrNull()?.id)
        assertEquals(1, repository.updateSpaceCallCount)
    }
}
