package com.nus.folio.presentation.home.pane

import com.nus.folio.presentation.home.AskButtonBackground
import com.nus.folio.presentation.home.AskInputMaxHeight
import com.nus.folio.presentation.home.AskInputMinHeight
import com.nus.folio.presentation.home.AskInputShape
import com.nus.folio.presentation.home.AskSourceChipBackground
import com.nus.folio.presentation.home.AskSourceChipShape
import com.nus.folio.presentation.home.AskSparkleBorder
import com.nus.folio.presentation.home.AskSparkleCircleShape
import com.nus.folio.presentation.home.AskSubmitShape
import com.nus.folio.presentation.home.AskSuggestionShape
import com.nus.folio.presentation.home.HomeUiState
import com.nus.folio.presentation.home.askScopeChipLabelRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nus.folio.R
import com.nus.folio.ui.theme.FolioAndroidTheme
import com.nus.folio.ui.theme.HomeBackground
import com.nus.folio.ui.theme.HomeCardBackground
import com.nus.folio.ui.theme.HomeCardBorder
import com.nus.folio.ui.theme.HomeHeader
import com.nus.folio.ui.theme.HomeStatusFailedText
import com.nus.folio.ui.theme.HomeTextPrimary
import com.nus.folio.ui.theme.HomeTextSecondary
import com.nus.folio.ui.theme.LoginPlaceholder

@Composable
internal fun AskPane(
    uiState: HomeUiState,
    onRetry: () -> Unit,
    onAskSubmit: () -> Unit,
    onScopeChipClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        uiState.isLoading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = HomeHeader)
            }
        }
        uiState.askError != null -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = uiState.askError.ifBlank {
                            stringResource(R.string.home_error_generic)
                        },
                        color = HomeStatusFailedText,
                        fontSize = 14.sp,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.home_retry),
                        modifier = Modifier.clickable(onClick = onRetry),
                        color = HomeHeader,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
        else -> {
            AskPaneContent(
                scopeChipLabelRes = uiState.askScopeChipLabelRes(),
                onScopeChipClick = onScopeChipClick,
                onAskSubmit = onAskSubmit,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun AskPaneContent(
    scopeChipLabelRes: Int,
    onScopeChipClick: () -> Unit,
    onAskSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var askQuery by rememberSaveable { mutableStateOf("") }
    val suggestions = listOf(
        stringResource(R.string.home_ask_suggestion_1),
        stringResource(R.string.home_ask_suggestion_2),
        stringResource(R.string.home_ask_suggestion_3),
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(28.dp))
            AskPaneHeader()
            Spacer(modifier = Modifier.height(28.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                suggestions.forEach { suggestion ->
                    AskSuggestionCard(
                        text = suggestion,
                        onClick = { askQuery = suggestion },
                    )
                }
            }
        }
        AskInputPanel(
            query = askQuery,
            onQueryChange = { askQuery = it },
            scopeChipLabelRes = scopeChipLabelRes,
            onScopeChipClick = onScopeChipClick,
            onAskSubmit = {
                if (askQuery.isNotBlank()) {
                    onAskSubmit()
                }
            },
            modifier = Modifier.padding(bottom = 35.dp),
        )
    }
}

@Composable
private fun AskPaneHeader() {
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
private fun AskSuggestionCard(
    text: String,
    onClick: () -> Unit,
) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .clip(AskSuggestionShape)
            .background(HomeCardBackground)
            .border(1.dp, HomeCardBorder, AskSuggestionShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = HomeTextSecondary,
        lineHeight = 20.sp,
    )
}

@Composable
private fun AskInputPanel(
    query: String,
    onQueryChange: (String) -> Unit,
    scopeChipLabelRes: Int,
    onScopeChipClick: () -> Unit,
    onAskSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val canSubmit = query.isNotBlank()
    val inputScrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(AskInputShape)
            .background(HomeCardBackground)
            .border(1.dp, HomeCardBorder, AskInputShape)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = AskInputMinHeight, max = AskInputMaxHeight),
        ) {
            if (query.isEmpty()) {
                Text(
                    text = stringResource(R.string.home_ask_input_hint),
                    color = LoginPlaceholder,
                    fontSize = 15.sp,
                    modifier = Modifier.align(Alignment.TopStart),
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                textStyle = TextStyle(
                    color = HomeTextPrimary,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                ),
                cursorBrush = SolidColor(HomeTextPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = AskInputMinHeight, max = AskInputMaxHeight)
                    .verticalScroll(inputScrollState),
            )
            AskResizeHint(modifier = Modifier.align(Alignment.BottomEnd))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AskSourceScopeChip(
                labelRes = scopeChipLabelRes,
                onClick = onScopeChipClick,
            )
            Spacer(modifier = Modifier.weight(1f))
            AskSubmitButton(
                enabled = canSubmit,
                onClick = onAskSubmit,
            )
        }
    }
}

@Composable
private fun AskSourceScopeChip(
    labelRes: Int,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(AskSourceChipShape)
            .background(AskSourceChipBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(labelRes),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = HomeTextPrimary,
        )
        Icon(
            painter = painterResource(R.drawable.ic_chevron_down),
            contentDescription = null,
            tint = HomeTextPrimary,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun AskSubmitButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val contentColor = if (enabled) {
        Color.White
    } else {
        Color.White.copy(alpha = 0.7f)
    }
    Row(
        modifier = Modifier
            .clip(AskSubmitShape)
            .background(
                if (enabled) {
                    AskButtonBackground
                } else {
                    AskButtonBackground.copy(alpha = 0.35f)
                },
            )
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = stringResource(R.string.home_ask_submit),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
        )
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun AskResizeHint(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(2.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .height(1.dp)
                    .background(HomeCardBorder),
            )
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .height(1.dp)
                    .background(HomeCardBorder),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .height(1.dp)
                    .background(HomeCardBorder),
            )
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .height(1.dp)
                    .background(HomeCardBorder),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 700, name = "Ask — question pane")
@Composable
private fun AskPanePreview() {
    FolioAndroidTheme(dynamicColor = false) {
        AskPane(
            uiState = HomeUiState(),
            onRetry = {},
            onAskSubmit = {},
            onScopeChipClick = {},
            modifier = Modifier.background(HomeBackground),
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 700, name = "Ask — loading")
@Composable
private fun AskPaneLoadingPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        AskPane(
            uiState = HomeUiState(isLoading = true),
            onRetry = {},
            onAskSubmit = {},
            onScopeChipClick = {},
            modifier = Modifier.background(HomeBackground),
        )
    }
}
