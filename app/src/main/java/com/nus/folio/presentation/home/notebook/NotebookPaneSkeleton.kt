package com.nus.folio.presentation.home.notebook

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.nus.folio.R
import com.nus.folio.components.FolioSkeletonBar
import com.nus.folio.components.FolioSkeletonColumn
import com.nus.folio.components.FolioSkeletonList
import com.nus.folio.presentation.home.HomeBadgeShape
import com.nus.folio.presentation.home.HomeCardShape
import com.nus.folio.ui.theme.HomeCardBackground
import com.nus.folio.ui.theme.HomeCardBorder

@Composable
internal fun NotebookPaneSkeleton(
    showNotesSidebar: Boolean,
    modifier: Modifier = Modifier,
) {
    val loadingDescription = stringResource(R.string.notebook_loading)
    Row(
        modifier = modifier
            .fillMaxSize()
            .semantics { contentDescription = loadingDescription },
    ) {
        NotebookEditorSkeleton(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        )
        if (showNotesSidebar) {
            NotebookNotesSidebarSkeleton(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun NotebookEditorSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FolioSkeletonBar(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape = HomeCardShape,
        )
        FolioSkeletonColumn(
            lineCount = 10,
            lineHeight = 14.dp,
            spacing = 10.dp,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun NotebookNotesSidebarSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(HomeCardBackground)
            .border(1.dp, HomeCardBorder)
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        FolioSkeletonBar(
            modifier = Modifier
                .fillMaxWidth(0.45f)
                .height(20.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        FolioSkeletonList(itemCount = 4) {
            NotebookNoteItemSkeleton()
        }
    }
}

@Composable
private fun NotebookNoteItemSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(HomeCardShape)
            .background(HomeCardBackground)
            .border(1.dp, HomeCardBorder, HomeCardShape)
            .padding(12.dp),
        verticalAlignment = androidx.compose.ui.Alignment.Top,
    ) {
        FolioSkeletonBar(
            modifier = Modifier
                .width(32.dp)
                .height(32.dp),
            shape = HomeBadgeShape,
        )
        Spacer(modifier = Modifier.width(10.dp))
        FolioSkeletonColumn(
            lineCount = 2,
            lineHeight = 10.dp,
            spacing = 6.dp,
            modifier = Modifier.weight(1f),
        )
    }
}
