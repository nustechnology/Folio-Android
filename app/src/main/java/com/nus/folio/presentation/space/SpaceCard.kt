package com.nus.folio.presentation.space

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nus.folio.R
import com.nus.folio.components.FolioSkeletonBar
import com.nus.folio.components.FolioSkeletonColumn
import com.nus.folio.components.FolioSkeletonList
import com.nus.folio.domain.model.Space
import com.nus.folio.ui.theme.CormorantGaramond
import com.nus.folio.ui.theme.HomeCardBackground
import com.nus.folio.ui.theme.HomeCardBorder
import com.nus.folio.ui.theme.HomeTextPrimary
import com.nus.folio.ui.theme.HomeTextSecondary
import com.nus.folio.ui.theme.HomeTypeBadgeBackground

@Composable
internal fun SpacesSkeletonList() {
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
internal fun SpaceCard(
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
