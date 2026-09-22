package com.nus.folio.data.datasource

import com.nus.folio.data.network.AuthApi
import com.nus.folio.domain.model.AuthSession
import com.nus.folio.domain.model.UserProfile
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class AuthDataSourceTest {

    private class FakeAuthApi(
        var signUpResult: Result<AuthSession> = Result.success(
            AuthSession(email = "jordan@folio.app", displayName = "Jordan Lee"),
        ),
        var loginResult: Result<AuthSession> = Result.success(
            AuthSession(email = "jordan@folio.app", displayName = "Jordan Lee"),
        ),
        var refreshResult: Result<AuthSession> = Result.success(
            AuthSession(
                email = "jordan@folio.app",
                displayName = "Jordan Lee",
                accessToken = "access-2",
                refreshToken = "refresh-2",
            ),
        ),
        var logoutResult: Result<Unit> = Result.success(Unit),
        var getUserResult: Result<UserProfile> = Result.success(
            UserProfile(
                id = "d9c069fd-6c17-468b-82bd-1528512c8899",
                name = "alice",
                email = "alice@example.com",
            ),
        ),
    ) : AuthApi {
        var lastRefreshToken: String? = null
        var lastLogoutAccessToken: String? = null
        var lastGetUserId: String? = null
        var lastGetUserAccessToken: String? = null

        override suspend fun signUp(
            name: String,
            email: String,
            password: String,
            confirmPassword: String,
        ): AuthSession = signUpResult.getOrThrow().let { session ->
            AuthSession(
                email = email.trim().ifBlank { session.email },
                displayName = name.trim().ifBlank { session.displayName },
                userId = session.userId,
                accessToken = session.accessToken,
                refreshToken = session.refreshToken,
            )
        }

        override suspend fun login(
            email: String,
            password: String,
        ): AuthSession = loginResult.getOrThrow().let { session ->
            AuthSession(
                email = email.trim().ifBlank { session.email },
                displayName = session.displayName,
                userId = session.userId,
                accessToken = session.accessToken,
                refreshToken = session.refreshToken,
            )
        }

        override suspend fun refresh(refreshToken: String): AuthSession {
            lastRefreshToken = refreshToken
            return refreshResult.getOrThrow()
        }

        override suspend fun logout(accessToken: String?) {
            lastLogoutAccessToken = accessToken
            logoutResult.getOrThrow()
        }

        override suspend fun getUser(id: String, accessToken: String): UserProfile {
            lastGetUserId = id
            lastGetUserAccessToken = accessToken
            return getUserResult.getOrThrow()
        }
    }

    private fun dataSource(
        api: FakeAuthApi = FakeAuthApi(),
    ): AuthDataSource = AuthDataSource(authApi = api)

    @Test
    fun `signUp returns session from api`() = runTest {
        val session = dataSource().signUp("Jordan Lee", "  jordan@folio.app  ", "secret", "secret")

        assertEquals("jordan@folio.app", session.email)
        assertEquals("Jordan Lee", session.displayName)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `signUp throws when name is blank`() = runTest {
        dataSource().signUp("  ", "jordan@folio.app", "secret", "secret")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `signUp throws when email is blank`() = runTest {
        dataSource().signUp("Jordan Lee", "", "secret", "secret")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `signUp throws when password is blank`() = runTest {
        dataSource().signUp("Jordan Lee", "jordan@folio.app", " ", "secret")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `signUp throws when confirm password is blank`() = runTest {
        dataSource().signUp("Jordan Lee", "jordan@folio.app", "secret", " ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `signUp throws when passwords do not match`() = runTest {
        dataSource().signUp("Jordan Lee", "jordan@folio.app", "secret", "other")
    }

    @Test(expected = IOException::class)
    fun `signUp propagates api failure`() = runTest {
        dataSource(FakeAuthApi(signUpResult = Result.failure(IOException("exists")))).signUp(
            "Jordan Lee",
            "jordan@folio.app",
            "secret",
            "secret",
        )
    }

    @Test
    fun `signIn returns session from api`() = runTest {
        val api = FakeAuthApi(
            loginResult = Result.success(
                AuthSession(
                    email = "jordan@folio.app",
                    displayName = "Jordan Lee",
                    accessToken = "access",
                    refreshToken = "refresh",
                ),
            ),
        )

        val session = dataSource(api).signIn("  Jordan@folio.app  ", "secret")

        assertEquals("Jordan@folio.app", session.email)
        assertEquals("Jordan Lee", session.displayName)
        assertEquals("access", session.accessToken)
        assertEquals("refresh", session.refreshToken)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `signIn throws when email is blank`() = runTest {
        dataSource().signIn("", "folio-debug")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `signIn throws when password is blank`() = runTest {
        dataSource().signIn("jordan@folio.app", "")
    }

    @Test(expected = IOException::class)
    fun `signIn propagates api failure`() = runTest {
        dataSource(FakeAuthApi(loginResult = Result.failure(IOException("invalid")))).signIn(
            "jordan@folio.app",
            "wrong",
        )
    }

    @Test
    fun `refresh returns session from api`() = runTest {
        val api = FakeAuthApi()

        val session = dataSource(api).refresh("  refresh-1  ")

        assertEquals("refresh-1", api.lastRefreshToken)
        assertEquals("access-2", session.accessToken)
        assertEquals("refresh-2", session.refreshToken)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `refresh throws when token is blank`() = runTest {
        dataSource().refresh(" ")
    }

    @Test(expected = IOException::class)
    fun `refresh propagates api failure`() = runTest {
        dataSource(FakeAuthApi(refreshResult = Result.failure(IOException("expired")))).refresh(
            "refresh-1",
        )
    }

    @Test
    fun `logout forwards access token to api`() = runTest {
        val api = FakeAuthApi()

        dataSource(api).logout("access-token")

        assertEquals("access-token", api.lastLogoutAccessToken)
    }

    @Test(expected = IOException::class)
    fun `logout propagates api failure`() = runTest {
        dataSource(FakeAuthApi(logoutResult = Result.failure(IOException("failed")))).logout(
            "access-token",
        )
    }

    @Test
    fun `getUser returns profile from api`() = runTest {
        val api = FakeAuthApi()

        val profile = dataSource(api).getUser("me", "access-token")

        assertEquals("me", api.lastGetUserId)
        assertEquals("access-token", api.lastGetUserAccessToken)
        assertEquals("d9c069fd-6c17-468b-82bd-1528512c8899", profile.id)
        assertEquals("alice", profile.name)
        assertEquals("alice@example.com", profile.email)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `getUser throws when id is blank`() = runTest {
        dataSource().getUser(" ", "access-token")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `getUser throws when access token is blank`() = runTest {
        dataSource().getUser("me", " ")
    }

    @Test(expected = IOException::class)
    fun `getUser propagates api failure`() = runTest {
        dataSource(FakeAuthApi(getUserResult = Result.failure(IOException("unauthorized")))).getUser(
            "me",
            "access-token",
        )
    }
}
