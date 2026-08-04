package com.nus.folio.presentation.signup

data class SignUpUiState(
    val authUnavailable: Boolean = false,
    val passwordVisible: Boolean = false,
    val confirmPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val nameError: SignUpError? = null,
    val emailError: SignUpError? = null,
    val passwordError: SignUpError? = null,
    val confirmPasswordError: SignUpError? = null,
    val formError: SignUpError? = null,
    val toastMessage: String? = null,
    val shouldNavigateToHome: Boolean = false,
)

enum class SignUpError {
    NAME_REQUIRED,
    EMAIL_REQUIRED,
    PASSWORD_REQUIRED,
    CONFIRM_PASSWORD_REQUIRED,
    PASSWORDS_DO_NOT_MATCH,
    SIGN_UP_FAILED,
    NETWORK_ERROR,
}
