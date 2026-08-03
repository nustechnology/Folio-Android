package com.nus.folio.presentation.home.bottomsheet

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
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
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.nus.folio.presentation.home.HomeCardShape
import com.nus.folio.presentation.home.HomeSheetShape
import com.nus.folio.ui.theme.CormorantGaramond
import com.nus.folio.ui.theme.FolioAndroidTheme
import com.nus.folio.ui.theme.HomeCardBackground
import com.nus.folio.ui.theme.HomeCardBorder
import com.nus.folio.ui.theme.HomeHeader
import com.nus.folio.ui.theme.HomeSheetBackground
import com.nus.folio.ui.theme.HomeStatusReadyText
import com.nus.folio.ui.theme.HomeTextPrimary
import com.nus.folio.ui.theme.HomeTextSecondary
import kotlinx.coroutines.delay

private val ProcessingStepLineWidth = 2.dp
private val ProcessingStepSize = 28.dp
private const val ProcessingStepCount = 4
private const val ProcessingStepDelayMs = 700L
private const val ProcessingStartDelayMs = 350L
private const val ProcessingProgressAnimMs = 550

@Composable
internal fun SourceProcessingBottomSheet(
    sourceTitle: String,
    onDismiss: () -> Unit,
    onOpenSource: () -> Unit = {},
    onAsk: () -> Unit = {},
) {
    AnimatedModalSheet(
        onDismiss = onDismiss,
    ) { requestDismiss ->
        AddSourceDragHandle()
        SourceProcessingSheetContent(
            sourceTitle = sourceTitle,
            onOpenSourceClick = { requestDismiss { onOpenSource() } },
            onAskClick = { requestDismiss { onAsk() } },
        )
    }
}

@Composable
private fun SourceProcessingSheetContent(
    sourceTitle: String,
    onOpenSourceClick: () -> Unit,
    onAskClick: () -> Unit,
) {
    val steps = listOf(
        stringResource(R.string.source_processing_step_added),
        stringResource(R.string.source_processing_step_extract),
        stringResource(R.string.source_processing_step_index),
        stringResource(R.string.source_processing_step_ready),
    )
    var completedStepCount by remember { mutableIntStateOf(0) }
    val isFinished = completedStepCount >= ProcessingStepCount
    val animatedProgress by animateFloatAsState(
        targetValue = completedStepCount / ProcessingStepCount.toFloat(),
        animationSpec = tween(durationMillis = ProcessingProgressAnimMs),
        label = "sourceProcessingProgress",
    )
    val statusLabel = if (isFinished) {
        steps.last()
    } else {
        steps[completedStepCount.coerceIn(0, steps.lastIndex)]
    }

    LaunchedEffect(Unit) {
        delay(ProcessingStartDelayMs)
        for (step in 1..ProcessingStepCount) {
            completedStepCount = step
            delay(ProcessingStepDelayMs)
        }
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
                val isCompleted = index < completedStepCount
                val isCurrent = !isFinished && index == completedStepCount
                ProcessingStepRow(
                    stepNumber = index + 1,
                    label = label,
                    isCompleted = isCompleted,
                    isCurrent = isCurrent,
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
                AnimatedContent(
                    targetState = statusLabel,
                    transitionSpec = {
                        fadeIn(tween(220)) togetherWith fadeOut(tween(160))
                    },
                    label = "sourceProcessingStatus",
                ) { label ->
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        color = HomeTextSecondary,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AddSourceSubmitButton(
                    enabled = isFinished,
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
    }
}

@Composable
private fun ProcessingStepRow(
    stepNumber: Int,
    label: String,
    isCompleted: Boolean,
    isCurrent: Boolean,
    showConnector: Boolean,
) {
    val connectorColor by animateColorAsState(
        targetValue = if (isCompleted) HomeStatusReadyText else HomeCardBorder,
        animationSpec = tween(durationMillis = 320),
        label = "processingConnector",
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
            color = HomeTextPrimary,
        )
    }
}

@Composable
private fun ProcessingStepIndicator(
    stepNumber: Int,
    isCompleted: Boolean,
    isCurrent: Boolean,
) {
    val state = when {
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
                    onOpenSourceClick = {},
                    onAskClick = {},
                )
            }
        }
    }
}
