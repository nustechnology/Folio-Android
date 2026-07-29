package com.nus.folio.presentation.signup

data class SignUpUiState(
    val authUnavailable: Boolean = false,
    val passwordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val error: SignUpError? = null,
    val shouldNavigateToHome: Boolean = false,
)

enum class SignUpError {
    NAME_REQUIRED,
    EMAIL_REQUIRED,
    PASSWORD_REQUIRED,
    SIGN_UP_FAILED,
}
