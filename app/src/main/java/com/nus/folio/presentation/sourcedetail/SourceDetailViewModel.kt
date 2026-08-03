package com.nus.folio.presentation.sourcedetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nus.folio.domain.usecase.GetSourceDetailUseCase
import com.nus.folio.domain.usecase.GetSourceOriginalFileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SourceDetailViewModel(
    private val spaceId: String,
    private val sourceId: String,
    private val getSourceDetailUseCase: GetSourceDetailUseCase,
    private val getSourceOriginalFileUseCase: GetSourceOriginalFileUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SourceDetailUiState())
    val uiState: StateFlow<SourceDetailUiState> = _uiState.asStateFlow()

    init {
        loadDetail()
    }

    fun loadDetail() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            getSourceDetailUseCase(spaceId, sourceId)
                .onSuccess { detail ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = null,
                            detail = detail,
                            selectedSheetIndex = 0,
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.message ?: "Failed to load source",
                        )
                    }
                }
        }
    }

    fun onSheetSelected(index: Int) {
        _uiState.update { it.copy(selectedSheetIndex = index) }
    }

    fun onOpenOriginalClick() {
        viewModelScope.launch {
            getSourceOriginalFileUseCase(spaceId, sourceId)
                .onSuccess { location ->
                    _uiState.update { it.copy(openOriginalRequest = location) }
                }
                .onFailure {
                    _uiState.update { it.copy(userMessage = SourceDetailUserMessage.OPEN_ORIGINAL_FAILED) }
                }
        }
    }

    fun onOpenOriginalResult(result: SourceOriginalOpenResult) {
        if (result == SourceOriginalOpenResult.NO_APP) {
            _uiState.update { it.copy(userMessage = SourceDetailUserMessage.OPEN_ORIGINAL_NO_APP) }
        } else if (result == SourceOriginalOpenResult.FAILED) {
            _uiState.update { it.copy(userMessage = SourceDetailUserMessage.OPEN_ORIGINAL_FAILED) }
        }
        _uiState.update { it.copy(openOriginalRequest = null) }
    }

    fun onUserMessageShown() {
        _uiState.update { it.copy(userMessage = null) }
    }

    class Factory(
        private val spaceId: String,
        private val sourceId: String,
        private val getSourceDetailUseCase: GetSourceDetailUseCase,
        private val getSourceOriginalFileUseCase: GetSourceOriginalFileUseCase,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SourceDetailViewModel::class.java)) {
                return SourceDetailViewModel(
                    spaceId,
                    sourceId,
                    getSourceDetailUseCase,
                    getSourceOriginalFileUseCase,
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
