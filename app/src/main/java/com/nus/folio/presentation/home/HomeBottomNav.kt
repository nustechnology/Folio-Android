package com.nus.folio.presentation.home

import android.annotation.SuppressLint
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nus.folio.R
import com.nus.folio.ui.theme.FolioAndroidTheme
import com.nus.folio.ui.theme.HomeBackground
import com.nus.folio.ui.theme.HomeHeader
import com.nus.folio.ui.theme.HomeNavAccent

private val NavIndicatorWidth = 28.dp
private val NavIndicatorHeight = 2.5.dp

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
internal fun HomeBottomNav(
    selectedTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = listOf(
        Triple(HomeTab.SOURCES, R.drawable.ic_nav_sources, R.string.home_tab_sources),
        Triple(HomeTab.ASK, R.drawable.ic_nav_ask, R.string.home_tab_ask),
        Triple(HomeTab.NOTES, R.drawable.ic_nav_notes, R.string.home_tab_notes),
        Triple(HomeTab.NOTEBOOK, R.drawable.ic_nav_notebook, R.string.home_tab_notebook),
    )
    val selectedIndex = tabs.indexOfFirst { it.first == selectedTab }.coerceAtLeast(0)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = HomeNavPillShape,
                ambientColor = Color.Black.copy(alpha = 0.25f),
                spotColor = Color.Black.copy(alpha = 0.25f),
            )
            .clip(HomeNavPillShape)
            .background(HomeNavPillBackground)
            .padding(vertical = 12.dp, horizontal = 8.dp),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val tabWidth = maxWidth / tabs.size
            val indicatorOffset by animateDpAsState(
                targetValue = tabWidth * selectedIndex + (tabWidth - NavIndicatorWidth) / 2,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
                label = "navIndicatorOffset",
            )

            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(NavIndicatorWidth)
                    .height(NavIndicatorHeight)
                    .align(Alignment.TopStart)
                    .clip(RoundedCornerShape(1.dp))
                    .background(HomeNavAccent),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEach { (tab, iconRes, labelRes) ->
                    HomeNavItem(
                        selected = selectedTab == tab,
                        iconRes = iconRes,
                        labelRes = labelRes,
                        onClick = { onTabSelected(tab) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeNavItem(
    selected: Boolean,
    iconRes: Int,
    labelRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            HomeNavAccent
        } else {
            HomeNavInactiveTint
        },
        label = "navItemColor",
    )

    Column(
        modifier = modifier
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            )
            .padding(top = 6.dp, bottom = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = stringResource(labelRes),
            fontSize = 11.sp,
            color = contentColor,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
        )
    }
}

@Preview(showBackground = true, widthDp = 393, name = "Bottom nav — Sources")
@Composable
private fun HomeBottomNavSourcesPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(HomeBackground)
                .padding(16.dp),
        ) {
            HomeBottomNav(
                selectedTab = HomeTab.SOURCES,
                onTabSelected = {},
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 393, name = "Bottom nav — Ask")
@Composable
private fun HomeBottomNavAskPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(HomeBackground)
                .padding(16.dp),
        ) {
            HomeBottomNav(
                selectedTab = HomeTab.ASK,
                onTabSelected = {},
            )
        }
    }
}
