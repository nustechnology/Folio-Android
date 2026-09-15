package com.nus.folio.presentation.sourcedetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nus.folio.presentation.home.HomeCardShape
import com.nus.folio.ui.theme.HomeCardBackground
import com.nus.folio.ui.theme.HomeCardBorder
import com.nus.folio.ui.theme.HomeHeader
import com.nus.folio.ui.theme.HomeStatusFailedText
import com.nus.folio.ui.theme.HomeTextSecondary

@Composable
internal fun SourceDetailContentLoading() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = HomeHeader)
    }
}

@Composable
internal fun SourceDetailHtmlLoading() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(HomeCardShape)
            .background(HomeCardBackground)
            .border(1.dp, HomeCardBorder, HomeCardShape),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = HomeHeader)
    }
}

@Composable
internal fun SourceDetailMessageState(
    message: String,
    actionLabel: String?,
    onAction: () -> Unit,
    isError: Boolean,
    isActionLoading: Boolean = false,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = message,
                fontSize = 15.sp,
                color = if (isError) HomeStatusFailedText else HomeTextSecondary,
                textAlign = TextAlign.Center,
            )
            if (actionLabel != null) {
                Spacer(modifier = Modifier.height(12.dp))
                if (isActionLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = HomeHeader,
                        strokeWidth = 2.5.dp,
                    )
                } else {
                    Text(
                        text = actionLabel,
                        modifier = Modifier.clickable(onClick = onAction),
                        color = HomeHeader,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}
