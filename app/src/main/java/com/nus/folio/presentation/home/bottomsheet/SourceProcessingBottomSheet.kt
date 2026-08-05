package com.nus.folio.presentation.home.bottomsheet

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nus.folio.R
import com.nus.folio.components.AnimatedModalSheet
import com.nus.folio.domain.model.SourceProcessingEvent
import com.nus.folio.domain.model.SourceProcessingState
import com.nus.folio.presentation.home.HomeCardShape
import com.nus.folio.presentation.home.HomeSheetShape
import com.nus.folio.ui.theme.CormorantGaramond
import com.nus.folio.ui.theme.FolioAndroidTheme
import com.nus.folio.ui.theme.HomeCardBackground
import com.nus.folio.ui.theme.HomeCardBorder
import com.nus.folio.ui.theme.HomeHeader
import com.nus.folio.ui.theme.HomeSheetBackground
import com.nus.folio.ui.theme.HomeStatusFailedBackground
import com.nus.folio.ui.theme.HomeStatusFailedText
import com.nus.folio.ui.theme.HomeStatusReadyText
import com.nus.folio.ui.theme.HomeTextPrimary
import com.nus.folio.ui.theme.HomeTextSecondary

private val ProcessingStepLineWidth = 2.dp
private val ProcessingStepSize = 28.dp
private const val ProcessingStepCount = 4
private const val ProcessingProgressAnimMs = 550

@Composable
internal fun SourceProcessingBottomSheet(
    sourceTitle: String,
    progress: Int,
    state: SourceProcessingState?,
    onDismiss: () -> Unit,
    onOpenSource: () -> Unit = {},
    onAsk: () -> Unit = {},
    onRetry: () -> Unit = {},
) {
    AnimatedModalSheet(
        onDismiss = onDismiss,
    ) { requestDismiss ->
        AddSourceDragHandle()
        SourceProcessingSheetContent(
            sourceTitle = sourceTitle,
            progress = progress,
            state = state,
            onOpenSourceClick = { requestDismiss { onOpenSource() } },
            onAskClick = { requestDismiss { onAsk() } },
            onRetryClick = onRetry,
        )
    }
}

@Composable
private fun SourceProcessingSheetContent(
    sourceTitle: String,
    progress: Int,
    state: SourceProcessingState?,
    onOpenSourceClick: () -> Unit,
    onAskClick: () -> Unit,
    onRetryClick: () -> Unit,
) {
    val steps = listOf(
        stringResource(R.string.source_processing_step_added),
        stringResource(R.string.source_processing_step_extract),
        stringResource(R.string.source_processing_step_index),
        stringResource(R.string.source_processing_step_ready),
    )
    val completedStepCount = SourceProcessingEvent(
        sourceId = "",
        state = state ?: SourceProcessingState.ADDED,
        progress = progress,
    ).completedStepCount()
    val isFinished = completedStepCount >= ProcessingStepCount
    val isFailed = state == SourceProcessingState.FAILED
    val showReadyActions = state == SourceProcessingState.READY
    val showRetryAction = isFailed
    val animatedProgress by animateFloatAsState(
        targetValue = (progress.coerceIn(0, 100) / 100f),
        animationSpec = tween(durationMillis = ProcessingProgressAnimMs),
        label = "sourceProcessingProgress",
    )
    val statusLabel = when {
        isFailed -> stringResource(R.string.source_processing_step_failed)
        isFinished -> steps.last()
        else -> null
    }

    Column {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.source_processing_title),
            fontFamily = CormorantGaramond,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            color = HomeTextPrimary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = sourceTitle,
            fontSize = 14.sp,
            color = HomeTextSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 20.sp,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(HomeCardShape)
                .background(HomeCardBackground)
                .border(1.dp, HomeCardBorder, HomeCardShape)
                .padding(20.dp),
        ) {
            steps.forEachIndexed { index, label ->
                val isFailedStep = isFailed && index == steps.lastIndex
                val isCompleted = index < completedStepCount && !isFailedStep
                val isCurrent = !isFinished && !isFailed && index == completedStepCount
                ProcessingStepRow(
                    stepNumber = index + 1,
                    label = if (isFailedStep) {
                        stringResource(R.string.source_processing_step_failed)
                    } else {
                        label
                    },
                    isCompleted = isCompleted,
                    isCurrent = isCurrent,
                    isFailed = isFailedStep,
                    showConnector = index < steps.lastIndex,
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(
                        R.string.source_processing_percent,
                        (animatedProgress * 100).toInt(),
                    ),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HomeTextPrimary,
                )
                if (statusLabel == null) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = HomeHeader,
                        strokeWidth = 2.dp,
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = HomeHeader,
                trackColor = HomeCardBorder,
                strokeCap = StrokeCap.Round,
            )
            Spacer(modifier = Modifier.height(20.dp))
            AnimatedVisibility(
                visible = showReadyActions,
                enter = fadeIn(tween(280)) +
                    expandVertically(animationSpec = tween(320)) +
                    slideInVertically(
                        animationSpec = tween(320),
                        initialOffsetY = { it / 3 },
                    ),
                exit = fadeOut(tween(160)) + shrinkVertically(animationSpec = tween(200)),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AddSourceSubmitButton(
                        enabled = true,
                        onClick = onOpenSourceClick,
                        labelRes = R.string.source_processing_open,
                        modifier = Modifier.weight(1f),
                    )
                    AddSourceCancelButton(
                        onClick = onAskClick,
                        labelRes = R.string.home_ask_submit,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            AnimatedVisibility(
                visible = showRetryAction,
                enter = fadeIn(tween(280)) +
                    expandVertically(animationSpec = tween(320)) +
                    slideInVertically(
                        animationSpec = tween(320),
                        initialOffsetY = { it / 3 },
                    ),
                exit = fadeOut(tween(160)) + shrinkVertically(animationSpec = tween(200)),
            ) {
                AddSourceSubmitButton(
                    enabled = true,
                    onClick = onRetryClick,
                    labelRes = R.string.home_retry,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ProcessingStepRow(
    stepNumber: Int,
    label: String,
    isCompleted: Boolean,
    isCurrent: Boolean,
    isFailed: Boolean,
    showConnector: Boolean,
) {
    val connectorColor by animateColorAsState(
        targetValue = when {
            isFailed -> HomeStatusFailedText
            isCompleted -> HomeStatusReadyText
            else -> HomeCardBorder
        },
        animationSpec = tween(durationMillis = 320),
        label = "processingConnector",
    )
    val labelColor by animateColorAsState(
        targetValue = if (isFailed) HomeStatusFailedText else HomeTextPrimary,
        animationSpec = tween(durationMillis = 320),
        label = "processingStepLabel",
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(ProcessingStepSize),
        ) {
            ProcessingStepIndicator(
                stepNumber = stepNumber,
                isCompleted = isCompleted,
                isCurrent = isCurrent,
                isFailed = isFailed,
            )
            if (showConnector) {
                Box(
                    modifier = Modifier
                        .width(ProcessingStepLineWidth)
                        .height(20.dp)
                        .background(connectorColor),
                )
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = label,
            modifier = Modifier.padding(top = 4.dp),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = labelColor,
        )
    }
}

@Composable
private fun ProcessingStepIndicator(
    stepNumber: Int,
    isCompleted: Boolean,
    isCurrent: Boolean,
    isFailed: Boolean,
) {
    val state = when {
        isFailed -> StepVisualState.Failed
        isCompleted -> StepVisualState.Completed
        isCurrent -> StepVisualState.Current
        else -> StepVisualState.Pending
    }

    AnimatedContent(
        targetState = state,
        transitionSpec = {
            (
                fadeIn(tween(220)) + scaleIn(
                    initialScale = 0.82f,
                    animationSpec = tween(260),
                )
                ) togetherWith fadeOut(tween(140))
        },
        label = "processingStepIndicator",
    ) { visualState ->
        when (visualState) {
            StepVisualState.Completed -> {
                Box(
                    modifier = Modifier
                        .size(ProcessingStepSize)
                        .clip(CircleShape)
                        .background(HomeStatusReadyText),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        tint = HomeCardBackground,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            StepVisualState.Failed -> {
                Box(
                    modifier = Modifier
                        .size(ProcessingStepSize)
                        .clip(CircleShape)
                        .background(HomeStatusFailedBackground)
                        .border(1.dp, HomeStatusFailedText, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_toast_error),
                        contentDescription = null,
                        tint = HomeStatusFailedText,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            StepVisualState.Current -> {
                Box(
                    modifier = Modifier
                        .size(ProcessingStepSize)
                        .clip(CircleShape)
                        .border(2.dp, HomeHeader, CircleShape)
                        .background(HomeCardBackground),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stepNumber.toString(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = HomeHeader,
                    )
                }
            }
            StepVisualState.Pending -> {
                Box(
                    modifier = Modifier
                        .size(ProcessingStepSize)
                        .clip(CircleShape)
                        .border(1.dp, HomeCardBorder, CircleShape)
                        .background(HomeCardBackground),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stepNumber.toString(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = HomeTextSecondary,
                    )
                }
            }
        }
    }
}

private enum class StepVisualState {
    Pending,
    Current,
    Completed,
    Failed,
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852, backgroundColor = 0xFFF7F1E6)
@Composable
private fun SourceProcessingSheetContentPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(HomeSheetBackground, HomeSheetShape)
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 20.dp),
            ) {
                AddSourceDragHandle()
                SourceProcessingSheetContent(
                    sourceTitle = "Care Technology Adoption Survey 2026",
                    progress = 25,
                    state = SourceProcessingState.EXTRACTING_TEXT,
                    onOpenSourceClick = {},
                    onAskClick = {},
                    onRetryClick = {},
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852, backgroundColor = 0xFFF7F1E6, name = "Processing — failed")
@Composable
private fun SourceProcessingSheetFailedPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(HomeSheetBackground, HomeSheetShape)
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 20.dp),
            ) {
                AddSourceDragHandle()
                SourceProcessingSheetContent(
                    sourceTitle = "Care Technology Adoption Survey 2026",
                    progress = 100,
                    state = SourceProcessingState.FAILED,
                    onOpenSourceClick = {},
                    onAskClick = {},
                    onRetryClick = {},
                )
            }
        }
    }
}
