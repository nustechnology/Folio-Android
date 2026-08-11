package com.nus.folio.domain.usecase

import com.nus.folio.domain.model.AuthSession
import com.nus.folio.testing.FakeAuthRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GetCurrentSessionUseCaseTest {

    private val repository = FakeAuthRepository()
    private val useCase = GetCurrentSessionUseCase(repository)

    @Test
    fun `invoke returns null when no session`() {
        assertNull(useCase())
    }

    @Test
    fun `invoke returns session after successful sign in`() = runTest {
        repository.signInResult = Result.success(AuthSession("user@folio.app", "User"))
        repository.signIn("user@folio.app", "secret")

        assertEquals("User", useCase()?.displayName)
        assertEquals("user@folio.app", useCase()?.email)
    }
}
