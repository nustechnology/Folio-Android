package com.nus.folio.domain.usecase

import com.nus.folio.domain.model.Space
import com.nus.folio.testing.FakeSpaceRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateSpaceUseCaseTest {

    private val repository = FakeSpaceRepository()
    private val useCase = CreateSpaceUseCase(repository)

    @Test
    fun `invoke forwards fields and returns space`() = runTest {
        repository.createSpaceResult = Result.success(
            Space(
                id = "b2c3d4e5-f6a7-8901-bcde-f12345678901",
                title = "AI Ethics Research",
                description = "Explore ethical frameworks for AI decision-making.",
                sourceCount = 0,
                noteCount = 0,
                updatedLabel = "Updated just now",
            ),
        )

        val result = useCase(
            name = "AI Ethics Research",
            researchObjective = "Explore ethical frameworks for AI decision-making.",
        )

        assertTrue(result.isSuccess)
        assertEquals("AI Ethics Research", repository.lastCreateName)
        assertEquals(
            "Explore ethical frameworks for AI decision-making.",
            repository.lastCreateObjective,
        )
        assertEquals("b2c3d4e5-f6a7-8901-bcde-f12345678901", result.getOrNull()?.id)
    }
}
