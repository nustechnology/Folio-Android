package com.nus.folio.presentation.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nus.folio.domain.usecase.SignInWithAppleUseCase
import com.nus.folio.domain.usecase.SignUpUseCase
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

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun onNavigationHandled() {
        _uiState.update { it.copy(shouldNavigateToHome = false) }
    }

    fun onTogglePasswordVisibility() {
        _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    fun onSignUpClick(name: String, email: String, password: String) {
        when {
            name.isBlank() -> {
                _uiState.update { it.copy(error = SignUpError.NAME_REQUIRED) }
            }
            email.isBlank() -> {
                _uiState.update { it.copy(error = SignUpError.EMAIL_REQUIRED) }
            }
            password.isBlank() -> {
                _uiState.update { it.copy(error = SignUpError.PASSWORD_REQUIRED) }
            }
            else -> performSignUp(name, email, password)
        }
    }

    fun onContinueWithAppleClick() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, shouldNavigateToHome = false) }

            signInWithAppleUseCase()
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, shouldNavigateToHome = true) }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(isLoading = false, error = SignUpError.SIGN_UP_FAILED)
                    }
                }
        }
    }

    private fun performSignUp(name: String, email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, shouldNavigateToHome = false) }

            signUpUseCase(name, email, password)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, shouldNavigateToHome = true) }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(isLoading = false, error = SignUpError.SIGN_UP_FAILED)
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
