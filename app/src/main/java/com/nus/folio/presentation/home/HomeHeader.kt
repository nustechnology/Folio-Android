package com.nus.folio.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nus.folio.R
import com.nus.folio.ui.theme.CormorantGaramond
import com.nus.folio.ui.theme.FolioAndroidTheme
import com.nus.folio.ui.theme.HomeHeader
import com.nus.folio.ui.theme.HomeSearchField
import com.nus.folio.ui.theme.HomeSearchPlaceholder

@Composable
internal fun HomeHeaderRow(
    selectedTab: HomeTab,
    onBackClick: () -> Unit,
    onAddClick: () -> Unit,
    spaceTitle: String = "",
    titleOverride: String = "",
) {
    val titleRes = when (selectedTab) {
        HomeTab.SOURCES -> R.string.home_sources_title
        HomeTab.ASK -> R.string.home_ask_title
        HomeTab.NOTES -> R.string.home_notes_title
        HomeTab.NOTEBOOK -> R.string.home_notebook_title
    }
    val subtitleRes = when (selectedTab) {
        HomeTab.SOURCES -> R.string.home_sources_subtitle
        HomeTab.ASK -> R.string.home_ask_subtitle
        HomeTab.NOTES -> R.string.home_notes_subtitle
        HomeTab.NOTEBOOK -> R.string.home_notebook_subtitle
    }
    val resolvedTitle = titleOverride.trim().ifBlank { stringResource(titleRes) }
    val showSubtitle = titleOverride.isBlank()
    val subtitle = spaceTitle.ifBlank { stringResource(subtitleRes) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HomeBackButton(onClick = onBackClick)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = resolvedTitle,
                    modifier = Modifier.weight(1f),
                    fontFamily = CormorantGaramond,
                    fontSize = if (titleOverride.isBlank()) 36.sp else 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontStyle = FontStyle.Italic,
                    color = Color.White,
                    letterSpacing = (-0.4).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (showSubtitle) {
                Text(
                    text = subtitle,
                    modifier = Modifier.padding(start = 30.dp),
                    fontSize = 14.sp,
                    color = HomeSearchPlaceholder,
                    maxLines = 1,
                )
            }
        }
        if (
            selectedTab == HomeTab.SOURCES ||
            selectedTab == HomeTab.ASK ||
            selectedTab == HomeTab.NOTES ||
            selectedTab == HomeTab.NOTEBOOK
        ) {
            HomeHeaderAddButton(onClick = onAddClick)
        }
    }
}

@Composable
private fun HomeHeaderAddButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .border(1.dp, Color.White, CircleShape)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_add),
            contentDescription = stringResource(R.string.home_add),
            tint = Color.White,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
internal fun HomeBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(30.dp)
            .border(1.dp, Color.White, CircleShape)
            .clip(CircleShape)
            .background(HomeSearchField)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_back),
            contentDescription = stringResource(R.string.home_back),
            tint = Color.White,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun HomeHeaderPreviewContent(
    selectedTab: HomeTab,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(HomeHeader)
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 16.dp),
    ) {
        HomeHeaderRow(
            selectedTab = selectedTab,
            onBackClick = {},
            onAddClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 393, name = "Header — Sources")
@Composable
private fun HomeHeaderSourcesPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        HomeHeaderPreviewContent(selectedTab = HomeTab.SOURCES)
    }
}

@Preview(showBackground = true, widthDp = 393, name = "Header — Ask")
@Composable
private fun HomeHeaderAskPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        HomeHeaderPreviewContent(selectedTab = HomeTab.ASK)
    }
}

@Preview(showBackground = true, widthDp = 393, name = "Header — Notebook")
@Composable
private fun HomeHeaderNotebookPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        HomeHeaderPreviewContent(selectedTab = HomeTab.NOTEBOOK)
    }
}
