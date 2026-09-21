package com.nus.folio.domain.util

import java.net.URI
import java.util.Locale

/**
 * Restricts img src values in source HTML so rendering cannot trigger
 * scriptable schemes or requests to local-network / private hosts (SSRF).
 *
 * Allowed: relative paths, data-image URIs, and http(s) / protocol-relative
 * URLs with a parseable public host. Loopback, link-local, and private
 * addresses are rejected, including Chromium legacy numeric IPv4 host forms
 * (decimal, octal, hex, and short dotted forms). Everything else is treated
 * as disallowed.
 *
 * `srcset` and `imagesrcset` are stripped entirely: WebView can pick any
 * candidate, so leaving them would bypass per-URL [isAllowed] checks on `src`.
 *
 * Relative paths are resolved against [apiBaseUrl] before rendering because
 * the WebView document base is `file:///android_res/` (for bundled fonts) and
 * would otherwise fail to load Folio-hosted document images.
 */
object SourceImageUrlRules {
    private val SRC_ATTR_REGEX = Regex(
        """(?i)\bsrc\s*=\s*("[^"]*"|'[^']*'|[^\s>]+)""",
    )
    private val SRCSET_ATTR_REGEX = Regex(
        """(?i)\s*\b(?:imagesrcset|srcset)\s*=\s*("[^"]*"|'[^']*'|[^\s>]+)""",
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
     * Strips `srcset` / `imagesrcset`, blanks disallowed `src` values, and
     * rewrites relative paths to absolute URLs under [apiBaseUrl] so WebView
     * can fetch Folio document images.
     */
    fun prepareSources(html: String, apiBaseUrl: String): String {
        val base = apiBaseUrl.trim().trimEnd('/')
        return SRC_ATTR_REGEX.replace(stripSrcsetAttributes(html)) { match ->
            val value = unquote(match.groupValues[1])
            when {
                !isAllowed(value, base) -> """src="""""
                isRelativePath(value) && base.isNotEmpty() ->
                    """src="${escapeAttr(resolveRelative(base, value))}""""
                else -> match.value
            }
        }
    }

    /**
     * Strips `srcset` / `imagesrcset` and blanks disallowed `src` values without rewriting relative paths.
     */
    fun neutralizeDisallowedSources(html: String, trustedBaseUrl: String = ""): String {
        val base = trustedBaseUrl.trim().trimEnd('/')
        return SRC_ATTR_REGEX.replace(stripSrcsetAttributes(html)) { match ->
            val value = unquote(match.groupValues[1])
            if (!isAllowed(value, base)) """src=""""" else match.value
        }
    }

    /** Removes responsive-image candidate lists that would bypass `src` checks. */
    private fun stripSrcsetAttributes(html: String): String =
        SRCSET_ATTR_REGEX.replace(html, "")

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
        // Chromium resolves decimal / octal / hex IPv4 forms (e.g. 2130706433,
        // 0177.0.0.1, 0x7f000001). Reject hosts that look numeric but fail to
        // parse so they cannot bypass the private-range check as "hostnames".
        if (looksLikeNumericIpv4(normalized)) return true
        if (normalized.contains(':')) {
            return isPrivateOrLocalIpv6(normalized)
        }
        return false
    }

    /**
     * Parses dotted and legacy numeric IPv4 host forms the way Chromium does:
     * 1–4 parts, each decimal / octal (`0…`) / hex (`0x…`), with fewer than
     * four parts expanding into the trailing 32-bit / 24-bit / 16-bit field.
     */
    private fun parseIpv4(host: String): IntArray? {
        val parts = host.split('.')
        if (parts.size !in 1..4 || parts.any { it.isEmpty() }) return null
        val numbers = LongArray(parts.size) { parseIpv4Part(parts[it]) ?: return null }
        return when (parts.size) {
            1 -> expandIpv4Parts(numbers[0], bitWidths = intArrayOf(32))
            2 -> expandIpv4Parts(numbers[0], numbers[1], bitWidths = intArrayOf(8, 24))
            3 -> expandIpv4Parts(
                numbers[0],
                numbers[1],
                numbers[2],
                bitWidths = intArrayOf(8, 8, 16),
            )
            4 -> expandIpv4Parts(
                numbers[0],
                numbers[1],
                numbers[2],
                numbers[3],
                bitWidths = intArrayOf(8, 8, 8, 8),
            )
            else -> null
        }
    }

    private fun parseIpv4Part(part: String): Long? {
        if (part.isEmpty()) return null
        val lower = part.lowercase(Locale.US)
        return when {
            lower.startsWith("0x") -> {
                if (lower.length == 2) return null
                lower.substring(2).toLongOrNull(16)
            }
            lower.length > 1 && lower[0] == '0' -> {
                if (lower.any { it !in '0'..'7' }) return null
                lower.toLongOrNull(8)
            }
            else -> {
                if (lower.any { it !in '0'..'9' }) return null
                lower.toLongOrNull(10)
            }
        }
    }

    private fun expandIpv4Parts(vararg numbers: Long, bitWidths: IntArray): IntArray? {
        if (numbers.size != bitWidths.size) return null
        var value = 0L
        for (i in numbers.indices) {
            val n = numbers[i]
            val width = bitWidths[i]
            val max = (1L shl width) - 1L
            if (n !in 0..max) return null
            value = (value shl width) or n
        }
        if (value !in 0..0xffff_ffffL) return null
        return intArrayOf(
            ((value ushr 24) and 0xff).toInt(),
            ((value ushr 16) and 0xff).toInt(),
            ((value ushr 8) and 0xff).toInt(),
            (value and 0xff).toInt(),
        )
    }

    /** True when [host] is only digits / dots / `0x` hex parts (legacy IPv4 shape). */
    private fun looksLikeNumericIpv4(host: String): Boolean {
        val parts = host.split('.')
        if (parts.size !in 1..4 || parts.any { it.isEmpty() }) return false
        return parts.all { part ->
            val lower = part.lowercase(Locale.US)
            when {
                lower.startsWith("0x") ->
                    lower.length > 2 && lower.substring(2).all { it in '0'..'9' || it in 'a'..'f' }
                else -> lower.all { it in '0'..'9' }
            }
        }
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
        // IPv4-mapped IPv6 (::ffff:a.b.c.d or ::ffff:XXXX:YYYY hex)
        val mappedIpv4 = parseIpv4MappedHost(host)
        if (mappedIpv4 != null) {
            return isPrivateOrLocalIpv4(mappedIpv4)
        }
        val first = host.substringBefore(':').ifEmpty { "0" }
        val prefix = first.toIntOrNull(16) ?: return true
        // ::/8 (prefix 0) covers expanded loopback / unspecified (0:0:0:0:0:0:0:1,
        // 0:0:0:0:0:0:0:0); compressed ::1 / :: are handled earlier. Mapped
        // ::ffff: addresses are already classified above.
        if (prefix == 0) return true
        // fe80::/10 link-local, fc00::/7 ULA
        return prefix in 0xfc00..0xfdff || prefix in 0xfe80..0xfebf
    }

    /**
     * Decodes IPv4-mapped IPv6 (`::ffff:0:0/96`) to four octets.
     *
     * Accepts dotted (`::ffff:127.0.0.1`) and hexadecimal (`::ffff:7f00:1`)
     * embedded forms. Returns null when [host] is not an IPv4-mapped address.
     */
    private fun parseIpv4MappedHost(host: String): IntArray? {
        val marker = "ffff:"
        val markerIndex = host.indexOf(marker)
        if (markerIndex < 0) return null
        val before = host.substring(0, markerIndex).trimEnd(':')
        if (before.isNotEmpty() && !before.split(':').all { it.isEmpty() || it == "0" }) {
            return null
        }
        val embedded = host.substring(markerIndex + marker.length)
        parseIpv4(embedded)?.let { return it }
        val hextets = embedded.split(':')
        if (hextets.size != 2) return null
        val hi = hextets[0].toIntOrNull(16) ?: return null
        val lo = hextets[1].toIntOrNull(16) ?: return null
        if (hi !in 0..0xffff || lo !in 0..0xffff) return null
        return intArrayOf(
            (hi ushr 8) and 0xff,
            hi and 0xff,
            (lo ushr 8) and 0xff,
            lo and 0xff,
        )
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
