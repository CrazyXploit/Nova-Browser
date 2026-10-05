package com.nova.browser.data

import java.net.URI

/**
 * Extracts OSINT-style info about a URL.
 */
object SiteInfo {

    data class Info(
        val url: String,
        val scheme: String,
        val host: String,
        val port: String,
        val path: String,
        val query: String,
        val isSecure: Boolean,
        val isLocalhost: Boolean,
        val tld: String,
        val domainAgeHint: String,
    )

    fun analyze(rawUrl: String): Info {
        return try {
            val uri = URI(rawUrl)
            val host = uri.host ?: ""
            val tld = host.substringAfterLast(".", "")
            Info(
                url = rawUrl,
                scheme = uri.scheme ?: "unknown",
                host = host,
                port = if (uri.port == -1) defaultPort(uri.scheme) else uri.port.toString(),
                path = uri.path ?: "/",
                query = uri.query ?: "",
                isSecure = uri.scheme.equals("https", true),
                isLocalhost = host == "localhost" || host.startsWith("127."),
                tld = tld,
                domainAgeHint = when (tld) {
                    "gov" -> "Government — verified domain"
                    "edu" -> "Educational institution"
                    "mil" -> "Military — restricted"
                    "org" -> "Organization — non-profit usually"
                    "io" -> "Tech / startup common"
                    "com" -> "Commercial"
                    "net" -> "Network infrastructure"
                    else -> "Generic TLD"
                }
            )
        } catch (e: Exception) {
            Info(rawUrl, "unknown", "", "", "", "", false, false, "", "Could not parse")
        }
    }

    private fun defaultPort(scheme: String?): String = when (scheme) {
        "https" -> "443"
        "http" -> "80"
        "ftp" -> "21"
        else -> "?"
    }
}