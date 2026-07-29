package com.nus.folio.data.repository

import com.nus.folio.data.datasource.AuthDataSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRepositoryImplTest {

    private val repository = AuthRepositoryImpl(AuthDataSource())

    @Test
    fun `signUp returns success session and stores current session`() = runTest {
        val result = repository.signUp("Alex", "alex@folio.app", "secret")

        assertTrue(result.isSuccess)
        assertEquals("alex@folio.app", result.getOrNull()?.email)
        assertEquals("Alex", result.getOrNull()?.displayName)
        assertEquals("Alex", repository.getCurrentSession()?.displayName)
        assertEquals("alex@folio.app", repository.getCurrentSession()?.email)
    }

    @Test
    fun `signUp returns failure when name is blank`() = runTest {
        val result = repository.signUp(" ", "alex@folio.app", "secret")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertNull(repository.getCurrentSession())
    }

    @Test
    fun `signIn returns success session`() = runTest {
        val result = repository.signIn("researcher@folio.app", "folio-debug")

        assertTrue(result.isSuccess)
        assertEquals("researcher@folio.app", result.getOrNull()?.email)
        assertEquals("Folio Researcher", result.getOrNull()?.displayName)
        assertEquals("Folio Researcher", repository.getCurrentSession()?.displayName)
    }

    @Test
    fun `signIn returns failure when password is blank`() = runTest {
        val result = repository.signIn("researcher@folio.app", "")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `signInWithApple returns apple session`() = runTest {
        val result = repository.signInWithApple()

        assertTrue(result.isSuccess)
        assertEquals("apple.user@folio.app", result.getOrNull()?.email)
        assertEquals("Apple User", result.getOrNull()?.displayName)
        assertEquals("Apple User", repository.getCurrentSession()?.displayName)
    }

    @Test
    fun `clearSession removes current session`() = runTest {
        repository.signIn("researcher@folio.app", "folio-debug")

        repository.clearSession()

        assertNull(repository.getCurrentSession())
    }

    @Test
    fun `requestPasswordReset returns success`() = runTest {
        val result = repository.requestPasswordReset("user@folio.app")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `requestPasswordReset returns failure when email is blank`() = runTest {
        val result = repository.requestPasswordReset("")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }
}
