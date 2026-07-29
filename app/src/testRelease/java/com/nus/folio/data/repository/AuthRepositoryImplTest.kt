package com.nus.folio.data.repository

import com.nus.folio.data.datasource.AuthDataSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRepositoryImplTest {

    private val repository = AuthRepositoryImpl(AuthDataSource())

    @Test
    fun `signUp returns failure when backend unavailable`() = runTest {
        val result = repository.signUp("Alex", "alex@folio.app", "secret")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is UnsupportedOperationException)
    }

    @Test
    fun `signIn returns failure when backend unavailable`() = runTest {
        val result = repository.signIn("researcher@folio.app", "folio-debug")

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
