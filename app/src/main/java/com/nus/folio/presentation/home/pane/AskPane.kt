package com.nus.folio.presentation.home.pane

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nus.folio.R
import com.nus.folio.components.FolioEmptyState
import com.nus.folio.components.FolioSkeletonBar
import com.nus.folio.components.FolioSkeletonList
import com.nus.folio.domain.model.AskCitation
import com.nus.folio.domain.model.AskConversation
import com.nus.folio.domain.model.Source
import com.nus.folio.domain.model.SourceStatus
import com.nus.folio.domain.model.SourceType
import com.nus.folio.domain.model.initialsFromDisplayName
import com.nus.folio.presentation.home.AskFeedback
import com.nus.folio.presentation.home.AskMessage
import com.nus.folio.presentation.home.AskMessageRole
import com.nus.folio.presentation.home.AskScope
import com.nus.folio.presentation.home.ConversationIconBadgeColors
import com.nus.folio.presentation.home.HomeBadgeShape
import com.nus.folio.presentation.home.HomeCardShape
import com.nus.folio.presentation.home.HomeUiState
import com.nus.folio.presentation.home.askScopeSelectedSourceLabel
import com.nus.folio.presentation.home.hasAskEvidence
import com.nus.folio.presentation.home.isAskStreaming
import com.nus.folio.ui.theme.CormorantGaramond
import com.nus.folio.ui.theme.FolioAndroidTheme
import com.nus.folio.ui.theme.HomeBackground
import com.nus.folio.ui.theme.HomeCardBackground
import com.nus.folio.ui.theme.HomeCardBorder
import com.nus.folio.ui.theme.HomeHeader
import com.nus.folio.ui.theme.HomeTextPrimary
import com.nus.folio.ui.theme.HomeTextSecondary
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

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
    onConversationClick: (AskConversation) -> Unit = {},
    onConversationMoreClick: (AskConversation) -> Unit = {},
    onRefreshConversations: () -> Unit = {},
    onRetryConversations: () -> Unit = {},
    onNewConversationClick: () -> Unit = {},
    onLoadMore: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    when {
        uiState.isLoading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = HomeHeader)
            }
        }
        !uiState.isAskChatOpen -> {
            AskConversationList(
                conversations = uiState.askConversations,
                isRefreshing = uiState.isRefreshingAskConversations,
                isSearching = uiState.isSearchingAskConversations,
                isLoadingMore = uiState.isLoadingMoreAskConversations,
                hasMore = uiState.askConversationsHasMore,
                searchQuery = uiState.searchQuery,
                error = uiState.askConversationsError,
                onRetry = onRetryConversations,
                onRefresh = onRefreshConversations,
                onConversationClick = onConversationClick,
                onConversationMoreClick = onConversationMoreClick,
                onNewConversationClick = onNewConversationClick,
                onLoadMore = onLoadMore,
                modifier = modifier,
            )
        }
        uiState.isLoadingAskConversation -> {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AskConversationList(
    conversations: List<AskConversation>,
    isRefreshing: Boolean,
    isSearching: Boolean,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    searchQuery: String,
    error: String?,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onConversationClick: (AskConversation) -> Unit,
    onConversationMoreClick: (AskConversation) -> Unit,
    onNewConversationClick: () -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pullRefreshState = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = pullRefreshState,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullRefreshState,
                isRefreshing = isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter),
                containerColor = HomeCardBackground,
                color = HomeHeader,
            )
        },
        modifier = modifier.fillMaxSize(),
    ) {
        when {
            error != null && conversations.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = error.ifBlank {
                                stringResource(R.string.home_error_generic)
                            },
                            color = HomeTextSecondary,
                            fontSize = 14.sp,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.home_retry),
                            modifier = Modifier.clickable(onClick = onRetry),
                            color = HomeHeader,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
            isSearching && conversations.isEmpty() -> {
                ConversationsSkeletonList()
            }
            conversations.isEmpty() -> {
                if (searchQuery.isNotBlank()) {
                    FolioEmptyState(
                        iconRes = R.drawable.ic_search,
                        title = stringResource(R.string.search_empty_title),
                        message = stringResource(R.string.search_empty_message),
                    )
                } else {
                    FolioEmptyState(
                        iconRes = R.drawable.ic_ask_sparkle,
                        title = stringResource(R.string.home_empty_conversations),
                        message = stringResource(R.string.home_empty_conversations_subtitle),
                        actionLabel = stringResource(R.string.conversation_new),
                        onActionClick = onNewConversationClick,
                    )
                }
            }
            else -> {
                val listState = rememberLazyListState()
                val currentOnLoadMore by rememberUpdatedState(onLoadMore)
                val currentHasMore by rememberUpdatedState(hasMore)
                val currentIsLoadingMore by rememberUpdatedState(isLoadingMore)
                LaunchedEffect(listState) {
                    snapshotFlow {
                        val layoutInfo = listState.layoutInfo
                        val totalItems = layoutInfo.totalItemsCount
                        val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                        totalItems > 0 && lastVisible >= totalItems - LOAD_MORE_THRESHOLD
                    }
                        .distinctUntilChanged()
                        .filter { nearEnd -> nearEnd }
                        .collect {
                            if (currentHasMore && !currentIsLoadingMore) {
                                currentOnLoadMore()
                            }
                        }
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (isSearching) {
                        item(key = "conversations-search-loading") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp, bottom = 2.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = HomeHeader,
                                    strokeWidth = 2.dp,
                                )
                            }
                        }
                    }
                    items(conversations, key = { it.id }) { conversation ->
                        ConversationCard(
                            conversation = conversation,
                            onClick = { onConversationClick(conversation) },
                            onMoreClick = { onConversationMoreClick(conversation) },
                        )
                    }
                    if (isLoadingMore) {
                        item(key = "conversations-loading-more") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    color = HomeHeader,
                                    strokeWidth = 2.dp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationsSkeletonList() {
    FolioSkeletonList(itemCount = 6) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(HomeCardShape)
                .background(HomeCardBackground)
                .border(1.dp, HomeCardBorder, HomeCardShape)
                .padding(start = 12.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            FolioSkeletonBar(
                modifier = Modifier.size(40.dp),
                shape = HomeBadgeShape,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                FolioSkeletonBar(modifier = Modifier.fillMaxWidth(0.78f).height(18.dp))
                Spacer(modifier = Modifier.height(8.dp))
                FolioSkeletonBar(modifier = Modifier.fillMaxWidth(0.32f).height(12.dp))
            }
        }
    }
}

@Composable
private fun ConversationCard(
    conversation: AskConversation,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HomeCardShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .background(HomeCardBackground)
            .border(1.dp, HomeCardBorder, HomeCardShape)
            .padding(start = 12.dp, end = 4.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(HomeBadgeShape)
                .background(ConversationIconBadgeColors.background),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_ask_sparkle),
                contentDescription = null,
                tint = ConversationIconBadgeColors.content,
                modifier = Modifier.size(32.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = conversation.title,
                fontFamily = CormorantGaramond,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = HomeTextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 22.sp,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = conversation.dateLabel,
                fontSize = 12.sp,
                color = HomeTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(
            onClick = onMoreClick,
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_more_vertical),
                contentDescription = stringResource(R.string.home_ask_conversation_more),
                tint = HomeTextPrimary,
                modifier = Modifier.size(20.dp),
            )
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
                    .padding(top = 4.dp, bottom = 4.dp),
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
                .padding(bottom = if (isImeVisible) 8.dp else 32.dp)
                .onGloballyPositioned { coordinates ->
                    inputTopInWindow = coordinates.positionInWindow().y
                },
        )
    }
}

private const val LOAD_MORE_THRESHOLD = 3

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
    AskCitation(
        index = 1,
        sourceId = "1",
        sourceTitle = "Alan Turing: Computing Machinery",
        locationLabel = "Page 14",
        evidenceText = "The new form of the problem can be described in terms of a game which we call the \"imitation game.\"",
    ),
    AskCitation(
        index = 2,
        sourceId = "5",
        sourceTitle = "Attention Is All You Need",
        locationLabel = "Page 2",
        evidenceText = "The Transformer replaces recurrence with self-attention.",
    ),
)

private val previewConversations = listOf(
    AskConversation(
        id = "conv-1",
        title = "What were the operating costs in Q4?",
        dateLabel = "Aug 20, 07:54",
        spaceId = "1",
    ),
    AskConversation(
        id = "conv-2",
        title = "Summarize all the evidence.",
        dateLabel = "Aug 19, 14:12",
        spaceId = "1",
    ),
)

@Preview(showBackground = true, widthDp = 393, heightDp = 700, name = "Ask — conversation list")
@Composable
private fun AskPaneListPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        AskPane(
            uiState = HomeUiState(
                allSources = listOf(previewReadySource),
                askConversations = previewConversations,
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

@Preview(showBackground = true, widthDp = 393, heightDp = 700, name = "Ask — empty conversations")
@Composable
private fun AskPaneEmptyListPreview() {
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

@Preview(showBackground = true, widthDp = 393, heightDp = 700, name = "Ask — empty state")
@Composable
private fun AskPanePreview() {
    FolioAndroidTheme(dynamicColor = false) {
        AskPane(
            uiState = HomeUiState(
                allSources = listOf(previewReadySource),
                isAskChatOpen = true,
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

@Preview(showBackground = true, widthDp = 393, heightDp = 700, name = "Ask — streaming thinking")
@Composable
private fun AskPaneStreamingPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        AskPane(
            uiState = HomeUiState(
                allSources = listOf(previewReadySource),
                userDisplayName = "Ada Lovelace",
                userEmail = "ada@folio.app",
                isAskChatOpen = true,
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
                isAskChatOpen = true,
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
            uiState = HomeUiState(
                allSources = emptyList(),
                isAskChatOpen = true,
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
