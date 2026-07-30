package com.nus.folio.domain.usecase

import com.nus.folio.testing.FakeSpaceRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetSpacesUseCaseTest {

    private val repository = FakeSpaceRepository()
    private val useCase = GetSpacesUseCase(repository)

    @Test
    fun `invoke returns spaces on success`() = runTest {
        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals(4, result.getOrNull()?.size)
    }

    @Test
    fun `invoke returns failure when repository fails`() = runTest {
        repository.spacesResult = Result.failure(IllegalStateException("offline"))

        val result = useCase()

        assertTrue(result.isFailure)
        assertEquals("offline", result.exceptionOrNull()?.message)
    }
}
