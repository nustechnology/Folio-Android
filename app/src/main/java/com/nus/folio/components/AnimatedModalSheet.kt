package com.nus.folio.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.nus.folio.ui.theme.FolioSheetShape
import com.nus.folio.ui.theme.HomeSheetBackground
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private val ModalSheetScrim = Color.Black.copy(alpha = 0.32f)
private const val ModalSheetEnterMillis = 280
private const val ModalSheetExitMillis = 220
private val ModalSheetDismissDragThreshold = 120.dp
private const val ModalSheetDismissFlingVelocity = 1200f

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
 *
 * Drag the sheet downward to dismiss; scrollable content can still scroll when the sheet
 * is fully expanded.
 */
@Composable
internal fun AnimatedModalSheet(
    onDismiss: () -> Unit,
    dismissOnScrimClick: Boolean = false,
    /**
     * Called before a user-initiated dismiss (back, scrim, drag, cancel).
     * Return false to keep the sheet open (e.g. show a discard confirmation).
     * Programmatic dismiss with a non-null [ModalSheetDismiss] after-action always proceeds.
     */
    confirmDismiss: () -> Boolean = { true },
    contentWindowInsets: WindowInsets = WindowInsets.navigationBars,
    content: @Composable (requestDismiss: ModalSheetDismiss) -> Unit,
) {
    val visibleState = remember {
        MutableTransitionState(false).apply { targetState = true }
    }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val dismissThresholdPx = with(density) { ModalSheetDismissDragThreshold.toPx() }
    val dragOffsetY = remember { Animatable(0f) }

    val requestDismiss = ModalSheetDismiss { after ->
        if (!visibleState.targetState) return@ModalSheetDismiss
        if (after == null && !confirmDismiss()) {
            scope.launch {
                dragOffsetY.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(),
                )
            }
            return@ModalSheetDismiss
        }
        pendingAction = after
        visibleState.targetState = false
    }

    suspend fun settleDragDismiss() {
        if (dragOffsetY.value >= dismissThresholdPx) {
            requestDismiss()
        } else {
            dragOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = spring(),
            )
        }
    }

    val dragConnection = remember(dismissThresholdPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta < 0 && dragOffsetY.value > 0f) {
                    val previousOffset = dragOffsetY.value
                    val newOffset = (previousOffset + delta).coerceAtLeast(0f)
                    val consumed = newOffset - previousOffset
                    scope.launch { dragOffsetY.snapTo(newOffset) }
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                val delta = available.y
                if (delta > 0) {
                    val previousOffset = dragOffsetY.value
                    scope.launch { dragOffsetY.snapTo(previousOffset + delta) }
                    return Offset(0f, delta)
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (dragOffsetY.value >= dismissThresholdPx || available.y > ModalSheetDismissFlingVelocity) {
                    requestDismiss()
                } else if (dragOffsetY.value > 0f) {
                    dragOffsetY.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(),
                    )
                }
                return available
            }
        }
    }

    val draggableState = rememberDraggableState { delta ->
        scope.launch {
            dragOffsetY.snapTo((dragOffsetY.value + delta).coerceAtLeast(0f))
        }
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
                    .offset { IntOffset(0, dragOffsetY.value.roundToInt()) }
                    .nestedScroll(dragConnection)
                    .draggable(
                        state = draggableState,
                        orientation = Orientation.Vertical,
                        onDragStopped = { velocity ->
                            scope.launch {
                                if (
                                    dragOffsetY.value >= dismissThresholdPx ||
                                    velocity > ModalSheetDismissFlingVelocity
                                ) {
                                    requestDismiss()
                                } else {
                                    settleDragDismiss()
                                }
                            }
                        },
                    )
                    .background(HomeSheetBackground, FolioSheetShape)
                    .windowInsetsPadding(contentWindowInsets)
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 20.dp),
            ) {
                content(requestDismiss)
            }
        }
    }
}
