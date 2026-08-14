package com.nus.folio.presentation.home.pane

import com.nus.folio.presentation.home.HomeBadgeShape
import com.nus.folio.presentation.home.HomeCardShape
import com.nus.folio.presentation.home.HomeChipShape
import com.nus.folio.presentation.home.HomeSourceFilterChipSelected
import com.nus.folio.presentation.home.HomeSourceFilterChipSelectedBorder
import com.nus.folio.presentation.home.HomeStatusShape
import com.nus.folio.presentation.home.HomeUiState
import com.nus.folio.presentation.home.sourceFilterBadgeColors
import com.nus.folio.presentation.home.sourceTypeBadgeColors
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nus.folio.R
import com.nus.folio.components.FolioEmptyState
import com.nus.folio.components.FolioSkeletonBar
import com.nus.folio.components.FolioSkeletonList
import com.nus.folio.domain.model.Source
import com.nus.folio.domain.model.SourceFilter
import com.nus.folio.domain.model.SourceStatus
import com.nus.folio.domain.model.SourceType
import com.nus.folio.ui.theme.CormorantGaramond
import com.nus.folio.ui.theme.FolioAndroidTheme
import com.nus.folio.ui.theme.HomeBackground
import com.nus.folio.ui.theme.HomeCardBackground
import com.nus.folio.ui.theme.HomeCardBorder
import com.nus.folio.ui.theme.HomeHeader
import com.nus.folio.ui.theme.HomeStatusFailedBackground
import com.nus.folio.ui.theme.HomeStatusFailedText
import com.nus.folio.ui.theme.HomeStatusProcessingBackground
import com.nus.folio.ui.theme.HomeStatusProcessingText
import com.nus.folio.ui.theme.HomeStatusReadyBackground
import com.nus.folio.ui.theme.HomeStatusReadyText
import com.nus.folio.ui.theme.HomeTextPrimary
import com.nus.folio.ui.theme.HomeTextSecondary
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SourcesPane(
    uiState: HomeUiState,
    onRetry: () -> Unit,
    onRefresh: () -> Unit = {},
    onAddClick: () -> Unit,
    onFilterSelected: (SourceFilter) -> Unit,
    onSourceMoreClick: (Source) -> Unit,
    onSourceClick: (Source) -> Unit,
    onLoadMore: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val pullRefreshState = rememberPullToRefreshState()
    val showFilters = !uiState.isLoading &&
        uiState.sourcesError == null &&
        (uiState.allSources.isNotEmpty() || uiState.selectedFilter != SourceFilter.ALL)

    Column(modifier = modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(16.dp))
        if (showFilters) {
            HomeFilterChips(
                uiState = uiState,
                onFilterSelected = onFilterSelected,
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        PullToRefreshBox(
            isRefreshing = uiState.isRefreshingSources,
            onRefresh = onRefresh,
            state = pullRefreshState,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullRefreshState,
                    isRefreshing = uiState.isRefreshingSources,
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
                uiState.isLoading || uiState.isFilteringSources -> {
                    SourcesSkeletonList()
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
                    if (uiState.searchQuery.isNotBlank()) {
                        FolioEmptyState(
                            iconRes = R.drawable.ic_search,
                            title = stringResource(R.string.search_empty_title),
                            message = stringResource(R.string.search_empty_message),
                        )
                    } else {
                        FolioEmptyState(
                            iconRes = R.drawable.ic_document,
                            title = stringResource(R.string.home_empty_sources),
                            message = stringResource(R.string.home_empty_sources_subtitle),
                            actionLabel = stringResource(R.string.home_empty_sources_action),
                            onActionClick = onAddClick,
                        )
                    }
                }
                else -> {
                    val listState = rememberLazyListState()
                    val currentOnLoadMore by rememberUpdatedState(onLoadMore)
                    val currentSourcesHasMore by rememberUpdatedState(uiState.sourcesHasMore)
                    val currentIsLoadingMoreSources by rememberUpdatedState(uiState.isLoadingMoreSources)
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
                                if (currentSourcesHasMore && !currentIsLoadingMoreSources) {
                                    currentOnLoadMore()
                                }
                            }
                    }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(uiState.visibleSources, key = { it.id }) { source ->
                            SourceCard(
                                source = source,
                                onClick = { onSourceClick(source) },
                                onMoreClick = { onSourceMoreClick(source) },
                            )
                        }
                        if (uiState.isLoadingMoreSources) {
                            item(key = "sources-loading-more") {
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
private fun SourcesSkeletonList() {
    FolioSkeletonList {
        SourceCardSkeleton()
    }
}

@Composable
private fun SourceCardSkeleton() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HomeCardShape)
            .background(HomeCardBackground)
            .border(1.dp, HomeCardBorder, HomeCardShape)
            .padding(start = 12.dp, end = 4.dp, top = 14.dp, bottom = 14.dp)
            .clearAndSetSemantics { },
        verticalAlignment = Alignment.Top,
    ) {
        Box {
            Text(
                text = "PDF",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Transparent,
            )
            FolioSkeletonBar(
                modifier = Modifier.matchParentSize(),
                shape = HomeBadgeShape,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Reserve the same title metrics as SourceCard (min/maxLines = 2).
                Text(
                    text = " ",
                    fontFamily = CormorantGaramond,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Transparent,
                    maxLines = 2,
                    lineHeight = 22.sp,
                )
                FolioSkeletonBar(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(12.dp),
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Meta",
                    fontSize = 12.sp,
                    color = Color.Transparent,
                    maxLines = 1,
                )
                FolioSkeletonBar(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxWidth(0.55f)
                        .height(10.dp),
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box {
            Text(
                text = stringResource(R.string.home_status_ready),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Transparent,
            )
            FolioSkeletonBar(
                modifier = Modifier.matchParentSize(),
                shape = HomeStatusShape,
            )
        }
        Spacer(modifier = Modifier.size(32.dp))
    }
}

@Composable
private fun HomeFilterChips(
    uiState: HomeUiState,
    onFilterSelected: (SourceFilter) -> Unit,
) {
    val filters = listOf(
        SourceFilter.ALL to stringResource(R.string.home_filter_all_label),
        SourceFilter.FILE to stringResource(R.string.home_type_file),
        SourceFilter.WEB to stringResource(R.string.home_type_web),
        SourceFilter.TEXT to stringResource(R.string.home_type_text),
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        filters.forEach { (filter, label) ->
            SourceFilterChip(
                label = label,
                selected = uiState.selectedFilter == filter,
                filter = filter,
                onClick = { onFilterSelected(filter) },
            )
        }
    }
}

@Composable
private fun SourceFilterChip(
    label: String,
    selected: Boolean,
    filter: SourceFilter,
    onClick: () -> Unit,
) {
    val typeColors = sourceFilterBadgeColors(filter)
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
            .padding(horizontal = 20.dp, vertical = 10.dp),
        fontSize = 13.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        color = contentColor,
    )
}

@Composable
private fun SourceCard(
    source: Source,
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
        val badgeColors = sourceTypeBadgeColors(source.type)
        Text(
            text = sourceBadgeLabel(source),
            modifier = Modifier
                .clip(HomeBadgeShape)
                .background(badgeColors.background)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = badgeColors.content,
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
                text = if (source.author.isBlank()) {
                    source.addedLabel
                } else {
                    stringResource(R.string.home_source_meta, source.author, source.addedLabel)
                },
                fontSize = 12.sp,
                color = HomeTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        StatusPill(status = source.status)
        IconButton(
            onClick = onMoreClick,
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_more_vertical),
                contentDescription = stringResource(R.string.home_sources_more),
                tint = HomeTextPrimary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun sourceBadgeLabel(source: Source): String {
    when (source.type) {
        SourceType.TEXT -> return sourceTypeLabel(SourceType.TEXT)
        SourceType.WEB -> return sourceTypeLabel(SourceType.WEB)
        SourceType.FILE, SourceType.BOOK -> Unit
    }
    val extension = source.fileExtension.trim()
        .takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
    if (extension != null) return extension.uppercase()
    return sourceTypeLabel(source.type)
}

@Composable
private fun sourceTypeLabel(type: SourceType): String = when (type) {
    SourceType.FILE -> stringResource(R.string.home_type_file)
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
                    Source("1", "Alan Turing: Computing Machinery", SourceType.FILE, "Alan Turing", "Added 2d ago", SourceStatus.READY, "1", "pdf"),
                    Source("2", "The Origins of Totalitarianism", SourceType.FILE, "Hannah Arendt", "Added 2d ago", SourceStatus.READY, "1", "pdf"),
                    Source("3", "Weapons of Math Destruction", SourceType.BOOK, "Cathy O'Neil", "Added 2d ago", SourceStatus.PROCESSING, "1", "epub"),
                    Source("4", "The Age of Surveillance Capitalism", SourceType.FILE, "Shoshana Zuboff", "Added 2d ago", SourceStatus.FAILED, "1", "pdf"),
                    Source("5", "Attention Is All You Need", SourceType.FILE, "Vaswani et al.", "Added 2d ago", SourceStatus.READY, "1", "pdf"),
                ),
                allCount = 128,
            ),
            onRetry = {},
            onAddClick = {},
            onFilterSelected = {},
            onSourceMoreClick = {},
            onSourceClick = {},
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
            onAddClick = {},
            onFilterSelected = {},
            onSourceMoreClick = {},
            onSourceClick = {},
            modifier = Modifier.background(HomeBackground),
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 700, name = "Sources — loading")
@Composable
private fun SourcesPaneLoadingPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        SourcesPane(
            uiState = HomeUiState(isLoading = true),
            onRetry = {},
            onAddClick = {},
            onFilterSelected = {},
            onSourceMoreClick = {},
            onSourceClick = {},
            modifier = Modifier.background(HomeBackground),
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 700, name = "Sources — filtering")
@Composable
private fun SourcesPaneFilteringPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        SourcesPane(
            uiState = HomeUiState(
                isFilteringSources = true,
                selectedFilter = SourceFilter.FILE,
                allSources = listOf(
                    Source("1", "Alan Turing", SourceType.FILE, "Alan Turing", "Added 2d ago", SourceStatus.READY, "1", "pdf"),
                ),
                allCount = 1,
            ),
            onRetry = {},
            onAddClick = {},
            onFilterSelected = {},
            onSourceMoreClick = {},
            onSourceClick = {},
            modifier = Modifier.background(HomeBackground),
        )
    }
}
