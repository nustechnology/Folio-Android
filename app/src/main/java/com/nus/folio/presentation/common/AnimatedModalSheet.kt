package com.nus.folio.presentation.common

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nus.folio.presentation.home.HomeSheetShape
import com.nus.folio.ui.theme.HomeSheetBackground

private val ModalSheetScrim = Color.Black.copy(alpha = 0.32f)
private const val ModalSheetEnterMillis = 280
private const val ModalSheetExitMillis = 220

/**
 * Dismiss handle for [AnimatedModalSheet].
 *
 * Call [invoke] (or as `requestDismiss()`) to animate out. Optionally pass [after]
 * to run once the exit finishes, before the sheet's [AnimatedModalSheet.onDismiss].
 */
fun interface ModalSheetDismiss {
    fun dismiss(after: (() -> Unit)?)

    operator fun invoke(after: (() -> Unit)? = null) = dismiss(after)
}

/**
 * Shared modal bottom-sheet shell with scrim fade + sheet slide animations.
 *
 * Call [ModalSheetDismiss] to animate out, optionally running an action once the exit
 * finishes, then [onDismiss] so the parent can remove the sheet from composition.
 */
@Composable
internal fun AnimatedModalSheet(
    onDismiss: () -> Unit,
    dismissOnScrimClick: Boolean = false,
    contentWindowInsets: WindowInsets = WindowInsets.navigationBars,
    content: @Composable (requestDismiss: ModalSheetDismiss) -> Unit,
) {
    val visibleState = remember {
        MutableTransitionState(false).apply { targetState = true }
    }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val requestDismiss = ModalSheetDismiss { after ->
        if (!visibleState.targetState) return@ModalSheetDismiss
        pendingAction = after
        visibleState.targetState = false
    }

    LaunchedEffect(visibleState.currentState, visibleState.targetState, visibleState.isIdle) {
        if (!visibleState.currentState && !visibleState.targetState && visibleState.isIdle) {
            val action = pendingAction
            pendingAction = null
            action?.invoke()
            onDismiss()
        }
    }

    BackHandler { requestDismiss() }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visibleState = visibleState,
            enter = fadeIn(
                animationSpec = tween(ModalSheetEnterMillis, easing = FastOutSlowInEasing),
            ),
            exit = fadeOut(
                animationSpec = tween(ModalSheetExitMillis, easing = FastOutSlowInEasing),
            ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ModalSheetScrim)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            if (dismissOnScrimClick) requestDismiss()
                        },
                    ),
            )
        }
        AnimatedVisibility(
            visibleState = visibleState,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(
                animationSpec = tween(ModalSheetEnterMillis, easing = FastOutSlowInEasing),
                initialOffsetY = { fullHeight -> fullHeight },
            ) + fadeIn(
                animationSpec = tween(ModalSheetEnterMillis, easing = FastOutSlowInEasing),
            ),
            exit = slideOutVertically(
                animationSpec = tween(ModalSheetExitMillis, easing = FastOutSlowInEasing),
                targetOffsetY = { fullHeight -> fullHeight },
            ) + fadeOut(
                animationSpec = tween(ModalSheetExitMillis, easing = FastOutSlowInEasing),
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HomeSheetBackground, HomeSheetShape)
                    .windowInsetsPadding(contentWindowInsets)
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 20.dp),
            ) {
                content(requestDismiss)
            }
        }
    }
}
