package com.nus.folio.domain.usecase

import com.nus.folio.domain.model.AuthSession
import com.nus.folio.testing.FakeAuthRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncCurrentUserUseCaseTest {

    private val repository = FakeAuthRepository()
    private val useCase = SyncCurrentUserUseCase(repository)

    @Test
    fun `invoke forwards to repository and returns session`() = runTest {
        repository.signInResult = Result.success(
            AuthSession(
                email = "alice@example.com",
                displayName = "alice",
                userId = "d9c069fd-6c17-468b-82bd-1528512c8899",
                accessToken = "access",
            ),
        )
        repository.signIn("alice@example.com", "secret")
        repository.syncCurrentUserResult = Result.success(
            AuthSession(
                email = "alice@example.com",
                displayName = "alice",
                userId = "d9c069fd-6c17-468b-82bd-1528512c8899",
                accessToken = "access",
            ),
        )

        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals(1, repository.syncCurrentUserCallCount)
        assertEquals("me", repository.lastSyncUserId)
        assertEquals("alice", result.getOrNull()?.displayName)
    }
}
