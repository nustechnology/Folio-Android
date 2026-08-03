package com.nus.folio.presentation.account

data class AccountUiState(
    val displayName: String = "",
    val email: String = "",
    val userMessage: AccountUserMessage? = null,
)

enum class AccountUserMessage {
    PROFILE_SETTINGS_NOT_SUPPORTED,
    SECURITY_NOT_SUPPORTED,
    PRIVACY_DATA_NOT_SUPPORTED,
    EXPORT_DATA_NOT_SUPPORTED,
}
