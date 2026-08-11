package com.nus.folio.presentation.home.notebook

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type

internal enum class NotebookShortcutAction {
    BOLD,
    ITALIC,
    HEADING_1,
    HEADING_2,
    HEADING_3,
    BULLET_LIST,
    ORDERED_LIST,
    BLOCKQUOTE,
    UNDO,
    REDO,
}

internal fun notebookShortcutAction(event: KeyEvent): NotebookShortcutAction? {
    if (event.type != KeyEventType.KeyDown) return null
    val modifier = event.isCtrlPressed || event.isMetaPressed
    if (!modifier) return null

    return when (event.key) {
        Key.B -> NotebookShortcutAction.BOLD
        Key.I -> NotebookShortcutAction.ITALIC
        Key.Z -> if (event.isShiftPressed) NotebookShortcutAction.REDO else NotebookShortcutAction.UNDO
        Key.Y -> NotebookShortcutAction.REDO
        Key.Eight, Key.NumPad8 -> if (event.isShiftPressed) NotebookShortcutAction.BULLET_LIST else null
        Key.Seven, Key.NumPad7 -> if (event.isShiftPressed) NotebookShortcutAction.ORDERED_LIST else null
        Key.Nine, Key.NumPad9 -> if (event.isShiftPressed) NotebookShortcutAction.BLOCKQUOTE else null
        Key.One, Key.NumPad1 -> if (event.isAltPressed) NotebookShortcutAction.HEADING_1 else null
        Key.Two, Key.NumPad2 -> if (event.isAltPressed) NotebookShortcutAction.HEADING_2 else null
        Key.Three, Key.NumPad3 -> if (event.isAltPressed) NotebookShortcutAction.HEADING_3 else null
        else -> null
    }
}
