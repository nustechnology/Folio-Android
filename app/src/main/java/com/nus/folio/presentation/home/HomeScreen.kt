package com.nus.folio.presentation.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nus.folio.R
import com.nus.folio.di.LocalAppContainer
import com.nus.folio.domain.model.AskTopic
import com.nus.folio.domain.model.Note
import com.nus.folio.domain.model.NoteFilter
import com.nus.folio.domain.model.Source
import com.nus.folio.domain.model.SourceFilter
import com.nus.folio.domain.model.SourceStatus
import com.nus.folio.domain.model.SourceType
import com.nus.folio.presentation.common.ItemOptionAction
import com.nus.folio.presentation.common.ItemOptionStyle
import com.nus.folio.presentation.common.ItemOptionsBottomSheet
import com.nus.folio.ui.theme.FolioAndroidTheme
import com.nus.folio.ui.theme.HomeBackground
import com.nus.folio.ui.theme.HomeHeader

@Composable
fun HomeScreen(
    spaceId: String,
    spaceTitle: String,
    onNavigateBack: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(
            spaceId = spaceId,
            spaceTitle = spaceTitle,
            getSourcesUseCase = LocalAppContainer.current.getSourcesUseCase,
            getAskTopicsUseCase = LocalAppContainer.current.getAskTopicsUseCase,
            getNotesUseCase = LocalAppContainer.current.getNotesUseCase,
        ),
    ),
) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddSourceSheet by remember { mutableStateOf(false) }
    var showAddNoteSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userMessage) {
        val message = when (uiState.userMessage) {
            HomeUserMessage.ADD_SOURCE_NOT_SUPPORTED ->
                context.getString(R.string.home_add_source_not_supported)
            HomeUserMessage.ASK_NOT_SUPPORTED ->
                context.getString(R.string.home_ask_not_supported)
            HomeUserMessage.ADD_NOTE_NOT_SUPPORTED ->
                context.getString(R.string.add_note_not_supported)
            HomeUserMessage.ADD_NOTEBOOK_NOT_SUPPORTED ->
                context.getString(R.string.home_add_notebook_not_supported)
            HomeUserMessage.EDIT_SOURCE_NOT_SUPPORTED ->
                context.getString(R.string.source_edit_not_supported)
            HomeUserMessage.DELETE_SOURCE_NOT_SUPPORTED ->
                context.getString(R.string.source_delete_not_supported)
            HomeUserMessage.VIEW_NOTE_NOT_SUPPORTED ->
                context.getString(R.string.note_view_not_supported)
            HomeUserMessage.EDIT_NOTE_NOT_SUPPORTED ->
                context.getString(R.string.note_edit_not_supported)
            HomeUserMessage.CONVERT_NOTE_NOT_SUPPORTED ->
                context.getString(R.string.note_convert_not_supported)
            HomeUserMessage.DELETE_NOTE_NOT_SUPPORTED ->
                context.getString(R.string.note_delete_not_supported)
            null -> return@LaunchedEffect
        }
        snackbarHostState.showSnackbar(message)
        viewModel.onUserMessageShown()
    }

    Box(modifier = modifier.fillMaxSize()) {
        HomeContent(
            uiState = uiState,
            onRetry = viewModel::loadSources,
            onSearchQueryChange = viewModel::onSearchQueryChange,
            onSearchClick = viewModel::onSearchClick,
            onFilterSelected = viewModel::onFilterSelected,
            onNoteFilterSelected = viewModel::onNoteFilterSelected,
            onTabSelected = viewModel::onTabSelected,
            onAddClick = {
                when (uiState.selectedTab) {
                    HomeTab.NOTES -> showAddNoteSheet = true
                    HomeTab.SOURCES, HomeTab.ASK -> showAddSourceSheet = true
                    HomeTab.NOTEBOOK -> viewModel.onNotebookAddClick()
                }
            },
            onBackClick = onNavigateBack,
            onAskSubmit = viewModel::onAskSubmit,
            onSourceEditClick = viewModel::onEditSourceClick,
            onSourceDeleteClick = viewModel::onDeleteSourceClick,
            onNoteMoreClick = viewModel::onNoteOptionsClick,
            onSignOut = {
                container.clearAuthSessionUseCase()
                onSignOut()
            },
            modifier = Modifier.fillMaxSize(),
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = HomeBottomNavClearance),
        )

        if (showAddSourceSheet) {
            AddSourceBottomSheet(
                onDismiss = { showAddSourceSheet = false },
                onSubmit = viewModel::onAddSourceSubmit,
            )
        }

        if (showAddNoteSheet) {
            AddNoteBottomSheet(
                onDismiss = { showAddNoteSheet = false },
                onSubmit = { title, content ->
                    viewModel.onAddNoteSubmit(title, content)
                },
            )
        }

        uiState.optionsNote?.let { note ->
            ItemOptionsBottomSheet(
                title = note.title,
                actions = listOf(
                    ItemOptionAction(
                        label = stringResource(R.string.note_options_view),
                        onClick = viewModel::onViewNoteClick,
                    ),
                    ItemOptionAction(
                        label = stringResource(R.string.note_options_edit),
                        onClick = viewModel::onEditNoteClick,
                    ),
                    ItemOptionAction(
                        label = stringResource(R.string.note_options_convert),
                        onClick = viewModel::onConvertNoteClick,
                    ),
                    ItemOptionAction(
                        label = stringResource(R.string.note_options_delete),
                        style = ItemOptionStyle.Destructive,
                        onClick = viewModel::onDeleteNoteClick,
                    ),
                ),
                onDismiss = viewModel::onNoteOptionsDismiss,
            )
        }
    }
}

@Composable
internal fun HomeContent(
    uiState: HomeUiState,
    onRetry: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearchClick: () -> Unit,
    onFilterSelected: (SourceFilter) -> Unit,
    onNoteFilterSelected: (NoteFilter) -> Unit,
    onTabSelected: (HomeTab) -> Unit,
    onAddClick: () -> Unit,
    onBackClick: () -> Unit,
    onAskSubmit: () -> Unit,
    onSourceEditClick: (Source) -> Unit,
    onSourceDeleteClick: (Source) -> Unit,
    onNoteMoreClick: (Note) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HomeBackground),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HomeHeader)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(top = 12.dp, bottom = 16.dp),
            ) {
                HomeHeaderRow(
                    selectedTab = uiState.selectedTab,
                    spaceTitle = uiState.spaceTitle,
                    onBackClick = onBackClick,
                    onSearchClick = onSearchClick,
                    onAddClick = onAddClick,
                )
                AnimatedVisibility(
                    visible = uiState.isSearchVisible &&
                        (uiState.selectedTab == HomeTab.SOURCES || uiState.selectedTab == HomeTab.NOTES),
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        HomeSearchField(
                            query = uiState.searchQuery,
                            onQueryChange = onSearchQueryChange,
                        )
                    }
                }
            }

            when (uiState.selectedTab) {
                HomeTab.SOURCES -> SourcesPane(
                    uiState = uiState,
                    onRetry = onRetry,
                    onAddClick = onAddClick,
                    onFilterSelected = onFilterSelected,
                    onSourceEditClick = onSourceEditClick,
                    onSourceDeleteClick = onSourceDeleteClick,
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = HomeBottomNavClearance),
                )
                HomeTab.ASK -> AskPane(
                    uiState = uiState,
                    onRetry = onRetry,
                    onAskSubmit = onAskSubmit,
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = HomeBottomNavClearance),
                )
                HomeTab.NOTES -> NotesPane(
                    uiState = uiState,
                    onRetry = onRetry,
                    onAddClick = onAddClick,
                    onFilterSelected = onNoteFilterSelected,
                    onNoteMoreClick = onNoteMoreClick,
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = HomeBottomNavClearance),
                )
                HomeTab.NOTEBOOK -> NotebookPane(
                    onSignOut = onSignOut,
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = HomeBottomNavClearance),
                )
            }
        }

        HomeBottomNav(
            selectedTab = uiState.selectedTab,
            onTabSelected = onTabSelected,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852, name = "Sources — populated")
@Composable
private fun HomeContentPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        HomeContent(
            uiState = HomeUiState(
                visibleSources = listOf(
                    Source("1", "Alan Turing: Computing Machinery", SourceType.PDF, "Added 2d ago", SourceStatus.READY, "1"),
                    Source("2", "The Origins of Totalitarianism", SourceType.PDF, "Added 2d ago", SourceStatus.READY, "1"),
                    Source("3", "Weapons of Math Destruction", SourceType.BOOK, "Added 2d ago", SourceStatus.PROCESSING, "1"),
                    Source("4", "The Age of Surveillance Capitalism", SourceType.PDF, "Added 2d ago", SourceStatus.FAILED, "1"),
                    Source("5", "Attention Is All You Need", SourceType.PDF, "Added 2d ago", SourceStatus.READY, "1"),
                ),
                spaceTitle = "Dissertation Research",
                allCount = 128,
                papersCount = 80,
                booksCount = 24,
                webCount = 18,
                textCount = 6,
            ),
            onRetry = {},
            onSearchQueryChange = {},
            onSearchClick = {},
            onFilterSelected = {},
            onNoteFilterSelected = {},
            onTabSelected = {},
            onAddClick = {},
            onBackClick = {},
            onAskSubmit = {},
            onSourceEditClick = {},
            onSourceDeleteClick = {},
            onNoteMoreClick = {},
            onSignOut = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852, name = "Sources — empty")
@Composable
private fun HomeContentEmptyPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        HomeContent(
            uiState = HomeUiState(
                selectedTab = HomeTab.SOURCES,
                visibleSources = emptyList(),
            ),
            onRetry = {},
            onSearchQueryChange = {},
            onSearchClick = {},
            onFilterSelected = {},
            onNoteFilterSelected = {},
            onTabSelected = {},
            onAddClick = {},
            onBackClick = {},
            onAskSubmit = {},
            onSourceEditClick = {},
            onSourceDeleteClick = {},
            onNoteMoreClick = {},
            onSignOut = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852, name = "Ask — question pane")
@Composable
private fun HomeAskPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        HomeContent(
            uiState = HomeUiState(
                selectedTab = HomeTab.ASK,
                visibleAskTopics = listOf(
                    AskTopic("1a", "Core dissertation arguments", 4, 2, "1"),
                    AskTopic("1b", "Turing and modern AI", 3, 1, "1"),
                ),
                spaceTitle = "Dissertation Research",
            ),
            onRetry = {},
            onSearchQueryChange = {},
            onSearchClick = {},
            onFilterSelected = {},
            onNoteFilterSelected = {},
            onTabSelected = {},
            onAddClick = {},
            onBackClick = {},
            onAskSubmit = {},
            onSourceEditClick = {},
            onSourceDeleteClick = {},
            onNoteMoreClick = {},
            onSignOut = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852, name = "Notes — list")
@Composable
private fun HomeNotesPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        HomeContent(
            uiState = HomeUiState(
                selectedTab = HomeTab.NOTES,
                visibleNotes = listOf(
                    Note("1", "Research Question Draft", "Urban Mobility", "Updated 1d ago", true, "1"),
                    Note("2", "Literature Review Outline", "Dissertation Research", "Updated 2d ago", true, "1"),
                    Note("3", "Turing Test — Key Takeaways", "Dissertation Research", "Updated 3d ago", false, "1"),
                    Note("4", "Policy Implications", "Urban Mobility", "Updated 4d ago", false, "1"),
                    Note("5", "Teaching Prep — Week 7", "Urban Mobility", "Updated 5d ago", false, "1"),
                ),
                spaceTitle = "Dissertation Research",
                notesAllCount = 32,
                notesPinnedCount = 8,
                notesUnfiledCount = 4,
            ),
            onRetry = {},
            onSearchQueryChange = {},
            onSearchClick = {},
            onFilterSelected = {},
            onNoteFilterSelected = {},
            onTabSelected = {},
            onAddClick = {},
            onBackClick = {},
            onAskSubmit = {},
            onSourceEditClick = {},
            onSourceDeleteClick = {},
            onNoteMoreClick = {},
            onSignOut = {},
        )
    }
}
