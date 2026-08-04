package com.nus.folio.presentation.home.bottomsheet

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nus.folio.R
import com.nus.folio.ui.theme.HomeCardBackground
import com.nus.folio.ui.theme.HomeChipBorder
import com.nus.folio.ui.theme.HomeHeader
import com.nus.folio.ui.theme.HomeSheetHandle
import com.nus.folio.ui.theme.HomeStatusFailedBackground
import com.nus.folio.ui.theme.HomeStatusFailedText
import com.nus.folio.ui.theme.HomeTextPrimary

internal val HomeBottomSheetButtonShape = RoundedCornerShape(12.dp)
private val SheetCloseButtonShape = RoundedCornerShape(10.dp)

@Composable
internal fun SheetCloseIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(SheetCloseButtonShape)
            .border(1.dp, HomeChipBorder, SheetCloseButtonShape)
            .background(HomeCardBackground)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_close),
            contentDescription = stringResource(R.string.note_view_close),
            tint = HomeTextPrimary,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
internal fun AddSourceCancelButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    labelRes: Int = R.string.add_source_cancel,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = HomeBottomSheetButtonShape,
        border = BorderStroke(1.dp, HomeChipBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = HomeCardBackground,
            contentColor = HomeTextPrimary,
        ),
    ) {
        Text(
            text = stringResource(labelRes),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
internal fun AddSourceDestructiveButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    labelRes: Int = R.string.source_delete_confirm,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = HomeBottomSheetButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = HomeStatusFailedBackground,
            contentColor = HomeStatusFailedText,
        ),
    ) {
        Text(
            text = stringResource(labelRes),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
internal fun AddSourceSubmitButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    labelRes: Int = R.string.add_source_submit,
    isLoading: Boolean = false,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier.height(52.dp),
        shape = HomeBottomSheetButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = HomeHeader,
            contentColor = Color.White,
            disabledContainerColor = HomeHeader.copy(alpha = 0.35f),
            disabledContentColor = Color.White.copy(alpha = 0.7f),
        ),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color.White,
                strokeWidth = 2.5.dp,
            )
        } else {
            Text(
                text = stringResource(labelRes),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
internal fun AddSourceDragHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(HomeSheetHandle),
        )
    }
}
