package com.nus.folio.presentation.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nus.folio.domain.model.AuthApiException
import com.nus.folio.domain.usecase.SignInWithAppleUseCase
import com.nus.folio.domain.usecase.SignUpUseCase
import java.io.InterruptedIOException
import java.net.SocketException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SignUpViewModel(
    private val signUpUseCase: SignUpUseCase,
    private val signInWithAppleUseCase: SignInWithAppleUseCase,
    isAuthAvailable: Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignUpUiState(authUnavailable = !isAuthAvailable))
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    fun clearNameError() {
        _uiState.update { it.copy(nameError = null, formError = null) }
    }

    fun clearEmailError() {
        _uiState.update { it.copy(emailError = null, formError = null) }
    }

    fun clearPasswordError() {
        _uiState.update { it.copy(passwordError = null, formError = null) }
    }

    fun clearConfirmPasswordError() {
        _uiState.update { it.copy(confirmPasswordError = null, formError = null) }
    }

    fun onToastMessageShown() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    fun onNavigationHandled() {
        _uiState.update { it.copy(shouldNavigateToHome = false, isLoading = false) }
    }

    fun onTogglePasswordVisibility() {
        _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    fun onToggleConfirmPasswordVisibility() {
        _uiState.update { it.copy(confirmPasswordVisible = !it.confirmPasswordVisible) }
    }

    fun onSignUpClick(name: String, email: String, password: String, confirmPassword: String) {
        val nameError = if (name.isBlank()) SignUpError.NAME_REQUIRED else null
        val emailError = if (email.isBlank()) SignUpError.EMAIL_REQUIRED else null
        val passwordError = if (password.isBlank()) SignUpError.PASSWORD_REQUIRED else null
        val confirmPasswordError = when {
            confirmPassword.isBlank() -> SignUpError.CONFIRM_PASSWORD_REQUIRED
            password.isNotBlank() && password != confirmPassword -> SignUpError.PASSWORDS_DO_NOT_MATCH
            else -> null
        }

        if (nameError != null ||
            emailError != null ||
            passwordError != null ||
            confirmPasswordError != null
        ) {
            _uiState.update {
                it.copy(
                    nameError = nameError,
                    emailError = emailError,
                    passwordError = passwordError,
                    confirmPasswordError = confirmPasswordError,
                    formError = null,
                )
            }
            return
        }

        performSignUp(name, email, password, confirmPassword)
    }

    fun onContinueWithAppleClick() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    nameError = null,
                    emailError = null,
                    passwordError = null,
                    confirmPasswordError = null,
                    formError = null,
                    toastMessage = null,
                    shouldNavigateToHome = false,
                )
            }

            signInWithAppleUseCase()
                .onSuccess {
                    _uiState.update { it.copy(shouldNavigateToHome = true) }
                }
                .onFailure { error ->
                    applyFailure(error)
                }
        }
    }

    private fun performSignUp(
        name: String,
        email: String,
        password: String,
        confirmPassword: String,
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    nameError = null,
                    emailError = null,
                    passwordError = null,
                    confirmPasswordError = null,
                    formError = null,
                    toastMessage = null,
                    shouldNavigateToHome = false,
                )
            }

            signUpUseCase(name, email, password, confirmPassword)
                .onSuccess {
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
                        formError = SignUpError.NETWORK_ERROR,
                        toastMessage = null,
                    )
                }
            }
            // AuthApiClient surfaces HTTP error bodies as AuthApiException.
            error is AuthApiException -> {
                val apiMessage = error.message?.takeIf { it.isNotBlank() }
                _uiState.update {
                    if (apiMessage != null) {
                        it.copy(
                            isLoading = false,
                            formError = null,
                            toastMessage = apiMessage,
                        )
                    } else {
                        it.copy(
                            isLoading = false,
                            formError = SignUpError.SIGN_UP_FAILED,
                            toastMessage = null,
                        )
                    }
                }
            }
            else -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        formError = SignUpError.SIGN_UP_FAILED,
                        toastMessage = null,
                    )
                }
            }
        }
    }

    class Factory(
        private val signUpUseCase: SignUpUseCase,
        private val signInWithAppleUseCase: SignInWithAppleUseCase,
        private val isAuthAvailable: Boolean,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SignUpViewModel(signUpUseCase, signInWithAppleUseCase, isAuthAvailable) as T
        }
    }
}

private fun Throwable.isTransportFailure(): Boolean =
    this is UnknownHostException ||
        this is SocketException ||
        this is InterruptedIOException ||
        this is SSLException
