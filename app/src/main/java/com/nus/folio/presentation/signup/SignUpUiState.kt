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
    /** Fixed client toast (e.g. email already exists); mapped to strings in the screen. */
    val toastError: SignUpError? = null,
    /** Dynamic toast text from API error messages. */
    val toastMessage: String? = null,
    val shouldNavigateToHome: Boolean = false,
)

enum class SignUpError {
    NAME_REQUIRED,
    EMAIL_REQUIRED,
    EMAIL_INVALID,
    EMAIL_ALREADY_EXISTS,
    PASSWORD_REQUIRED,
    PASSWORD_TOO_SHORT,
    CONFIRM_PASSWORD_REQUIRED,
    PASSWORDS_DO_NOT_MATCH,
    SIGN_UP_FAILED,
    NETWORK_ERROR,
}
