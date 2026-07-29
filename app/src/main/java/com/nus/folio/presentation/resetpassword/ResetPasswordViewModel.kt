package com.nus.folio.presentation.resetpassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nus.folio.domain.usecase.RequestPasswordResetUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ResetPasswordViewModel(
    private val requestPasswordResetUseCase: RequestPasswordResetUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResetPasswordUiState())
    val uiState: StateFlow<ResetPasswordUiState> = _uiState.asStateFlow()

    fun clearFeedback() {
        _uiState.update { it.copy(error = null, info = null) }
    }

    fun onSendRecoveryLinkClick(email: String) {
        if (email.isBlank()) {
            _uiState.update { it.copy(error = ResetPasswordError.EMAIL_REQUIRED, info = null) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, info = null) }

            requestPasswordResetUseCase(email)
                .onSuccess {
                    _uiState.update {
                        it.copy(isLoading = false, info = ResetPasswordInfo.LINK_SENT)
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(isLoading = false, error = ResetPasswordError.SEND_FAILED)
                    }
                }
        }
    }

    class Factory(
        private val requestPasswordResetUseCase: RequestPasswordResetUseCase,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ResetPasswordViewModel(requestPasswordResetUseCase) as T
        }
    }
}
