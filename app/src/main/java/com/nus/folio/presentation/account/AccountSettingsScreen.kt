package com.nus.folio.presentation.account

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nus.folio.R
import com.nus.folio.ui.theme.AccountAvatar
import com.nus.folio.ui.theme.AccountBackground
import com.nus.folio.ui.theme.AccountCardBackground
import com.nus.folio.ui.theme.AccountCardBorder
import com.nus.folio.ui.theme.AccountSubtitle
import com.nus.folio.ui.theme.AccountTextPrimary
import com.nus.folio.ui.theme.AccountTextSecondary
import com.nus.folio.ui.theme.CormorantGaramond
import com.nus.folio.ui.theme.FolioAndroidTheme

private val MenuItemShape = RoundedCornerShape(14.dp)

@Composable
fun AccountSettingsScreen(
    displayName: String,
    email: String,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AccountSettingsContent(
        displayName = displayName,
        email = email,
        onSignOutClick = onSignOut,
        modifier = modifier,
    )
}

@Composable
private fun AccountSettingsContent(
    displayName: String,
    email: String,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AccountBackground)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 28.dp),
    ) {
        AccountHeaderRow()

        Spacer(modifier = Modifier.height(36.dp))

        AccountProfileSection(
            displayName = displayName,
            email = email,
        )

        Spacer(modifier = Modifier.height(40.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AccountMenuItem(
                label = stringResource(R.string.account_sign_out),
                onClick = onSignOutClick,
            )
        }
    }
}

@Composable
private fun AccountHeaderRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.account_title),
                fontFamily = CormorantGaramond,
                fontSize = 40.sp,
                fontWeight = FontWeight.SemiBold,
                fontStyle = FontStyle.Italic,
                color = AccountAvatar,
                letterSpacing = (-0.4).sp,
            )
            Text(
                text = stringResource(R.string.account_subtitle),
                fontFamily = CormorantGaramond,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = AccountSubtitle,
            )
        }
    }
}

@Composable
private fun AccountProfileSection(
    displayName: String,
    email: String,
) {
    val initial = (
        displayName.firstOrNull()?.uppercaseChar()?.toString()
            ?: email.firstOrNull()?.uppercaseChar()?.toString()
        ).orEmpty()

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(AccountAvatar),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initial,
                fontFamily = CormorantGaramond,
                fontSize = 40.sp,
                fontWeight = FontWeight.SemiBold,
                color = AccountTextPrimary,
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = displayName,
            fontFamily = CormorantGaramond,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = AccountTextPrimary,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = email,
            fontFamily = CormorantGaramond,
            fontSize = 15.sp,
            color = AccountTextSecondary,
        )
    }
}

@Composable
private fun AccountMenuItem(
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MenuItemShape)
            .background(AccountCardBackground)
            .border(1.dp, AccountCardBorder, MenuItemShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = AccountTextPrimary,
        )
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = stringResource(R.string.account_navigate),
            tint = AccountTextPrimary,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun AccountSettingsContentPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        AccountSettingsContent(
            displayName = "Alex Nguyen",
            email = "alex@folio.app",
            onSignOutClick = {},
        )
    }
}
