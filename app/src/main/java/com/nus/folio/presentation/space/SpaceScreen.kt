package com.nus.folio.presentation.space

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nus.folio.R
import com.nus.folio.components.FolioEmptyState
import com.nus.folio.components.FolioSearchField
import com.nus.folio.components.FolioToastHost
import com.nus.folio.components.FolioToastStyle
import com.nus.folio.components.FolioToastVisuals
import com.nus.folio.components.ItemOptionAction
import com.nus.folio.components.ItemOptionStyle
import com.nus.folio.components.ItemOptionsBottomSheet
import com.nus.folio.components.dismissKeyboardOnTapOutside
import com.nus.folio.components.rememberDismissKeyboardThen
import com.nus.folio.components.rememberFolioToastHostState
import com.nus.folio.di.LocalAppContainer
import com.nus.folio.domain.model.Space
import com.nus.folio.domain.model.SpaceSort
import com.nus.folio.domain.model.initialsFromDisplayName
import com.nus.folio.presentation.home.bottomsheet.DeleteConfirmationBottomSheet
import com.nus.folio.presentation.home.bottomsheet.findActivityOrNull
import com.nus.folio.ui.theme.FolioAndroidTheme
import com.nus.folio.ui.theme.HomeBackground
import com.nus.folio.ui.theme.HomeCardBackground
import com.nus.folio.ui.theme.HomeHeader
import com.nus.folio.ui.theme.HomeStatusFailedText
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

@Composable
fun SpaceScreen(
    onSpaceSelected: (Space) -> Unit,
    onSignOut: suspend () -> Result<Unit>,
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
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val toastHostState = rememberFolioToastHostState()
    var showSignOutConfirm by remember { mutableStateOf(false) }
    var isSigningOut by remember { mutableStateOf(false) }
    val onAccountClick = rememberDismissKeyboardThen(viewModel::onAccountClick)
    val onAddClick = rememberDismissKeyboardThen(viewModel::onAddClick)
    val onFilterSortClick = rememberDismissKeyboardThen(viewModel::onFilterSortClick)
    val onOpenSignOutConfirm = rememberDismissKeyboardThen { showSignOutConfirm = true }
    val selectedAccount = uiState.accounts.firstOrNull { it.isSelected }
    val avatarInitial = selectedAccount?.let {
        initialsFromDisplayName(it.displayName, it.email)
    }.orEmpty()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onScreenFocused()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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

    DisposableEffect(Unit) {
        val activity = context.findActivityOrNull()
        onDispose {
            if (activity?.isChangingConfigurations != true) {
                viewModel.clearSearch()
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        SpaceContent(
            uiState = uiState,
            avatarInitial = avatarInitial,
            onRetry = viewModel::onRetry,
            onRefresh = viewModel::onRefresh,
            onSearchQueryChange = viewModel::onSearchQueryChange,
            onFilterSortClick = onFilterSortClick,
            onAvatarClick = onAccountClick,
            onAddClick = onAddClick,
            onLoadMore = viewModel::onLoadMore,
            onSpaceClick = onSpaceSelected,
            onSpaceMoreClick = viewModel::onSpaceOptionsClick,
            modifier = Modifier.fillMaxSize(),
        )

        SpaceAddFab(
            onClick = onAddClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 20.dp, bottom = 24.dp),
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
                onSignOutClick = onOpenSignOutConfirm,
            )
        }

        if (showSignOutConfirm) {
            DeleteConfirmationBottomSheet(
                titleRes = R.string.account_sign_out_title,
                messageRes = R.string.account_sign_out_message,
                confirmLabelRes = R.string.account_sign_out,
                isSubmitting = isSigningOut,
                closeOnConfirm = false,
                onDismiss = {
                    if (!isSigningOut) showSignOutConfirm = false
                },
                onConfirm = {
                    scope.launch {
                        isSigningOut = true
                        try {
                            val result = onSignOut()
                            if (result.isSuccess) {
                                showSignOutConfirm = false
                            } else {
                                toastHostState.showToast(
                                    FolioToastVisuals(
                                        title = context.getString(R.string.home_error_generic),
                                        style = FolioToastStyle.Error,
                                    ),
                                )
                            }
                        } catch (e: CancellationException) {
                            throw e
                        } finally {
                            isSigningOut = false
                        }
                    }
                },
            )
        }

        if (uiState.showSortSheet) {
            SortSpacesBottomSheet(
                selectedSort = uiState.selectedSort,
                onSortSelected = viewModel::onSortSelected,
                onDismiss = viewModel::onSortSheetDismiss,
            )
        }

        FolioToastHost(
            hostState = toastHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 12.dp),
        )
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
            .background(HomeBackground)
            .dismissKeyboardOnTapOutside(),
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
