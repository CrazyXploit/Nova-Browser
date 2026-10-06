package com.nova.browser.data

import android.content.Context

/**
 * Custom home background — gradient presets + custom color.
 */
object HomeBackgroundManager {

    enum class Preset(val id: String, val label: String) {
        NOVA("nova", "Nova"),
        OCEAN("ocean", "Ocean"),
        SUNSET("sunset", "Sunset"),
        FOREST("forest", "Forest"),
        MONO("mono", "Mono"),
        COSMIC("cosmic", "Cosmic"),
        SOLID("solid", "Solid"),
    }

    private const val PREFS = "nova_bg_prefs"
    private const val KEY_PRESET = "preset"
    private const val KEY_CUSTOM = "custom_color"

    @Volatile var preset: Preset = Preset.NOVA
    @Volatile var customColor: Long = 0xFF7C5CFF

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        preset = try {
            Preset.valueOf(prefs.getString(KEY_PRESET, "NOVA") ?: "NOVA")
        } catch (_: Exception) { Preset.NOVA }
        customColor = prefs.getLong(KEY_CUSTOM, 0xFF7C5CFF)
    }

    fun save(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PRESET, preset.name)
            .putLong(KEY_CUSTOM, customColor)
            .apply()
    }

    fun updatePreset(p: Preset) { preset = p }
    fun updateCustomColor(color: Long) { customColor = color; preset = Preset.SOLID }
}