package com.nus.folio.testing

import com.nus.folio.domain.model.AuthSession
import com.nus.folio.domain.repository.AuthRepository

class FakeAuthRepository : AuthRepository {

    var signUpResult: Result<AuthSession> = Result.success(AuthSession("new.user@folio.app"))
    var signInResult: Result<AuthSession> = Result.success(AuthSession("user@folio.app"))
    var signInWithAppleResult: Result<AuthSession> =
        Result.success(AuthSession("apple.user@folio.app"))
    var requestPasswordResetResult: Result<Unit> = Result.success(Unit)

    var lastSignUpName: String? = null
    var lastSignUpEmail: String? = null
    var lastSignUpPassword: String? = null
    var lastSignInEmail: String? = null
    var lastSignInPassword: String? = null
    var lastPasswordResetEmail: String? = null

    var signUpCallCount = 0
    var signInCallCount = 0
    var signInWithAppleCallCount = 0
    var requestPasswordResetCallCount = 0
    var clearSessionCallCount = 0

    private var currentSession: AuthSession? = null

    override suspend fun signUp(
        name: String,
        email: String,
        password: String,
    ): Result<AuthSession> {
        signUpCallCount++
        lastSignUpName = name
        lastSignUpEmail = email
        lastSignUpPassword = password
        return signUpResult.also { result ->
            result.onSuccess { currentSession = it }
        }
    }

    override suspend fun signIn(
        email: String,
        password: String,
    ): Result<AuthSession> {
        signInCallCount++
        lastSignInEmail = email
        lastSignInPassword = password
        return signInResult.also { result ->
            result.onSuccess { currentSession = it }
        }
    }

    override suspend fun signInWithApple(): Result<AuthSession> {
        signInWithAppleCallCount++
        return signInWithAppleResult.also { result ->
            result.onSuccess { currentSession = it }
        }
    }

    override suspend fun requestPasswordReset(email: String): Result<Unit> {
        requestPasswordResetCallCount++
        lastPasswordResetEmail = email
        return requestPasswordResetResult
    }

    override fun getCurrentSession(): AuthSession? = currentSession

    override fun clearSession() {
        clearSessionCallCount++
        currentSession = null
    }
}
