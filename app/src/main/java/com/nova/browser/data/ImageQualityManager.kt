package com.nova.browser.data

import android.content.Context
import android.webkit.WebSettings

/**
 * Controls image loading quality + data saver mode.
 * When Data Saver is ON, quality is FORCED to LOW, regardless of stored setting.
 */
object ImageQualityManager {

    enum class Quality { HIGH, MEDIUM, LOW, OFF }

    private const val PREFS = "nova_img_prefs"
    private const val KEY_QUALITY = "img_quality"
    private const val KEY_DATA_SAVER = "data_saver"

    @Volatile var quality: Quality = Quality.HIGH
    @Volatile var dataSaver: Boolean = false

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        quality = try {
            Quality.valueOf(prefs.getString(KEY_QUALITY, "HIGH") ?: "HIGH")
        } catch (_: Exception) { Quality.HIGH }
        dataSaver = prefs.getBoolean(KEY_DATA_SAVER, false)
    }

    fun save(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_QUALITY, quality.name)
            .putBoolean(KEY_DATA_SAVER, dataSaver)
            .apply()
    }

    fun updateQuality(q: Quality) { quality = q }
    fun updateDataSaver(enabled: Boolean) { dataSaver = enabled }

    /**
     * Data Saver overrides quality to LOW.
     */
    fun effectiveQuality(): Quality = if (dataSaver) Quality.LOW else quality

    /**
     * The label shown in UI — reflects effective behavior, not stored setting.
     */
    fun effectiveQualityLabel(): String = when (effectiveQuality()) {
        Quality.HIGH -> "High"
        Quality.MEDIUM -> "Medium"
        Quality.LOW -> "Low"
        Quality.OFF -> "Off"
    }

    fun shouldBlockImages(): Boolean = effectiveQuality() == Quality.OFF

    fun extraHeaders(): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        if (dataSaver) {
            headers["Save-Data"] = "on"
            headers["X-Data-Saver"] = "on"
        }
        when (effectiveQuality()) {
            Quality.LOW -> {
                headers["DPR"] = "1"
                headers["Viewport-Width"] = "360"
                headers["Width"] = "360"
                headers["Accept-CH"] = "DPR, Viewport-Width, Width, Save-Data"
            }
            Quality.MEDIUM -> {
                headers["DPR"] = "1.5"
                headers["Viewport-Width"] = "412"
            }
            Quality.HIGH -> { }
            Quality.OFF -> {
                headers["Save-Data"] = "on"
            }
        }
        return headers
    }

    /**
     * Inject JS to force low-quality image sources.
     */
    fun buildLowQualityJs(): String = """
        (function() {
            if (!window.__novaLowQuality) {
                window.__novaLowQuality = true;
                var config = { childList: true, subtree: true };
                function process() {
                    var imgs = document.querySelectorAll('img');
                    for (var i = 0; i < imgs.length; i++) {
                        var img = imgs[i];
                        // Force lazy loading
                        img.loading = 'lazy';
                        // Use smallest srcset entry
                        if (img.srcset && !img.dataset.novaProcessed) {
                            img.dataset.novaProcessed = '1';
                            var entries = img.srcset.split(',').map(function(s) {
                                var parts = s.trim().split(/\s+/);
                                return { url: parts[0], w: parseInt(parts[1]) || 9999 };
                            });
                            entries.sort(function(a, b) { return a.w - b.w; });
                            if (entries.length > 0) {
                                img.src = entries[0].url;
                                img.removeAttribute('srcset');
                            }
                        }
                    }
                }
                process();
                try {
                    var obs = new MutationObserver(process);
                    obs.observe(document.documentElement, config);
                } catch(e) {}
            }
        })();
    """.trimIndent()

    fun applyTo(settings: WebSettings) {
        settings.loadsImagesAutomatically = effectiveQuality() != Quality.OFF
        settings.blockNetworkImage = effectiveQuality() == Quality.OFF
    }
}