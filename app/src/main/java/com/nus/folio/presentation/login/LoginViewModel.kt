package com.nus.folio.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nus.folio.domain.model.AuthApiException
import com.nus.folio.domain.usecase.SignInUseCase
import com.nus.folio.domain.util.AuthInputRules
import java.io.InterruptedIOException
import java.net.SocketException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val signInUseCase: SignInUseCase,
    private val isAuthAvailable: Boolean,
    private val signInLoadingDelayMs: Long = SIGN_IN_LOADING_DELAY_MS,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        LoginUiState(authUnavailable = !isAuthAvailable),
    )
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private var activeJob: Job? = null

    fun clearFeedback() {
        _uiState.update { it.copy(error = null) }
    }

    fun onToastMessageShown() {
        _uiState.update { it.copy(toastMessage = null, toastError = null) }
    }

    fun onNavigationHandled() {
        _uiState.update { it.copy(shouldNavigateToHome = false, isLoading = false) }
    }

    fun onTogglePasswordVisibility() {
        _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    fun onSignInClick(email: String, password: String) {
        if (!isAuthAvailable) {
            _uiState.update { it.copy(error = LoginError.AUTH_UNAVAILABLE) }
            return
        }
        when {
            email.isBlank() -> {
                _uiState.update {
                    it.copy(
                        error = LoginError.EMAIL_REQUIRED,
                        toastError = null,
                        toastMessage = null,
                    )
                }
            }
            !AuthInputRules.isValidEmail(email) -> {
                _uiState.update {
                    it.copy(
                        error = LoginError.EMAIL_INVALID,
                        toastError = null,
                        toastMessage = null,
                    )
                }
            }
            password.isBlank() -> {
                _uiState.update {
                    it.copy(
                        error = LoginError.PASSWORD_REQUIRED,
                        toastError = null,
                        toastMessage = null,
                    )
                }
            }
            else -> performSignIn(email, password)
        }
    }

    private fun performSignIn(email: String, password: String) {
        launchExclusive {
            val loadingStartedAt = System.currentTimeMillis()
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    toastError = null,
                    toastMessage = null,
                    shouldNavigateToHome = false,
                )
            }

            val result = signInUseCase(email, password)
            ensureActive()

            result
                .onSuccess {
                    val elapsed = System.currentTimeMillis() - loadingStartedAt
                    delay((signInLoadingDelayMs - elapsed).coerceAtLeast(0))
                    ensureActive()
                    _uiState.update { it.copy(shouldNavigateToHome = true) }
                }
                .onFailure { error ->
                    applyFailure(error)
                }
        }
    }

    private fun applyFailure(error: Throwable) {
        when {
            error.isTransportFailure() -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = LoginError.SIGN_IN_FAILED,
                        toastError = null,
                        toastMessage = null,
                    )
                }
            }
            error is AuthApiException && error.isCredentialRejection() -> {
                // AC3 Case 5: unregistered email or wrong password → fixed toast.
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = null,
                        toastError = LoginError.INVALID_CREDENTIALS,
                        toastMessage = null,
                    )
                }
            }
            error is AuthApiException -> {
                // Rate limits, server errors, and other non-credential API failures.
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = LoginError.SIGN_IN_FAILED,
                        toastError = null,
                        toastMessage = null,
                    )
                }
            }
            else -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = null,
                        toastError = LoginError.INVALID_CREDENTIALS,
                        toastMessage = null,
                    )
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
        private val isAuthAvailable: Boolean,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LoginViewModel(signInUseCase, isAuthAvailable) as T
        }
    }

    private companion object {
        const val SIGN_IN_LOADING_DELAY_MS = 2_000L
    }
}

private fun AuthApiException.isCredentialRejection(): Boolean =
    statusCode == HTTP_UNAUTHORIZED

private const val HTTP_UNAUTHORIZED = 401

private fun Throwable.isTransportFailure(): Boolean =
    this is UnknownHostException ||
        this is SocketException ||
        this is InterruptedIOException ||
        this is SSLException
