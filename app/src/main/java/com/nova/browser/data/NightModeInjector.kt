package com.nova.browser.data

import android.content.Context

/**
 * Via-style night mode — injects CSS that darkens any page.
 * Works on sites that don't have native dark mode.
 */
object NightModeInjector {

    enum class Mode { OFF, AUTO, ON }

    private const val PREFS = "nova_night_prefs"
    private const val KEY_MODE = "mode"

    @Volatile var mode: Mode = Mode.AUTO

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        mode = try {
            Mode.valueOf(prefs.getString(KEY_MODE, "AUTO") ?: "AUTO")
        } catch (_: Exception) { Mode.AUTO }
    }

    fun save(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MODE, mode.name)
            .apply()
    }

    fun updateMode(newMode: Mode) { mode = newMode }

    fun shouldApply(isSystemDark: Boolean): Boolean = when (mode) {
        Mode.OFF -> false
        Mode.ON -> true
        Mode.AUTO -> isSystemDark
    }

    fun buildCss(): String = """
        (function() {
            if (window.__novaNightMode) return;
            window.__novaNightMode = true;

            var css = `
                html {
                    filter: invert(90%) hue-rotate(180deg) !important;
                    background: #1a1a1a !important;
                }
                img, video, iframe, canvas, svg[data-src], [style*="background-image"] {
                    filter: invert(100%) hue-rotate(180deg) !important;
                }
                input, textarea, select, button {
                    filter: invert(10%) hue-rotate(180deg) !important;
                    background: #2a2a2a !important;
                    color: #ececec !important;
                }
                a {
                    color: #7c5cff !important;
                }
            `;

            var style = document.createElement('style');
            style.id = 'nova-night-mode';
            style.textContent = css;
            (document.head || document.documentElement).appendChild(style);
        })();
    """.trimIndent()

    fun removeCss(): String = """
        (function() {
            window.__novaNightMode = false;
            var s = document.getElementById('nova-night-mode');
            if (s) s.remove();
        })();
    """.trimIndent()
}