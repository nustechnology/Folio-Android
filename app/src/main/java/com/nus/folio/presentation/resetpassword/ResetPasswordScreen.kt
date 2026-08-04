package com.nus.folio.presentation.resetpassword

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nus.folio.R
import com.nus.folio.di.LocalAppContainer
import com.nus.folio.ui.theme.CormorantGaramond
import com.nus.folio.ui.theme.FolioAndroidTheme
import com.nus.folio.ui.theme.LoginBackground
import com.nus.folio.ui.theme.LoginBorder
import com.nus.folio.ui.theme.LoginButtonGlow
import com.nus.folio.ui.theme.LoginCopper
import com.nus.folio.ui.theme.LoginPlaceholder
import com.nus.folio.ui.theme.LoginPrimary
import com.nus.folio.ui.theme.LoginTextMuted
import com.nus.folio.ui.theme.LoginTextPrimary
import com.nus.folio.ui.theme.LoginTextSecondary

private val FieldShape = RoundedCornerShape(12.dp)
private val ButtonShape = RoundedCornerShape(12.dp)
private val InfoCardShape = RoundedCornerShape(12.dp)

@Composable
fun ResetPasswordScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ResetPasswordViewModel = viewModel(
        factory = ResetPasswordViewModel.Factory(
            requestPasswordResetUseCase = LocalAppContainer.current.requestPasswordResetUseCase,
        ),
    ),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

    ResetPasswordContent(
        uiState = uiState,
        onClearFeedback = viewModel::clearFeedback,
        onSendRecoveryLinkClick = viewModel::onSendRecoveryLinkClick,
        onBackClick = onNavigateBack,
        modifier = modifier,
    )
}

@Composable
private fun ResetPasswordContent(
    uiState: ResetPasswordUiState,
    onClearFeedback: () -> Unit,
    onSendRecoveryLinkClick: (email: String) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var email by rememberSaveable { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LoginBackground)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp)
                .padding(top = 32.dp, bottom = 100.dp),
        ) {
            ResetPasswordHeader()

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = stringResource(R.string.reset_password_email_label),
                fontSize = 13.sp,
                color = LoginTextSecondary,
                fontWeight = FontWeight.Medium,
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    onClearFeedback()
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = stringResource(R.string.reset_password_email_hint),
                        color = LoginPlaceholder,
                        fontSize = 15.sp,
                    )
                },
                singleLine = true,
                shape = FieldShape,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = resetPasswordTextFieldColors(),
            )

            ResetPasswordFeedback(uiState = uiState)

            Spacer(modifier = Modifier.height(28.dp))

            ResetPasswordPrimaryButton(
                onClick = { onSendRecoveryLinkClick(email) },
                isLoading = uiState.isLoading,
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onBackClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = ButtonShape,
                border = androidx.compose.foundation.BorderStroke(1.dp, LoginBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = LoginBackground,
                    contentColor = LoginTextPrimary,
                ),
            ) {
                Text(
                    text = stringResource(R.string.reset_password_back_to_sign_in),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        ResetPasswordInfoCard(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 28.dp, vertical = 28.dp),
        )
    }
}

@Composable
private fun ResetPasswordHeader() {
    Text(
        text = stringResource(R.string.app_name),
        fontFamily = CormorantGaramond,
        fontSize = 40.sp,
        fontWeight = FontWeight.SemiBold,
        color = LoginTextPrimary,
        letterSpacing = (-0.5).sp,
    )

    Spacer(modifier = Modifier.height(4.dp))

    Text(
        text = stringResource(R.string.reset_password_tagline),
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        color = LoginCopper,
        letterSpacing = 1.6.sp,
    )

    Spacer(modifier = Modifier.height(12.dp))

    Text(
        text = stringResource(R.string.reset_password_headline),
        fontFamily = CormorantGaramond,
        fontSize = 34.sp,
        fontWeight = FontWeight.SemiBold,
        color = LoginTextPrimary,
        letterSpacing = (-0.4).sp,
        lineHeight = 40.sp,
    )

    Spacer(modifier = Modifier.height(4.dp))

    Text(
        text = stringResource(R.string.reset_password_description),
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        color = LoginTextSecondary,
        lineHeight = 22.sp,
    )
}

@Composable
private fun ResetPasswordPrimaryButton(
    onClick: () -> Unit,
    isLoading: Boolean,
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .alpha(if (isLoading) 0.6f else 1f)
            .shadow(
                elevation = 8.dp,
                shape = ButtonShape,
                ambientColor = LoginButtonGlow,
                spotColor = LoginButtonGlow,
            )
            .border(1.dp, LoginButtonGlow, ButtonShape),
        shape = ButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = LoginPrimary,
            contentColor = Color.White,
            disabledContainerColor = LoginPrimary,
            disabledContentColor = Color.White,
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
                text = stringResource(R.string.reset_password_send_link),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun ResetPasswordFeedback(uiState: ResetPasswordUiState) {
    val errorText = when (uiState.error) {
        ResetPasswordError.EMAIL_REQUIRED -> stringResource(R.string.reset_password_error_email_required)
        ResetPasswordError.SEND_FAILED -> stringResource(R.string.reset_password_error_send_failed)
        null -> null
    }
    val infoText = when (uiState.info) {
        ResetPasswordInfo.LINK_SENT -> stringResource(R.string.reset_password_link_sent)
        null -> null
    }

    when {
        errorText != null -> {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = errorText,
                fontSize = 13.sp,
                color = LoginCopper,
                lineHeight = 18.sp,
            )
        }
        infoText != null -> {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = infoText,
                fontSize = 13.sp,
                color = LoginTextSecondary,
                lineHeight = 18.sp,
            )
        }
    }
}

@Composable
private fun ResetPasswordInfoCard(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, LoginBorder, InfoCardShape)
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Text(
            text = stringResource(R.string.reset_password_info_title),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = LoginTextPrimary,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.reset_password_info_body),
            fontSize = 14.sp,
            color = LoginTextMuted,
            lineHeight = 20.sp,
        )
    }
}

@Composable
private fun resetPasswordTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = LoginBackground,
    unfocusedContainerColor = LoginBackground,
    disabledContainerColor = LoginBackground,
    focusedBorderColor = LoginBorder,
    unfocusedBorderColor = LoginBorder,
    disabledBorderColor = LoginBorder,
    cursorColor = LoginTextPrimary,
    focusedTextColor = LoginTextPrimary,
    unfocusedTextColor = LoginTextPrimary,
)

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun ResetPasswordContentPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        ResetPasswordContent(
            uiState = ResetPasswordUiState(),
            onClearFeedback = {},
            onSendRecoveryLinkClick = {},
            onBackClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852, name = "Reset Password Loading")
@Composable
private fun ResetPasswordContentLoadingPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        ResetPasswordContent(
            uiState = ResetPasswordUiState(isLoading = true),
            onClearFeedback = {},
            onSendRecoveryLinkClick = {},
            onBackClick = {},
        )
    }
}
