package com.nus.folio.domain.usecase

import com.nus.folio.testing.FakeAuthRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RequestPasswordResetUseCaseTest {

    private val repository = FakeAuthRepository()
    private val useCase = RequestPasswordResetUseCase(repository)

    @Test
    fun `invoke forwards email and returns success`() = runTest {
        repository.requestPasswordResetResult = Result.success(Unit)

        val result = useCase("user@folio.app")

        assertTrue(result.isSuccess)
        assertEquals("user@folio.app", repository.lastPasswordResetEmail)
        assertEquals(1, repository.requestPasswordResetCallCount)
    }

    @Test
    fun `invoke returns failure when repository fails`() = runTest {
        repository.requestPasswordResetResult = Result.failure(IllegalStateException("send failed"))

        val result = useCase("user@folio.app")

        assertTrue(result.isFailure)
        assertEquals("send failed", result.exceptionOrNull()?.message)
    }
}
