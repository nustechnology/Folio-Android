package com.nus.folio.presentation.home.pane

import com.nus.folio.presentation.home.HomeBadgeShape
import com.nus.folio.presentation.home.HomeCardShape
import com.nus.folio.presentation.home.HomeChipShape
import com.nus.folio.presentation.home.HomeSourceFilterChipSelected
import com.nus.folio.presentation.home.HomeSourceFilterChipSelectedBorder
import com.nus.folio.presentation.home.HomeUiState
import com.nus.folio.presentation.home.NoteIconBadgeColors
import com.nus.folio.presentation.home.SourceTypeBadgeColors
import com.nus.folio.presentation.home.noteFilterBadgeColors
import com.nus.folio.presentation.home.noteOriginBadgeColors
import com.nus.folio.presentation.home.notebook.NotebookMarkdownVisuals
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nus.folio.R
import com.nus.folio.components.FolioEmptyState
import com.nus.folio.components.FolioSkeletonBar
import com.nus.folio.components.FolioSkeletonColumn
import com.nus.folio.components.FolioSkeletonList
import com.nus.folio.domain.model.Note
import com.nus.folio.domain.model.NoteFilter
import com.nus.folio.domain.model.NoteOrigin
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NotesPane(
    uiState: HomeUiState,
    onRetry: () -> Unit,
    onRefresh: () -> Unit = {},
    onAddClick: () -> Unit,
    onFilterSelected: (NoteFilter) -> Unit,
    onNoteClick: (Note) -> Unit,
    onNoteMoreClick: (Note) -> Unit,
    onLoadMore: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val pullRefreshState = rememberPullToRefreshState()
    val showFilters = !uiState.isLoading &&
        uiState.notesError == null &&
        (uiState.allNotes.isNotEmpty() || uiState.selectedNoteFilter != NoteFilter.ALL)

    Column(modifier = modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(16.dp))
        if (showFilters) {
            NotesFilterChips(
                uiState = uiState,
                onFilterSelected = onFilterSelected,
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        PullToRefreshBox(
            isRefreshing = uiState.isRefreshingNotes,
            onRefresh = onRefresh,
            state = pullRefreshState,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullRefreshState,
                    isRefreshing = uiState.isRefreshingNotes,
                    modifier = Modifier.align(Alignment.TopCenter),
                    containerColor = HomeCardBackground,
                    color = HomeHeader,
                )
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            when {
                uiState.isLoading || uiState.isFilteringNotes -> {
                    NotesSkeletonList()
                }
                uiState.notesError != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = uiState.notesError.ifBlank {
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
                uiState.isSearchingNotes && uiState.visibleNotes.isEmpty() -> {
                    NotesSkeletonList()
                }
                uiState.visibleNotes.isEmpty() -> {
                    if (uiState.searchQuery.isNotBlank()) {
                        FolioEmptyState(
                            iconRes = R.drawable.ic_search,
                            title = stringResource(R.string.search_empty_title),
                            message = stringResource(R.string.search_empty_message),
                        )
                    } else {
                        FolioEmptyState(
                            iconRes = R.drawable.ic_note,
                            title = stringResource(R.string.home_empty_notes),
                            message = stringResource(R.string.home_empty_notes_subtitle),
                            actionLabel = stringResource(R.string.home_empty_notes_action),
                            onActionClick = onAddClick,
                        )
                    }
                }
                else -> {
                    val listState = rememberLazyListState()
                    LaunchedEffect(listState, uiState.notesHasMore, uiState.isLoadingMoreNotes) {
                        snapshotFlow {
                            val layoutInfo = listState.layoutInfo
                            val totalItems = layoutInfo.totalItemsCount
                            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                            totalItems > 0 && lastVisible >= totalItems - LOAD_MORE_THRESHOLD
                        }
                            .distinctUntilChanged()
                            .filter { nearEnd -> nearEnd }
                            .collect {
                                if (uiState.notesHasMore && !uiState.isLoadingMoreNotes) {
                                    onLoadMore()
                                }
                            }
                    }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (uiState.isSearchingNotes) {
                            item(key = "notes-search-loading") {
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
                        items(uiState.visibleNotes, key = { it.id }) { note ->
                            NoteCard(
                                note = note,
                                onClick = { onNoteClick(note) },
                                onMoreClick = { onNoteMoreClick(note) },
                            )
                        }
                        if (uiState.isLoadingMoreNotes) {
                            item(key = "notes-loading-more") {
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
                        } else {
                            item { Spacer(modifier = Modifier.height(8.dp)) }
                        }
                    }
                }
            }
        }
    }
}

private const val LOAD_MORE_THRESHOLD = 3

@Composable
private fun NotesSkeletonList(modifier: Modifier = Modifier) {
    val loadingDescription = stringResource(R.string.home_notes_loading)
    FolioSkeletonList(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = loadingDescription
        },
    ) {
        NoteCardSkeleton(
            modifier = Modifier.clearAndSetSemantics { },
        )
    }
}

@Composable
private fun NoteCardSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(HomeCardShape)
            .background(HomeCardBackground)
            .border(1.dp, HomeCardBorder, HomeCardShape)
            .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        FolioSkeletonBar(
            modifier = Modifier.size(40.dp),
            shape = HomeBadgeShape,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            FolioSkeletonColumn(
                lineCount = 3,
                lineHeight = 12.dp,
                spacing = 8.dp,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FolioSkeletonBar(
                    modifier = Modifier.size(width = 72.dp, height = 20.dp),
                    shape = HomeBadgeShape,
                )
                FolioSkeletonBar(
                    modifier = Modifier.size(width = 56.dp, height = 20.dp),
                    shape = HomeBadgeShape,
            )
        }
        }
    }
}

@Composable
private fun NotesFilterChips(
    uiState: HomeUiState,
    onFilterSelected: (NoteFilter) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        NoteFilterChip(
            label = stringResource(R.string.home_filter_notes_all),
            selected = uiState.selectedNoteFilter == NoteFilter.ALL,
            filter = NoteFilter.ALL,
            onClick = { onFilterSelected(NoteFilter.ALL) },
        )
        NoteFilterChip(
            label = stringResource(R.string.home_filter_notes_user_created),
            selected = uiState.selectedNoteFilter == NoteFilter.USER_CREATED,
            filter = NoteFilter.USER_CREATED,
            onClick = { onFilterSelected(NoteFilter.USER_CREATED) },
        )
        NoteFilterChip(
            label = stringResource(R.string.home_filter_notes_saved_answers),
            selected = uiState.selectedNoteFilter == NoteFilter.SAVED_ANSWER,
            filter = NoteFilter.SAVED_ANSWER,
            onClick = { onFilterSelected(NoteFilter.SAVED_ANSWER) },
        )
    }
}

@Composable
private fun NoteFilterChip(
    label: String,
    selected: Boolean,
    filter: NoteFilter,
    onClick: () -> Unit,
) {
    val typeColors = noteFilterBadgeColors(filter)
    val background = when {
        selected && typeColors != null -> typeColors.background
        selected -> HomeSourceFilterChipSelected
        else -> HomeCardBackground
    }
    val border = when {
        selected && typeColors != null -> typeColors.content.copy(alpha = 0.35f)
        selected -> HomeSourceFilterChipSelectedBorder
        else -> HomeCardBorder
    }
    val contentColor = when {
        selected && typeColors != null -> typeColors.content
        else -> HomeTextPrimary
    }
    Text(
        text = label,
        modifier = Modifier
            .clip(HomeChipShape)
            .background(background)
            .border(1.dp, border, HomeChipShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        fontSize = 13.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        color = contentColor,
    )
}

@Composable
private fun NoteCard(
    note: Note,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HomeCardShape)
            .background(HomeCardBackground)
            .border(1.dp, HomeCardBorder, HomeCardShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(start = 12.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(HomeBadgeShape)
                .background(NoteIconBadgeColors.background),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_note),
                contentDescription = null,
                tint = NoteIconBadgeColors.content,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = note.title,
                fontFamily = CormorantGaramond,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = HomeTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = remember(note.content) {
                    NotebookMarkdownVisuals.visualize(note.content).text
                },
                fontSize = 13.sp,
                color = HomeTextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NoteOriginBadges(note = note)
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = note.updatedLabel,
                    fontSize = 12.sp,
                    color = HomeTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        IconButton(
            onClick = onMoreClick,
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_more_vertical),
                contentDescription = stringResource(R.string.home_notes_more),
                tint = HomeTextPrimary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun NoteOriginBadges(note: Note) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (note.origin) {
            NoteOrigin.USER_CREATED -> {
                NoteBadge(
                    label = stringResource(R.string.home_note_badge_user_created),
                    colors = noteOriginBadgeColors(NoteOrigin.USER_CREATED),
                )
            }
            NoteOrigin.SAVED_ANSWER -> {
                NoteBadge(
                    label = stringResource(R.string.home_note_badge_saved_answer),
                    colors = noteOriginBadgeColors(NoteOrigin.SAVED_ANSWER),
                )
                if (note.citationCount > 0) {
                    NoteBadge(
                        label = stringResource(
                            R.string.home_note_badge_citations,
                            note.citationCount,
                        ),
                        colors = noteOriginBadgeColors(NoteOrigin.SAVED_ANSWER),
                    )
                }
            }
        }
    }
}

@Composable
private fun NoteBadge(
    label: String,
    colors: SourceTypeBadgeColors,
) {
    Text(
        text = label,
        modifier = Modifier
            .clip(HomeBadgeShape)
            .background(colors.background)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = colors.content,
    )
}

@Preview(showBackground = true, widthDp = 393, heightDp = 700, name = "Notes — list")
@Composable
private fun NotesPanePreview() {
    FolioAndroidTheme(dynamicColor = false) {
        NotesPane(
            uiState = HomeUiState(
                visibleNotes = listOf(
                    Note(
                        id = "1",
                        title = "Research Question Draft",
                        content = "How do informal transit networks reshape access in mid-sized cities?",
                        project = "Urban Mobility",
                        updatedLabel = "Updated 1d ago",
                        isPinned = true,
                        spaceId = "1",
                        origin = NoteOrigin.USER_CREATED,
                    ),
                    Note(
                        id = "2",
                        title = "Literature Review Outline",
                        content = "Map debates on machine intelligence, imitation games, and measurement.",
                        project = "Dissertation Research",
                        updatedLabel = "Updated 2d ago",
                        isPinned = true,
                        spaceId = "1",
                        origin = NoteOrigin.USER_CREATED,
                    ),
                    Note(
                        id = "3",
                        title = "Turing Test — Key Takeaways",
                        content = "The imitation game reframes intelligence as observable linguistic behavior.",
                        project = "Dissertation Research",
                        updatedLabel = "Updated 3d ago",
                        isPinned = false,
                        spaceId = "1",
                        origin = NoteOrigin.SAVED_ANSWER,
                        citationCount = 4,
                    ),
                    Note(
                        id = "4",
                        title = "Policy Implications",
                        content = "Zoning reform alone underestimates last-mile coordination costs.",
                        project = "Urban Mobility",
                        updatedLabel = "Updated 4d ago",
                        isPinned = false,
                        spaceId = "1",
                        origin = NoteOrigin.SAVED_ANSWER,
                        citationCount = 2,
                    ),
                    Note(
                        id = "5",
                        title = "Teaching Prep — Week 7",
                        content = "Seminar prompts on archival silence and source criticism.",
                        project = "Urban Mobility",
                        updatedLabel = "Updated 5d ago",
                        isPinned = false,
                        spaceId = "1",
                        origin = NoteOrigin.USER_CREATED,
                    ),
                ),
                notesAllCount = 32,
                notesUserCreatedCount = 20,
                notesSavedAnswerCount = 12,
            ),
            onRetry = {},
            onAddClick = {},
            onFilterSelected = {},
            onNoteClick = {},
            onNoteMoreClick = {},
            modifier = Modifier.background(HomeBackground),
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 700, name = "Notes — empty")
@Composable
private fun NotesPaneEmptyPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        NotesPane(
            uiState = HomeUiState(
                visibleNotes = emptyList(),
                notesAllCount = 0,
                notesUserCreatedCount = 0,
                notesSavedAnswerCount = 0,
            ),
            onRetry = {},
            onAddClick = {},
            onFilterSelected = {},
            onNoteClick = {},
            onNoteMoreClick = {},
            modifier = Modifier.background(HomeBackground),
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 700, name = "Notes — loading")
@Composable
private fun NotesPaneLoadingPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        NotesPane(
            uiState = HomeUiState(isLoading = true),
            onRetry = {},
            onAddClick = {},
            onFilterSelected = {},
            onNoteClick = {},
            onNoteMoreClick = {},
            modifier = Modifier.background(HomeBackground),
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 700, name = "Notes — filtering")
@Composable
private fun NotesPaneFilteringPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        NotesPane(
            uiState = HomeUiState(
                isFilteringNotes = true,
                selectedNoteFilter = NoteFilter.USER_CREATED,
                allNotes = listOf(
                    Note(
                        id = "1",
                        title = "Research Question Draft",
                        content = "Draft content",
                        project = "Urban Mobility",
                        updatedLabel = "Updated 1d ago",
                        isPinned = true,
                        spaceId = "1",
                        origin = NoteOrigin.USER_CREATED,
                    ),
                ),
                notesAllCount = 1,
                notesUserCreatedCount = 1,
                notesSavedAnswerCount = 0,
            ),
            onRetry = {},
            onAddClick = {},
            onFilterSelected = {},
            onNoteClick = {},
            onNoteMoreClick = {},
            modifier = Modifier.background(HomeBackground),
        )
    }
}