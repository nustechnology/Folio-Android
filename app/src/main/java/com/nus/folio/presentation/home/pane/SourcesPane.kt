package com.nus.folio.presentation.home.pane

import com.nus.folio.presentation.home.HomeBadgeShape
import com.nus.folio.presentation.home.HomeCardShape
import com.nus.folio.presentation.home.HomeChipShape
import com.nus.folio.presentation.home.HomeSourceFilterChipSelected
import com.nus.folio.presentation.home.HomeSourceFilterChipSelectedBorder
import com.nus.folio.presentation.home.HomeStatusShape
import com.nus.folio.presentation.home.HomeUiState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nus.folio.R
import com.nus.folio.components.FolioEmptyState
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
import com.nus.folio.ui.theme.HomeTypeBadgeBackground
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private val SourceSwipeActionWidth = 64.dp

@Composable
internal fun SourcesPane(
    uiState: HomeUiState,
    onRetry: () -> Unit,
    onAddClick: () -> Unit,
    onFilterSelected: (SourceFilter) -> Unit,
    onSourceEditClick: (Source) -> Unit,
    onSourceDeleteClick: (Source) -> Unit,
    onSourceClick: (Source) -> Unit,
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
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(uiState.visibleSources, key = { it.id }) { source ->
                        SourceCard(
                            source = source,
                            onClick = { onSourceClick(source) },
                            onEditClick = { onSourceEditClick(source) },
                            onDeleteClick = { onSourceDeleteClick(source) },
                        )
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
    val filters = listOf(
        SourceFilter.ALL to stringResource(R.string.home_filter_all_label),
        SourceFilter.PDF to stringResource(R.string.home_type_pdf),
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
                onClick = { onFilterSelected(filter) },
            )
        }
    }
}

@Composable
private fun SourceFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val background = if (selected) HomeSourceFilterChipSelected else HomeCardBackground
    val border = if (selected) HomeSourceFilterChipSelectedBorder else HomeCardBorder
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
        color = HomeTextPrimary,
    )
}

@Composable
private fun SourceCard(
    source: Source,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val revealWidthPx = with(density) { (SourceSwipeActionWidth * 2).toPx() }
    val offsetX = remember { Animatable(0f) }
    val editActionLabel = stringResource(R.string.source_swipe_edit)
    val deleteActionLabel = stringResource(R.string.source_swipe_delete)

    fun settle(targetOpen: Boolean) {
        scope.launch {
            offsetX.animateTo(
                targetValue = if (targetOpen) -revealWidthPx else 0f,
                animationSpec = tween(durationMillis = 220),
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HomeCardShape),
    ) {
        Row(
            modifier = Modifier
                .matchParentSize()
                .align(Alignment.CenterEnd)
                .clearAndSetSemantics { },
            horizontalArrangement = Arrangement.End,
        ) {
            SourceSwipeAction(
                iconRes = R.drawable.ic_edit,
                contentDescription = null,
                background = HomeHeader,
                contentColor = Color.White,
                onClick = {
                    settle(targetOpen = false)
                    onEditClick()
                },
            )
            SourceSwipeAction(
                iconRes = R.drawable.ic_delete,
                contentDescription = null,
                background = HomeStatusFailedBackground,
                contentColor = HomeStatusFailedText,
                onClick = {
                    settle(targetOpen = false)
                    onDeleteClick()
                },
            )
        }
        SourceCardContent(
            source = source,
            onClick = onClick,
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .semantics {
                    customActions = listOf(
                        CustomAccessibilityAction(editActionLabel) {
                            onEditClick()
                            true
                        },
                        CustomAccessibilityAction(deleteActionLabel) {
                            onDeleteClick()
                            true
                        },
                    )
                }
                .pointerInput(revealWidthPx) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val shouldOpen = offsetX.value <= -revealWidthPx / 2f
                            settle(targetOpen = shouldOpen)
                        },
                        onDragCancel = {
                            settle(targetOpen = offsetX.value <= -revealWidthPx / 2f)
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            scope.launch {
                                val next = (offsetX.value + dragAmount).coerceIn(-revealWidthPx, 0f)
                                offsetX.snapTo(next)
                            }
                        },
                    )
                },
        )
    }
}

@Composable
private fun SourceSwipeAction(
    iconRes: Int,
    contentDescription: String?,
    background: Color,
    contentColor: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .width(SourceSwipeActionWidth)
            .fillMaxHeight()
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun SourceCardContent(
    source: Source,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
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
                    Source("1", "Alan Turing: Computing Machinery", SourceType.PDF, "Alan Turing", "Added 2d ago", SourceStatus.READY, "1"),
                    Source("2", "The Origins of Totalitarianism", SourceType.PDF, "Hannah Arendt", "Added 2d ago", SourceStatus.READY, "1"),
                    Source("3", "Weapons of Math Destruction", SourceType.BOOK, "Cathy O'Neil", "Added 2d ago", SourceStatus.PROCESSING, "1"),
                    Source("4", "The Age of Surveillance Capitalism", SourceType.PDF, "Shoshana Zuboff", "Added 2d ago", SourceStatus.FAILED, "1"),
                    Source("5", "Attention Is All You Need", SourceType.PDF, "Vaswani et al.", "Added 2d ago", SourceStatus.READY, "1"),
                ),
                allCount = 128,
            ),
            onRetry = {},
            onAddClick = {},
            onFilterSelected = {},
            onSourceEditClick = {},
            onSourceDeleteClick = {},
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
            onSourceEditClick = {},
            onSourceDeleteClick = {},
            onSourceClick = {},
            modifier = Modifier.background(HomeBackground),
        )
    }
}
