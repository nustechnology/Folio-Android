package com.nus.folio.presentation.home.notebook

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nus.folio.R
import com.nus.folio.ui.theme.CormorantGaramond
import com.nus.folio.ui.theme.HomeTextSecondary

@Composable
internal fun NotebookEmptyPlaceholder(
    heading: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = heading,
            fontFamily = CormorantGaramond,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            color = HomeTextSecondary.copy(alpha = 0.55f),
            lineHeight = 34.sp,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.notebook_empty_placeholder),
            fontSize = 16.sp,
            color = HomeTextSecondary.copy(alpha = 0.55f),
            lineHeight = 24.sp,
        )
    }
}

internal fun notebookEmptyHeading(spaceTitle: String, fallback: String): String =
    spaceTitle.trim().ifBlank { fallback }

internal fun isNotebookContentEmpty(content: String): Boolean = content.isBlank()
