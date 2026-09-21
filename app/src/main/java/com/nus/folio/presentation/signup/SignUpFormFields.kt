package com.nus.folio.presentation.signup

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nus.folio.R
import com.nus.folio.components.formatFieldLabel
import com.nus.folio.ui.theme.HomeStatusFailedText
import com.nus.folio.ui.theme.LoginBackground
import com.nus.folio.ui.theme.LoginBorder
import com.nus.folio.ui.theme.LoginCopper
import com.nus.folio.ui.theme.LoginPlaceholder
import com.nus.folio.ui.theme.LoginTextPrimary
import com.nus.folio.ui.theme.LoginTextSecondary

private val FieldShape = RoundedCornerShape(12.dp)

@Composable
internal fun SignUpLabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    enabled: Boolean,
    keyboardType: KeyboardType,
    errorMessage: String? = null,
) {
    val isError = errorMessage != null

    Text(
        text = remember(label) { formatFieldLabel(label) },
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
        isError = isError,
        placeholder = {
            Text(
                text = placeholder,
                color = LoginPlaceholder,
                fontSize = 15.sp,
            )
        },
        supportingText = errorMessage?.let { message ->
            {
                Text(
                    text = message,
                    color = HomeStatusFailedText,
                    fontSize = 12.sp,
                )
            }
        },
        singleLine = true,
        shape = FieldShape,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = signUpTextFieldColors(),
    )
}

@Composable
internal fun SignUpPasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    passwordVisible: Boolean,
    onToggleVisibility: () -> Unit,
    showPasswordDescription: String,
    hidePasswordDescription: String,
    enabled: Boolean,
    errorMessage: String? = null,
) {
    val isError = errorMessage != null

    Text(
        text = remember(label) { formatFieldLabel(label) },
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
        isError = isError,
        placeholder = {
            Text(
                text = placeholder,
                color = LoginPlaceholder,
                fontSize = 15.sp,
            )
        },
        supportingText = errorMessage?.let { message ->
            {
                Text(
                    text = message,
                    color = HomeStatusFailedText,
                    fontSize = 12.sp,
                )
            }
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
                    contentDescription = if (passwordVisible) {
                        hidePasswordDescription
                    } else {
                        showPasswordDescription
                    },
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
    errorContainerColor = LoginBackground,
    focusedBorderColor = LoginBorder,
    unfocusedBorderColor = LoginBorder,
    disabledBorderColor = LoginBorder,
    errorBorderColor = HomeStatusFailedText,
    cursorColor = LoginTextPrimary,
    errorCursorColor = HomeStatusFailedText,
    focusedTextColor = LoginTextPrimary,
    unfocusedTextColor = LoginTextPrimary,
    errorSupportingTextColor = HomeStatusFailedText,
)

@Composable
internal fun signUpErrorMessage(error: SignUpError): String = when (error) {
    SignUpError.NAME_REQUIRED -> stringResource(R.string.signup_error_name_required)
    SignUpError.EMAIL_REQUIRED -> stringResource(R.string.signup_error_email_required)
    SignUpError.EMAIL_INVALID -> stringResource(R.string.signup_error_email_invalid)
    SignUpError.EMAIL_ALREADY_EXISTS ->
        stringResource(R.string.signup_error_email_already_exists)
    SignUpError.PASSWORD_REQUIRED -> stringResource(R.string.signup_error_password_required)
    SignUpError.PASSWORD_TOO_SHORT -> stringResource(R.string.signup_error_password_too_short)
    SignUpError.CONFIRM_PASSWORD_REQUIRED ->
        stringResource(R.string.signup_error_confirm_password_required)
    SignUpError.PASSWORDS_DO_NOT_MATCH ->
        stringResource(R.string.signup_error_passwords_do_not_match)
    SignUpError.SIGN_UP_FAILED -> stringResource(R.string.signup_error_sign_up_failed)
    SignUpError.NETWORK_ERROR -> stringResource(R.string.signup_error_network)
}

@Composable
internal fun SignUpFormErrorFeedback(uiState: SignUpUiState) {
    val errorText = when {
        uiState.formError == SignUpError.SIGN_UP_FAILED ->
            stringResource(R.string.signup_error_sign_up_failed)
        uiState.formError == SignUpError.NETWORK_ERROR ->
            stringResource(R.string.signup_error_network)
        uiState.authUnavailable -> stringResource(R.string.login_error_auth_unavailable)
        else -> null
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
