package com.nus.folio.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nus.folio.ui.theme.FolioAndroidTheme
import com.nus.folio.ui.theme.LoginBackground
import com.nus.folio.ui.theme.LoginPrimary

@Composable
fun BouncingDotsIndicator(
    modifier: Modifier = Modifier,
    dotColor: Color = LoginPrimary,
    dotSize: Dp = 10.dp,
    dotSpacing: Dp = 10.dp,
    bounceHeight: Dp = 10.dp,
    staggerDelayMillis: Int = 140,
    bounceDurationMillis: Int = 380,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(dotSpacing),
        verticalAlignment = Alignment.Bottom,
    ) {
        repeat(3) { index ->
            BouncingDot(
                color = dotColor,
                size = dotSize,
                bounceHeight = bounceHeight,
                delayMillis = index * staggerDelayMillis,
                bounceDurationMillis = bounceDurationMillis,
            )
        }
    }
}

@Composable
private fun BouncingDot(
    color: Color,
    size: Dp,
    bounceHeight: Dp,
    delayMillis: Int,
    bounceDurationMillis: Int,
) {
    val transition = rememberInfiniteTransition(label = "bouncingDot")
    val offsetFraction by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = bounceDurationMillis,
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(delayMillis),
        ),
        label = "dotOffset",
    )
    Box(
        modifier = Modifier
            .size(size)
            .offset(y = -bounceHeight * offsetFraction)
            .clip(CircleShape)
            .background(color),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFFCF6E9)
@Composable
private fun BouncingDotsIndicatorPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        Box(
            modifier = Modifier
                .background(LoginBackground)
                .padding(24.dp),
        ) {
            BouncingDotsIndicator()
        }
    }
}
