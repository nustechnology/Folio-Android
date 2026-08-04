package com.nus.folio.presentation.login

data class LoginUiState(
    val passwordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val authUnavailable: Boolean = false,
    val error: LoginError? = null,
    val toastMessage: String? = null,
    val shouldNavigateToHome: Boolean = false,
)

enum class LoginError {
    EMAIL_REQUIRED,
    PASSWORD_REQUIRED,
    SIGN_IN_FAILED,
    AUTH_UNAVAILABLE,
}
