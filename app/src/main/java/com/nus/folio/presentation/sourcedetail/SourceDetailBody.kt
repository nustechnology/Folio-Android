package com.nus.folio.presentation.sourcedetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nus.folio.R
import com.nus.folio.domain.model.SourceContentFormat
import com.nus.folio.domain.model.SourceDetail
import com.nus.folio.domain.model.SourceSheetTab
import com.nus.folio.domain.model.SourceStatus
import com.nus.folio.domain.model.StructuredContent
import com.nus.folio.domain.model.StructuredContentHtml
import com.nus.folio.presentation.home.HomeCardShape
import com.nus.folio.ui.theme.HomeCardBackground
import com.nus.folio.ui.theme.HomeCardBorder
import com.nus.folio.ui.theme.HomeTextPrimary
import com.nus.folio.ui.theme.HomeTextSecondary

private val ContentCardVerticalPadding = 12.dp

@Composable
internal fun SourceDetailBody(
    detail: SourceDetail,
    selectedSheetIndex: Int,
    isContentLoading: Boolean,
    highlightText: String?,
    onSheetSelected: (Int) -> Unit,
) {
    if (detail.status != SourceStatus.READY) return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .padding(bottom = 16.dp),
    ) {
        if (!isContentLoading &&
            detail.contentFormat == SourceContentFormat.SHEET &&
            detail.sheets.size > 1
        ) {
            SheetTabSelector(
                sheets = detail.sheets,
                selectedIndex = selectedSheetIndex,
                onSheetSelected = onSheetSelected,
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (isContentLoading) {
            SourceDetailHtmlLoading()
        } else {
            val htmlBody = when (val structured = detail.structuredContent) {
                is StructuredContent.Document -> structured.html
                is StructuredContent.Slides ->
                    StructuredContentHtml.slidesToHtml(structured.slides)
                is StructuredContent.Sheets ->
                    detail.sheets.getOrNull(selectedSheetIndex)?.htmlTable
                        ?: StructuredContentHtml.body(structured, selectedSheetIndex)
                null -> when {
                    detail.contentFormat == SourceContentFormat.SHEET ->
                        detail.sheets.getOrNull(selectedSheetIndex)?.htmlTable
                            ?: detail.htmlContent.orEmpty()
                    else -> detail.htmlContent.orEmpty()
                }
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
            ) {
                val maxWebHeight = (maxHeight - ContentCardVerticalPadding * 2)
                    .coerceAtLeast(0.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .clip(HomeCardShape)
                        .background(HomeCardBackground)
                        .border(1.dp, HomeCardBorder, HomeCardShape)
                        .padding(
                            horizontal = 16.dp,
                            vertical = ContentCardVerticalPadding,
                        ),
                ) {
                    SourceHtmlRenderer(
                        htmlBody = htmlBody,
                        contentFormat = detail.contentFormat,
                        maxHeight = maxWebHeight,
                        highlightText = highlightText,
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetTabSelector(
    sheets: List<SourceSheetTab>,
    selectedIndex: Int,
    onSheetSelected: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedSheet = sheets.getOrNull(selectedIndex)

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(HomeCardShape)
                .background(HomeCardBackground)
                .border(1.dp, HomeCardBorder, HomeCardShape)
                .clickable { expanded = true }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.source_detail_sheet_label),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = HomeTextSecondary,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = selectedSheet?.name.orEmpty(),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = HomeTextPrimary,
            )
            Spacer(modifier = Modifier.size(8.dp))
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = HomeTextSecondary,
                modifier = Modifier.size(18.dp),
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            sheets.forEachIndexed { index, sheet ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = sheet.name,
                            fontWeight = if (index == selectedIndex) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    },
                    onClick = {
                        expanded = false
                        onSheetSelected(index)
                    },
                )
            }
        }
    }
}
