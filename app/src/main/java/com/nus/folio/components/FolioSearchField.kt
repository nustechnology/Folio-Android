package com.nus.folio.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nus.folio.R
import com.nus.folio.ui.theme.FolioAndroidTheme
import com.nus.folio.ui.theme.HomeHeader
import com.nus.folio.ui.theme.HomeSearchField
import com.nus.folio.ui.theme.HomeSearchPlaceholder

private val FolioSearchShape = RoundedCornerShape(12.dp)

@Composable
fun FolioSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    searchContentDescription: String = stringResource(R.string.home_search),
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(FolioSearchShape)
            .background(HomeSearchField)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_search),
            contentDescription = searchContentDescription,
            tint = HomeSearchPlaceholder,
            modifier = Modifier.size(20.dp),
        )
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = placeholder,
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
        if (query.isNotEmpty()) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = stringResource(R.string.search_clear),
                tint = HomeSearchPlaceholder,
                modifier = Modifier
                    .size(20.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onQueryChange("") },
                    ),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 393, backgroundColor = 0xFF0B2A24, name = "Empty")
@Composable
private fun FolioSearchFieldEmptyPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        Column(
            modifier = Modifier
                .background(HomeHeader)
                .padding(20.dp),
        ) {
            FolioSearchField(
                query = "",
                onQueryChange = {},
                placeholder = "Search spaces",
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 393, backgroundColor = 0xFF0B2A24, name = "With text")
@Composable
private fun FolioSearchFieldFilledPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        Column(
            modifier = Modifier
                .background(HomeHeader)
                .padding(20.dp),
        ) {
            FolioSearchField(
                query = "Dissertation",
                onQueryChange = {},
                placeholder = "Search spaces",
            )
        }
    }
}
