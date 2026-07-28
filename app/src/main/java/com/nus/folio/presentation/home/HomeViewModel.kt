package com.nus.folio.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nus.folio.domain.usecase.GetGreetingUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val getGreetingUseCase: GetGreetingUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadGreeting()
    }

    fun loadGreeting() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            getGreetingUseCase()
                .onSuccess { greeting ->
                    _uiState.update {
                        it.copy(isLoading = false, message = greeting.message)
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.message ?: "Something went wrong",
                        )
                    }
                }
        }
    }

    class Factory(
        private val getGreetingUseCase: GetGreetingUseCase,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(getGreetingUseCase) as T
        }
    }
}
