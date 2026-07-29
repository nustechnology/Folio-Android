package com.nus.folio.data.datasource

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthDataSourceTest {

    private val dataSource = AuthDataSource()

    @Test
    fun `signUp returns trimmed email session`() = runTest {
        val session = dataSource.signUp("Alex", "  alex@folio.app  ", "secret")

        assertEquals("alex@folio.app", session.email)
        assertEquals("Alex", session.displayName)
    }

    @Test
    fun `signIn accepts credentials registered via signUp`() = runTest {
        dataSource.signUp("Alex", "alex@folio.app", "secret")

        val session = dataSource.signIn("  Alex@folio.app  ", "secret")

        assertEquals("Alex@folio.app", session.email)
        assertEquals("Alex", session.displayName)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `signUp throws when account already exists`() = runTest {
        dataSource.signUp("Alex", "alex@folio.app", "secret")
        dataSource.signUp("Alex", "ALEX@folio.app", "other")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `signUp throws when name is blank`() = runTest {
        dataSource.signUp("  ", "alex@folio.app", "secret")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `signUp throws when email is blank`() = runTest {
        dataSource.signUp("Alex", "", "secret")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `signUp throws when password is blank`() = runTest {
        dataSource.signUp("Alex", "alex@folio.app", " ")
    }

    @Test
    fun `signIn returns trimmed email session for demo credentials`() = runTest {
        val session = dataSource.signIn("  researcher@folio.app  ", "folio-debug")

        assertEquals("researcher@folio.app", session.email)
        assertEquals("Folio Researcher", session.displayName)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `signIn throws when email is blank`() = runTest {
        dataSource.signIn("", "folio-debug")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `signIn throws when password is blank`() = runTest {
        dataSource.signIn("researcher@folio.app", "")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `signIn throws for invalid credentials`() = runTest {
        dataSource.signIn("user@folio.app", "secret")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `signIn throws for wrong password on registered account`() = runTest {
        dataSource.signUp("Alex", "alex@folio.app", "secret")
        dataSource.signIn("alex@folio.app", "wrong")
    }

    @Test
    fun `signInWithApple returns fixed apple session`() = runTest {
        val session = dataSource.signInWithApple()

        assertEquals("apple.user@folio.app", session.email)
        assertEquals("Apple User", session.displayName)
    }

    @Test
    fun `requestPasswordReset succeeds for valid email`() = runTest {
        dataSource.requestPasswordReset("user@folio.app")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `requestPasswordReset throws when email is blank`() = runTest {
        dataSource.requestPasswordReset(" ")
    }
}
