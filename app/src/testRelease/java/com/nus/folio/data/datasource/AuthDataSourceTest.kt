package com.nus.folio.data.datasource

import kotlinx.coroutines.test.runTest
import org.junit.Test

class AuthDataSourceTest {

    private val dataSource = AuthDataSource()

    @Test(expected = UnsupportedOperationException::class)
    fun `signUp is unavailable in release`() = runTest {
        dataSource.signUp("Alex", "alex@folio.app", "secret")
    }

    @Test(expected = UnsupportedOperationException::class)
    fun `signIn is unavailable in release`() = runTest {
        dataSource.signIn("researcher@folio.app", "folio-debug")
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
