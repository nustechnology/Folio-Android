package com.nus.folio.presentation.sourcedetail

import com.nus.folio.domain.model.SourceDetail
import com.nus.folio.domain.model.SourceFileLocation

enum class SourceDetailUserMessage {
    OPEN_ORIGINAL_NO_APP,
    OPEN_ORIGINAL_FAILED,
}

data class SourceDetailUiState(
    val isLoading: Boolean = false,
    val isContentLoading: Boolean = false,
    val isRetrying: Boolean = false,
    val error: String? = null,
    val detail: SourceDetail? = null,
    val selectedSheetIndex: Int = 0,
    val previewUrl: String? = null,
    val openOriginalRequest: SourceFileLocation? = null,
    val userMessage: SourceDetailUserMessage? = null,
    val actionError: String? = null,
)
