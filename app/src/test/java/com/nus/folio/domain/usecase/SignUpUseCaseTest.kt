package com.nus.folio.domain.usecase

import com.nus.folio.domain.model.AuthSession
import com.nus.folio.testing.FakeAuthRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SignUpUseCaseTest {

    private val repository = FakeAuthRepository()
    private val useCase = SignUpUseCase(repository)

    @Test
    fun `invoke forwards fields and returns session`() = runTest {
        repository.signUpResult = Result.success(AuthSession("new@folio.app"))

        val result = useCase("Alex Morgan", "new@folio.app", "secret")

        assertTrue(result.isSuccess)
        assertEquals("new@folio.app", result.getOrNull()?.email)
        assertEquals("Alex Morgan", repository.lastSignUpName)
        assertEquals("new@folio.app", repository.lastSignUpEmail)
        assertEquals("secret", repository.lastSignUpPassword)
        assertEquals(1, repository.signUpCallCount)
    }

    @Test
    fun `invoke returns failure when repository fails`() = runTest {
        repository.signUpResult = Result.failure(IllegalStateException("exists"))

        val result = useCase("Alex", "new@folio.app", "secret")

        assertTrue(result.isFailure)
        assertEquals("exists", result.exceptionOrNull()?.message)
    }
}
