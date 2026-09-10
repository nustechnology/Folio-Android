package com.nus.folio.domain.util

import java.net.URI
import java.util.Locale

/**
 * Restricts img src values in source HTML so rendering cannot trigger
 * scriptable schemes or requests to local-network / private hosts (SSRF).
 *
 * Allowed: relative paths, data-image URIs, and http(s) / protocol-relative
 * URLs with a parseable public host. Loopback, link-local, and private
 * addresses are rejected. Everything else is treated as disallowed.
 *
 * Relative paths are resolved against [apiBaseUrl] before rendering because
 * the WebView document base is `file:///android_res/` (for bundled fonts) and
 * would otherwise fail to load Folio-hosted document images.
 */
object SourceImageUrlRules {
    private val SRC_ATTR_REGEX = Regex(
        """(?i)\bsrc\s*=\s*("[^"]*"|'[^']*'|[^\s>]+)""",
    )
    private val IPV4_REGEX = Regex(
        """^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})$""",
    )

    fun isAllowed(src: String, trustedBaseUrl: String = ""): Boolean {
        val value = src.trim()
        if (value.isEmpty()) return true
        val lower = value.lowercase(Locale.US)
        if (lower.startsWith("javascript:") || lower.startsWith("vbscript:")) return false
        if (lower.startsWith("data:")) return lower.startsWith("data:image/")
        if (lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("//")) {
            // trustedBaseUrl is reserved for relative rewrite; network URLs only
            // need a public host (blocks loopback / RFC1918 / link-local SSRF).
            return isPublicNetworkUrl(value)
        }
        // Unknown schemes (file:, blob:, …) must not load; bare relative paths are OK.
        return !hasExplicitScheme(value)
    }

    /**
     * Blanks disallowed `src` values and rewrites relative paths to absolute
     * URLs under [apiBaseUrl] so WebView can fetch Folio document images.
     */
    fun prepareSources(html: String, apiBaseUrl: String): String {
        val base = apiBaseUrl.trim().trimEnd('/')
        return SRC_ATTR_REGEX.replace(html) { match ->
            val value = unquote(match.groupValues[1])
            when {
                !isAllowed(value, base) -> """src="""""
                isRelativePath(value) && base.isNotEmpty() ->
                    """src="${escapeAttr(resolveRelative(base, value))}""""
                else -> match.value
            }
        }
    }

    /** Replaces disallowed `src` attribute values with an empty string. */
    fun neutralizeDisallowedSources(html: String, trustedBaseUrl: String = ""): String =
        SRC_ATTR_REGEX.replace(html) { match ->
            val value = unquote(match.groupValues[1])
            if (isAllowed(value, trustedBaseUrl)) match.value else """src="""""
        }

    private fun isPublicNetworkUrl(src: String): Boolean {
        val host = networkHost(src) ?: return false
        return !isLocalOrPrivateHost(host)
    }

    private fun isLocalOrPrivateHost(host: String): Boolean {
        val normalized = host.trim().lowercase(Locale.US).removeSurrounding("[", "]")
        if (
            normalized == "localhost" ||
            normalized.endsWith(".localhost") ||
            normalized == "0.0.0.0" ||
            normalized == "::" ||
            normalized == "::1"
        ) {
            return true
        }
        val ipv4 = parseIpv4(normalized)
        if (ipv4 != null) return isPrivateOrLocalIpv4(ipv4)
        if (normalized.contains(':')) {
            return isPrivateOrLocalIpv6(normalized)
        }
        return false
    }

    private fun parseIpv4(host: String): IntArray? {
        val match = IPV4_REGEX.matchEntire(host) ?: return null
        val octets = IntArray(4) { match.groupValues[it + 1].toInt() }
        if (octets.any { it !in 0..255 }) return null
        return octets
    }

    private fun isPrivateOrLocalIpv4(octets: IntArray): Boolean {
        val a = octets[0]
        val b = octets[1]
        return when {
            a == 127 -> true // loopback
            a == 10 -> true // RFC1918
            a == 0 -> true
            a == 169 && b == 254 -> true // link-local / metadata
            a == 172 && b in 16..31 -> true // RFC1918
            a == 192 && b == 168 -> true // RFC1918
            a == 100 && b in 64..127 -> true // CGNAT
            else -> false
        }
    }

    private fun isPrivateOrLocalIpv6(host: String): Boolean {
        // IPv4-mapped IPv6 (::ffff:x.x.x.x)
        val mapped = host.substringAfterLast(":", missingDelimiterValue = "")
        val mappedIpv4 = parseIpv4(mapped)
        if (host.contains('.') && mappedIpv4 != null && host.contains("ffff", ignoreCase = true)) {
            return isPrivateOrLocalIpv4(mappedIpv4)
        }
        val first = host.substringBefore(':').ifEmpty { "0" }
        val prefix = first.toIntOrNull(16) ?: return true
        // fe80::/10 link-local, fc00::/7 ULA, ::1 already handled
        return prefix in 0xfc00..0xfdff || prefix in 0xfe80..0xfebf
    }

    private fun isRelativePath(src: String): Boolean {
        val value = src.trim()
        if (value.isEmpty()) return false
        val lower = value.lowercase(Locale.US)
        if (
            lower.startsWith("http://") ||
            lower.startsWith("https://") ||
            lower.startsWith("//") ||
            lower.startsWith("data:")
        ) {
            return false
        }
        return !hasExplicitScheme(value)
    }

    private fun hasExplicitScheme(value: String): Boolean {
        val schemeEnd = value.indexOf(':')
        if (schemeEnd <= 0) return false
        val beforeSlash = value.asSequence()
            .takeWhile { it != '/' && it != '?' && it != '#' }
            .count()
        return schemeEnd < beforeSlash
    }

    private fun resolveRelative(base: String, relative: String): String {
        val path = relative.trim()
        return if (path.startsWith("/")) "$base$path" else "$base/$path"
    }

    private fun escapeAttr(value: String): String =
        value
            .replace("&", "&amp;")
            .replace("\"", "&quot;")
            .replace("<", "&lt;")

    private fun networkHost(src: String): String? {
        if (src.isBlank()) return null
        val absolute = if (src.startsWith("//")) "https:$src" else src
        return runCatching { URI(absolute.trim()).host?.lowercase(Locale.US) }.getOrNull()
    }

    private fun unquote(raw: String): String {
        val trimmed = raw.trim()
        return if (
            trimmed.length >= 2 &&
            (
                (trimmed.first() == '"' && trimmed.last() == '"') ||
                    (trimmed.first() == '\'' && trimmed.last() == '\'')
                )
        ) {
            trimmed.substring(1, trimmed.lastIndex)
        } else {
            trimmed
        }
    }
}
