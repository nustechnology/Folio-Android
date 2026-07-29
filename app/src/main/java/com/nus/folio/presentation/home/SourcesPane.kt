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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nus.folio.R
import com.nus.folio.domain.model.Source
import com.nus.folio.domain.model.SourceFilter
import com.nus.folio.domain.model.SourceStatus
import com.nus.folio.domain.model.SourceType
import com.nus.folio.ui.theme.CormorantGaramond
import com.nus.folio.ui.theme.FolioAndroidTheme
import com.nus.folio.ui.theme.HomeBackground
import com.nus.folio.ui.theme.HomeCardBackground
import com.nus.folio.ui.theme.HomeCardBorder
import com.nus.folio.ui.theme.HomeChipBorder
import com.nus.folio.ui.theme.HomeChipSelected
import com.nus.folio.ui.theme.HomeHeader
import com.nus.folio.ui.theme.HomeStatusFailedBackground
import com.nus.folio.ui.theme.HomeStatusFailedText
import com.nus.folio.ui.theme.HomeStatusProcessingBackground
import com.nus.folio.ui.theme.HomeStatusProcessingText
import com.nus.folio.ui.theme.HomeStatusReadyBackground
import com.nus.folio.ui.theme.HomeStatusReadyText
import com.nus.folio.ui.theme.HomeTextPrimary
import com.nus.folio.ui.theme.HomeTextSecondary
import com.nus.folio.ui.theme.HomeTypeBadgeBackground

@Composable
internal fun SourcesPane(
    uiState: HomeUiState,
    onRetry: () -> Unit,
    onFilterSelected: (SourceFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(16.dp))
        HomeFilterChips(
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
            uiState.sourcesError != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = uiState.sourcesError.ifBlank {
                                stringResource(R.string.home_error_generic)
                            },
                            color = HomeStatusFailedText,
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
            uiState.visibleSources.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.home_empty_sources),
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
                    items(uiState.visibleSources, key = { it.id }) { source ->
                        SourceCard(source = source)
                    }
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }
        }
    }
}

@Composable
private fun HomeFilterChips(
    uiState: HomeUiState,
    onFilterSelected: (SourceFilter) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            label = stringResource(R.string.home_filter_all, uiState.allCount),
            selected = uiState.selectedFilter == SourceFilter.ALL,
            onClick = { onFilterSelected(SourceFilter.ALL) },
        )
        FilterChip(
            label = stringResource(R.string.home_filter_papers, uiState.papersCount),
            selected = uiState.selectedFilter == SourceFilter.PAPERS,
            onClick = { onFilterSelected(SourceFilter.PAPERS) },
        )
        FilterChip(
            label = stringResource(R.string.home_filter_books, uiState.booksCount),
            selected = uiState.selectedFilter == SourceFilter.BOOKS,
            onClick = { onFilterSelected(SourceFilter.BOOKS) },
        )
        FilterChip(
            label = stringResource(R.string.home_filter_web, uiState.webCount),
            selected = uiState.selectedFilter == SourceFilter.WEB,
            onClick = { onFilterSelected(SourceFilter.WEB) },
        )
        FilterChip(
            label = stringResource(R.string.home_filter_text, uiState.textCount),
            selected = uiState.selectedFilter == SourceFilter.TEXT,
            onClick = { onFilterSelected(SourceFilter.TEXT) },
        )
    }
}

@Composable
private fun FilterChip(
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
private fun SourceCard(source: Source) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HomeCardShape)
            .background(HomeCardBackground)
            .border(1.dp, HomeCardBorder, HomeCardShape)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = sourceTypeLabel(source.type),
            modifier = Modifier
                .clip(HomeBadgeShape)
                .background(HomeTypeBadgeBackground)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = HomeTextSecondary,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = source.title,
                fontFamily = CormorantGaramond,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = HomeTextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 22.sp,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = source.addedLabel,
                fontSize = 12.sp,
                color = HomeTextSecondary,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        StatusPill(status = source.status)
    }
}

@Composable
private fun sourceTypeLabel(type: SourceType): String = when (type) {
    SourceType.PDF -> stringResource(R.string.home_type_pdf)
    SourceType.BOOK -> stringResource(R.string.home_type_book)
    SourceType.WEB -> stringResource(R.string.home_type_web)
    SourceType.TEXT -> stringResource(R.string.home_type_text)
}

@Composable
private fun StatusPill(status: SourceStatus) {
    val (background, textColor, labelRes) = when (status) {
        SourceStatus.READY -> Triple(
            HomeStatusReadyBackground,
            HomeStatusReadyText,
            R.string.home_status_ready,
        )
        SourceStatus.PROCESSING -> Triple(
            HomeStatusProcessingBackground,
            HomeStatusProcessingText,
            R.string.home_status_processing,
        )
        SourceStatus.FAILED -> Triple(
            HomeStatusFailedBackground,
            HomeStatusFailedText,
            R.string.home_status_failed,
        )
    }
    Text(
        text = stringResource(labelRes),
        modifier = Modifier
            .clip(HomeStatusShape)
            .background(background)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = textColor,
    )
}

@Preview(showBackground = true, widthDp = 393, heightDp = 700, name = "Sources — list")
@Composable
private fun SourcesPanePreview() {
    FolioAndroidTheme(dynamicColor = false) {
        SourcesPane(
            uiState = HomeUiState(
                visibleSources = listOf(
                    Source("1", "Alan Turing: Computing Machinery", SourceType.PDF, "Added 2d ago", SourceStatus.READY),
                    Source("2", "The Origins of Totalitarianism", SourceType.PDF, "Added 2d ago", SourceStatus.READY),
                    Source("3", "Weapons of Math Destruction", SourceType.BOOK, "Added 2d ago", SourceStatus.PROCESSING),
                    Source("4", "The Age of Surveillance Capitalism", SourceType.PDF, "Added 2d ago", SourceStatus.FAILED),
                    Source("5", "Attention Is All You Need", SourceType.PDF, "Added 2d ago", SourceStatus.READY),
                ),
                allCount = 128,
                papersCount = 80,
                booksCount = 24,
                webCount = 18,
                textCount = 6,
            ),
            onRetry = {},
            onFilterSelected = {},
            modifier = Modifier.background(HomeBackground),
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 700, name = "Sources — empty")
@Composable
private fun SourcesPaneEmptyPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        SourcesPane(
            uiState = HomeUiState(visibleSources = emptyList()),
            onRetry = {},
            onFilterSelected = {},
            modifier = Modifier.background(HomeBackground),
        )
    }
}
