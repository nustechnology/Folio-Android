package com.nus.folio.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

/** Returns a callback that clears focus and hides the IME. Safe to invoke from event handlers. */
@Composable
fun rememberDismissKeyboard(): () -> Unit {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    return remember(focusManager, keyboardController) {
        {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }
}

/**
 * Returns [action] wrapped so the keyboard is dismissed immediately before it runs.
 * Use when opening a bottom sheet or other overlay that should not compete with the IME.
 */
@Composable
fun rememberDismissKeyboardThen(action: () -> Unit): () -> Unit {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    return remember(focusManager, keyboardController, action) {
        {
            focusManager.clearFocus()
            keyboardController?.hide()
            action()
        }
    }
}

@Composable
fun <T> rememberDismissKeyboardThen(action: (T) -> Unit): (T) -> Unit {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    return remember(focusManager, keyboardController, action) {
        { value ->
            focusManager.clearFocus()
            keyboardController?.hide()
            action(value)
        }
    }
}

/**
 * Clears the focused text field when the user taps outside of it.
 * Interactive children still receive taps first.
 */
fun Modifier.dismissKeyboardOnTapOutside(): Modifier = composed {
    val focusManager = LocalFocusManager.current
    pointerInput(focusManager) {
        detectTapGestures(onTap = { focusManager.clearFocus() })
    }
}
