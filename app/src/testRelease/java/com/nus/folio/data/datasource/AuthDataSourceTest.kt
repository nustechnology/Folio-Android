package com.nus.folio.data.datasource

import kotlinx.coroutines.test.runTest
import org.junit.Test

class AuthDataSourceTest {

    private val dataSource = AuthDataSource()

    @Test(expected = UnsupportedOperationException::class)
    fun `signUp is unavailable in release`() = runTest {
        dataSource.signUp("Jordan Lee", "jordan@folio.app", "secret", "secret")
    }

    @Test(expected = UnsupportedOperationException::class)
    fun `signIn is unavailable in release`() = runTest {
        dataSource.signIn("jordan@folio.app", "folio-debug")
    }

    @Test(expected = UnsupportedOperationException::class)
    fun `refresh is unavailable in release`() = runTest {
        dataSource.refresh("refresh-token")
    }

    @Test(expected = UnsupportedOperationException::class)
    fun `logout is unavailable in release`() = runTest {
        dataSource.logout("access-token")
    }

    @Test(expected = UnsupportedOperationException::class)
    fun `getUser is unavailable in release`() = runTest {
        dataSource.getUser("me", "access-token")
    }

    @Test(expected = UnsupportedOperationException::class)
    fun `signInWithApple is unavailable in release`() = runTest {
        dataSource.signInWithApple()
    }

    @Test(expected = UnsupportedOperationException::class)
    fun `requestPasswordReset is unavailable in release`() = runTest {
        dataSource.requestPasswordReset("user@folio.app")
    }
}
