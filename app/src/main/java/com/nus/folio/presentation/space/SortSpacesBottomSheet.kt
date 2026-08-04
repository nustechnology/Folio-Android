package com.nus.folio.presentation.space

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nus.folio.R
import com.nus.folio.components.AnimatedModalSheet
import com.nus.folio.domain.model.SpaceSort
import com.nus.folio.presentation.home.AskSuggestionShape
import com.nus.folio.presentation.home.HomeSheetShape
import com.nus.folio.presentation.home.HomeSourceFilterChipSelected
import com.nus.folio.presentation.home.bottomsheet.AddSourceDragHandle
import com.nus.folio.ui.theme.CormorantGaramond
import com.nus.folio.ui.theme.FolioAndroidTheme
import com.nus.folio.ui.theme.HomeCardBackground
import com.nus.folio.ui.theme.HomeCardBorder
import com.nus.folio.ui.theme.HomeHeader
import com.nus.folio.ui.theme.HomeSheetBackground
import com.nus.folio.ui.theme.HomeTextPrimary

@Composable
internal fun SortSpacesBottomSheet(
    selectedSort: SpaceSort,
    onSortSelected: (SpaceSort) -> Unit,
    onDismiss: () -> Unit,
) {
    AnimatedModalSheet(
        onDismiss = onDismiss,
    ) { requestDismiss ->
        AddSourceDragHandle()
        SortSpacesSheetContent(
            selectedSort = selectedSort,
            onSortSelected = { sort ->
                requestDismiss { onSortSelected(sort) }
            },
        )
    }
}

@Composable
private fun SortSpacesSheetContent(
    selectedSort: SpaceSort,
    onSortSelected: (SpaceSort) -> Unit,
) {
    Column {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.space_sort_title),
            fontFamily = CormorantGaramond,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            color = HomeTextPrimary,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SpaceSort.entries.forEach { sort ->
                SortSpacesOption(
                    label = stringResource(sort.labelRes),
                    selected = selectedSort == sort,
                    onClick = { onSortSelected(sort) },
                )
            }
        }
    }
}

@Composable
private fun SortSpacesOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) HomeHeader else HomeCardBorder
    val backgroundColor = if (selected) {
        HomeSourceFilterChipSelected
    } else {
        HomeCardBackground
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AskSuggestionShape)
            .background(backgroundColor)
            .border(1.dp, borderColor, AskSuggestionShape)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            )
            .padding(start = 4.dp, end = 18.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = HomeHeader,
                unselectedColor = HomeCardBorder,
            ),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = HomeTextPrimary,
            lineHeight = 20.sp,
        )
    }
}

private val SpaceSort.labelRes: Int
    get() = when (this) {
        SpaceSort.RECENTLY_CREATED -> R.string.space_sort_recently_created
        SpaceSort.RECENTLY_UPDATED -> R.string.space_sort_recently_updated
        SpaceSort.ALPHABETICAL_AZ -> R.string.space_sort_alphabetical_az
        SpaceSort.ALPHABETICAL_ZA -> R.string.space_sort_alphabetical_za
    }

@Preview(showBackground = true, widthDp = 393, heightDp = 852, backgroundColor = 0xFFF7F1E6)
@Composable
private fun SortSpacesSheetContentPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(HomeSheetBackground, HomeSheetShape)
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 20.dp),
            ) {
                AddSourceDragHandle()
                SortSpacesSheetContent(
                    selectedSort = SpaceSort.RECENTLY_UPDATED,
                    onSortSelected = {},
                )
            }
        }
    }
}
