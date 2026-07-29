package com.nus.folio.domain.usecase

import com.nus.folio.domain.model.Greeting
import com.nus.folio.testing.FakeGreetingRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetGreetingUseCaseTest {

    private val repository = FakeGreetingRepository()
    private val useCase = GetGreetingUseCase(repository)

    @Test
    fun `invoke returns greeting on success`() = runTest {
        repository.getGreetingResult = Result.success(Greeting("Hello Folio!"))

        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals("Hello Folio!", result.getOrNull()?.message)
        assertEquals(1, repository.getGreetingCallCount)
    }

    @Test
    fun `invoke returns failure when repository fails`() = runTest {
        repository.getGreetingResult = Result.failure(IllegalStateException("network"))

        val result = useCase()

        assertTrue(result.isFailure)
        assertEquals("network", result.exceptionOrNull()?.message)
    }
}
