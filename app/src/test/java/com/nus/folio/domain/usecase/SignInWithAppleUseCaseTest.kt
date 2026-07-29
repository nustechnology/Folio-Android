package com.nus.folio.domain.usecase

import com.nus.folio.domain.model.AuthSession
import com.nus.folio.testing.FakeAuthRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SignInWithAppleUseCaseTest {

    private val repository = FakeAuthRepository()
    private val useCase = SignInWithAppleUseCase(repository)

    @Test
    fun `invoke returns session on success`() = runTest {
        repository.signInWithAppleResult = Result.success(AuthSession("apple.user@folio.app"))

        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals("apple.user@folio.app", result.getOrNull()?.email)
        assertEquals(1, repository.signInWithAppleCallCount)
    }

    @Test
    fun `invoke returns failure when repository fails`() = runTest {
        repository.signInWithAppleResult = Result.failure(IllegalStateException("apple failed"))

        val result = useCase()

        assertTrue(result.isFailure)
        assertEquals("apple failed", result.exceptionOrNull()?.message)
    }
}
