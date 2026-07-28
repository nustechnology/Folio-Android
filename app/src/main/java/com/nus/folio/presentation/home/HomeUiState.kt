package com.nus.folio.presentation.home

data class HomeUiState(
    val isLoading: Boolean = false,
    val message: String = "",
    val error: String? = null,
)
