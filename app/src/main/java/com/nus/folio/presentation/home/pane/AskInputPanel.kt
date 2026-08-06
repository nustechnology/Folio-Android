package com.nus.folio.presentation.home.pane

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nus.folio.R
import com.nus.folio.presentation.home.AskButtonBackground
import com.nus.folio.presentation.home.AskInputMaxHeight
import com.nus.folio.presentation.home.AskInputMinHeight
import com.nus.folio.presentation.home.AskInputShape
import com.nus.folio.presentation.home.AskSourceChipBackground
import com.nus.folio.presentation.home.AskSourceChipShape
import com.nus.folio.presentation.home.AskSubmitShape
import com.nus.folio.ui.theme.HomeCardBackground
import com.nus.folio.ui.theme.HomeCardBorder
import com.nus.folio.ui.theme.HomeTextPrimary
import com.nus.folio.ui.theme.LoginPlaceholder

@Composable
internal fun AskInputPanel(
    query: String,
    onQueryChange: (String) -> Unit,
    scopeChipLabel: String,
    onScopeChipClick: () -> Unit,
    onAskSubmit: () -> Unit,
    inputEnabled: Boolean,
    scopeChipEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val canSubmit = inputEnabled && query.isNotBlank()
    val inputScrollState = rememberScrollState()
    val panelAlpha = if (inputEnabled) 1f else 0.55f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .alpha(panelAlpha)
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
                    lineHeight = 22.sp,
                    modifier = Modifier.align(Alignment.TopStart),
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                enabled = inputEnabled,
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
            if (query.lines().size > 2 || query.length > 80) {
                AskResizeHint(modifier = Modifier.align(Alignment.BottomEnd))
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AskSourceScopeChip(
                label = scopeChipLabel,
                enabled = scopeChipEnabled,
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
internal fun AskSourceScopeChip(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .widthIn(max = 200.dp)
            .alpha(if (enabled) 1f else 0.55f)
            .clip(AskSourceChipShape)
            .background(AskSourceChipBackground)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = HomeTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Icon(
            painter = painterResource(R.drawable.ic_chevron_down),
            contentDescription = stringResource(R.string.answer_scope_title),
            tint = HomeTextPrimary,
            modifier = Modifier.size(14.dp),
        )
    }
}

@Composable
internal fun AskSubmitButton(
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
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.home_ask_submit),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
        )
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
internal fun AskResizeHint(modifier: Modifier = Modifier) {
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
