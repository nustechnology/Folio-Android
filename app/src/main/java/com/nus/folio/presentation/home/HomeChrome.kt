package com.nus.folio.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nus.folio.R
import com.nus.folio.components.BouncingDotsIndicator
import com.nus.folio.ui.theme.HomeSearchField
import com.nus.folio.ui.theme.HomeSearchPlaceholder

@Composable
internal fun HomeFilterSortButton(
    onClick: () -> Unit,
    showActiveIndicator: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(HomeCardShape)
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
            contentDescription = stringResource(R.string.home_filter_sort),
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
internal fun SourceOpeningScreen(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.48f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            ),
        contentAlignment = Alignment.Center,
    ) {
        BouncingDotsIndicator()
    }
}
