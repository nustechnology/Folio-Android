package com.nus.folio.domain.model

/**
 * Avatar initials from first and last name (e.g. "Jordan Lee" → "JL").
 * Falls back to the first letter of [emailFallback] when [displayName] is blank.
 */
fun initialsFromDisplayName(displayName: String, emailFallback: String = ""): String {
    val parts = displayName.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        parts.size >= 2 -> buildString {
            append(parts.first().first().uppercaseChar())
            append(parts.last().first().uppercaseChar())
        }
        parts.size == 1 -> parts[0].first().uppercaseChar().toString()
        else -> emailFallback.firstOrNull()?.uppercaseChar()?.toString().orEmpty()
    }
}
