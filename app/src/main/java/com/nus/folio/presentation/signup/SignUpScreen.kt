package com.nus.folio.presentation.signup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.nus.folio.components.FolioToastHost
import com.nus.folio.components.FolioToastStyle
import com.nus.folio.components.rememberFolioToastHostState
import com.nus.folio.di.LocalAppContainer
import com.nus.folio.ui.theme.CormorantGaramond
import com.nus.folio.ui.theme.FolioAndroidTheme
import com.nus.folio.ui.theme.LoginBackground
import com.nus.folio.ui.theme.LoginCopper
import com.nus.folio.ui.theme.LoginTextPrimary
import com.nus.folio.ui.theme.LoginTextSecondary

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
    val toastHostState = rememberFolioToastHostState()

    LaunchedEffect(uiState.shouldNavigateToHome) {
        if (uiState.shouldNavigateToHome) {
            onNavigateToSpaces()
            viewModel.onNavigationHandled()
        }
    }

    val emailAlreadyExistsToast = stringResource(R.string.signup_error_email_already_exists)
    LaunchedEffect(uiState.toastError, uiState.toastMessage) {
        val message = when (uiState.toastError) {
            SignUpError.EMAIL_ALREADY_EXISTS -> emailAlreadyExistsToast
            else -> uiState.toastMessage
        } ?: return@LaunchedEffect
        toastHostState.showToast(title = message, style = FolioToastStyle.Error)
        viewModel.onToastMessageShown()
    }

    Box(modifier = modifier.fillMaxSize()) {
        SignUpContent(
            uiState = uiState,
            onClearNameError = viewModel::clearNameError,
            onClearEmailError = viewModel::clearEmailError,
            onClearPasswordError = viewModel::clearPasswordError,
            onClearConfirmPasswordError = viewModel::clearConfirmPasswordError,
            onTogglePasswordVisibility = viewModel::onTogglePasswordVisibility,
            onToggleConfirmPasswordVisibility = viewModel::onToggleConfirmPasswordVisibility,
            onSignUpClick = viewModel::onSignUpClick,
            onContinueWithAppleClick = viewModel::onContinueWithAppleClick,
            onSignInClick = onNavigateToLogin,
            modifier = Modifier.fillMaxSize(),
        )

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
internal fun SignUpContent(
    uiState: SignUpUiState,
    onClearNameError: () -> Unit,
    onClearEmailError: () -> Unit,
    onClearPasswordError: () -> Unit,
    onClearConfirmPasswordError: () -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onToggleConfirmPasswordVisibility: () -> Unit,
    onSignUpClick: (name: String, email: String, password: String, confirmPassword: String) -> Unit,
    onContinueWithAppleClick: () -> Unit,
    onSignInClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val inputsEnabled = !uiState.authUnavailable && !uiState.isLoading

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LoginBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp)
            .padding(top = 32.dp, bottom = 28.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        SignUpHeader()

        Spacer(modifier = Modifier.height(28.dp))

        SignUpLabeledField(
            label = stringResource(R.string.signup_name_label),
            value = name,
            onValueChange = {
                name = it
                onClearNameError()
            },
            placeholder = stringResource(R.string.signup_name_hint),
            enabled = inputsEnabled,
            keyboardType = KeyboardType.Text,
            errorMessage = uiState.nameError?.let { signUpErrorMessage(it) },
        )

        Spacer(modifier = Modifier.height(16.dp))

        SignUpLabeledField(
            label = stringResource(R.string.signup_email_label),
            value = email,
            onValueChange = {
                email = it
                onClearEmailError()
            },
            placeholder = stringResource(R.string.signup_email_hint),
            enabled = inputsEnabled,
            keyboardType = KeyboardType.Email,
            errorMessage = uiState.emailError?.let { signUpErrorMessage(it) },
        )

        Spacer(modifier = Modifier.height(16.dp))

        SignUpPasswordField(
            label = stringResource(R.string.signup_password_label),
            value = password,
            onValueChange = {
                password = it
                onClearPasswordError()
            },
            placeholder = stringResource(R.string.signup_password_hint),
            passwordVisible = uiState.passwordVisible,
            onToggleVisibility = onTogglePasswordVisibility,
            showPasswordDescription = stringResource(R.string.signup_show_password),
            hidePasswordDescription = stringResource(R.string.signup_hide_password),
            enabled = inputsEnabled,
            errorMessage = uiState.passwordError?.let { signUpErrorMessage(it) },
        )

        Spacer(modifier = Modifier.height(16.dp))

        SignUpPasswordField(
            label = stringResource(R.string.signup_confirm_password_label),
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it
                onClearConfirmPasswordError()
            },
            placeholder = stringResource(R.string.signup_confirm_password_hint),
            passwordVisible = uiState.confirmPasswordVisible,
            onToggleVisibility = onToggleConfirmPasswordVisibility,
            showPasswordDescription = stringResource(R.string.signup_show_confirm_password),
            hidePasswordDescription = stringResource(R.string.signup_hide_confirm_password),
            enabled = inputsEnabled,
            errorMessage = uiState.confirmPasswordError?.let { signUpErrorMessage(it) },
        )

        SignUpFormErrorFeedback(uiState = uiState)

        Spacer(modifier = Modifier.height(28.dp))

        SignUpPrimaryButton(
            onClick = { onSignUpClick(name, email, password, confirmPassword) },
            enabled = inputsEnabled,
            isLoading = uiState.isLoading,
        )

        Spacer(modifier = Modifier.height(16.dp))

        SignUpAppleButton(
            onClick = onContinueWithAppleClick,
            enabled = inputsEnabled,
        )

        Spacer(modifier = Modifier.height(28.dp))

        SignUpFooter(
            onSignInClick = onSignInClick,
            enabled = inputsEnabled,
            modifier = Modifier.fillMaxWidth(),
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

    Spacer(modifier = Modifier.height(12.dp))

    Text(
        text = stringResource(R.string.signup_headline),
        fontFamily = CormorantGaramond,
        fontSize = 34.sp,
        fontWeight = FontWeight.SemiBold,
        color = LoginTextPrimary,
        letterSpacing = (-0.4).sp,
        lineHeight = 40.sp,
    )

    Spacer(modifier = Modifier.height(4.dp))

    Text(
        text = stringResource(R.string.signup_description),
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        color = LoginTextSecondary,
        lineHeight = 22.sp,
    )
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun SignUpContentPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        SignUpContent(
            uiState = SignUpUiState(),
            onClearNameError = {},
            onClearEmailError = {},
            onClearPasswordError = {},
            onClearConfirmPasswordError = {},
            onTogglePasswordVisibility = {},
            onToggleConfirmPasswordVisibility = {},
            onSignUpClick = { _, _, _, _ -> },
            onContinueWithAppleClick = {},
            onSignInClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852, name = "Sign Up Loading")
@Composable
private fun SignUpLoadingPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        SignUpContent(
            uiState = SignUpUiState(isLoading = true),
            onClearNameError = {},
            onClearEmailError = {},
            onClearPasswordError = {},
            onClearConfirmPasswordError = {},
            onTogglePasswordVisibility = {},
            onToggleConfirmPasswordVisibility = {},
            onSignUpClick = { _, _, _, _ -> },
            onContinueWithAppleClick = {},
            onSignInClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852, name = "Sign Up Field Errors")
@Composable
private fun SignUpFieldErrorsPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        SignUpContent(
            uiState = SignUpUiState(
                nameError = SignUpError.NAME_REQUIRED,
                emailError = SignUpError.EMAIL_REQUIRED,
                passwordError = SignUpError.PASSWORD_REQUIRED,
                confirmPasswordError = SignUpError.CONFIRM_PASSWORD_REQUIRED,
            ),
            onClearNameError = {},
            onClearEmailError = {},
            onClearPasswordError = {},
            onClearConfirmPasswordError = {},
            onTogglePasswordVisibility = {},
            onToggleConfirmPasswordVisibility = {},
            onSignUpClick = { _, _, _, _ -> },
            onContinueWithAppleClick = {},
            onSignInClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852, name = "Sign Up Password Mismatch")
@Composable
private fun SignUpPasswordMismatchPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        SignUpContent(
            uiState = SignUpUiState(
                confirmPasswordError = SignUpError.PASSWORDS_DO_NOT_MATCH,
            ),
            onClearNameError = {},
            onClearEmailError = {},
            onClearPasswordError = {},
            onClearConfirmPasswordError = {},
            onTogglePasswordVisibility = {},
            onToggleConfirmPasswordVisibility = {},
            onSignUpClick = { _, _, _, _ -> },
            onContinueWithAppleClick = {},
            onSignInClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852, name = "Sign Up Form Error")
@Composable
private fun SignUpFormErrorPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        SignUpContent(
            uiState = SignUpUiState(
                formError = SignUpError.SIGN_UP_FAILED,
            ),
            onClearNameError = {},
            onClearEmailError = {},
            onClearPasswordError = {},
            onClearConfirmPasswordError = {},
            onTogglePasswordVisibility = {},
            onToggleConfirmPasswordVisibility = {},
            onSignUpClick = { _, _, _, _ -> },
            onContinueWithAppleClick = {},
            onSignInClick = {},
        )
    }
}
