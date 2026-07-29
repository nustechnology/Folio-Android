package com.nus.folio.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nus.folio.domain.usecase.RequestPasswordResetUseCase
import com.nus.folio.domain.usecase.SignInUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val signInUseCase: SignInUseCase,
    private val requestPasswordResetUseCase: RequestPasswordResetUseCase,
    private val isAuthAvailable: Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        LoginUiState(authUnavailable = !isAuthAvailable),
    )
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private var activeJob: Job? = null

    fun clearFeedback() {
        _uiState.update { it.copy(error = null, info = null) }
    }

    fun onNavigationHandled() {
        _uiState.update { it.copy(shouldNavigateToHome = false) }
    }

    fun onTogglePasswordVisibility() {
        _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    fun onSignInClick(email: String, password: String) {
        if (!isAuthAvailable) {
            _uiState.update {
                it.copy(error = LoginError.AUTH_UNAVAILABLE, info = null)
            }
            return
        }
        when {
            email.isBlank() -> {
                _uiState.update { it.copy(error = LoginError.EMAIL_REQUIRED, info = null) }
            }
            password.isBlank() -> {
                _uiState.update { it.copy(error = LoginError.PASSWORD_REQUIRED, info = null) }
            }
            else -> performSignIn(email, password)
        }
    }

    fun onForgotPasswordClick(email: String) {
        if (!isAuthAvailable) {
            _uiState.update {
                it.copy(error = LoginError.AUTH_UNAVAILABLE, info = null)
            }
            return
        }
        if (email.isBlank()) {
            _uiState.update { it.copy(error = LoginError.EMAIL_REQUIRED, info = null) }
            return
        }

        launchExclusive {
            _uiState.update {
                it.copy(isLoading = true, error = null, info = null, shouldNavigateToHome = false)
            }

            val result = requestPasswordResetUseCase(email)
            ensureActive()

            result
                .onSuccess {
                    _uiState.update {
                        it.copy(isLoading = false, info = LoginInfo.PASSWORD_RESET_SENT)
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(isLoading = false, error = LoginError.PASSWORD_RESET_FAILED)
                    }
                }
        }
    }

    private fun performSignIn(email: String, password: String) {
        launchExclusive {
            _uiState.update {
                it.copy(isLoading = true, error = null, info = null, shouldNavigateToHome = false)
            }

            val result = signInUseCase(email, password)
            ensureActive()

            result
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, shouldNavigateToHome = true) }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(isLoading = false, error = LoginError.SIGN_IN_FAILED)
                    }
                }
        }
    }

    private fun launchExclusive(block: suspend CoroutineScope.() -> Unit) {
        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            block()
        }
    }

    class Factory(
        private val signInUseCase: SignInUseCase,
        private val requestPasswordResetUseCase: RequestPasswordResetUseCase,
        private val isAuthAvailable: Boolean,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LoginViewModel(
                signInUseCase,
                requestPasswordResetUseCase,
                isAuthAvailable,
            ) as T
        }
    }
}
