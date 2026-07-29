package com.nus.folio.domain.usecase

import com.nus.folio.domain.model.AuthSession
import com.nus.folio.testing.FakeAuthRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SignInUseCaseTest {

    private val repository = FakeAuthRepository()
    private val useCase = SignInUseCase(repository)

    @Test
    fun `invoke forwards credentials and returns session`() = runTest {
        repository.signInResult = Result.success(AuthSession("user@folio.app"))

        val result = useCase("user@folio.app", "secret")

        assertTrue(result.isSuccess)
        assertEquals("user@folio.app", result.getOrNull()?.email)
        assertEquals("user@folio.app", repository.lastSignInEmail)
        assertEquals("secret", repository.lastSignInPassword)
        assertEquals(1, repository.signInCallCount)
    }

    @Test
    fun `invoke returns failure when repository fails`() = runTest {
        repository.signInResult = Result.failure(IllegalArgumentException("invalid"))

        val result = useCase("user@folio.app", "secret")

        assertTrue(result.isFailure)
        assertEquals("invalid", result.exceptionOrNull()?.message)
    }
}
