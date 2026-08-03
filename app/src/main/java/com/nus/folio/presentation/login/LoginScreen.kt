package com.nus.folio.presentation.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nus.folio.R
import com.nus.folio.di.LocalAppContainer
import com.nus.folio.components.BouncingDotsIndicator
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

@Composable
fun LoginScreen(
    onNavigateToSpaces: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    onNavigateToResetPassword: (email: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = viewModel(
        factory = LoginViewModel.Factory(
            signInUseCase = LocalAppContainer.current.signInUseCase,
            isAuthAvailable = LocalAppContainer.current.isAuthAvailable,
        ),
    ),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

    LaunchedEffect(uiState.shouldNavigateToHome) {
        if (uiState.shouldNavigateToHome) {
            onNavigateToSpaces()
            viewModel.onNavigationHandled()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LoginContent(
            uiState = uiState,
            initialEmail = LocalAppContainer.current.defaultLoginEmail,
            initialPassword = LocalAppContainer.current.defaultLoginPassword,
            onClearFeedback = viewModel::clearFeedback,
            onTogglePasswordVisibility = viewModel::onTogglePasswordVisibility,
            onSignInClick = viewModel::onSignInClick,
            onForgotPasswordClick = onNavigateToResetPassword,
            onSignUpClick = onNavigateToSignUp,
            modifier = Modifier.fillMaxSize(),
        )
        if (uiState.isLoading) {
            LoginLoadingScreen(modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun LoginContent(
    uiState: LoginUiState,
    onClearFeedback: () -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onSignInClick: (email: String, password: String) -> Unit,
    onForgotPasswordClick: (email: String) -> Unit,
    onSignUpClick: () -> Unit,
    modifier: Modifier = Modifier,
    initialEmail: String = "",
    initialPassword: String = "",
) {
    var email by rememberSaveable { mutableStateOf(initialEmail) }
    var password by remember { mutableStateOf(initialPassword) }
    val inputsEnabled = !uiState.authUnavailable

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
                .padding(top = 48.dp, bottom = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LoginHeader()

            Spacer(modifier = Modifier.height(40.dp))

            LoginEmailField(
                value = email,
                onValueChange = {
                    email = it
                    onClearFeedback()
                },
                enabled = inputsEnabled,
            )

            Spacer(modifier = Modifier.height(16.dp))

            LoginPasswordField(
                value = password,
                onValueChange = {
                    password = it
                    onClearFeedback()
                },
                passwordVisible = uiState.passwordVisible,
                onToggleVisibility = onTogglePasswordVisibility,
                enabled = inputsEnabled,
            )

            Spacer(modifier = Modifier.height(12.dp))

            LoginForgotPassword(
                onClick = { onForgotPasswordClick(email) },
                enabled = inputsEnabled,
            )

            LoginFeedback(uiState = uiState)

            Spacer(modifier = Modifier.height(24.dp))

            LoginPrimaryButton(
                onClick = { onSignInClick(email, password) },
                enabled = inputsEnabled,
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp, start = 28.dp, end = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (!uiState.authUnavailable) {
                LoginSignUpPrompt(
                    onSignUpClick = onSignUpClick,
                    enabled = inputsEnabled,
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.login_footer),
                fontSize = 12.sp,
                color = LoginTextMuted,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
            )
        }
    }
}

@Composable
private fun LoginSignUpPrompt(
    onSignUpClick: () -> Unit,
    enabled: Boolean,
) {
    val prompt = stringResource(R.string.login_no_account)
    val action = stringResource(R.string.login_sign_up)

    Text(
        text = buildAnnotatedString {
            append(prompt)
            append(" ")
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = LoginTextPrimary)) {
                append(action)
            }
        },
        modifier = Modifier.clickable(
            enabled = enabled,
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onSignUpClick,
        ),
        fontSize = 14.sp,
        color = LoginTextMuted,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun LoginFeedback(uiState: LoginUiState) {
    val errorText = when (uiState.error) {
        LoginError.EMAIL_REQUIRED -> stringResource(R.string.login_error_email_required)
        LoginError.PASSWORD_REQUIRED -> stringResource(R.string.login_error_password_required)
        LoginError.SIGN_IN_FAILED -> stringResource(R.string.login_error_sign_in_failed)
        LoginError.AUTH_UNAVAILABLE -> stringResource(R.string.login_error_auth_unavailable)
        null -> if (uiState.authUnavailable) {
            stringResource(R.string.login_error_auth_unavailable)
        } else {
            null
        }
    }

    if (errorText == null) return

    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = errorText,
        modifier = Modifier.fillMaxWidth(),
        fontSize = 13.sp,
        color = LoginCopper,
        textAlign = TextAlign.Center,
        lineHeight = 18.sp,
    )
}

@Composable
private fun LoginHeader() {
    FolioLogo()

    Spacer(modifier = Modifier.height(20.dp))

    Text(
        text = stringResource(R.string.app_name),
        fontFamily = CormorantGaramond,
        fontSize = 40.sp,
        fontWeight = FontWeight.SemiBold,
        color = LoginTextPrimary,
        letterSpacing = (-0.5).sp,
    )

    Text(
        text = stringResource(R.string.login_tagline),
        modifier = Modifier.padding(horizontal = 12.dp),
        fontSize = 9.sp,
        fontWeight = FontWeight.SemiBold,
        color = LoginCopper,
        letterSpacing = 1.2.sp,
        maxLines = 1,
    )

    Spacer(modifier = Modifier.height(8.dp))

    Box(
        modifier = Modifier
            .width(145.dp)
            .height(1.dp)
            .background(LoginCopper),
    )

    Spacer(modifier = Modifier.height(20.dp))

    Text(
        text = stringResource(R.string.login_description),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 50.dp),
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        color = LoginTextSecondary,
        textAlign = TextAlign.Center,
        lineHeight = 22.sp,
    )
}

@Composable
private fun FolioLogo() {
    Image(
        painter = painterResource(R.drawable.ic_folio_logo),
        contentDescription = stringResource(R.string.app_name),
        modifier = Modifier.size(86.dp),
        contentScale = ContentScale.Fit,
    )
}

@Composable
private fun LoginEmailField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        placeholder = {
            Text(
                text = stringResource(R.string.login_email_hint),
                color = LoginPlaceholder,
                fontSize = 15.sp,
            )
        },
        singleLine = true,
        shape = FieldShape,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        colors = loginTextFieldColors(),
    )
}

@Composable
private fun LoginPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    passwordVisible: Boolean,
    onToggleVisibility: () -> Unit,
    enabled: Boolean,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        placeholder = {
            Text(
                text = stringResource(R.string.login_password_hint),
                color = LoginPlaceholder,
                fontSize = 15.sp,
            )
        },
        singleLine = true,
        shape = FieldShape,
        visualTransformation = if (passwordVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        trailingIcon = {
            IconButton(onClick = onToggleVisibility, enabled = enabled) {
                Icon(
                    painter = painterResource(
                        if (passwordVisible) R.drawable.ic_visibility_off else R.drawable.ic_visibility,
                    ),
                    contentDescription = stringResource(
                        if (passwordVisible) R.string.login_hide_password else R.string.login_show_password,
                    ),
                    tint = Color.Unspecified,
                )
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        colors = loginTextFieldColors(),
    )
}

@Composable
private fun loginTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = LoginBackground,
    unfocusedContainerColor = LoginBackground,
    focusedBorderColor = LoginBorder,
    unfocusedBorderColor = LoginBorder,
    cursorColor = LoginTextPrimary,
    focusedTextColor = LoginTextPrimary,
    unfocusedTextColor = LoginTextPrimary,
)

@Composable
private fun LoginForgotPassword(
    onClick: () -> Unit,
    enabled: Boolean,
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.login_forgot_password),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .clickable(
                    enabled = enabled,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ),
            fontSize = 13.sp,
            color = LoginTextSecondary,
        )
    }
}

@Composable
private fun LoginLoadingScreen(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.48f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            ),
        contentAlignment = Alignment.Center,
    ) {
        BouncingDotsIndicator()
    }
}

@Composable
private fun LoginPrimaryButton(
    onClick: () -> Unit,
    enabled: Boolean,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
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
        Text(
            text = stringResource(R.string.login_sign_in),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun LoginContentPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        LoginContent(
            uiState = LoginUiState(),
            onClearFeedback = {},
            onTogglePasswordVisibility = {},
            onSignInClick = { _, _ -> },
            onForgotPasswordClick = {},
            onSignUpClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852, name = "Login Loading")
@Composable
private fun LoginLoadingScreenPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        Box(modifier = Modifier.fillMaxSize()) {
            LoginContent(
                uiState = LoginUiState(isLoading = true),
                onClearFeedback = {},
                onTogglePasswordVisibility = {},
                onSignInClick = { _, _ -> },
                onForgotPasswordClick = {},
                onSignUpClick = {},
            )
            LoginLoadingScreen()
        }
    }
}
