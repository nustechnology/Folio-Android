package com.nus.folio.presentation.sourcedetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nus.folio.R
import com.nus.folio.domain.model.SourceDetail
import com.nus.folio.domain.model.SourceStatus
import com.nus.folio.domain.model.SourceType
import com.nus.folio.presentation.home.HomeBadgeShape
import com.nus.folio.presentation.home.HomeStatusShape
import com.nus.folio.presentation.home.sourceTypeBadgeColors
import com.nus.folio.ui.theme.CormorantGaramond
import com.nus.folio.ui.theme.HomeCardBorder
import com.nus.folio.ui.theme.HomeHeader
import com.nus.folio.ui.theme.HomeStatusFailedBackground
import com.nus.folio.ui.theme.HomeStatusFailedText
import com.nus.folio.ui.theme.HomeStatusProcessingBackground
import com.nus.folio.ui.theme.HomeStatusProcessingText
import com.nus.folio.ui.theme.HomeStatusReadyBackground
import com.nus.folio.ui.theme.HomeStatusReadyText
import com.nus.folio.ui.theme.HomeTextPrimary
import com.nus.folio.ui.theme.HomeTextSecondary

@Composable
internal fun SourceDetailHeader(
    detail: SourceDetail?,
    showOpenOriginal: Boolean,
    onBackClick: () -> Unit,
    onMoreClick: () -> Unit,
    onAskSourceClick: () -> Unit,
    onOpenOriginalClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .border(1.dp, HomeCardBorder, CircleShape)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onBackClick,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_back),
                    contentDescription = stringResource(R.string.home_back),
                    tint = HomeTextPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(modifier = Modifier.size(12.dp))
            Text(
                text = detail?.title ?: stringResource(R.string.source_detail_title),
                fontFamily = CormorantGaramond,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                color = HomeHeader,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 32.sp,
            )
            if (detail != null) {
                Spacer(modifier = Modifier.width(8.dp))
                MoreOptionsIconButton(onClick = onMoreClick)
            }
        }

        if (detail != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    SourceTypeBadge(
                        label = sourceDetailBadgeLabel(detail),
                        type = detail.type,
                    )
                    SourceStatusBadge(status = detail.status)
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (detail.status == SourceStatus.READY) {
                        AskSourceButton(onClick = onAskSourceClick)
                    }
                    if (showOpenOriginal) {
                        OpenOriginalIconButton(onClick = onOpenOriginalClick)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (detail.author.isBlank()) {
                    detail.addedLabel
                } else {
                    stringResource(R.string.home_source_meta, detail.author, detail.addedLabel)
                },
                fontSize = 13.sp,
                color = HomeTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MoreOptionsIconButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .border(1.dp, HomeCardBorder, CircleShape)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_more_vertical),
            contentDescription = stringResource(R.string.home_notes_more),
            tint = HomeTextPrimary,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun OpenOriginalIconButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .border(1.dp, HomeCardBorder, CircleShape)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_open_external),
            contentDescription = stringResource(R.string.source_detail_open_original),
            tint = HomeTextPrimary,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun AskSourceButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(
            horizontal = 12.dp,
            vertical = 8.dp,
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = HomeHeader,
            contentColor = Color.White,
        ),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_ask_sparkle),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.source_detail_ask_source),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun SourceTypeBadge(
    label: String,
    type: SourceType,
) {
    val colors = sourceTypeBadgeColors(type)
    Text(
        text = label,
        modifier = Modifier
            .clip(HomeBadgeShape)
            .background(colors.background)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = colors.content,
    )
}

@Composable
private fun SourceStatusBadge(status: SourceStatus) {
    val (background, textColor, labelRes) = when (status) {
        SourceStatus.READY -> Triple(
            HomeStatusReadyBackground,
            HomeStatusReadyText,
            R.string.home_status_ready,
        )
        SourceStatus.PROCESSING -> Triple(
            HomeStatusProcessingBackground,
            HomeStatusProcessingText,
            R.string.home_status_processing,
        )
        SourceStatus.FAILED -> Triple(
            HomeStatusFailedBackground,
            HomeStatusFailedText,
            R.string.home_status_failed,
        )
    }
    Text(
        text = stringResource(labelRes),
        modifier = Modifier
            .clip(HomeStatusShape)
            .background(background)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = textColor,
    )
}

@Composable
private fun sourceDetailBadgeLabel(detail: SourceDetail): String {
    when (detail.type) {
        SourceType.TEXT -> return sourceTypeLabel(SourceType.TEXT)
        SourceType.WEB -> return sourceTypeLabel(SourceType.WEB)
        SourceType.FILE, SourceType.BOOK -> Unit
    }
    val extension = detail.fileExtension.trim()
        .takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
    if (extension != null) return extension.uppercase()
    return sourceTypeLabel(detail.type)
}

@Composable
private fun sourceTypeLabel(type: SourceType): String = when (type) {
    SourceType.FILE -> stringResource(R.string.home_type_file)
    SourceType.BOOK -> stringResource(R.string.home_type_book)
    SourceType.WEB -> stringResource(R.string.home_type_web)
    SourceType.TEXT -> stringResource(R.string.home_type_text)
}
