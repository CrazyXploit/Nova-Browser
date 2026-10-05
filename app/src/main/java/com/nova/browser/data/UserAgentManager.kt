package com.nova.browser.data

import android.content.Context
import android.webkit.WebSettings

/**
 * Manages User Agent + Desktop Mode.
 * Persists across app restarts via SharedPreferences.
 */
object UserAgentManager {

    enum class Mode { MOBILE, DESKTOP, CUSTOM }

    private const val PREFS = "nova_ua_prefs"
    private const val KEY_MODE = "ua_mode"
    private const val KEY_CUSTOM = "ua_custom"

    @Volatile var mode: Mode = Mode.MOBILE
    @Volatile var customUa: String = ""
    @Volatile private var defaultMobileUa: String = ""

    private const val DESKTOP_UA =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_MODE, "MOBILE") ?: "MOBILE"
        mode = try { Mode.valueOf(saved) } catch (_: Exception) { Mode.MOBILE }
        customUa = prefs.getString(KEY_CUSTOM, "") ?: ""
    }

    fun save(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MODE, mode.name)
            .putString(KEY_CUSTOM, customUa)
            .apply()
    }

    /**
     * Call once when WebView is created — remembers the device's default UA.
     */
    fun captureDefault(settings: WebSettings) {
        if (defaultMobileUa.isBlank()) {
            defaultMobileUa = settings.userAgentString ?: ""
        }
    }

    fun setMode(newMode: Mode, custom: String? = null) {
        mode = newMode
        if (newMode == Mode.CUSTOM && custom != null) {
            customUa = custom
        }
    }

    fun applyTo(settings: WebSettings) {
        val mobileUa = defaultMobileUa.ifBlank {
            settings.userAgentString?.replace(" NovaIncognito/1.0", "") ?: ""
        }
        settings.userAgentString = when (mode) {
            Mode.MOBILE -> mobileUa
            Mode.DESKTOP -> DESKTOP_UA
            Mode.CUSTOM -> customUa.ifBlank { mobileUa }
        }
    }

    fun currentUa(settings: WebSettings): String = when (mode) {
        Mode.MOBILE -> defaultMobileUa.ifBlank { settings.userAgentString ?: "" }
        Mode.DESKTOP -> DESKTOP_UA
        Mode.CUSTOM -> customUa.ifBlank { settings.userAgentString ?: "" }
    }
}