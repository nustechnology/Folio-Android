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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nus.folio.R
import com.nus.folio.components.FolioToastHost
import com.nus.folio.components.FolioToastStyle
import com.nus.folio.components.FolioToastVisuals
import com.nus.folio.components.rememberFolioToastHostState
import com.nus.folio.di.LocalAppContainer
import com.nus.folio.domain.model.initialsFromDisplayName
import com.nus.folio.presentation.home.HomeBackButton
import com.nus.folio.presentation.home.bottomsheet.DeleteConfirmationBottomSheet
import com.nus.folio.ui.theme.AccountAvatar
import com.nus.folio.ui.theme.AccountCardBackground
import com.nus.folio.ui.theme.AccountCardBorder
import com.nus.folio.ui.theme.AccountTextPrimary
import com.nus.folio.ui.theme.AccountTextSecondary
import com.nus.folio.ui.theme.CormorantGaramond
import com.nus.folio.ui.theme.FolioAndroidTheme
import com.nus.folio.ui.theme.HomeBackground
import com.nus.folio.ui.theme.HomeHeader

private val MenuItemShape = RoundedCornerShape(14.dp)

@Composable
fun AccountSettingsScreen(
    onSignOut: () -> Unit,
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: AccountViewModel = viewModel(
        factory = AccountViewModel.Factory(
            getCurrentSessionUseCase = LocalAppContainer.current.getCurrentSessionUseCase,
            syncCurrentUserUseCase = LocalAppContainer.current.syncCurrentUserUseCase,
        ),
    ),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val toastHostState = rememberFolioToastHostState()
    var showSignOutConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.userMessage) {
        val message = uiState.userMessage ?: return@LaunchedEffect
        toastHostState.showToast(message.toAccountToastVisuals(context))
        viewModel.onUserMessageShown()
    }

    Box(modifier = modifier.fillMaxSize()) {
        AccountSettingsContent(
            uiState = uiState,
            onSignOutClick = { showSignOutConfirm = true },
            onBackClick = onBackClick,
            onProfileSettingsClick = viewModel::onProfileSettingsClick,
            onSecurityClick = viewModel::onSecurityClick,
            onPrivacyDataClick = viewModel::onPrivacyDataClick,
            onExportDataClick = viewModel::onExportDataClick,
        )

        if (showSignOutConfirm) {
            DeleteConfirmationBottomSheet(
                titleRes = R.string.account_sign_out_title,
                messageRes = R.string.account_sign_out_message,
                confirmLabelRes = R.string.account_sign_out,
                onDismiss = { showSignOutConfirm = false },
                onConfirm = onSignOut,
            )
        }

        FolioToastHost(
            hostState = toastHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 12.dp),
        )
    }
}

@Composable
private fun AccountSettingsContent(
    uiState: AccountUiState,
    onSignOutClick: () -> Unit,
    onBackClick: () -> Unit,
    onProfileSettingsClick: () -> Unit,
    onSecurityClick: () -> Unit,
    onPrivacyDataClick: () -> Unit,
    onExportDataClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HomeBackground),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(HomeBackground)
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 16.dp),
        ) {
            AccountHeaderRow(onBackClick = onBackClick)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 28.dp, bottom = 28.dp),
        ) {
            AccountProfileSection(
                displayName = uiState.displayName,
                email = uiState.email,
            )

            Spacer(modifier = Modifier.height(40.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AccountMenuItem(
                    label = stringResource(R.string.account_profile_settings),
                    onClick = onProfileSettingsClick,
                )
                AccountMenuItem(
                    label = stringResource(R.string.account_security),
                    onClick = onSecurityClick,
                )
                AccountMenuItem(
                    label = stringResource(R.string.account_privacy_data),
                    onClick = onPrivacyDataClick,
                )
                AccountMenuItem(
                    label = stringResource(R.string.account_export_data),
                    onClick = onExportDataClick,
                )
                AccountMenuItem(
                    label = stringResource(R.string.account_sign_out),
                    onClick = onSignOutClick,
                )
            }
        }
    }
}

@Composable
private fun AccountHeaderRow(
    onBackClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HomeBackButton(onClick = onBackClick)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.account_subtitle),
                    modifier = Modifier.weight(1f),
                    fontFamily = CormorantGaramond,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontStyle = FontStyle.Italic,
                    color = HomeHeader,
                    letterSpacing = (-0.4).sp,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun AccountProfileSection(
    displayName: String,
    email: String,
) {
    val initial = initialsFromDisplayName(displayName, email)

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

private fun AccountUserMessage.toAccountToastVisuals(context: android.content.Context): FolioToastVisuals {
    val messageRes = when (this) {
        AccountUserMessage.PROFILE_SETTINGS_NOT_SUPPORTED -> R.string.account_profile_settings_not_supported
        AccountUserMessage.SECURITY_NOT_SUPPORTED -> R.string.account_security_not_supported
        AccountUserMessage.PRIVACY_DATA_NOT_SUPPORTED -> R.string.account_privacy_data_not_supported
        AccountUserMessage.EXPORT_DATA_NOT_SUPPORTED -> R.string.account_export_data_not_supported
    }
    return FolioToastVisuals(
        title = context.getString(messageRes),
        style = FolioToastStyle.Warning,
    )
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun AccountSettingsContentPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        AccountSettingsContent(
            uiState = AccountUiState(
                displayName = "Jordan Lee",
                email = "jordan@folio.app",
            ),
            onSignOutClick = {},
            onBackClick = {},
            onProfileSettingsClick = {},
            onSecurityClick = {},
            onPrivacyDataClick = {},
            onExportDataClick = {},
        )
    }
}
