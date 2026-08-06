package com.nus.folio.presentation.home.pane

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nus.folio.R
import com.nus.folio.presentation.home.AskButtonBackground
import com.nus.folio.presentation.home.AskSparkleBorder
import com.nus.folio.presentation.home.AskSparkleCircleShape
import com.nus.folio.presentation.home.AskSuggestionShape
import com.nus.folio.ui.theme.HomeCardBackground
import com.nus.folio.ui.theme.HomeCardBorder
import com.nus.folio.ui.theme.HomeHeader
import com.nus.folio.ui.theme.HomeStatusProcessingBackground
import com.nus.folio.ui.theme.HomeStatusProcessingText
import com.nus.folio.ui.theme.HomeTextPrimary
import com.nus.folio.ui.theme.HomeTextSecondary

@Composable
internal fun AskNoEvidenceBanner(
    onAddSourceClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(AskSuggestionShape)
            .background(HomeStatusProcessingBackground)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.home_ask_no_evidence),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = HomeStatusProcessingText,
        )
        Text(
            text = stringResource(R.string.home_ask_no_evidence_cta),
            modifier = Modifier.clickable(onClick = onAddSourceClick),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = HomeHeader,
        )
    }
}

@Composable
internal fun AskPaneHeader() {
    Box(
        modifier = Modifier
            .size(48.dp)
            .border(1.dp, AskSparkleBorder, AskSparkleCircleShape)
            .clip(AskSparkleCircleShape)
            .background(HomeCardBackground),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_ask_sparkle),
            contentDescription = null,
            tint = AskButtonBackground,
            modifier = Modifier.size(24.dp),
        )
    }
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = stringResource(R.string.home_ask_question_title),
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        color = HomeTextPrimary,
        textAlign = TextAlign.Center,
    )
}

@Composable
internal fun AskSuggestionCard(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f)
            .clip(AskSuggestionShape)
            .background(HomeCardBackground)
            .border(1.dp, HomeCardBorder, AskSuggestionShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = HomeTextSecondary,
        lineHeight = 20.sp,
    )
}
