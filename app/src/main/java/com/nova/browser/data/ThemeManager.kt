package com.nova.browser.data

import android.content.Context

/**
 * Global theme mode. Default = DARK.
 */
object ThemeManager {

    enum class Mode { LIGHT, DARK, SYSTEM }

    private const val PREFS = "nova_theme_prefs"
    private const val KEY_MODE = "theme_mode"

    @Volatile var mode: Mode = Mode.DARK

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_MODE, "DARK") ?: "DARK"
        mode = try { Mode.valueOf(saved) } catch (_: Exception) { Mode.DARK }
    }

    fun save(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MODE, mode.name)
            .apply()
    }

    // Renamed from setMode -> updateMode to avoid JVM clash with the var setter
    fun updateMode(newMode: Mode) {
        mode = newMode
    }

    fun isDark(context: Context): Boolean = when (mode) {
        Mode.DARK -> true
        Mode.LIGHT -> false
        Mode.SYSTEM -> {
            val uiMode = context.resources.configuration.uiMode and
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK
            uiMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
        }
    }
}