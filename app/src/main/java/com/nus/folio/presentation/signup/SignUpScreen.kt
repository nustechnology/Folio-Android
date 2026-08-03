package com.nus.folio.presentation.signup

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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
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
import com.nus.folio.components.BouncingDotsIndicator
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

@Composable
fun SignUpScreen(
    onNavigateToSpaces: () -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SignUpViewModel = viewModel(
        factory = SignUpViewModel.Factory(
            signUpUseCase = LocalAppContainer.current.signUpUseCase,
            signInWithAppleUseCase = LocalAppContainer.current.signInWithAppleUseCase,
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
        SignUpContent(
            uiState = uiState,
            onClearError = viewModel::clearError,
            onTogglePasswordVisibility = viewModel::onTogglePasswordVisibility,
            onSignUpClick = viewModel::onSignUpClick,
            onContinueWithAppleClick = viewModel::onContinueWithAppleClick,
            onSignInClick = onNavigateToLogin,
            modifier = Modifier.fillMaxSize(),
        )
        if (uiState.isLoading) {
            SignUpLoadingScreen(modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
internal fun SignUpContent(
    uiState: SignUpUiState,
    onClearError: () -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onSignUpClick: (name: String, email: String, password: String) -> Unit,
    onContinueWithAppleClick: () -> Unit,
    onSignInClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val inputsEnabled = !uiState.authUnavailable && !uiState.isLoading

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
                .padding(top = 40.dp, bottom = 88.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            SignUpHeader()

            Spacer(modifier = Modifier.height(36.dp))

            SignUpLabeledField(
                label = stringResource(R.string.signup_name_label),
                value = name,
                onValueChange = {
                    name = it
                    onClearError()
                },
                placeholder = stringResource(R.string.signup_name_hint),
                enabled = inputsEnabled,
                keyboardType = KeyboardType.Text,
            )

            Spacer(modifier = Modifier.height(16.dp))

            SignUpLabeledField(
                label = stringResource(R.string.signup_email_label),
                value = email,
                onValueChange = {
                    email = it
                    onClearError()
                },
                placeholder = stringResource(R.string.signup_email_hint),
                enabled = inputsEnabled,
                keyboardType = KeyboardType.Email,
            )

            Spacer(modifier = Modifier.height(16.dp))

            SignUpPasswordField(
                value = password,
                onValueChange = {
                    password = it
                    onClearError()
                },
                passwordVisible = uiState.passwordVisible,
                onToggleVisibility = onTogglePasswordVisibility,
                enabled = inputsEnabled,
            )

            SignUpErrorFeedback(uiState = uiState)

            Spacer(modifier = Modifier.height(28.dp))

            SignUpPrimaryButton(
                onClick = { onSignUpClick(name, email, password) },
                enabled = inputsEnabled,
            )

            Spacer(modifier = Modifier.height(16.dp))

            SignUpAppleButton(
                onClick = onContinueWithAppleClick,
                enabled = inputsEnabled,
            )
        }

        SignUpFooter(
            onSignInClick = onSignInClick,
            enabled = inputsEnabled,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp, start = 28.dp, end = 28.dp),
        )
    }
}

@Composable
private fun SignUpHeader() {
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
        text = stringResource(R.string.signup_tagline),
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        color = LoginCopper,
        letterSpacing = 1.6.sp,
    )

    Spacer(modifier = Modifier.height(28.dp))

    Text(
        text = stringResource(R.string.signup_headline),
        fontFamily = CormorantGaramond,
        fontSize = 34.sp,
        fontWeight = FontWeight.SemiBold,
        color = LoginTextPrimary,
        letterSpacing = (-0.4).sp,
        lineHeight = 40.sp,
    )

    Spacer(modifier = Modifier.height(12.dp))

    Text(
        text = stringResource(R.string.signup_description),
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        color = LoginTextSecondary,
        lineHeight = 22.sp,
    )
}

@Composable
private fun SignUpLabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    enabled: Boolean,
    keyboardType: KeyboardType,
) {
    Text(
        text = label,
        fontSize = 13.sp,
        color = LoginTextSecondary,
        fontWeight = FontWeight.Medium,
    )

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        placeholder = {
            Text(
                text = placeholder,
                color = LoginPlaceholder,
                fontSize = 15.sp,
            )
        },
        singleLine = true,
        shape = FieldShape,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = signUpTextFieldColors(),
    )
}

@Composable
private fun SignUpPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    passwordVisible: Boolean,
    onToggleVisibility: () -> Unit,
    enabled: Boolean,
) {
    Text(
        text = stringResource(R.string.signup_password_label),
        fontSize = 13.sp,
        color = LoginTextSecondary,
        fontWeight = FontWeight.Medium,
    )

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        placeholder = {
            Text(
                text = stringResource(R.string.signup_password_hint),
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
                        if (passwordVisible) R.string.signup_hide_password else R.string.signup_show_password,
                    ),
                    tint = Color.Unspecified,
                )
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        colors = signUpTextFieldColors(),
    )
}

@Composable
private fun signUpTextFieldColors() = OutlinedTextFieldDefaults.colors(
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

@Composable
private fun SignUpErrorFeedback(uiState: SignUpUiState) {
    val errorText = when (uiState.error) {
        SignUpError.NAME_REQUIRED -> stringResource(R.string.signup_error_name_required)
        SignUpError.EMAIL_REQUIRED -> stringResource(R.string.signup_error_email_required)
        SignUpError.PASSWORD_REQUIRED -> stringResource(R.string.signup_error_password_required)
        SignUpError.SIGN_UP_FAILED -> stringResource(R.string.signup_error_sign_up_failed)
        null -> if (uiState.authUnavailable) {
            stringResource(R.string.login_error_auth_unavailable)
        } else {
            null
        }
    } ?: return

    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = errorText,
        modifier = Modifier.fillMaxWidth(),
        fontSize = 13.sp,
        color = LoginCopper,
        textAlign = TextAlign.Start,
        lineHeight = 18.sp,
    )
}

@Composable
private fun SignUpPrimaryButton(
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
        ),
    ) {
        Text(
            text = stringResource(R.string.signup_create_account),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun SignUpAppleButton(
    onClick: () -> Unit,
    enabled: Boolean,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
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
        Icon(
            painter = painterResource(R.drawable.ic_apple),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = Color.Unspecified,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.signup_continue_with_apple),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun SignUpFooter(
    onSignInClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val prompt = stringResource(R.string.signup_already_have_account)
    val action = stringResource(R.string.signup_sign_in)

    Text(
        text = buildAnnotatedString {
            append(prompt)
            append(" ")
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = LoginTextPrimary)) {
                append(action)
            }
        },
        modifier = modifier.clickable(
            enabled = enabled,
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onSignInClick,
        ),
        fontSize = 14.sp,
        color = LoginTextMuted,
        textAlign = TextAlign.Center,
    )
}

@Composable
internal fun SignUpLoadingScreen(
    modifier: Modifier = Modifier,
) {
    val loadingLabel = stringResource(R.string.signup_loading)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.48f))
            .semantics {
                contentDescription = loadingLabel
                progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
            },
        contentAlignment = Alignment.Center,
    ) {
        BouncingDotsIndicator()
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun SignUpContentPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        SignUpContent(
            uiState = SignUpUiState(),
            onClearError = {},
            onTogglePasswordVisibility = {},
            onSignUpClick = { _, _, _ -> },
            onContinueWithAppleClick = {},
            onSignInClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852, name = "Sign Up Loading")
@Composable
private fun SignUpLoadingScreenPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        Box(modifier = Modifier.fillMaxSize()) {
            SignUpContent(
                uiState = SignUpUiState(isLoading = true),
                onClearError = {},
                onTogglePasswordVisibility = {},
                onSignUpClick = { _, _, _ -> },
                onContinueWithAppleClick = {},
                onSignInClick = {},
            )
            SignUpLoadingScreen()
        }
    }
}
