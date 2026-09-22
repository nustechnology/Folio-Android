package com.nus.folio.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.nus.folio.ui.theme.HomeStatusFailedText

internal fun formatFieldLabel(
    label: String,
): AnnotatedString {
    val asteriskIndex = label.indexOf('*')
    if (asteriskIndex == -1) {
        return AnnotatedString(label)
    }
    return buildAnnotatedString {
        append(label.substring(0, asteriskIndex))
        withStyle(SpanStyle(color = HomeStatusFailedText)) {
            append("*")
        }
        if (asteriskIndex + 1 < label.length) {
            append(label.substring(asteriskIndex + 1))
        }
    }
}
