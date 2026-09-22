package com.nus.folio.presentation.home.pane

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nus.folio.R
import com.nus.folio.domain.model.AskCitation
import com.nus.folio.presentation.home.AskButtonBackground
import com.nus.folio.presentation.home.AskFeedback
import com.nus.folio.presentation.home.AskFeedbackChoiceBackground
import com.nus.folio.presentation.home.AskFeedbackChoiceSelectedBackground
import com.nus.folio.presentation.home.AskFeedbackNoSelectedBackground
import com.nus.folio.presentation.home.AskFeedbackNoSelectedText
import com.nus.folio.presentation.home.AskMessage
import com.nus.folio.presentation.home.AskMessageRole
import com.nus.folio.presentation.home.AskSourceChipBackground
import com.nus.folio.presentation.home.AskSourceChipShape
import com.nus.folio.presentation.home.AskSubmitShape
import com.nus.folio.presentation.home.AskSuggestionShape
import com.nus.folio.presentation.home.CitedAnswerContent
import com.nus.folio.presentation.home.HomeSourceFilterChipSelected
import com.nus.folio.ui.theme.HomeCardBackground
import com.nus.folio.ui.theme.HomeCardBorder
import com.nus.folio.ui.theme.HomeHeader
import com.nus.folio.ui.theme.HomeStatusProcessingBackground
import com.nus.folio.ui.theme.HomeStatusProcessingText
import com.nus.folio.ui.theme.HomeTextPrimary
import com.nus.folio.ui.theme.HomeTextSecondary

private const val AskUserBubbleEnterMillis = 360

@Composable
internal fun AskMessageBubble(
    message: AskMessage,
    userAvatarLabel: String,
    inputTopInWindow: Float,
    animateEnter: Boolean,
    isSavingAsNote: Boolean,
    onAskStop: () -> Unit,
    onSaveAsNote: (messageId: String) -> Unit,
    onFeedback: (messageId: String, useful: Boolean) -> Unit,
    onCitationClick: (AskCitation) -> Unit,
    onUserEnterFinished: (messageId: String) -> Unit,
) {
    when (message.role) {
        AskMessageRole.USER -> AskUserMessageBubble(
            content = message.content,
            avatarLabel = userAvatarLabel,
            messageId = message.id,
            inputTopInWindow = inputTopInWindow,
            animateEnter = animateEnter,
            onEnterFinished = { onUserEnterFinished(message.id) },
        )
        AskMessageRole.ASSISTANT -> AskAssistantMessageCard(
            message = message,
            isSavingAsNote = isSavingAsNote,
            onAskStop = onAskStop,
            onSaveAsNote = onSaveAsNote,
            onFeedback = onFeedback,
            onCitationClick = onCitationClick,
        )
    }
}

@Composable
internal fun AskUserMessageBubble(
    content: String,
    avatarLabel: String,
    messageId: String,
    inputTopInWindow: Float,
    animateEnter: Boolean,
    onEnterFinished: () -> Unit,
) {
    val appearance = remember(messageId, animateEnter) {
        Animatable(if (animateEnter) 0f else 1f)
    }
    var travelY by remember(messageId) { mutableFloatStateOf(0f) }
    var measured by remember(messageId, animateEnter) { mutableStateOf(!animateEnter) }
    val minTravelPx = with(LocalDensity.current) { 96.dp.toPx() }
    val enterFinishedSignaled = remember(messageId) { booleanArrayOf(false) }
    fun signalEnterFinished() {
        if (enterFinishedSignaled[0]) return
        enterFinishedSignaled[0] = true
        onEnterFinished()
    }

    DisposableEffect(messageId) {
        onDispose { signalEnterFinished() }
    }

    LaunchedEffect(messageId, animateEnter, measured) {
        when {
            !animateEnter -> signalEnterFinished()
            measured -> {
                appearance.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = AskUserBubbleEnterMillis,
                        easing = FastOutSlowInEasing,
                    ),
                )
                signalEnterFinished()
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                if (animateEnter && !measured) {
                    val bubbleTop = coordinates.positionInWindow().y
                    travelY = if (inputTopInWindow.isNaN()) {
                        minTravelPx
                    } else {
                        (inputTopInWindow - bubbleTop).coerceAtLeast(minTravelPx)
                    }
                    measured = true
                }
            }
            .graphicsLayer {
                val progress = appearance.value
                alpha = if (animateEnter && !measured) {
                    0f
                } else {
                    0.4f + (0.6f * progress)
                }
                translationY = travelY * (1f - progress)
                val scale = 0.94f + (0.06f * progress)
                scaleX = scale
                scaleY = scale
                // Anchor near the input / trailing edge so it rises into the bubble slot.
                transformOrigin = TransformOrigin(1f, 1f)
            },
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
    ) {
        Text(
            text = content,
            modifier = Modifier
                .widthIn(max = 320.dp)
                .weight(1f, fill = false)
                .clip(AskSuggestionShape)
                .background(AskButtonBackground)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            fontSize = 15.sp,
            color = Color.White,
            lineHeight = 20.sp,
        )
        AskMessageAvatar(
            label = avatarLabel,
            backgroundColor = AskButtonBackground,
            contentColor = Color.White,
        )
    }
}

@Composable
internal fun AskAssistantMessageCard(
    message: AskMessage,
    isSavingAsNote: Boolean,
    onAskStop: () -> Unit,
    onSaveAsNote: (messageId: String) -> Unit,
    onFeedback: (messageId: String, useful: Boolean) -> Unit,
    onCitationClick: (AskCitation) -> Unit,
) {
    val showThinkingRow = message.isStreaming
    val showContent = message.content.isNotBlank()
    val showToolbar = !message.isStreaming && !message.isFailed && message.content.isNotBlank()
    val showLimitation = !message.isStreaming && message.limitation != null
    val showEvidence = message.citations.any { citation ->
        citation.sourceTitle.isNotBlank() || citation.locationLabel.isNotBlank()
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AskMessageAvatar(
            label = stringResource(R.string.home_ask_avatar_ai),
            backgroundColor = HomeSourceFilterChipSelected,
            contentColor = HomeHeader,
        )
        Column(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .weight(1f, fill = false)
                .clip(AskSuggestionShape)
                .background(HomeCardBackground)
                .border(1.dp, HomeCardBorder, AskSuggestionShape)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (showThinkingRow && !showContent) {
                AskThinkingStopRow(onAskStop = onAskStop)
            }
            if (showContent) {
                AskAssistantContent(
                    content = message.content,
                    citations = message.citations,
                    onCitationClick = onCitationClick,
                )
            }
            if (showThinkingRow && showContent) {
                AskThinkingStopRow(onAskStop = onAskStop)
            }
            if (showLimitation) {
                AskLimitationBanner(text = message.limitation.orEmpty())
            }
            if (showEvidence) {
                if (showContent || showLimitation) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(HomeCardBorder),
                    )
                }
                AskMessageEvidenceList(
                    messageId = message.id,
                    citations = message.citations,
                    onCitationClick = onCitationClick,
                )
            }
            if (showToolbar) {
                AskResponseToolbar(
                    messageId = message.id,
                    isSavedAsNote = message.isSavedAsNote,
                    isSavingAsNote = isSavingAsNote,
                    feedback = message.feedback,
                    onSaveAsNote = onSaveAsNote,
                    onFeedback = onFeedback,
                )
            }
        }
    }
}

@Composable
internal fun AskMessageAvatar(
    label: String,
    backgroundColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(AskSourceChipShape)
            .background(backgroundColor)
            .border(2.dp, Color.White, AskSourceChipShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
        )
    }
}

@Composable
internal fun AskAssistantContent(
    content: String,
    citations: List<AskCitation>,
    onCitationClick: (AskCitation) -> Unit,
) {
    CitedAnswerContent(
        content = content,
        citations = citations,
        onCitationClick = onCitationClick,
    )
}

private const val AskEvidenceCollapsedCount = 2

@Composable
private fun AskMessageEvidenceList(
    messageId: String,
    citations: List<AskCitation>,
    onCitationClick: (AskCitation) -> Unit,
) {
    val visibleCitations = citations
        .sortedBy { it.index }
        .filter { citation ->
            citation.sourceTitle.isNotBlank() || citation.locationLabel.isNotBlank()
        }
    if (visibleCitations.isEmpty()) return
    val canToggle = visibleCitations.size > AskEvidenceCollapsedCount
    var expanded by rememberSaveable(messageId) { mutableStateOf(false) }
    val shownCitations = if (canToggle && !expanded) {
        visibleCitations.take(AskEvidenceCollapsedCount)
    } else {
        visibleCitations
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.citation_preview_evidence_label),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = HomeTextSecondary,
        )
        shownCitations.forEach { citation ->
            Text(
                text = formatAskEvidenceLabel(citation),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AskSubmitShape)
                    .clickable { onCitationClick(citation) }
                    .background(HomeStatusProcessingBackground)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = HomeHeader,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (canToggle) {
            Text(
                text = stringResource(
                    if (expanded) {
                        R.string.home_ask_evidence_show_less
                    } else {
                        R.string.home_ask_evidence_show_more
                    },
                ),
                modifier = Modifier
                    .align(Alignment.End)
                    .clip(AskSourceChipShape)
                    .clickable { expanded = !expanded }
                    .padding(vertical = 2.dp),
                fontSize = 12.sp,
                fontStyle = FontStyle.Italic,
                textDecoration = TextDecoration.Underline,
                color = HomeTextSecondary,
            )
        }
    }
}

private fun formatAskEvidenceLabel(citation: AskCitation): String {
    val title = citation.sourceTitle.trim()
    val location = citation.locationLabel.trim()
    return buildString {
        append('[')
        append(citation.index)
        append(']')
        if (title.isNotEmpty()) {
            append(' ')
            append(title)
        }
        if (location.isNotEmpty()) {
            if (title.isNotEmpty()) append(" -- ") else append(' ')
            append(location)
        }
    }
}

@Composable
internal fun AskThinkingStopRow(
    onAskStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(R.string.home_ask_thinking),
            fontSize = 14.sp,
            color = HomeTextSecondary,
            fontStyle = FontStyle.Italic,
        )
        AskStopButton(onClick = onAskStop)
    }
}

@Composable
internal fun AskStopButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(R.string.home_ask_stop),
        modifier = modifier
            .clip(AskSubmitShape)
            .background(HomeCardBackground)
            .border(1.dp, HomeCardBorder, AskSubmitShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = HomeHeader,
    )
}

@Composable
internal fun AskLimitationBanner(
    text: String,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.home_ask_limitation_label)
    val detail = text.trim().removePrefix("Limitation:").trim()
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                append(label)
            }
            if (detail.isNotEmpty()) {
                append(' ')
                append(detail)
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .clip(AskSubmitShape)
            .background(HomeStatusProcessingBackground)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = HomeStatusProcessingText,
    )
}

private const val AskFeedbackEnterMillis = 220
private const val AskFeedbackExitMillis = 140

@Composable
internal fun AskResponseToolbar(
    messageId: String,
    isSavedAsNote: Boolean,
    isSavingAsNote: Boolean,
    feedback: AskFeedback,
    onSaveAsNote: (messageId: String) -> Unit,
    onFeedback: (messageId: String, useful: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showFeedbackChoices by rememberSaveable(messageId) { mutableStateOf(false) }
    // Keep Yes/No visible after a rating so the choice stays on screen and can be changed.
    val showChoices = showFeedbackChoices || feedback != AskFeedback.NONE
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        AskToolbarButton(
            text = stringResource(
                when {
                    isSavedAsNote -> R.string.home_ask_saved
                    isSavingAsNote -> R.string.home_ask_saving_note
                    else -> R.string.home_ask_save_as_note
                },
            ),
            enabled = !isSavedAsNote && !isSavingAsNote,
            selected = isSavedAsNote,
            onClick = { onSaveAsNote(messageId) },
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.home_ask_useful_label),
                modifier = Modifier
                    .clip(AskSourceChipShape)
                    .clickable(enabled = !showChoices) { showFeedbackChoices = true }
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = HomeTextSecondary,
            )
            AnimatedVisibility(
                visible = showChoices,
                enter = fadeIn(animationSpec = tween(AskFeedbackEnterMillis)) +
                    expandHorizontally(
                        animationSpec = tween(AskFeedbackEnterMillis),
                        expandFrom = Alignment.Start,
                    ),
                exit = fadeOut(animationSpec = tween(AskFeedbackExitMillis)) +
                    shrinkHorizontally(
                        animationSpec = tween(AskFeedbackExitMillis),
                        shrinkTowards = Alignment.Start,
                    ),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    AskFeedbackChoice(
                        text = stringResource(R.string.home_ask_feedback_yes),
                        selected = feedback == AskFeedback.USEFUL,
                        onClick = { onFeedback(messageId, true) },
                    )
                    AskFeedbackChoice(
                        text = stringResource(R.string.home_ask_feedback_no),
                        selected = feedback == AskFeedback.NOT_USEFUL,
                        onClick = { onFeedback(messageId, false) },
                        isNegative = true,
                    )
                }
            }
        }
    }
}

@Composable
private fun AskFeedbackChoice(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isNegative: Boolean = false,
) {
    val backgroundColor = when {
        selected && isNegative -> AskFeedbackNoSelectedBackground
        selected -> AskFeedbackChoiceSelectedBackground
        else -> AskFeedbackChoiceBackground
    }
    val textColor = when {
        selected && isNegative -> AskFeedbackNoSelectedText
        else -> HomeHeader
    }
    Text(
        text = text,
        modifier = modifier
            .clip(AskSourceChipShape)
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .defaultMinSize(minWidth = 44.dp)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = textColor,
        textAlign = TextAlign.Center,
    )
}

@Composable
internal fun AskToolbarButton(
    text: String,
    enabled: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = when {
        selected -> AskButtonBackground
        else -> AskSourceChipBackground
    }
    val contentColor = when {
        selected -> Color.White
        !enabled -> HomeTextSecondary
        else -> HomeTextPrimary
    }
    Text(
        text = text,
        modifier = modifier
            .alpha(if (enabled) 1f else 0.6f)
            .clip(AskSourceChipShape)
            .background(backgroundColor)
            .then(
                if (enabled) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = contentColor,
    )
}
