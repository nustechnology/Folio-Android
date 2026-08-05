package com.nus.folio.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nus.folio.ui.theme.HomeTypeBadgeBackground

@Composable
fun FolioSkeletonBar(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(6.dp),
) {
    val transition = rememberInfiniteTransition(label = "folio_skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "folio_skeleton_alpha",
    )
    Box(
        modifier = modifier
            .clip(shape)
            .background(HomeTypeBadgeBackground.copy(alpha = alpha)),
    )
}

@Composable
fun FolioSkeletonList(
    itemCount: Int = 6,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
    verticalSpacing: Dp = 10.dp,
    modifier: Modifier = Modifier,
    itemContent: @Composable () -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(verticalSpacing),
        userScrollEnabled = false,
    ) {
        items(itemCount) {
            itemContent()
        }
    }
}

@Composable
fun FolioSkeletonColumn(
    lineCount: Int,
    lineHeight: Dp = 12.dp,
    spacing: Dp = 8.dp,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing),
    ) {
        repeat(lineCount) { index ->
            val widthFraction = when (index % 3) {
                0 -> 0.92f
                1 -> 0.72f
                else -> 0.55f
            }
            FolioSkeletonBar(
                modifier = Modifier
                    .fillMaxWidth(widthFraction)
                    .height(lineHeight),
            )
        }
    }
}
