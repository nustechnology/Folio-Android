package com.nus.folio.domain.usecase

import com.nus.folio.domain.model.AuthSession
import com.nus.folio.testing.FakeAuthRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClearAuthSessionUseCaseTest {

    private val repository = FakeAuthRepository()
    private val useCase = ClearAuthSessionUseCase(repository)

    @Test
    fun `invoke clears current session via signOut`() = runTest {
        repository.signInResult = Result.success(AuthSession("user@folio.app", "User"))
        repository.signIn("user@folio.app", "secret")

        val result = useCase()

        assertTrue(result.isSuccess)
        assertNull(repository.getCurrentSession())
        assertEquals(1, repository.signOutCallCount)
    }

    @Test
    fun `invoke succeeds when already signed out`() = runTest {
        val result = useCase()

        assertTrue(result.isSuccess)
        assertNull(repository.getCurrentSession())
        assertEquals(1, repository.signOutCallCount)
    }
}
