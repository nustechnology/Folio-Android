package com.nus.folio.presentation.sourcedetail

import com.nus.folio.domain.model.SourceDetail
import com.nus.folio.domain.model.SourceFileLocation
import com.nus.folio.domain.model.Source

enum class SourceDetailUserMessage {
    OPEN_ORIGINAL_NO_APP,
    OPEN_ORIGINAL_FAILED,
    SOURCE_UPDATED,
    SOURCE_DELETED,
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
    val editingSource: Source? = null,
    val editingSourceContent: String = "",
    val isUpdatingSource: Boolean = false,
    val deletingSource: Source? = null,
    val isDeletingSource: Boolean = false,
    val sourceDeleted: Boolean = false,
    val userMessage: SourceDetailUserMessage? = null,
    val actionError: String? = null,
)
