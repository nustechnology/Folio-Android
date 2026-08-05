package com.nus.folio.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nus.folio.R
import com.nus.folio.ui.theme.FolioAndroidTheme
import com.nus.folio.ui.theme.HomeCardBackground
import com.nus.folio.ui.theme.HomeSearchField
import com.nus.folio.ui.theme.HomeStatusFailedBackground
import com.nus.folio.ui.theme.HomeStatusFailedText
import com.nus.folio.ui.theme.HomeStatusProcessingBackground
import com.nus.folio.ui.theme.HomeStatusProcessingText
import com.nus.folio.ui.theme.HomeStatusReadyBackground
import com.nus.folio.ui.theme.HomeStatusReadyText
import com.nus.folio.ui.theme.HomeTextPrimary
import com.nus.folio.ui.theme.HomeTextSecondary
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val FolioToastShape = RoundedCornerShape(12.dp)
private const val DefaultToastDurationMillis = 3_000L
private val ToastDismissDragThreshold = 56.dp
private const val ToastDismissFlingVelocity = 800f

enum class FolioToastStyle {
    Info,
    Warning,
    Success,
    Error,
}

data class FolioToastVisuals(
    /** Primary line shown at the top. */
    val title: String,
    val style: FolioToastStyle,
    /** Supporting line shown directly under [title]. */
    val description: String? = null,
)

private data class FolioToastColors(
    val background: Color,
    val borderColor: Color?,
    val contentColor: Color,
    val iconBackground: Color,
    val iconTint: Color,
    val iconRes: Int,
)

@Stable
class FolioToastHostState {
    var currentToast by mutableStateOf<FolioToastVisuals?>(null)
        private set

    private val mutex = Mutex()
    private var showGeneration = 0

    fun dismiss() {
        showGeneration++
        currentToast = null
    }

    suspend fun showToast(
        title: String,
        style: FolioToastStyle = FolioToastStyle.Info,
        description: String? = null,
        durationMillis: Long = DefaultToastDurationMillis,
    ) {
        val generation = mutex.withLock {
            showGeneration++
            currentToast = FolioToastVisuals(
                title = title,
                style = style,
                description = description,
            )
            showGeneration
        }
        delay(durationMillis)
        mutex.withLock {
            if (showGeneration == generation) {
                currentToast = null
            }
        }
    }

    suspend fun showToast(visuals: FolioToastVisuals, durationMillis: Long = DefaultToastDurationMillis) {
        showToast(
            title = visuals.title,
            style = visuals.style,
            description = visuals.description,
            durationMillis = durationMillis,
        )
    }
}

@Composable
fun rememberFolioToastHostState(): FolioToastHostState = remember { FolioToastHostState() }

@Composable
fun FolioToastHost(
    hostState: FolioToastHostState,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val dismissThresholdPx = with(density) { ToastDismissDragThreshold.toPx() }
    val dragOffsetY = remember { Animatable(0f) }
    val currentToast = hostState.currentToast
    // Retain last non-null toast so AnimatedVisibility can animate exit content.
    var displayedToast by remember { mutableStateOf(currentToast) }
    if (currentToast != null) {
        displayedToast = currentToast
    }
    val draggableState = rememberDraggableState { delta ->
        scope.launch {
            // Toast sits at the top — only drag upward to dismiss.
            dragOffsetY.snapTo((dragOffsetY.value + delta).coerceAtMost(0f))
        }
    }

    LaunchedEffect(currentToast) {
        dragOffsetY.snapTo(0f)
    }

    AnimatedVisibility(
        visible = currentToast != null,
        modifier = modifier.fillMaxWidth(),
        enter = fadeIn(animationSpec = tween(220)) +
            slideInVertically(animationSpec = tween(280)) { -it },
        exit = fadeOut(animationSpec = tween(180)) +
            slideOutVertically(animationSpec = tween(220)) { -it },
    ) {
        displayedToast?.let { toast ->
            FolioToast(
                title = toast.title,
                description = toast.description,
                style = toast.style,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .offset { IntOffset(0, dragOffsetY.value.roundToInt()) }
                    .draggable(
                        state = draggableState,
                        orientation = Orientation.Vertical,
                        onDragStopped = { velocity ->
                            scope.launch {
                                if (
                                    dragOffsetY.value <= -dismissThresholdPx ||
                                    velocity < -ToastDismissFlingVelocity
                                ) {
                                    hostState.dismiss()
                                } else {
                                    dragOffsetY.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(),
                                    )
                                }
                            }
                        },
                    )
                    .semantics(mergeDescendants = true) {
                        liveRegion = LiveRegionMode.Polite
                    },
            )
        }
    }
}

private fun FolioToastStyle.colors(): FolioToastColors = when (this) {
    FolioToastStyle.Info -> FolioToastColors(
        background = HomeCardBackground,
        borderColor = HomeSearchField.copy(alpha = 0.25f),
        contentColor = HomeTextPrimary,
        iconBackground = Color(0xFFE8EEF2),
        iconTint = HomeSearchField,
        iconRes = R.drawable.ic_toast_info,
    )
    FolioToastStyle.Warning -> FolioToastColors(
        background = HomeCardBackground,
        borderColor = HomeStatusProcessingText.copy(alpha = 0.35f),
        contentColor = HomeTextPrimary,
        iconBackground = HomeStatusProcessingBackground,
        iconTint = HomeStatusProcessingText,
        iconRes = R.drawable.ic_toast_warning,
    )
    FolioToastStyle.Success -> FolioToastColors(
        background = Color.White,
        borderColor = HomeStatusReadyText.copy(alpha = 0.35f),
        contentColor = HomeTextPrimary,
        iconBackground = HomeStatusReadyBackground,
        iconTint = HomeStatusReadyText,
        iconRes = R.drawable.ic_check,
    )
    FolioToastStyle.Error -> FolioToastColors(
        background = HomeCardBackground,
        borderColor = HomeStatusFailedText.copy(alpha = 0.35f),
        contentColor = HomeTextPrimary,
        iconBackground = HomeStatusFailedBackground,
        iconTint = HomeStatusFailedText,
        iconRes = R.drawable.ic_toast_error,
    )
}

@Composable
internal fun FolioToast(
    title: String,
    style: FolioToastStyle,
    modifier: Modifier = Modifier,
    description: String? = null,
) {
    val colors = style.colors()
    val hasDescription = !description.isNullOrBlank()

    Row(
        modifier = modifier
            .shadow(
                elevation = 10.dp,
                shape = FolioToastShape,
                ambientColor = Color.Black.copy(alpha = 0.18f),
                spotColor = Color.Black.copy(alpha = 0.18f),
            )
            .clip(FolioToastShape)
            .background(colors.background)
            .then(
                if (colors.borderColor != null) {
                    Modifier.border(1.dp, colors.borderColor, FolioToastShape)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = if (hasDescription) Alignment.Top else Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .padding(top = if (hasDescription) 2.dp else 0.dp)
                .size(32.dp)
                .clip(CircleShape)
                .background(colors.iconBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(colors.iconRes),
                contentDescription = null,
                tint = colors.iconTint,
                modifier = Modifier.size(18.dp),
            )
        }
        // Title on top, description on the next line — always stacked vertically.
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(start = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = title,
                modifier = Modifier.fillMaxWidth(),
                color = colors.contentColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 20.sp,
            )
            if (hasDescription) {
                Text(
                    text = description.orEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    color = HomeTextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 18.sp,
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF7F1E6)
@Composable
private fun FolioToastInfoPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        FolioToast(
            title = "Ask is coming soon.",
            style = FolioToastStyle.Info,
            modifier = Modifier.padding(20.dp),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF7F1E6)
@Composable
private fun FolioToastWarningPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        FolioToast(
            title = "Adding sources is not supported yet.",
            style = FolioToastStyle.Warning,
            modifier = Modifier.padding(20.dp),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF7F1E6)
@Composable
private fun FolioToastSuccessPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        FolioToast(
            title = "Source added",
            description = "We're processing it so you can ask and cite soon.",
            style = FolioToastStyle.Success,
            modifier = Modifier.padding(20.dp),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF7F1E6)
@Composable
private fun FolioToastErrorPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        FolioToast(
            title = "Could not delete source. Please try again.",
            style = FolioToastStyle.Error,
            modifier = Modifier.padding(20.dp),
        )
    }
}
