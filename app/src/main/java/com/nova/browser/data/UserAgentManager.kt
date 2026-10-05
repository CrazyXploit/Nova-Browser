package com.nova.browser.data

import android.webkit.WebSettings

/**
 * Manages User Agent strings for desktop mode + UA switching.
 */
object UserAgentManager {

    enum class Mode { MOBILE, DESKTOP, CUSTOM }

    var mode: Mode = Mode.MOBILE
    var customUa: String = ""

    // Standard Chrome desktop UA
    private const val DESKTOP_UA =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    fun applyTo(settings: WebSettings) {
        val base = settings.userAgentString ?: ""
        settings.userAgentString = when (mode) {
            Mode.DESKTOP -> DESKTOP_UA
            Mode.CUSTOM -> customUa.ifBlank { base }
            Mode.MOBILE -> base.replace(" NovaIncognito/1.0", "")
        }
    }

    fun reset(settings: WebSettings) {
        // Reset to default
        mode = Mode.MOBILE
        applyTo(settings)
    }
}