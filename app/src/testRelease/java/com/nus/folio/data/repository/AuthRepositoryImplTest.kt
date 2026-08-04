package com.nus.folio.data.repository

import com.nus.folio.data.auth.InMemoryAuthSessionStore
import com.nus.folio.data.datasource.AuthDataSource
import com.nus.folio.domain.model.AuthSession
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRepositoryImplTest {

    private val sessionStore = InMemoryAuthSessionStore()

    private val repository = AuthRepositoryImpl(
        dataSource = AuthDataSource(),
        sessionStore = sessionStore,
    )

    @Test
    fun `signUp returns failure when backend unavailable`() = runTest {
        val result = repository.signUp("Jordan Lee", "jordan@folio.app", "secret", "secret")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is UnsupportedOperationException)
    }

    @Test
    fun `signIn returns failure when backend unavailable`() = runTest {
        val result = repository.signIn("jordan@folio.app", "folio-debug")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is UnsupportedOperationException)
    }

    @Test
    fun `refreshSession returns failure when backend unavailable`() = runTest {
        sessionStore.write(
            AuthSession(
                email = "jordan@folio.app",
                displayName = "Jordan Lee",
                accessToken = "access",
                refreshToken = "refresh",
            ),
        )
        val repositoryWithSession = AuthRepositoryImpl(
            dataSource = AuthDataSource(),
            sessionStore = sessionStore,
        )
        repositoryWithSession.restoreSession()

        val result = repositoryWithSession.refreshSession()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is UnsupportedOperationException)
    }

    @Test
    fun `syncCurrentUser returns failure when no session`() = runTest {
        val result = repository.syncCurrentUser()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun `signOut returns failure when backend unavailable`() = runTest {
        val result = repository.signOut()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is UnsupportedOperationException)
    }

    @Test
    fun `signInWithApple returns failure when backend unavailable`() = runTest {
        val result = repository.signInWithApple()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is UnsupportedOperationException)
    }

    @Test
    fun `requestPasswordReset returns failure when backend unavailable`() = runTest {
        val result = repository.requestPasswordReset("user@folio.app")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is UnsupportedOperationException)
    }
}
