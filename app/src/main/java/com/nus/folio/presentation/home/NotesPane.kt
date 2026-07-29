package com.nus.folio.presentation.home

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import com.nus.folio.domain.model.Note
import com.nus.folio.domain.model.NoteFilter
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

@Composable
internal fun NotesPane(
    uiState: HomeUiState,
    onRetry: () -> Unit,
    onFilterSelected: (NoteFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(16.dp))
        NotesFilterChips(
            uiState = uiState,
            onFilterSelected = onFilterSelected,
        )
        Spacer(modifier = Modifier.height(12.dp))

        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.home_empty_notes),
                        color = HomeTextSecondary,
                        fontSize = 14.sp,
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(uiState.visibleNotes, key = { it.id }) { note ->
                        NoteCard(note = note)
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
private fun NoteCard(note: Note) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HomeCardShape)
            .background(HomeCardBackground)
            .border(1.dp, HomeCardBorder, HomeCardShape)
            .padding(start = 14.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
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
                text = note.project ?: stringResource(R.string.home_notes_unfiled),
                fontSize = 13.sp,
                color = HomeTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = note.updatedLabel,
                fontSize = 12.sp,
                color = HomeTextSecondary,
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_more),
            contentDescription = null,
            tint = HomeTextPrimary,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 700, name = "Notes — list")
@Composable
private fun NotesPanePreview() {
    FolioAndroidTheme(dynamicColor = false) {
        NotesPane(
            uiState = HomeUiState(
                visibleNotes = listOf(
                    Note("1", "Research Question Draft", "Urban Mobility", "Updated 1d ago", true),
                    Note("2", "Literature Review Outline", "Dissertation Research", "Updated 2d ago", true),
                    Note("3", "Turing Test — Key Takeaways", "Dissertation Research", "Updated 3d ago", false),
                    Note("4", "Policy Implications", "Urban Mobility", "Updated 4d ago", false),
                    Note("5", "Teaching Prep — Week 7", "Urban Mobility", "Updated 5d ago", false),
                ),
                notesAllCount = 32,
                notesPinnedCount = 8,
                notesUnfiledCount = 4,
            ),
            onRetry = {},
            onFilterSelected = {},
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
            onFilterSelected = {},
            modifier = Modifier.background(HomeBackground),
        )
    }
}
