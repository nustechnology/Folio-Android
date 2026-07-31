package com.nus.folio.domain.usecase

import com.nus.folio.domain.model.SourceStatus
import com.nus.folio.domain.model.SourceType
import com.nus.folio.testing.FakeSourceRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateSourceUseCaseTest {

    private val repository = FakeSourceRepository()
    private val useCase = UpdateSourceUseCase(repository)

    @Test
    fun `invoke updates source on success`() = runTest {
        val original = FakeSourceRepository.sampleSources.first { it.id == "1" }
        val updated = original.copy(title = "Updated title", author = "Updated author")

        val result = useCase(updated)

        assertTrue(result.isSuccess)
        assertEquals(updated, result.getOrNull())
        assertEquals(1, repository.updateSourceCallCount)
        assertEquals(updated, repository.lastUpdatedSource)
    }

    @Test
    fun `invoke returns failure when repository fails`() = runTest {
        repository.updateSourceResult = Result.failure(IllegalStateException("offline"))
        val source = FakeSourceRepository.sampleSources.first().copy(
            title = "Nope",
            type = SourceType.PDF,
            status = SourceStatus.READY,
        )

        val result = useCase(source)

        assertTrue(result.isFailure)
        assertEquals("offline", result.exceptionOrNull()?.message)
    }
}
