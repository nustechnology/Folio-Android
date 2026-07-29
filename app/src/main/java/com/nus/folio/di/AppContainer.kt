package com.nus.folio.di

import com.nus.folio.data.auth.AuthCapabilities
import com.nus.folio.data.datasource.AuthDataSource
import com.nus.folio.data.datasource.GreetingDataSource
import com.nus.folio.data.repository.AuthRepositoryImpl
import com.nus.folio.data.repository.GreetingRepositoryImpl
import com.nus.folio.domain.repository.AuthRepository
import com.nus.folio.domain.repository.GreetingRepository
import com.nus.folio.domain.usecase.GetGreetingUseCase
import com.nus.folio.domain.usecase.RequestPasswordResetUseCase
import com.nus.folio.domain.usecase.SignInUseCase

class AppContainer {

    /** False in release until AuthDataSource is wired to a real backend. */
    val isAuthAvailable: Boolean = AuthCapabilities.isBackendAvailable

    private val greetingDataSource: GreetingDataSource by lazy { GreetingDataSource() }

    private val greetingRepository: GreetingRepository by lazy {
        GreetingRepositoryImpl(greetingDataSource)
    }

    val getGreetingUseCase: GetGreetingUseCase by lazy {
        GetGreetingUseCase(greetingRepository)
    }

    private val authDataSource: AuthDataSource by lazy { AuthDataSource() }

    private val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(authDataSource)
    }

    val signInUseCase: SignInUseCase by lazy {
        SignInUseCase(authRepository)
    }

    val requestPasswordResetUseCase: RequestPasswordResetUseCase by lazy {
        RequestPasswordResetUseCase(authRepository)
    }
}
