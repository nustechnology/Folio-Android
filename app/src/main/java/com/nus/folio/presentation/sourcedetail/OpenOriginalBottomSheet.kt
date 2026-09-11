package com.nus.folio.presentation.sourcedetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nus.folio.R
import com.nus.folio.components.AnimatedModalSheet
import com.nus.folio.domain.model.SourceType
import com.nus.folio.presentation.home.HomeSheetShape
import com.nus.folio.presentation.home.bottomsheet.AddSourceCancelButton
import com.nus.folio.presentation.home.bottomsheet.AddSourceDragHandle
import com.nus.folio.presentation.home.bottomsheet.AddSourceSubmitButton
import com.nus.folio.ui.theme.CormorantGaramond
import com.nus.folio.ui.theme.FolioAndroidTheme
import com.nus.folio.ui.theme.HomeSheetBackground
import com.nus.folio.ui.theme.HomeTextPrimary
import com.nus.folio.ui.theme.HomeTextSecondary

@Composable
internal fun OpenOriginalBottomSheet(
    sourceType: SourceType,
    displayValue: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit = {},
) {
    AnimatedModalSheet(
        onDismiss = onDismiss,
    ) { requestDismiss ->
        AddSourceDragHandle()
        OpenOriginalSheetContent(
            sourceType = sourceType,
            displayValue = displayValue,
            onCancelClick = { requestDismiss() },
            onConfirm = { requestDismiss { onConfirm() } },
        )
    }
}

@Composable
private fun OpenOriginalSheetContent(
    sourceType: SourceType,
    displayValue: String,
    onCancelClick: () -> Unit,
    onConfirm: () -> Unit,
) {
    val isWeb = sourceType == SourceType.WEB
    Column {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.source_detail_open_sheet_title),
            fontFamily = CormorantGaramond,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            color = HomeTextPrimary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(
                if (isWeb) {
                    R.string.source_detail_open_sheet_message_web
                } else {
                    R.string.source_detail_open_sheet_message
                },
            ),
            fontSize = 14.sp,
            color = HomeTextPrimary,
            lineHeight = 20.sp,
        )
        if (displayValue.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(
                    if (isWeb) {
                        R.string.source_detail_open_sheet_link
                    } else {
                        R.string.source_detail_open_sheet_file
                    },
                    displayValue,
                ),
                modifier = Modifier.fillMaxWidth(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = HomeTextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AddSourceCancelButton(
                onClick = onCancelClick,
                modifier = Modifier.weight(1f),
            )
            AddSourceSubmitButton(
                enabled = true,
                onClick = onConfirm,
                modifier = Modifier.weight(1f),
                labelRes = R.string.source_detail_open_original,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852, backgroundColor = 0xFFF7F1E6)
@Composable
private fun OpenOriginalBottomSheetFilePreview() {
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
                OpenOriginalSheetContent(
                    sourceType = SourceType.FILE,
                    displayValue = "alan-turing-computing-machinery.pdf",
                    onCancelClick = {},
                    onConfirm = {},
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852, backgroundColor = 0xFFF7F1E6)
@Composable
private fun OpenOriginalBottomSheetWebPreview() {
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
                OpenOriginalSheetContent(
                    sourceType = SourceType.WEB,
                    displayValue = "https://en.wikipedia.org/wiki/Artificial_neural_network",
                    onCancelClick = {},
                    onConfirm = {},
                )
            }
        }
    }
}
