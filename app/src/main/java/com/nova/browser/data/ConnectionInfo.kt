package com.nova.browser.data

/**
 * Read-only connection info. No enforcement — just reporting.
 */
object ConnectionInfo {

    fun isSecure(url: String): Boolean = url.startsWith("https://")

    fun isInsecure(url: String): Boolean = url.startsWith("http://")

    /**
     * Returns one of: "secure", "insecure", "internal", "unknown"
     */
    fun classify(url: String): String = when {
        url.isBlank() -> "internal"
        url.startsWith("https://") -> "secure"
        url.startsWith("http://") -> "insecure"
        url.startsWith("about:") -> "internal"
        url.startsWith("data:") -> "internal"
        url.startsWith("file:") -> "internal"
        else -> "unknown"
    }
}