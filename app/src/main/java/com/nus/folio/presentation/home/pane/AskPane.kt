package com.nus.folio.presentation.home.pane

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nus.folio.R
import com.nus.folio.domain.model.AskCitation
import com.nus.folio.domain.model.Source
import com.nus.folio.domain.model.SourceStatus
import com.nus.folio.domain.model.SourceType
import com.nus.folio.domain.model.initialsFromDisplayName
import com.nus.folio.presentation.home.AskFeedback
import com.nus.folio.presentation.home.AskMessage
import com.nus.folio.presentation.home.AskMessageRole
import com.nus.folio.presentation.home.AskScope
import com.nus.folio.presentation.home.HomeUiState
import com.nus.folio.presentation.home.askScopeSelectedSourceLabel
import com.nus.folio.presentation.home.hasAskEvidence
import com.nus.folio.presentation.home.isAskStreaming
import com.nus.folio.ui.theme.FolioAndroidTheme
import com.nus.folio.ui.theme.HomeBackground
import com.nus.folio.ui.theme.HomeHeader

@Composable
internal fun AskPane(
    uiState: HomeUiState,
    onAskSubmit: (String) -> Unit,
    onAskStop: () -> Unit,
    onScopeChipClick: () -> Unit,
    onAddSourceClick: () -> Unit,
    onSaveAsNote: (messageId: String) -> Unit,
    onFeedback: (messageId: String, useful: Boolean) -> Unit,
    onCitationClick: (AskCitation) -> Unit,
    onUserEnterAnimationFinished: (messageId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        uiState.isLoading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = HomeHeader)
            }
        }
        else -> {
            val hasAskEvidence = uiState.hasAskEvidence()
            val isAskStreaming = uiState.isAskStreaming()
            val scopeChipLabel = when (uiState.askScope) {
                AskScope.ENTIRE_SPACE -> stringResource(R.string.answer_scope_entire_space)
                AskScope.CURRENT_SOURCE -> uiState.askScopeSelectedSourceLabel().ifBlank {
                    stringResource(R.string.home_ask_current_source)
                }
            }
            val fallbackSuggestions = listOf(
                stringResource(R.string.home_ask_suggestion_1),
                stringResource(R.string.home_ask_suggestion_2),
                stringResource(R.string.home_ask_suggestion_3),
            )
            val suggestions = uiState.askSuggestions
                .take(3)
                .ifEmpty { fallbackSuggestions }
            val userAvatarLabel = initialsFromDisplayName(
                displayName = uiState.userDisplayName,
                emailFallback = uiState.userEmail,
            ).ifBlank { stringResource(R.string.home_ask_avatar_user) }
            key(uiState.askConversationEpoch) {
                AskPaneContent(
                    messages = uiState.askMessages,
                    suggestions = suggestions,
                    scopeChipLabel = scopeChipLabel,
                    hasAskEvidence = hasAskEvidence,
                    isAskStreaming = isAskStreaming,
                    savingAskMessageId = uiState.savingAskMessageId,
                    pendingUserEnterAnimationId = uiState.pendingUserEnterAnimationId,
                    userAvatarLabel = userAvatarLabel,
                    onScopeChipClick = onScopeChipClick,
                    onAskSubmit = onAskSubmit,
                    onAskStop = onAskStop,
                    onAddSourceClick = onAddSourceClick,
                    onSaveAsNote = onSaveAsNote,
                    onFeedback = onFeedback,
                    onCitationClick = onCitationClick,
                    onUserEnterAnimationFinished = onUserEnterAnimationFinished,
                    modifier = modifier,
                )
            }
        }
    }
}

@Composable
private fun AskPaneContent(
    messages: List<AskMessage>,
    suggestions: List<String>,
    scopeChipLabel: String,
    hasAskEvidence: Boolean,
    isAskStreaming: Boolean,
    savingAskMessageId: String?,
    pendingUserEnterAnimationId: String?,
    userAvatarLabel: String,
    onScopeChipClick: () -> Unit,
    onAskSubmit: (String) -> Unit,
    onAskStop: () -> Unit,
    onAddSourceClick: () -> Unit,
    onSaveAsNote: (messageId: String) -> Unit,
    onFeedback: (messageId: String, useful: Boolean) -> Unit,
    onCitationClick: (AskCitation) -> Unit,
    onUserEnterAnimationFinished: (messageId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var askQuery by rememberSaveable { mutableStateOf("") }
    val showEmptyState = messages.isEmpty() && hasAskEvidence
    val inputEnabled = hasAskEvidence && !isAskStreaming
    val listState = rememberLazyListState()
    var inputTopInWindow by remember { mutableFloatStateOf(Float.NaN) }
    val isImeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val heldAssistantId = remember(messages, pendingUserEnterAnimationId) {
        val lastTwo = messages.takeLast(2)
        if (
            pendingUserEnterAnimationId != null &&
            lastTwo.size == 2 &&
            lastTwo[0].role == AskMessageRole.USER &&
            lastTwo[0].id == pendingUserEnterAnimationId &&
            lastTwo[1].role == AskMessageRole.ASSISTANT
        ) {
            lastTwo[1].id
        } else {
            null
        }
    }
    val displayMessages = remember(messages, heldAssistantId) {
        if (heldAssistantId == null) {
            messages
        } else {
            messages.filter { it.id != heldAssistantId }
        }
    }

    // reverseLayout keeps the newest visible message pinned near the input.
    LaunchedEffect(displayMessages.size, displayMessages.lastOrNull()?.id) {
        if (displayMessages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        if (!hasAskEvidence) {
            Spacer(modifier = Modifier.height(16.dp))
            AskNoEvidenceBanner(onAddSourceClick = onAddSourceClick)
        }
        if (showEmptyState) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(28.dp))
                AskPaneHeader()
                Spacer(modifier = Modifier.height(28.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    suggestions.forEach { suggestion ->
                        AskSuggestionCard(
                            text = suggestion,
                            enabled = !isAskStreaming,
                            onClick = { onAskSubmit(suggestion) },
                        )
                    }
                }
            }
        } else if (displayMessages.isNotEmpty()) {
            LazyColumn(
                state = listState,
                reverseLayout = true,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(
                    items = displayMessages.asReversed(),
                    key = { it.id },
                ) { message ->
                    AskMessageBubble(
                        message = message,
                        userAvatarLabel = userAvatarLabel,
                        inputTopInWindow = inputTopInWindow,
                        animateEnter = message.id == pendingUserEnterAnimationId,
                        isSavingAsNote = savingAskMessageId == message.id,
                        onAskStop = onAskStop,
                        onSaveAsNote = onSaveAsNote,
                        onFeedback = onFeedback,
                        onCitationClick = onCitationClick,
                        onUserEnterFinished = onUserEnterAnimationFinished,
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
        AskInputPanel(
            query = askQuery,
            onQueryChange = { askQuery = it },
            scopeChipLabel = scopeChipLabel,
            onScopeChipClick = onScopeChipClick,
            onAskSubmit = {
                val question = askQuery.trim()
                if (question.isNotEmpty()) {
                    onAskSubmit(question)
                    askQuery = ""
                }
            },
            inputEnabled = inputEnabled,
            scopeChipEnabled = hasAskEvidence,
            modifier = Modifier
                .padding(bottom = if (isImeVisible) 0.dp else 35.dp)
                .onGloballyPositioned { coordinates ->
                    inputTopInWindow = coordinates.positionInWindow().y
                },
        )
    }
}

private val previewReadySource = Source(
    id = "1",
    title = "Alan Turing: Computing Machinery",
    type = SourceType.FILE,
    author = "Alan Turing",
    addedLabel = "Added 2d ago",
    status = SourceStatus.READY,
    spaceId = "1",
    fileExtension = "pdf",
)

private val previewCitations = listOf(
    AskCitation(1, "1", "Alan Turing: Computing Machinery"),
    AskCitation(2, "5", "Attention Is All You Need"),
)

@Preview(showBackground = true, widthDp = 393, heightDp = 700, name = "Ask — empty state")
@Composable
private fun AskPanePreview() {
    FolioAndroidTheme(dynamicColor = false) {
        AskPane(
            uiState = HomeUiState(allSources = listOf(previewReadySource)),
            onAskSubmit = {},
            onAskStop = {},
            onScopeChipClick = {},
            onAddSourceClick = {},
            onSaveAsNote = {},
            onFeedback = { _, _ -> },
            onCitationClick = {},
            onUserEnterAnimationFinished = {},
            modifier = Modifier.background(HomeBackground),
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 700, name = "Ask — streaming thinking")
@Composable
private fun AskPaneStreamingPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        AskPane(
            uiState = HomeUiState(
                allSources = listOf(previewReadySource),
                userDisplayName = "Ada Lovelace",
                userEmail = "ada@folio.app",
                askMessages = listOf(
                    AskMessage("1", AskMessageRole.USER, "Summarize all the evidence."),
                    AskMessage(
                        id = "2",
                        role = AskMessageRole.ASSISTANT,
                        content = "",
                        isStreaming = true,
                    ),
                ),
            ),
            onAskSubmit = {},
            onAskStop = {},
            onScopeChipClick = {},
            onAddSourceClick = {},
            onSaveAsNote = {},
            onFeedback = { _, _ -> },
            onCitationClick = {},
            onUserEnterAnimationFinished = {},
            modifier = Modifier.background(HomeBackground),
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 700, name = "Ask — completed response")
@Composable
private fun AskPaneCompletedPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        AskPane(
            uiState = HomeUiState(
                allSources = listOf(previewReadySource),
                userDisplayName = "Ada Lovelace",
                userEmail = "ada@folio.app",
                askMessages = listOf(
                    AskMessage("1", AskMessageRole.USER, "What problems appear most often?"),
                    AskMessage(
                        id = "2",
                        role = AskMessageRole.ASSISTANT,
                        content = "Several themes recur across the sources [1]. Neural architectures also appear [2].",
                        citations = previewCitations,
                        limitation = "Limitation: Evidence coverage is limited for this space.",
                        feedback = AskFeedback.USEFUL,
                    ),
                ),
            ),
            onAskSubmit = {},
            onAskStop = {},
            onScopeChipClick = {},
            onAddSourceClick = {},
            onSaveAsNote = {},
            onFeedback = { _, _ -> },
            onCitationClick = {},
            onUserEnterAnimationFinished = {},
            modifier = Modifier.background(HomeBackground),
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 700, name = "Ask — no evidence")
@Composable
private fun AskPaneNoEvidencePreview() {
    FolioAndroidTheme(dynamicColor = false) {
        AskPane(
            uiState = HomeUiState(allSources = emptyList()),
            onAskSubmit = {},
            onAskStop = {},
            onScopeChipClick = {},
            onAddSourceClick = {},
            onSaveAsNote = {},
            onFeedback = { _, _ -> },
            onCitationClick = {},
            onUserEnterAnimationFinished = {},
            modifier = Modifier.background(HomeBackground),
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 700, name = "Ask — loading")
@Composable
private fun AskPaneLoadingPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        AskPane(
            uiState = HomeUiState(isLoading = true),
            onAskSubmit = {},
            onAskStop = {},
            onScopeChipClick = {},
            onAddSourceClick = {},
            onSaveAsNote = {},
            onFeedback = { _, _ -> },
            onCitationClick = {},
            onUserEnterAnimationFinished = {},
            modifier = Modifier.background(HomeBackground),
        )
    }
}
