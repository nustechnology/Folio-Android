package com.nus.folio.domain.usecase

import com.nus.folio.domain.model.AuthSession
import com.nus.folio.testing.FakeAuthRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RefreshAuthSessionUseCaseTest {

    private val repository = FakeAuthRepository()
    private val useCase = RefreshAuthSessionUseCase(repository)

    @Test
    fun `invoke forwards to repository and returns session`() = runTest {
        repository.refreshSessionResult = Result.success(
            AuthSession(
                email = "jordan@folio.app",
                displayName = "Jordan Lee",
                accessToken = "access-2",
                refreshToken = "refresh-2",
            ),
        )

        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals("jordan@folio.app", result.getOrNull()?.email)
        assertEquals("access-2", result.getOrNull()?.accessToken)
        assertEquals("refresh-2", result.getOrNull()?.refreshToken)
        assertEquals(1, repository.refreshSessionCallCount)
    }

    @Test
    fun `invoke returns failure when repository fails`() = runTest {
        repository.refreshSessionResult = Result.failure(IllegalStateException("expired"))

        val result = useCase()

        assertTrue(result.isFailure)
        assertEquals("expired", result.exceptionOrNull()?.message)
    }
}
