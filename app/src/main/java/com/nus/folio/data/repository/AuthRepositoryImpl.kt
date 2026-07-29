package com.nus.folio.data.repository

import com.nus.folio.data.datasource.AuthDataSource
import com.nus.folio.domain.model.AuthSession
import com.nus.folio.domain.repository.AuthRepository
import kotlin.coroutines.cancellation.CancellationException

class AuthRepositoryImpl(
    private val dataSource: AuthDataSource,
) : AuthRepository {

    override suspend fun signIn(
        email: String,
        password: String,
    ): Result<AuthSession> = runSuspendCatching {
        dataSource.signIn(email, password)
    }

    override suspend fun requestPasswordReset(email: String): Result<Unit> = runSuspendCatching {
        dataSource.requestPasswordReset(email)
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
