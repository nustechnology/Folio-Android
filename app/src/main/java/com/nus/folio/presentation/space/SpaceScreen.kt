package com.nus.folio.presentation.space

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
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
import com.nus.folio.components.FolioToastHost
import com.nus.folio.components.FolioToastStyle
import com.nus.folio.components.FolioToastVisuals
import com.nus.folio.components.ItemOptionAction
import com.nus.folio.components.ItemOptionStyle
import com.nus.folio.components.ItemOptionsBottomSheet
import com.nus.folio.components.rememberFolioToastHostState
import com.nus.folio.ui.theme.AccountAvatar
import com.nus.folio.ui.theme.AccountTextPrimary
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

@Composable
fun SpaceScreen(
    onSpaceSelected: (Space) -> Unit,
    onNavigateToAccount: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SpaceViewModel = viewModel(
        factory = SpaceViewModel.Factory(
            getSpacesUseCase = LocalAppContainer.current.getSpacesUseCase,
        ),
    ),
) {
    val container = LocalAppContainer.current
    val session = container.getCurrentSessionUseCase()
    val avatarInitial = (
        session?.displayName?.firstOrNull()?.uppercaseChar()?.toString()
            ?: session?.email?.firstOrNull()?.uppercaseChar()?.toString()
        ).orEmpty()
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val toastHostState = rememberFolioToastHostState()

    LaunchedEffect(uiState.userMessage) {
        val message = uiState.userMessage ?: return@LaunchedEffect
        toastHostState.showToast(message.toSpaceToastVisuals(context))
        viewModel.onUserMessageShown()
    }

    Box(modifier = modifier.fillMaxSize()) {
        SpaceContent(
            uiState = uiState,
            avatarInitial = avatarInitial,
            onRetry = viewModel::loadSpaces,
            onSearchClick = viewModel::onSearchClick,
            onSearchQueryChange = viewModel::onSearchQueryChange,
            onAvatarClick = onNavigateToAccount,
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
    }
}

@Composable
internal fun SpaceContent(
    uiState: SpaceUiState,
    avatarInitial: String,
    onRetry: () -> Unit,
    onSearchClick: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onAvatarClick: () -> Unit,
    onSpaceClick: (Space) -> Unit,
    onSpaceMoreClick: (Space) -> Unit,
    modifier: Modifier = Modifier,
) {
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
                onSearchClick = onSearchClick,
                onAvatarClick = onAvatarClick,
            )
            AnimatedVisibility(
                visible = uiState.isSearchVisible,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    SpaceSearchField(
                        query = uiState.searchQuery,
                        onQueryChange = onSearchQueryChange,
                    )
                }
            }
        }

        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = HomeHeader)
                }
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
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.space_empty),
                        color = HomeTextSecondary,
                        fontSize = 14.sp,
                    )
                }
            }
            else -> {
                LazyColumn(
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
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }
        }
    }
}

@Composable
private fun SpaceHeaderRow(
    avatarInitial: String,
    onSearchClick: () -> Unit,
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            IconButton(onClick = onSearchClick, modifier = Modifier.size(40.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_search),
                    contentDescription = stringResource(R.string.space_search),
                    tint = Color.White,
                )
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(AccountAvatar)
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
                    color = AccountTextPrimary,
                )
            }
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
private fun SpaceSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(SpaceSearchShape)
            .background(HomeSearchField)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (query.isEmpty()) {
            Text(
                text = stringResource(R.string.space_search_hint),
                color = HomeSearchPlaceholder,
                fontSize = 15.sp,
            )
        }
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
            cursorBrush = SolidColor(Color.White),
            modifier = Modifier.fillMaxWidth(),
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
            avatarInitial = "A",
            onRetry = {},
            onSearchClick = {},
            onSearchQueryChange = {},
            onAvatarClick = {},
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
            avatarInitial = "A",
            onRetry = {},
            onSearchClick = {},
            onSearchQueryChange = {},
            onAvatarClick = {},
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
        SpaceUserMessage.ADD_SPACE_NOT_SUPPORTED -> R.string.space_add_not_supported
        SpaceUserMessage.RENAME_SPACE_NOT_SUPPORTED -> R.string.space_rename_not_supported
        SpaceUserMessage.DELETE_SPACE_NOT_SUPPORTED -> R.string.space_delete_not_supported
    }
    val style = when (this) {
        SpaceUserMessage.SPACE_CREATED,
        SpaceUserMessage.SPACE_UPDATED,
        SpaceUserMessage.SPACE_DELETED,
        -> FolioToastStyle.Success
        SpaceUserMessage.ADD_SPACE_NOT_SUPPORTED,
        SpaceUserMessage.RENAME_SPACE_NOT_SUPPORTED,
        -> FolioToastStyle.Warning
        SpaceUserMessage.DELETE_SPACE_NOT_SUPPORTED,
        -> FolioToastStyle.Error
    }
    return FolioToastVisuals(
        message = context.getString(messageRes),
        style = style,
    )
}
