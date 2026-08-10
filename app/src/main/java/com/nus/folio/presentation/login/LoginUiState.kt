package com.nus.folio.presentation.login

data class LoginUiState(
    val passwordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val authUnavailable: Boolean = false,
    val emailError: LoginError? = null,
    val passwordError: LoginError? = null,
    val formError: LoginError? = null,
    val toastError: LoginError? = null,
    val toastMessage: String? = null,
    val shouldNavigateToHome: Boolean = false,
)

enum class LoginError {
    EMAIL_REQUIRED,
    EMAIL_INVALID,
    PASSWORD_REQUIRED,
    INVALID_CREDENTIALS,
    SIGN_IN_FAILED,
    AUTH_UNAVAILABLE,
}
