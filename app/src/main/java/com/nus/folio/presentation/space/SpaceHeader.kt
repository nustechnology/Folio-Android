package com.nus.folio.presentation.space

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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nus.folio.R
import com.nus.folio.presentation.home.HomeFilterActiveDot
import com.nus.folio.ui.theme.CormorantGaramond
import com.nus.folio.ui.theme.HomeHeader
import com.nus.folio.ui.theme.HomeSearchField
import com.nus.folio.ui.theme.HomeSearchPlaceholder
import com.nus.folio.ui.theme.LoginCopper

@Composable
internal fun SpaceHeaderRow(
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
internal fun SpaceFilterSortButton(
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
internal fun SpaceAddFab(
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
