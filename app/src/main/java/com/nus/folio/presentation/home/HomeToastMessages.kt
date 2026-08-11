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
        HomeUserMessage.SOURCE_FILE_SELECTED -> R.string.toast_source_file_selected
        HomeUserMessage.SOURCE_DELETE_FAILED -> R.string.toast_source_delete_failed
        HomeUserMessage.SOURCE_CREATE_FAILED -> R.string.toast_source_create_failed
        HomeUserMessage.SOURCE_RETRY_FAILED -> R.string.toast_source_retry_failed
        HomeUserMessage.NOTE_UPDATED -> R.string.toast_note_updated
        HomeUserMessage.NOTE_DELETED -> R.string.toast_note_deleted
        HomeUserMessage.NOTE_SAVED -> R.string.toast_note_saved
        HomeUserMessage.NOTE_SAVED_FROM_ASK -> R.string.toast_note_saved_from_ask
        HomeUserMessage.ASK_FEEDBACK_RECORDED -> R.string.home_ask_feedback_recorded
        HomeUserMessage.NOTEBOOK_COPIED -> R.string.notebook_copied_to_clipboard
        HomeUserMessage.NOTEBOOK_EXPORTED -> R.string.notebook_exported
    }
    val descriptionRes = when (this) {
        HomeUserMessage.SOURCE_CREATED -> R.string.toast_source_created_description
        HomeUserMessage.NOTE_SAVED -> R.string.toast_note_saved_description
        HomeUserMessage.NOTE_SAVED_FROM_ASK -> R.string.toast_note_saved_from_ask_description
        else -> null
    }
    val style = when (this) {
        HomeUserMessage.SOURCE_UPDATED,
        HomeUserMessage.SOURCE_DELETED,
        HomeUserMessage.SOURCE_CREATED,
        HomeUserMessage.SOURCE_FILE_SELECTED,
        HomeUserMessage.NOTE_UPDATED,
        HomeUserMessage.NOTE_DELETED,
        HomeUserMessage.NOTE_SAVED,
        HomeUserMessage.NOTE_SAVED_FROM_ASK,
        HomeUserMessage.NOTEBOOK_COPIED,
        HomeUserMessage.NOTEBOOK_EXPORTED,
        -> FolioToastStyle.Success
        HomeUserMessage.ASK_FEEDBACK_RECORDED,
        -> FolioToastStyle.Info
        HomeUserMessage.SOURCE_DELETE_FAILED,
        HomeUserMessage.SOURCE_CREATE_FAILED,
        HomeUserMessage.SOURCE_RETRY_FAILED,
        -> FolioToastStyle.Error
    }
    return FolioToastVisuals(
        title = context.getString(messageRes),
        description = descriptionRes?.let(context::getString),
        style = style,
    )
}
