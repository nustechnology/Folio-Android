package com.nus.folio.presentation.space

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nus.folio.R
import com.nus.folio.components.AnimatedModalSheet
import com.nus.folio.domain.model.initialsFromDisplayName
import com.nus.folio.presentation.home.HomeSheetShape
import com.nus.folio.presentation.home.bottomsheet.AddSourceDragHandle
import com.nus.folio.ui.theme.CormorantGaramond
import com.nus.folio.ui.theme.FolioAndroidTheme
import com.nus.folio.ui.theme.HomeCardBackground
import com.nus.folio.ui.theme.HomeChipBorder
import com.nus.folio.ui.theme.HomeHeader
import com.nus.folio.ui.theme.HomeSearchField
import com.nus.folio.ui.theme.HomeSearchPlaceholder
import com.nus.folio.ui.theme.HomeSheetBackground
import com.nus.folio.ui.theme.HomeStatusFailedText
import com.nus.folio.ui.theme.HomeTextPrimary
import com.nus.folio.ui.theme.HomeTextSecondary

private val AccountRowShape = RoundedCornerShape(14.dp)
private val SettingsButtonShape = RoundedCornerShape(10.dp)

@Composable
internal fun AccountListBottomSheet(
    accounts: List<SpaceAccountItem>,
    onDismiss: () -> Unit,
    onSignOutClick: () -> Unit = {},
) {
    AnimatedModalSheet(
        onDismiss = onDismiss,
        dismissOnScrimClick = true,
    ) { requestDismiss ->
        AddSourceDragHandle()
        AccountListSheetContent(
            accounts = accounts,
            onSignOutClick = { requestDismiss(after = onSignOutClick) },
        )
    }
}

@Composable
private fun AccountListSheetContent(
    accounts: List<SpaceAccountItem>,
    onSignOutClick: () -> Unit,
) {
    Column {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.space_account_sheet_title),
                fontFamily = CormorantGaramond,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                color = HomeTextPrimary,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(SettingsButtonShape)
                    .border(1.dp, HomeChipBorder, SettingsButtonShape)
                    .background(HomeCardBackground)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onSignOutClick,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_logout),
                    contentDescription = stringResource(R.string.space_account_sign_out),
                    tint = HomeStatusFailedText,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            accounts.forEach { account ->
                AccountListRow(account = account)
            }
        }
    }
}

@Composable
private fun AccountListRow(
    account: SpaceAccountItem,
) {
    val initial = initialsFromDisplayName(account.displayName, account.email)
    val background = if (account.isSelected) HomeHeader else HomeCardBackground
    val borderColor = if (account.isSelected) HomeHeader else HomeChipBorder
    val nameColor = if (account.isSelected) Color.White else HomeTextPrimary
    val emailColor = if (account.isSelected) HomeSearchPlaceholder else HomeTextSecondary
    val avatarBackground = if (account.isSelected) HomeSearchField else HomeHeader

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AccountRowShape)
            .background(background)
            .border(1.dp, borderColor, AccountRowShape)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(avatarBackground),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initial,
                fontFamily = CormorantGaramond,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = account.displayName,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = nameColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = account.email,
                fontSize = 13.sp,
                color = emailColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852, backgroundColor = 0xFFF7F1E6)
@Composable
private fun AccountListSheetContentPreview() {
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
                AccountListSheetContent(
                    accounts = listOf(
                        SpaceAccountItem(
                            id = "jordan@folio.app",
                            displayName = "Jordan Lee",
                            email = "jordan@folio.app",
                            isSelected = true,
                        ),
                    ),
                    onSignOutClick = {},
                )
            }
        }
    }
}
