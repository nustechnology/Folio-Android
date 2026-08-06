package com.nus.folio.presentation.sourcedetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nus.folio.R
import com.nus.folio.presentation.home.HomeCardShape
import com.nus.folio.ui.theme.HomeCardBackground
import com.nus.folio.ui.theme.HomeCardBorder
import com.nus.folio.ui.theme.HomeHeader
import com.nus.folio.ui.theme.HomeStatusFailedBackground
import com.nus.folio.ui.theme.HomeStatusFailedText
import com.nus.folio.ui.theme.HomeStatusProcessingBackground
import com.nus.folio.ui.theme.HomeStatusProcessingText
import com.nus.folio.ui.theme.HomeTextPrimary
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
internal fun SourceDetailProcessingState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(HomeStatusProcessingBackground)
                    .border(1.dp, HomeStatusProcessingText.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_toast_warning),
                    contentDescription = null,
                    tint = HomeStatusProcessingText,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.source_detail_processing_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = HomeTextPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.source_detail_processing),
                fontSize = 14.sp,
                color = HomeTextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
            )
        }
    }
}

@Composable
internal fun SourceDetailFailedState(
    isRetrying: Boolean,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(HomeStatusFailedBackground)
                    .border(1.dp, HomeStatusFailedText.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_toast_error),
                    contentDescription = null,
                    tint = HomeStatusFailedText,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.source_detail_failed_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = HomeTextPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.source_detail_failed),
                fontSize = 14.sp,
                color = HomeTextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onRetry,
                enabled = !isRetrying,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HomeHeader,
                    contentColor = Color.White,
                    disabledContainerColor = HomeHeader.copy(alpha = 0.7f),
                    disabledContentColor = Color.White,
                ),
            ) {
                if (isRetrying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Text(
                    text = stringResource(R.string.source_detail_retry_processing),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
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
