package com.nus.folio.presentation.home

import android.content.Context
import com.nus.folio.R
import com.nus.folio.components.FolioToastStyle
import com.nus.folio.components.FolioToastVisuals

internal fun HomeUserMessage.toHomeToastVisuals(context: Context): FolioToastVisuals {
    val messageRes = when (this) {
        HomeUserMessage.SOURCE_UPDATED -> R.string.toast_source_updated
        HomeUserMessage.SOURCE_DELETED -> R.string.toast_source_deleted
        HomeUserMessage.SOURCE_CREATED -> R.string.toast_source_created
        HomeUserMessage.SOURCE_UPDATE_FAILED -> R.string.toast_source_update_failed
        HomeUserMessage.SOURCE_DELETE_FAILED -> R.string.toast_source_delete_failed
        HomeUserMessage.SOURCE_CREATE_FAILED -> R.string.toast_source_create_failed
        HomeUserMessage.SOURCE_RETRY_FAILED -> R.string.toast_source_retry_failed
        HomeUserMessage.NOTE_UPDATED -> R.string.toast_note_updated
        HomeUserMessage.NOTE_DELETED -> R.string.toast_note_deleted
        HomeUserMessage.NOTE_SAVED -> R.string.toast_note_saved
        HomeUserMessage.NOTE_SAVED_FROM_ASK -> R.string.toast_note_saved_from_ask
        HomeUserMessage.ASK_FEEDBACK_RECORDED -> R.string.home_ask_feedback_recorded
        HomeUserMessage.ASK_NOT_SUPPORTED -> R.string.home_ask_not_supported
        HomeUserMessage.COPY_NOTEBOOK_NOT_SUPPORTED -> R.string.notebook_copy_not_supported
        HomeUserMessage.EXPORT_NOTEBOOK_NOT_SUPPORTED -> R.string.notebook_export_not_supported
        HomeUserMessage.EDIT_NOTE_NOT_SUPPORTED -> R.string.note_edit_not_supported
        HomeUserMessage.CONVERT_NOTE_NOT_SUPPORTED -> R.string.note_convert_not_supported
        HomeUserMessage.DELETE_NOTE_NOT_SUPPORTED -> R.string.note_delete_not_supported
    }
    val descriptionRes = when (this) {
        HomeUserMessage.SOURCE_CREATED -> R.string.toast_source_created_description
        HomeUserMessage.NOTE_SAVED_FROM_ASK -> R.string.toast_note_saved_from_ask_description
        else -> null
    }
    val style = when (this) {
        HomeUserMessage.SOURCE_UPDATED,
        HomeUserMessage.SOURCE_DELETED,
        HomeUserMessage.SOURCE_CREATED,
        HomeUserMessage.NOTE_UPDATED,
        HomeUserMessage.NOTE_DELETED,
        HomeUserMessage.NOTE_SAVED,
        HomeUserMessage.NOTE_SAVED_FROM_ASK,
        -> FolioToastStyle.Success
        HomeUserMessage.ASK_NOT_SUPPORTED,
        HomeUserMessage.ASK_FEEDBACK_RECORDED,
        -> FolioToastStyle.Info
        HomeUserMessage.COPY_NOTEBOOK_NOT_SUPPORTED,
        HomeUserMessage.EXPORT_NOTEBOOK_NOT_SUPPORTED,
        HomeUserMessage.EDIT_NOTE_NOT_SUPPORTED,
        HomeUserMessage.CONVERT_NOTE_NOT_SUPPORTED,
        -> FolioToastStyle.Warning
        HomeUserMessage.SOURCE_UPDATE_FAILED,
        HomeUserMessage.SOURCE_DELETE_FAILED,
        HomeUserMessage.SOURCE_CREATE_FAILED,
        HomeUserMessage.SOURCE_RETRY_FAILED,
        HomeUserMessage.DELETE_NOTE_NOT_SUPPORTED,
        -> FolioToastStyle.Error
    }
    return FolioToastVisuals(
        title = context.getString(messageRes),
        description = descriptionRes?.let(context::getString),
        style = style,
    )
}
