package com.nus.folio.data.repository

import com.nus.folio.data.auth.InMemoryAuthSessionStore
import com.nus.folio.data.datasource.AuthDataSource
import com.nus.folio.data.network.AuthApi
import com.nus.folio.domain.model.AuthApiException
import com.nus.folio.domain.model.AuthSession
import com.nus.folio.domain.model.UserProfile
import kotlinx.coroutines.async
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

class AuthRepositoryImplTest {

    private val sessionStore = InMemoryAuthSessionStore()

    private val fakeApi = object : AuthApi {
        var getUserResult: Result<UserProfile> = Result.success(
            UserProfile(
                id = "d9c069fd-6c17-468b-82bd-1528512c8899",
                name = "alice",
                email = "alice@example.com",
            ),
        )
        var lastGetUserId: String? = null

        override suspend fun signUp(
            name: String,
            email: String,
            password: String,
            confirmPassword: String,
        ): AuthSession = AuthSession(
            email = email.trim(),
            displayName = name.trim(),
            accessToken = "access-sign-up",
            refreshToken = "refresh-sign-up",
        )

        override suspend fun login(
            email: String,
            password: String,
        ): AuthSession = AuthSession(
            email = email.trim(),
            displayName = "Logged In User",
            accessToken = "access-login",
            refreshToken = "refresh-login",
        )

        override suspend fun refresh(refreshToken: String): AuthSession = AuthSession(
            email = "",
            displayName = "",
            accessToken = "access-refreshed",
            refreshToken = "refresh-refreshed",
        )

        override suspend fun logout(accessToken: String?) {
            lastLogoutAccessToken = accessToken
        }

        override suspend fun getUser(id: String, accessToken: String): UserProfile {
            lastGetUserId = id
            return getUserResult.getOrThrow()
        }
    }

    private var lastLogoutAccessToken: String? = null

    private val repository = AuthRepositoryImpl(
        dataSource = AuthDataSource(authApi = fakeApi),
        sessionStore = sessionStore,
    )

    @Test
    fun `signUp returns success session and stores current session`() = runTest {
        val result = repository.signUp("Jordan Lee", "jordan@folio.app", "secret", "secret")

        assertTrue(result.isSuccess)
        assertEquals("alice@example.com", result.getOrNull()?.email)
        assertEquals("alice", result.getOrNull()?.displayName)
        assertEquals("d9c069fd-6c17-468b-82bd-1528512c8899", result.getOrNull()?.userId)
        assertEquals("refresh-sign-up", result.getOrNull()?.refreshToken)
        assertEquals("me", fakeApi.lastGetUserId)
        assertEquals("alice", repository.getCurrentSession()?.displayName)
        assertEquals("alice@example.com", repository.getCurrentSession()?.email)
    }

    @Test
    fun `signUp returns failure when name is blank`() = runTest {
        val result = repository.signUp(" ", "jordan@folio.app", "secret", "secret")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertNull(repository.getCurrentSession())
    }

    @Test
    fun `signIn enriches session from users me`() = runTest {
        val result = repository.signIn("jordan@folio.app", "secret")

        assertTrue(result.isSuccess)
        assertEquals("alice@example.com", result.getOrNull()?.email)
        assertEquals("alice", result.getOrNull()?.displayName)
        assertEquals("d9c069fd-6c17-468b-82bd-1528512c8899", result.getOrNull()?.userId)
        assertEquals("access-login", result.getOrNull()?.accessToken)
        assertEquals("alice", repository.getCurrentSession()?.displayName)
    }

    @Test
    fun `signIn keeps auth session when getUser fails`() = runTest {
        fakeApi.getUserResult = Result.failure(IOException("offline"))

        val result = repository.signIn("jordan@folio.app", "secret")

        assertTrue(result.isSuccess)
        assertEquals("jordan@folio.app", result.getOrNull()?.email)
        assertEquals("Logged In User", result.getOrNull()?.displayName)
        assertEquals("access-login", result.getOrNull()?.accessToken)
    }

    @Test
    fun `signIn returns failure when password is blank`() = runTest {
        val result = repository.signIn("jordan@folio.app", "")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `refreshSession updates tokens and keeps identity`() = runTest {
        repository.signIn("jordan@folio.app", "secret")

        val result = repository.refreshSession()

        assertTrue(result.isSuccess)
        assertEquals("alice@example.com", result.getOrNull()?.email)
        assertEquals("alice", result.getOrNull()?.displayName)
        assertEquals("access-refreshed", result.getOrNull()?.accessToken)
        assertEquals("refresh-refreshed", result.getOrNull()?.refreshToken)
        assertEquals("access-refreshed", repository.getCurrentSession()?.accessToken)
    }

    @Test
    fun `refreshSession preserves omitted refresh token`() = runTest {
        val store = InMemoryAuthSessionStore()
        val tokenOnlyApi = object : AuthApi by fakeApi {
            override suspend fun refresh(refreshToken: String): AuthSession = AuthSession(
                email = "",
                displayName = "",
                accessToken = "access-only",
                refreshToken = null,
            )
        }
        val tokenOnlyRepository = AuthRepositoryImpl(
            dataSource = AuthDataSource(authApi = tokenOnlyApi),
            sessionStore = store,
        )
        tokenOnlyRepository.signIn("jordan@folio.app", "secret")

        val result = tokenOnlyRepository.refreshSession()

        assertTrue(result.isSuccess)
        assertEquals("access-only", result.getOrNull()?.accessToken)
        assertEquals("refresh-login", result.getOrNull()?.refreshToken)
        assertEquals("alice@example.com", result.getOrNull()?.email)
        assertEquals("refresh-login", tokenOnlyRepository.getCurrentSession()?.refreshToken)
        assertEquals("access-only", tokenOnlyRepository.getCurrentSession()?.accessToken)
    }

    @Test
    fun `refreshSession clears session when server rejects refresh`() = runTest {
        repository.signIn("jordan@folio.app", "secret")
        assertEquals("access-login", repository.getCurrentSession()?.accessToken)

        val rejectedApi = object : AuthApi by fakeApi {
            override suspend fun refresh(refreshToken: String): AuthSession {
                throw AuthApiException("Refresh token expired")
            }
        }
        val rejectedRepository = AuthRepositoryImpl(
            dataSource = AuthDataSource(authApi = rejectedApi),
            sessionStore = sessionStore,
        )
        rejectedRepository.restoreSession()

        val result = rejectedRepository.refreshSession()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AuthApiException)
        assertNull(rejectedRepository.getCurrentSession())
        assertNull(sessionStore.read())
    }

    @Test
    fun `refreshSession fails when access token missing and does not clear session`() = runTest {
        val store = InMemoryAuthSessionStore()
        val missingAccessApi = object : AuthApi by fakeApi {
            override suspend fun refresh(refreshToken: String): AuthSession = AuthSession(
                email = "",
                displayName = "",
                accessToken = null,
                refreshToken = "refresh-refreshed",
            )
        }
        val seedingRepository = AuthRepositoryImpl(
            dataSource = AuthDataSource(authApi = fakeApi),
            sessionStore = store,
        )
        seedingRepository.signIn("jordan@folio.app", "secret")
        store.write(
            checkNotNull(seedingRepository.getCurrentSession()).copy(accessToken = null),
        )
        val missingAccessRepository = AuthRepositoryImpl(
            dataSource = AuthDataSource(authApi = missingAccessApi),
            sessionStore = store,
        )
        missingAccessRepository.restoreSession()

        val result = missingAccessRepository.refreshSession()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        assertEquals("refresh-login", missingAccessRepository.getCurrentSession()?.refreshToken)
        assertNull(missingAccessRepository.getCurrentSession()?.accessToken)
    }

    @Test
    fun `refreshSession reuses previous access token when refresh omits it`() = runTest {
        val store = InMemoryAuthSessionStore()
        val omittedAccessApi = object : AuthApi by fakeApi {
            override suspend fun refresh(refreshToken: String): AuthSession = AuthSession(
                email = "",
                displayName = "",
                accessToken = null,
                refreshToken = "refresh-rotated",
            )
        }
        val omittedAccessRepository = AuthRepositoryImpl(
            dataSource = AuthDataSource(authApi = omittedAccessApi),
            sessionStore = store,
        )
        omittedAccessRepository.signIn("jordan@folio.app", "secret")

        val result = omittedAccessRepository.refreshSession()

        assertTrue(result.isSuccess)
        assertEquals("access-login", result.getOrNull()?.accessToken)
        assertEquals("refresh-rotated", result.getOrNull()?.refreshToken)
        assertEquals("access-login", omittedAccessRepository.getCurrentSession()?.accessToken)
    }

    @Test
    fun `refreshSession fails when no session`() = runTest {
        val result = repository.refreshSession()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun `concurrent refreshSession calls share one network refresh`() = runTest {
        val concurrent = AtomicInteger(0)
        var maxConcurrent = 0
        val refreshCallCount = AtomicInteger(0)
        val refreshTokensSeen = mutableListOf<String>()

        val delayingApi = object : AuthApi by fakeApi {
            override suspend fun refresh(refreshToken: String): AuthSession {
                refreshCallCount.incrementAndGet()
                refreshTokensSeen += refreshToken
                val inFlight = concurrent.incrementAndGet()
                maxConcurrent = maxOf(maxConcurrent, inFlight)
                delay(50)
                concurrent.decrementAndGet()
                return AuthSession(
                    email = "",
                    displayName = "",
                    accessToken = "access-refreshed-${refreshCallCount.get()}",
                    refreshToken = "refresh-refreshed-${refreshCallCount.get()}",
                )
            }
        }
        val concurrentRepository = AuthRepositoryImpl(
            dataSource = AuthDataSource(authApi = delayingApi),
            sessionStore = InMemoryAuthSessionStore(),
        )
        concurrentRepository.signIn("jordan@folio.app", "secret")

        val results = coroutineScope {
            listOf(
                async { concurrentRepository.refreshSession() },
                async { concurrentRepository.refreshSession() },
            ).map { it.await() }
        }

        assertTrue(results.all { it.isSuccess })
        assertEquals(1, maxConcurrent)
        // Queued waiter reuses the session written by the first refresh.
        assertEquals(1, refreshCallCount.get())
        assertEquals(listOf("refresh-login"), refreshTokensSeen)
        val session0 = results[0].getOrThrow()
        val session1 = results[1].getOrThrow()
        assertEquals(session0, session1)
        assertEquals(session0, concurrentRepository.getCurrentSession())
        assertEquals("access-refreshed-1", session0.accessToken)
        assertEquals("refresh-refreshed-1", session0.refreshToken)
    }

    @Test
    fun `concurrent refreshSession calls share one network refresh when tokens unchanged`() = runTest {
        val concurrent = AtomicInteger(0)
        var maxConcurrent = 0
        val refreshCallCount = AtomicInteger(0)

        val delayingApi = object : AuthApi by fakeApi {
            override suspend fun refresh(refreshToken: String): AuthSession {
                refreshCallCount.incrementAndGet()
                val inFlight = concurrent.incrementAndGet()
                maxConcurrent = maxOf(maxConcurrent, inFlight)
                delay(50)
                concurrent.decrementAndGet()
                // Server acknowledges refresh but rotates neither token.
                return AuthSession(
                    email = "",
                    displayName = "",
                    accessToken = "access-login",
                    refreshToken = "refresh-login",
                )
            }
        }
        val concurrentRepository = AuthRepositoryImpl(
            dataSource = AuthDataSource(authApi = delayingApi),
            sessionStore = InMemoryAuthSessionStore(),
        )
        concurrentRepository.signIn("jordan@folio.app", "secret")

        val results = coroutineScope {
            listOf(
                async { concurrentRepository.refreshSession() },
                async { concurrentRepository.refreshSession() },
            ).map { it.await() }
        }

        assertTrue(results.all { it.isSuccess })
        assertEquals(1, maxConcurrent)
        // Completion is tracked by generation, not token equality.
        assertEquals(1, refreshCallCount.get())
        val session0 = results[0].getOrThrow()
        val session1 = results[1].getOrThrow()
        assertEquals(session0, session1)
        assertEquals(session0, concurrentRepository.getCurrentSession())
        assertEquals("access-login", session0.accessToken)
        assertEquals("refresh-login", session0.refreshToken)
    }

    @Test
    fun `signIn waits for in-flight refresh and becomes the final session`() = runTest {
        val refreshStarted = CompletableDeferred<Unit>()
        val allowRefreshToFinish = CompletableDeferred<Unit>()
        val signInStartedWhileRefreshHeld = AtomicInteger(0)

        val api = object : AuthApi by fakeApi {
            override suspend fun refresh(refreshToken: String): AuthSession {
                refreshStarted.complete(Unit)
                allowRefreshToFinish.await()
                return AuthSession(
                    email = "",
                    displayName = "",
                    accessToken = "access-refreshed-alice",
                    refreshToken = "refresh-refreshed-alice",
                )
            }

            override suspend fun login(
                email: String,
                password: String,
            ): AuthSession {
                // Only reachable after refresh releases the session mutex.
                if (refreshStarted.isCompleted && !allowRefreshToFinish.isCompleted) {
                    signInStartedWhileRefreshHeld.incrementAndGet()
                }
                return AuthSession(
                    email = email.trim(),
                    displayName = "Bob",
                    accessToken = "access-bob",
                    refreshToken = "refresh-bob",
                )
            }

            override suspend fun getUser(id: String, accessToken: String): UserProfile {
                return if (accessToken == "access-bob") {
                    UserProfile(
                        id = "bob-id",
                        name = "Bob",
                        email = "bob@folio.app",
                    )
                } else {
                    UserProfile(
                        id = "alice-id",
                        name = "alice",
                        email = "alice@example.com",
                    )
                }
            }
        }
        val store = InMemoryAuthSessionStore()
        store.write(
            AuthSession(
                email = "alice@example.com",
                displayName = "alice",
                userId = "alice-id",
                accessToken = "access-alice",
                refreshToken = "refresh-alice",
            ),
        )
        val repository = AuthRepositoryImpl(
            dataSource = AuthDataSource(authApi = api),
            sessionStore = store,
        )
        repository.restoreSession()

        val refreshJob = async { repository.refreshSession() }
        refreshStarted.await()
        val signInJob = async { repository.signIn("bob@folio.app", "secret") }
        // Give sign-in a chance to race; mutex should keep login from starting mid-refresh.
        delay(50)
        assertEquals(0, signInStartedWhileRefreshHeld.get())
        assertEquals("refresh-alice", repository.getCurrentSession()?.refreshToken)

        allowRefreshToFinish.complete(Unit)
        assertTrue(refreshJob.await().isSuccess)
        assertTrue(signInJob.await().isSuccess)

        assertEquals("bob@folio.app", repository.getCurrentSession()?.email)
        assertEquals("Bob", repository.getCurrentSession()?.displayName)
        assertEquals("access-bob", repository.getCurrentSession()?.accessToken)
        assertEquals("refresh-bob", repository.getCurrentSession()?.refreshToken)
        assertEquals(0, signInStartedWhileRefreshHeld.get())
    }

    @Test
    fun `syncCurrentUser refreshes profile for me`() = runTest {
        repository.signIn("jordan@folio.app", "secret")
        fakeApi.getUserResult = Result.success(
            UserProfile(
                id = "user-2",
                name = "Jordan Lee",
                email = "jordan@folio.app",
            ),
        )

        val result = repository.syncCurrentUser()

        assertTrue(result.isSuccess)
        assertEquals("me", fakeApi.lastGetUserId)
        assertEquals("user-2", result.getOrNull()?.userId)
        assertEquals("Jordan Lee", result.getOrNull()?.displayName)
        assertEquals("jordan@folio.app", result.getOrNull()?.email)
    }

    @Test
    fun `signOut clears session and calls logout api`() = runTest {
        repository.signIn("jordan@folio.app", "secret")

        val result = repository.signOut()

        assertTrue(result.isSuccess)
        assertNull(repository.getCurrentSession())
        assertEquals("access-login", lastLogoutAccessToken)
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
        repository.signIn("jordan@folio.app", "secret")

        repository.clearSession()

        assertNull(repository.getCurrentSession())
        assertNull(sessionStore.read())
    }

    @Test
    fun `restores session from store on restoreSession`() = runTest {
        repository.signIn("jordan@folio.app", "secret")

        val restored = AuthRepositoryImpl(
            dataSource = AuthDataSource(authApi = fakeApi),
            sessionStore = sessionStore,
        )
        restored.restoreSession()

        assertEquals("alice@example.com", restored.getCurrentSession()?.email)
        assertEquals("access-login", restored.getCurrentSession()?.accessToken)
        assertEquals("refresh-login", restored.getCurrentSession()?.refreshToken)
        assertEquals("d9c069fd-6c17-468b-82bd-1528512c8899", restored.getCurrentSession()?.userId)
    }
}
