package com.nus.folio.presentation.home.pane

import com.nus.folio.presentation.home.HomeBadgeShape
import com.nus.folio.presentation.home.HomeCardShape
import com.nus.folio.presentation.home.HomeChipShape
import com.nus.folio.presentation.home.HomeUiState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nus.folio.R
import com.nus.folio.components.FolioEmptyState
import com.nus.folio.domain.model.Note
import com.nus.folio.domain.model.NoteFilter
import com.nus.folio.domain.model.NoteOrigin
import com.nus.folio.ui.theme.CormorantGaramond
import com.nus.folio.ui.theme.FolioAndroidTheme
import com.nus.folio.ui.theme.HomeBackground
import com.nus.folio.ui.theme.HomeCardBackground
import com.nus.folio.ui.theme.HomeCardBorder
import com.nus.folio.ui.theme.HomeChipBorder
import com.nus.folio.ui.theme.HomeChipSelected
import com.nus.folio.ui.theme.HomeHeader
import com.nus.folio.ui.theme.HomeTextPrimary
import com.nus.folio.ui.theme.HomeTextSecondary
import com.nus.folio.ui.theme.HomeTypeBadgeBackground

@Composable
internal fun NotesPane(
    uiState: HomeUiState,
    onRetry: () -> Unit,
    onAddClick: () -> Unit,
    onFilterSelected: (NoteFilter) -> Unit,
    onNoteClick: (Note) -> Unit,
    onNoteMoreClick: (Note) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(16.dp))
        NotesFilterChips(
            uiState = uiState,
            onFilterSelected = onFilterSelected,
        )
        Spacer(modifier = Modifier.height(12.dp))
        val contentModifier = Modifier
            .weight(1f)
            .fillMaxWidth()

        when {
            uiState.isLoading -> {
                Box(modifier = contentModifier, contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = HomeHeader)
                }
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
            uiState.visibleNotes.isEmpty() -> {
                if (uiState.searchQuery.isNotBlank()) {
                    FolioEmptyState(
                        iconRes = R.drawable.ic_search,
                        title = stringResource(R.string.search_empty_title),
                        message = stringResource(R.string.search_empty_message),
                        modifier = contentModifier,
                    )
                } else {
                    FolioEmptyState(
                        iconRes = R.drawable.ic_note,
                        title = stringResource(R.string.home_empty_notes),
                        message = stringResource(R.string.home_empty_notes_subtitle),
                        actionLabel = stringResource(R.string.home_empty_notes_action),
                        onActionClick = onAddClick,
                        modifier = contentModifier,
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = contentModifier,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(uiState.visibleNotes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            onClick = { onNoteClick(note) },
                            onMoreClick = { onNoteMoreClick(note) },
                        )
                    }
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
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
            label = stringResource(R.string.home_filter_all, uiState.notesAllCount),
            selected = uiState.selectedNoteFilter == NoteFilter.ALL,
            onClick = { onFilterSelected(NoteFilter.ALL) },
        )
        NoteFilterChip(
            label = stringResource(R.string.home_filter_pinned, uiState.notesPinnedCount),
            selected = uiState.selectedNoteFilter == NoteFilter.PINNED,
            onClick = { onFilterSelected(NoteFilter.PINNED) },
        )
        NoteFilterChip(
            label = stringResource(R.string.home_filter_unfiled, uiState.notesUnfiledCount),
            selected = uiState.selectedNoteFilter == NoteFilter.UNFILED,
            onClick = { onFilterSelected(NoteFilter.UNFILED) },
        )
    }
}

@Composable
private fun NoteFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val background = if (selected) HomeChipSelected else Color.Transparent
    val border = if (selected) Color.Transparent else HomeChipBorder
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
        color = HomeTextPrimary,
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
                .background(HomeTypeBadgeBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_note),
                contentDescription = null,
                tint = HomeTextSecondary,
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
                text = note.content,
                fontSize = 13.sp,
                color = HomeTextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            NoteOriginBadges(note = note)
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
                NoteBadge(label = stringResource(R.string.home_note_badge_user_created))
            }
            NoteOrigin.SAVED_ANSWER -> {
                NoteBadge(label = stringResource(R.string.home_note_badge_saved_answer))
                if (note.citationCount > 0) {
                    NoteBadge(
                        label = stringResource(
                            R.string.home_note_badge_citations,
                            note.citationCount,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun NoteBadge(label: String) {
    Text(
        text = label,
        modifier = Modifier
            .clip(HomeBadgeShape)
            .background(HomeTypeBadgeBackground)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = HomeTextSecondary,
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
                notesPinnedCount = 8,
                notesUnfiledCount = 4,
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
                notesPinnedCount = 0,
                notesUnfiledCount = 0,
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