package com.nus.folio.data.repository

import com.nus.folio.data.auth.AuthSessionStore
import com.nus.folio.data.datasource.AuthDataSource
import com.nus.folio.domain.model.AuthSession
import com.nus.folio.domain.repository.AuthRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.cancellation.CancellationException

class AuthRepositoryImpl(
    private val dataSource: AuthDataSource,
    private val sessionStore: AuthSessionStore,
) : AuthRepository {

    @Volatile
    private var currentSession: AuthSession? = null

    @Volatile
    private var sessionRestored = false

    /**
     * Serializes every session read/mutate/write path so automatic 401 refresh cannot
     * merge tokens into a session that another flow concurrently replaced.
     */
    private val sessionMutex = Mutex()

    override suspend fun restoreSession() {
        sessionMutex.withLock {
            if (sessionRestored) return
            currentSession = sessionStore.read()
            sessionRestored = true
        }
    }

    override suspend fun signUp(
        name: String,
        email: String,
        password: String,
        confirmPassword: String,
    ): Result<AuthSession> = sessionMutex.withLock {
        runSuspendCatching {
            val session = dataSource.signUp(name, email, password, confirmPassword)
            enrichWithCurrentUser(session)
        }.also { result ->
            result.onSuccess { setSession(it) }
        }
    }

    override suspend fun signIn(
        email: String,
        password: String,
    ): Result<AuthSession> = sessionMutex.withLock {
        runSuspendCatching {
            val session = dataSource.signIn(email, password)
            enrichWithCurrentUser(session)
        }.also { result ->
            result.onSuccess { setSession(it) }
        }
    }

    override suspend fun signInWithApple(): Result<AuthSession> = sessionMutex.withLock {
        runSuspendCatching {
            dataSource.signInWithApple()
        }.also { result ->
            result.onSuccess { setSession(it) }
        }
    }

    override suspend fun refreshSession(): Result<AuthSession> = sessionMutex.withLock {
        runSuspendCatching {
            val previous = currentSession
            val refreshToken = previous?.refreshToken?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("No refresh token available")
            val refreshed = dataSource.refresh(refreshToken)
            val accessToken = refreshed.accessToken?.takeIf { it.isNotBlank() }
                ?: previous.accessToken?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("Refresh response missing access token")
            val mergedRefreshToken = refreshed.refreshToken?.takeIf { it.isNotBlank() }
                ?: previous.refreshToken
            val merged = AuthSession(
                email = refreshed.email.ifBlank { previous.email },
                displayName = refreshed.displayName.ifBlank { previous.displayName },
                userId = refreshed.userId ?: previous.userId,
                accessToken = accessToken,
                refreshToken = mergedRefreshToken,
            )
            enrichWithCurrentUser(merged)
        }.also { result ->
            result.onSuccess { setSession(it) }
        }
    }

    suspend fun syncCurrentUser(): Result<AuthSession> = syncCurrentUser(userId = "me")

    override suspend fun syncCurrentUser(userId: String): Result<AuthSession> = sessionMutex.withLock {
        runSuspendCatching {
            val session = currentSession
                ?: throw IllegalStateException("No session available")
            enrichWithCurrentUser(session, userId = userId.ifBlank { "me" })
        }.also { result ->
            result.onSuccess { setSession(it) }
        }
    }

    override suspend fun signOut(): Result<Unit> = sessionMutex.withLock {
        val accessToken = currentSession?.accessToken
        setSession(null)
        runSuspendCatching {
            dataSource.logout(accessToken)
        }
    }

    override suspend fun requestPasswordReset(email: String): Result<Unit> = runSuspendCatching {
        dataSource.requestPasswordReset(email)
    }

    override fun getCurrentSession(): AuthSession? = currentSession

    override suspend fun clearSession() {
        sessionMutex.withLock {
            setSession(null)
        }
    }

    private suspend fun enrichWithCurrentUser(
        session: AuthSession,
        userId: String = "me",
    ): AuthSession {
        val accessToken = session.accessToken?.takeIf { it.isNotBlank() } ?: return session
        return try {
            val profile = dataSource.getUser(userId, accessToken)
            session.copy(
                email = profile.email.ifBlank { session.email },
                displayName = profile.name.ifBlank { session.displayName },
                userId = profile.id.ifBlank { session.userId },
            )
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Keep auth session even if profile fetch fails.
            session
        }
    }

    private suspend fun setSession(session: AuthSession?) {
        currentSession = session
        sessionRestored = true
        if (session == null) {
            sessionStore.clear()
        } else {
            sessionStore.write(session)
        }
    }

    private suspend fun <T> runSuspendCatching(block: suspend () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
}
