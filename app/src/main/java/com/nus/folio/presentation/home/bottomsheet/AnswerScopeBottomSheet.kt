package com.nus.folio.presentation.home.bottomsheet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nus.folio.R
import com.nus.folio.components.AnimatedModalSheet
import com.nus.folio.domain.model.Source
import com.nus.folio.domain.model.SourceStatus
import com.nus.folio.domain.model.SourceType
import com.nus.folio.presentation.home.AskScope
import com.nus.folio.presentation.home.AskSuggestionShape
import com.nus.folio.presentation.home.HomeSheetShape
import com.nus.folio.presentation.home.HomeSourceFilterChipSelected
import com.nus.folio.ui.theme.CormorantGaramond
import com.nus.folio.ui.theme.FolioAndroidTheme
import com.nus.folio.ui.theme.HomeCardBackground
import com.nus.folio.ui.theme.HomeCardBorder
import com.nus.folio.ui.theme.HomeHeader
import com.nus.folio.ui.theme.HomeSheetBackground
import com.nus.folio.ui.theme.HomeTextPrimary
import com.nus.folio.ui.theme.HomeTextSecondary
import com.nus.folio.ui.theme.LoginPlaceholder

/**
 * Autocomplete-style scope picker: Entire Space (default) plus ready sources in the space.
 * [onScopeOptionSelected] receives `null` for Entire Space, or a source id for a single document.
 */
@Composable
internal fun AnswerScopeBottomSheet(
    selectedScope: AskScope,
    selectedSourceId: String?,
    sources: List<Source>,
    readySourceCount: Int,
    onScopeOptionSelected: (sourceId: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    AnimatedModalSheet(
        onDismiss = onDismiss,
        contentWindowInsets = WindowInsets.navigationBars.union(WindowInsets.ime),
    ) { requestDismiss ->
        AddSourceDragHandle()
        AnswerScopeSheetContent(
            selectedScope = selectedScope,
            selectedSourceId = selectedSourceId,
            sources = sources,
            readySourceCount = readySourceCount,
            onScopeOptionSelected = { sourceId ->
                requestDismiss { onScopeOptionSelected(sourceId) }
            },
        )
    }
}

@Composable
private fun AnswerScopeSheetContent(
    selectedScope: AskScope,
    selectedSourceId: String?,
    sources: List<Source>,
    readySourceCount: Int,
    onScopeOptionSelected: (sourceId: String?) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val trimmedQuery = query.trim()
    val entireSpaceLabel = stringResource(R.string.answer_scope_entire_space)
    val showEntireSpace = trimmedQuery.isEmpty() ||
        entireSpaceLabel.contains(trimmedQuery, ignoreCase = true)
    val filteredSources = remember(sources, trimmedQuery) {
        if (trimmedQuery.isEmpty()) {
            sources
        } else {
            sources.filter { it.title.contains(trimmedQuery, ignoreCase = true) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.5f),
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.answer_scope_title),
            fontFamily = CormorantGaramond,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            color = HomeTextPrimary,
        )
        Spacer(modifier = Modifier.height(16.dp))
        AnswerScopeSearchField(
            query = query,
            onQueryChange = { query = it },
        )
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (showEntireSpace) {
                item(key = "entire_space") {
                    AnswerScopeOption(
                        title = entireSpaceLabel,
                        subtitle = stringResource(
                            R.string.answer_scope_entire_space_subtitle,
                            readySourceCount,
                        ),
                        selected = selectedScope == AskScope.ENTIRE_SPACE,
                        onClick = { onScopeOptionSelected(null) },
                    )
                }
            }
            items(filteredSources, key = { it.id }) { source ->
                AnswerScopeOption(
                    title = source.title,
                    subtitle = source.author.ifBlank {
                        stringResource(R.string.answer_scope_source_subtitle)
                    },
                    selected = selectedScope == AskScope.CURRENT_SOURCE &&
                        selectedSourceId == source.id,
                    onClick = { onScopeOptionSelected(source.id) },
                )
            }
            if (!showEntireSpace && filteredSources.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = stringResource(R.string.answer_scope_no_matches),
                        fontSize = 14.sp,
                        color = HomeTextSecondary,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AnswerScopeSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(AskSuggestionShape)
            .background(HomeCardBackground)
            .border(1.dp, HomeCardBorder, AskSuggestionShape)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_search),
            contentDescription = stringResource(R.string.answer_scope_search),
            tint = HomeTextSecondary,
            modifier = Modifier.size(20.dp),
        )
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = stringResource(R.string.answer_scope_search_hint),
                    color = LoginPlaceholder,
                    fontSize = 15.sp,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(color = HomeTextPrimary, fontSize = 15.sp),
                cursorBrush = SolidColor(HomeTextPrimary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun AnswerScopeOption(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) HomeHeader else HomeCardBorder
    val backgroundColor = if (selected) {
        HomeSourceFilterChipSelected
    } else {
        HomeCardBackground
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AskSuggestionShape)
            .background(backgroundColor)
            .border(1.dp, borderColor, AskSuggestionShape)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            )
            .padding(start = 4.dp, end = 18.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = HomeHeader,
                unselectedColor = HomeCardBorder,
            ),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = HomeTextPrimary,
                lineHeight = 20.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = HomeTextSecondary,
                lineHeight = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852, backgroundColor = 0xFFF7F1E6)
@Composable
private fun AnswerScopeSheetContentPreview() {
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
                AnswerScopeSheetContent(
                    selectedScope = AskScope.CURRENT_SOURCE,
                    selectedSourceId = "1",
                    sources = listOf(
                        Source(
                            id = "1",
                            title = "Aged-Care Policy Guide.pdf",
                            type = SourceType.FILE,
                            author = "Policy Unit",
                            addedLabel = "Added 2d ago",
                            status = SourceStatus.READY,
                            spaceId = "1",
                            fileExtension = "pdf",
                        ),
                        Source(
                            id = "2",
                            title = "Interview Transcript 01.docx",
                            type = SourceType.FILE,
                            author = "Field notes",
                            addedLabel = "Added 1d ago",
                            status = SourceStatus.READY,
                            spaceId = "1",
                            fileExtension = "docx",
                        ),
                        Source(
                            id = "3",
                            title = "https://care-research.org",
                            type = SourceType.WEB,
                            author = "Care Research",
                            addedLabel = "Added 1d ago",
                            status = SourceStatus.READY,
                            spaceId = "1",
                        ),
                    ),
                    readySourceCount = 3,
                    onScopeOptionSelected = {},
                )
            }
        }
    }
}
