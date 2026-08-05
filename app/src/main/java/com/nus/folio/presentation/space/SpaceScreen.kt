package com.nus.folio.presentation.space

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nus.folio.R
import com.nus.folio.di.LocalAppContainer
import com.nus.folio.domain.model.Space
import com.nus.folio.domain.model.SpaceSort
import com.nus.folio.domain.model.initialsFromDisplayName
import com.nus.folio.components.FolioEmptyState
import com.nus.folio.components.FolioSearchField
import com.nus.folio.components.FolioSkeletonBar
import com.nus.folio.components.FolioSkeletonColumn
import com.nus.folio.components.FolioSkeletonList
import com.nus.folio.components.FolioToastHost
import com.nus.folio.components.FolioToastStyle
import com.nus.folio.components.FolioToastVisuals
import com.nus.folio.components.ItemOptionAction
import com.nus.folio.components.ItemOptionStyle
import com.nus.folio.components.ItemOptionsBottomSheet
import com.nus.folio.components.rememberFolioToastHostState
import com.nus.folio.presentation.home.HomeFilterActiveDot
import com.nus.folio.presentation.home.bottomsheet.DeleteConfirmationBottomSheet
import com.nus.folio.ui.theme.CormorantGaramond
import com.nus.folio.ui.theme.FolioAndroidTheme
import com.nus.folio.ui.theme.HomeBackground
import com.nus.folio.ui.theme.HomeCardBackground
import com.nus.folio.ui.theme.HomeCardBorder
import com.nus.folio.ui.theme.HomeHeader
import com.nus.folio.ui.theme.HomeSearchField
import com.nus.folio.ui.theme.HomeSearchPlaceholder
import com.nus.folio.ui.theme.HomeStatusFailedText
import com.nus.folio.ui.theme.HomeTextPrimary
import com.nus.folio.ui.theme.HomeTextSecondary
import com.nus.folio.ui.theme.HomeTypeBadgeBackground
import com.nus.folio.ui.theme.LoginCopper
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
@Composable
fun SpaceScreen(
    onSpaceSelected: (Space) -> Unit,
    onNavigateToAccount: () -> Unit,
    onRequiresReauth: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SpaceViewModel = viewModel(
        factory = SpaceViewModel.Factory(
            getSpacesUseCase = LocalAppContainer.current.getSpacesUseCase,
            createSpaceUseCase = LocalAppContainer.current.createSpaceUseCase,
            updateSpaceUseCase = LocalAppContainer.current.updateSpaceUseCase,
            deleteSpaceUseCase = LocalAppContainer.current.deleteSpaceUseCase,
            getCurrentSessionUseCase = LocalAppContainer.current.getCurrentSessionUseCase,
            syncCurrentUserUseCase = LocalAppContainer.current.syncCurrentUserUseCase,
            refreshAuthSessionUseCase = LocalAppContainer.current.refreshAuthSessionUseCase,
        ),
    ),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val toastHostState = rememberFolioToastHostState()
    val selectedAccount = uiState.accounts.firstOrNull { it.isSelected }
    val avatarInitial = selectedAccount?.let {
        initialsFromDisplayName(it.displayName, it.email)
    }.orEmpty()

    LaunchedEffect(uiState.requiresReauth) {
        if (uiState.requiresReauth) onRequiresReauth()
    }

    LaunchedEffect(uiState.userMessage) {
        val message = uiState.userMessage ?: return@LaunchedEffect
        toastHostState.showToast(message.toSpaceToastVisuals(context))
        viewModel.onUserMessageShown()
    }

    LaunchedEffect(uiState.actionError) {
        val message = uiState.actionError ?: return@LaunchedEffect
        toastHostState.showToast(
            FolioToastVisuals(
                title = message,
                style = FolioToastStyle.Error,
            ),
        )
        viewModel.onActionErrorShown()
    }

    Box(modifier = modifier.fillMaxSize()) {
        SpaceContent(
            uiState = uiState,
            avatarInitial = avatarInitial,
            onRetry = viewModel::onRetry,
            onRefresh = viewModel::onRefresh,
            onSearchQueryChange = viewModel::onSearchQueryChange,
            onFilterSortClick = viewModel::onFilterSortClick,
            onAvatarClick = viewModel::onAccountClick,
            onAddClick = viewModel::onAddClick,
            onLoadMore = viewModel::onLoadMore,
            onSpaceClick = onSpaceSelected,
            onSpaceMoreClick = viewModel::onSpaceOptionsClick,
            modifier = Modifier.fillMaxSize(),
        )

        SpaceAddFab(
            onClick = viewModel::onAddClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 20.dp, bottom = 24.dp),
        )

        FolioToastHost(
            hostState = toastHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 12.dp),
        )

        if (uiState.showAddSheet) {
            AddSpaceBottomSheet(
                isSubmitting = uiState.isCreatingSpace,
                onDismiss = viewModel::onAddSheetDismiss,
                onSubmit = { name, objective ->
                    viewModel.onAddSpaceSubmit(name, objective)
                },
            )
        }

        uiState.optionsSpace?.let { space ->
            ItemOptionsBottomSheet(
                title = space.title,
                actions = listOf(
                    ItemOptionAction(
                        label = stringResource(R.string.space_options_rename),
                        onClick = viewModel::onRenameSpaceClick,
                    ),
                    ItemOptionAction(
                        label = stringResource(R.string.space_options_delete),
                        style = ItemOptionStyle.Destructive,
                        onClick = viewModel::onDeleteSpaceClick,
                    ),
                ),
                onDismiss = viewModel::onSpaceOptionsDismiss,
            )
        }

        uiState.renamingSpace?.let { space ->
            EditSpaceBottomSheet(
                space = space,
                onDismiss = viewModel::onRenameSpaceDismiss,
                onSave = viewModel::onRenameSpaceSave,
                isSubmitting = uiState.isUpdatingSpace,
            )
        }

        uiState.deletingSpace?.let {
            DeleteConfirmationBottomSheet(
                onDismiss = viewModel::onDeleteSpaceDismiss,
                onConfirm = viewModel::onDeleteSpaceConfirm,
                titleRes = R.string.space_delete_title,
                messageRes = R.string.space_delete_message,
                confirmLabelRes = R.string.space_delete_confirm,
                isSubmitting = uiState.isDeletingSpace,
                closeOnConfirm = false,
            )
        }

        if (uiState.showAccountSheet) {
            AccountListBottomSheet(
                accounts = uiState.accounts,
                onDismiss = viewModel::onAccountSheetDismiss,
                onOpenSettings = onNavigateToAccount,
                onAccountSelected = viewModel::onAccountSelected,
            )
        }

        if (uiState.showSortSheet) {
            SortSpacesBottomSheet(
                selectedSort = uiState.selectedSort,
                onSortSelected = viewModel::onSortSelected,
                onDismiss = viewModel::onSortSheetDismiss,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SpaceContent(
    uiState: SpaceUiState,
    avatarInitial: String,
    onRetry: () -> Unit,
    onRefresh: () -> Unit = {},
    onSearchQueryChange: (String) -> Unit,
    onFilterSortClick: () -> Unit = {},
    onAvatarClick: () -> Unit,
    onAddClick: () -> Unit,
    onLoadMore: () -> Unit = {},
    onSpaceClick: (Space) -> Unit,
    onSpaceMoreClick: (Space) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val pullRefreshState = rememberPullToRefreshState()

    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            totalItems > 0 && lastVisible >= totalItems - LOAD_MORE_THRESHOLD
        }
            .distinctUntilChanged()
            .filter { nearEnd -> nearEnd }
            .collect { onLoadMore() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HomeBackground),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(HomeHeader)
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 16.dp),
        ) {
            SpaceHeaderRow(
                avatarInitial = avatarInitial,
                onAvatarClick = onAvatarClick,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FolioSearchField(
                    query = uiState.searchQuery,
                    onQueryChange = onSearchQueryChange,
                    placeholder = stringResource(R.string.space_search_hint),
                    modifier = Modifier.weight(1f),
                )
                SpaceFilterSortButton(
                    onClick = onFilterSortClick,
                    showActiveIndicator = uiState.selectedSort != SpaceSort.DEFAULT,
                )
            }
        }

        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = onRefresh,
            state = pullRefreshState,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullRefreshState,
                    isRefreshing = uiState.isRefreshing,
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
                uiState.isLoading -> {
                    SpacesSkeletonList()
                }
                uiState.error != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = uiState.error,
                                color = HomeStatusFailedText,
                                fontSize = 14.sp,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.space_retry),
                                modifier = Modifier.clickable(onClick = onRetry),
                                color = HomeHeader,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
                uiState.visibleSpaces.isEmpty() -> {
                    if (uiState.searchQuery.isNotBlank()) {
                        FolioEmptyState(
                            iconRes = R.drawable.ic_search,
                            title = stringResource(R.string.search_empty_title),
                            message = stringResource(R.string.search_empty_message),
                        )
                    } else {
                        FolioEmptyState(
                            iconRes = R.drawable.ic_space,
                            title = stringResource(R.string.space_empty_no_spaces),
                            message = stringResource(R.string.space_empty_no_spaces_subtitle),
                            actionLabel = stringResource(R.string.space_empty_no_spaces_action),
                            onActionClick = onAddClick,
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 16.dp,
                            bottom = 88.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(uiState.visibleSpaces, key = { it.id }) { space ->
                            SpaceCard(
                                space = space,
                                onClick = { onSpaceClick(space) },
                                onMoreClick = { onSpaceMoreClick(space) },
                            )
                        }
                        if (uiState.isLoadingMore) {
                            item(key = "spaces_loading_more") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(28.dp),
                                        color = HomeHeader,
                                        strokeWidth = 3.dp,
                                    )
                                }
                            }
                        } else {
                            item(key = "spaces_list_spacer") {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
        }
        }
    }
}

private const val LOAD_MORE_THRESHOLD = 3

@Composable
private fun SpacesSkeletonList() {
    FolioSkeletonList(
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp,
            bottom = 88.dp,
        ),
    ) {
        SpaceCardSkeleton()
    }
}

@Composable
private fun SpaceCardSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SpaceCardShape)
            .background(HomeCardBackground)
            .border(1.dp, HomeCardBorder, SpaceCardShape)
            .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            FolioSkeletonBar(
                modifier = Modifier.size(40.dp),
                shape = SpaceIconShape,
            )
            Spacer(modifier = Modifier.width(14.dp))
            FolioSkeletonColumn(
                lineCount = 2,
                lineHeight = 12.dp,
                spacing = 8.dp,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        FolioSkeletonColumn(
            lineCount = 2,
            lineHeight = 10.dp,
            spacing = 6.dp,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(HomeCardBorder),
        )
        Spacer(modifier = Modifier.height(12.dp))
        FolioSkeletonBar(
            modifier = Modifier
                .fillMaxWidth(0.4f)
                .height(12.dp),
        )
    }
}

@Composable
private fun SpaceHeaderRow(
    avatarInitial: String,
    onAvatarClick: () -> Unit,
) {
    val accountLabel = stringResource(R.string.space_account)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.space_brand_title),
                fontFamily = CormorantGaramond,
                fontSize = 36.sp,
                fontWeight = FontWeight.SemiBold,
                fontStyle = FontStyle.Italic,
                color = Color.White,
                letterSpacing = (-0.4).sp,
            )
            Text(
                text = stringResource(R.string.space_subtitle),
                fontSize = 14.sp,
                color = HomeSearchPlaceholder,
            )
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .border(1.dp, Color.White, CircleShape)
                .background(HomeSearchField)
                .semantics {
                    contentDescription = accountLabel
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onAvatarClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = avatarInitial,
                fontFamily = CormorantGaramond,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun SpaceFilterSortButton(
    onClick: () -> Unit,
    showActiveIndicator: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(SpaceCardShape)
            .background(HomeSearchField)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_filter_sort),
            contentDescription = stringResource(R.string.space_filter_sort),
            tint = HomeSearchPlaceholder,
            modifier = Modifier.size(20.dp),
        )
        if (showActiveIndicator) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 10.dp, end = 10.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(HomeFilterActiveDot),
            )
        }
    }
}

@Composable
private fun SpaceAddFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(56.dp)
            .border(1.dp, LoginCopper, SpaceAddButtonShape)
            .clip(SpaceAddButtonShape)
            .background(HomeHeader)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_add),
            contentDescription = stringResource(R.string.space_add),
            tint = Color.White,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun SpaceCard(
    space: Space,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
) {
    val titleInitial = space.title.firstOrNull()?.uppercaseChar()?.toString().orEmpty()
    val iconSize = 40.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SpaceCardShape)
            .background(HomeCardBackground)
            .border(1.dp, HomeCardBorder, SpaceCardShape)
            .clickable(onClick = onClick)
            .padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 14.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 28.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = Modifier
                        .size(iconSize)
                        .clip(SpaceIconShape)
                        .background(HomeTypeBadgeBackground)
                        .border(1.dp, HomeCardBorder, SpaceIconShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = titleInitial,
                        fontFamily = CormorantGaramond,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HomeTextPrimary,
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = space.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HomeTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = space.updatedLabel,
                        fontSize = 11.sp,
                        color = HomeTextSecondary,
                    )
                }
            }
            IconButton(
                onClick = onMoreClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(32.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_more_vertical),
                    contentDescription = stringResource(R.string.space_more),
                    tint = HomeTextPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        if (space.description.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = space.description,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 12.sp,
                color = HomeTextSecondary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp,
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(HomeCardBorder),
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = stringResource(
                R.string.space_meta,
                space.sourceCount,
                space.noteCount,
            ),
            fontSize = 12.sp,
            color = HomeTextSecondary,
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852, name = "Spaces — populated")
@Composable
private fun SpaceContentPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        SpaceContent(
            uiState = SpaceUiState(
                visibleSpaces = listOf(
                    Space("1", "Dissertation Research", "Primary research archive for doctoral thesis", 128, 32, "Updated 2d ago"),
                    Space("2", "Public Policy Insights", "Policy papers and legislative analysis", 64, 18, "Updated 5h ago"),
                    Space("3", "History of Science", "Scientific manuscripts and archival sources", 42, 12, "Updated 1w ago"),
                    Space("4", "Teaching Prep", "Course materials and lecture notes", 27, 8, "Updated 3d ago"),
                ),
            ),
            avatarInitial = "AN",
            onRetry = {},
            onSearchQueryChange = {},
            onAvatarClick = {},
            onAddClick = {},
            onSpaceClick = {},
            onSpaceMoreClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852, name = "Spaces — empty")
@Composable
private fun SpaceContentEmptyPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        SpaceContent(
            uiState = SpaceUiState(visibleSpaces = emptyList()),
            avatarInitial = "AN",
            onRetry = {},
            onSearchQueryChange = {},
            onAvatarClick = {},
            onAddClick = {},
            onSpaceClick = {},
            onSpaceMoreClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852, name = "Spaces — loading")
@Composable
private fun SpaceContentLoadingPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        SpaceContent(
            uiState = SpaceUiState(isLoading = true),
            avatarInitial = "AN",
            onRetry = {},
            onSearchQueryChange = {},
            onAvatarClick = {},
            onAddClick = {},
            onSpaceClick = {},
            onSpaceMoreClick = {},
        )
    }
}

private fun SpaceUserMessage.toSpaceToastVisuals(context: android.content.Context): FolioToastVisuals {
    val messageRes = when (this) {
        SpaceUserMessage.SPACE_CREATED -> R.string.toast_space_created
        SpaceUserMessage.SPACE_UPDATED -> R.string.toast_space_updated
        SpaceUserMessage.SPACE_DELETED -> R.string.toast_space_deleted
    }
    val descriptionRes = when (this) {
        SpaceUserMessage.SPACE_CREATED -> R.string.toast_space_created_description
        else -> null
    }
    val style = when (this) {
        SpaceUserMessage.SPACE_CREATED,
        SpaceUserMessage.SPACE_UPDATED,
        SpaceUserMessage.SPACE_DELETED,
        -> FolioToastStyle.Success
    }
    return FolioToastVisuals(
        title = context.getString(messageRes),
        description = descriptionRes?.let(context::getString),
        style = style,
    )
}
