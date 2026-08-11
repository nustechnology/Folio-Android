package com.nus.folio.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Tracks whether an editable sheet should confirm before discarding local drafts.
 *
 * Wire into [AnimatedModalSheet] via [confirmDismiss] / [dismissHolder], and host
 * `SheetDiscardConfirmBottomSheet` alongside the sheet.
 */
internal class SheetDiscardProtectionState {
    var hasUnsavedContent by mutableStateOf(false)
    var bypassDiscardConfirm by mutableStateOf(false)
    var showDiscardConfirm by mutableStateOf(false)
    val dismissHolder = SheetDismissHolder()

    fun confirmDismiss(blockWhileBusy: Boolean = false): Boolean {
        return when {
            blockWhileBusy -> false
            bypassDiscardConfirm || !hasUnsavedContent -> true
            else -> {
                showDiscardConfirm = true
                false
            }
        }
    }

    fun discardAndDismiss() {
        showDiscardConfirm = false
        bypassDiscardConfirm = true
        dismissHolder.requestDismiss?.invoke()
    }
}

internal class SheetDismissHolder {
    var requestDismiss: ModalSheetDismiss? = null
}

@Composable
internal fun rememberSheetDiscardProtectionState(): SheetDiscardProtectionState =
    remember { SheetDiscardProtectionState() }
