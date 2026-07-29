package com.nus.folio.data.repository

import com.nus.folio.data.datasource.AuthDataSource
import com.nus.folio.domain.model.AuthSession
import com.nus.folio.domain.repository.AuthRepository
import kotlin.coroutines.cancellation.CancellationException

class AuthRepositoryImpl(
    private val dataSource: AuthDataSource,
) : AuthRepository {

    @Volatile
    private var currentSession: AuthSession? = null

    override suspend fun signUp(
        name: String,
        email: String,
        password: String,
    ): Result<AuthSession> = runSuspendCatching {
        dataSource.signUp(name, email, password)
    }.also { result ->
        result.onSuccess { currentSession = it }
    }

    override suspend fun signIn(
        email: String,
        password: String,
    ): Result<AuthSession> = runSuspendCatching {
        dataSource.signIn(email, password)
    }.also { result ->
        result.onSuccess { currentSession = it }
    }

    override suspend fun signInWithApple(): Result<AuthSession> = runSuspendCatching {
        dataSource.signInWithApple()
    }.also { result ->
        result.onSuccess { currentSession = it }
    }

    override suspend fun requestPasswordReset(email: String): Result<Unit> = runSuspendCatching {
        dataSource.requestPasswordReset(email)
    }

    override fun getCurrentSession(): AuthSession? = currentSession

    override fun clearSession() {
        currentSession = null
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
