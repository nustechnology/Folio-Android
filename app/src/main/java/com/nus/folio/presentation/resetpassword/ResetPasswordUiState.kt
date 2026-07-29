package com.nus.folio.presentation.resetpassword

data class ResetPasswordUiState(
    val isLoading: Boolean = false,
    val error: ResetPasswordError? = null,
    val info: ResetPasswordInfo? = null,
)

enum class ResetPasswordError {
    EMAIL_REQUIRED,
    SEND_FAILED,
}

enum class ResetPasswordInfo {
    LINK_SENT,
}
