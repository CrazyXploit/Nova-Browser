package com.nova.browser.data

import android.content.Context

/**
 * Enforces HTTPS where possible + tracks insecure connections.
 */
object HttpsEnforcer {

    private const val PREFS = "nova_https_prefs"
    private const val KEY_ENFORCE = "enforce_https"

    @Volatile var enforceHttps: Boolean = true
    @Volatile var lastPageInsecure: Boolean = false

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        enforceHttps = prefs.getBoolean(KEY_ENFORCE, true)
    }

    fun save(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENFORCE, enforceHttps)
            .apply()
    }

    /**
     * Upgrade http:// → https:// if enforcement is on.
     */
    fun upgrade(url: String): String {
        if (!enforceHttps) return url
        return if (url.startsWith("http://")) {
            "https://" + url.substring(7)
        } else url
    }

    /**
     * Whether this URL is considered insecure.
     */
    fun isInsecure(url: String): Boolean =
        url.startsWith("http://") ||
        (!url.startsWith("https://") &&
            !url.startsWith("about:") &&
            !url.startsWith("data:") &&
            !url.startsWith("file:"))
}