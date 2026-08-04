package com.nus.folio.presentation.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nus.folio.domain.usecase.GetCurrentSessionUseCase
import com.nus.folio.domain.usecase.SyncCurrentUserUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AccountViewModel(
    private val getCurrentSessionUseCase: GetCurrentSessionUseCase,
    private val syncCurrentUserUseCase: SyncCurrentUserUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    init {
        applySession()
        viewModelScope.launch {
            syncCurrentUserUseCase()
            applySession()
        }
    }

    private fun applySession() {
        val session = getCurrentSessionUseCase()
        _uiState.update {
            it.copy(
                displayName = session?.displayName.orEmpty(),
                email = session?.email.orEmpty(),
            )
        }
    }

    fun onProfileSettingsClick() {
        _uiState.update { it.copy(userMessage = AccountUserMessage.PROFILE_SETTINGS_NOT_SUPPORTED) }
    }

    fun onSecurityClick() {
        _uiState.update { it.copy(userMessage = AccountUserMessage.SECURITY_NOT_SUPPORTED) }
    }

    fun onPrivacyDataClick() {
        _uiState.update { it.copy(userMessage = AccountUserMessage.PRIVACY_DATA_NOT_SUPPORTED) }
    }

    fun onExportDataClick() {
        _uiState.update { it.copy(userMessage = AccountUserMessage.EXPORT_DATA_NOT_SUPPORTED) }
    }

    fun onUserMessageShown() {
        _uiState.update { it.copy(userMessage = null) }
    }

    class Factory(
        private val getCurrentSessionUseCase: GetCurrentSessionUseCase,
        private val syncCurrentUserUseCase: SyncCurrentUserUseCase,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AccountViewModel::class.java)) {
                return AccountViewModel(
                    getCurrentSessionUseCase = getCurrentSessionUseCase,
                    syncCurrentUserUseCase = syncCurrentUserUseCase,
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
