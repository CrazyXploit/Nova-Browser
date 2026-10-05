package com.nova.browser.data

import android.webkit.CookieManager
import java.net.URI

/**
 * Reads cookies for a given URL. Shows name, value, domain, path.
 */
object CookieReader {

    data class CookieEntry(
        val name: String,
        val value: String,
        val domain: String,
        val path: String,
        val secure: Boolean,
        val httpOnly: Boolean,
    ) {
        val maskedValue: String
            get() = if (value.length <= 8) value
            else value.take(4) + "…" + value.takeLast(4)
    }

    /**
     * Returns all cookies applicable to the given URL.
     */
    fun cookiesFor(url: String): List<CookieEntry> {
        if (url.isBlank() ||
            url.startsWith("about:") ||
            url.startsWith("data:")) {
            return emptyList()
        }

        return try {
            val manager = CookieManager.getInstance()
            val cookieString = manager.getCookie(url) ?: return emptyList()

            cookieString
                .split(";")
                .mapNotNull { parseCookie(it.trim(), url) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Parse a single "name=value" cookie. Metadata comes from the URL host.
     */
    private fun parseCookie(pair: String, url: String): CookieEntry? {
        val eq = pair.indexOf('=')
        if (eq <= 0) return null

        val name = pair.substring(0, eq).trim()
        val value = pair.substring(eq + 1).trim()
        if (name.isBlank()) return null

        val host = try {
            URI(url).host ?: ""
        } catch (_: Exception) { "" }

        return CookieEntry(
            name = name,
            value = value,
            domain = host,
            path = "/",
            secure = url.startsWith("https://"),
            httpOnly = false,
        )
    }

    /**
     * Builds a cookie header string suitable for copy-paste.
     */
    fun buildCookieHeader(entries: List<CookieEntry>): String {
        return entries.joinToString("; ") { "${it.name}=${it.value}" }
    }

    /**
     * Builds a pretty-printed multi-line string for copy.
     */
    fun buildPretty(entries: List<CookieEntry>): String {
        return entries.joinToString("\n") {
            "${it.name}=${it.value}  (domain=${it.domain}, path=${it.path})"
        }
    }

    /**
     * Clears cookies for a specific URL (not implemented in WebView API — only global).
     * Falls back to full clear.
     */
    fun clearAll() {
        try {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
        } catch (_: Exception) { }
    }
}