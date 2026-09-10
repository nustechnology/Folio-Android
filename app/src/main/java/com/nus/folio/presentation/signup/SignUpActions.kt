package com.nus.folio.presentation.signup

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nus.folio.R
import com.nus.folio.ui.theme.LoginButtonGlow
import com.nus.folio.ui.theme.LoginPrimary
import com.nus.folio.ui.theme.LoginTextMuted
import com.nus.folio.ui.theme.LoginTextPrimary

private val ButtonShape = RoundedCornerShape(12.dp)

@Composable
internal fun SignUpPrimaryButton(
    onClick: () -> Unit,
    enabled: Boolean,
    isLoading: Boolean,
) {
    val loadingLabel = stringResource(R.string.signup_loading)
    val isEnabled = enabled && !isLoading
    Button(
        onClick = onClick,
        enabled = isEnabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .alpha(if (isEnabled) 1f else 0.6f)
            .shadow(
                elevation = 8.dp,
                shape = ButtonShape,
                ambientColor = LoginButtonGlow,
                spotColor = LoginButtonGlow,
            )
            .border(1.dp, LoginButtonGlow, ButtonShape)
            .semantics {
                if (isLoading) {
                    contentDescription = loadingLabel
                    progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
                }
            },
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
                text = stringResource(R.string.signup_create_account),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
internal fun SignUpFooter(
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
