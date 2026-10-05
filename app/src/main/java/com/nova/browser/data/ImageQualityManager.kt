package com.nova.browser.data

import android.content.Context
import android.webkit.WebSettings

/**
 * Controls image loading quality + data saver mode.
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

    fun updateQuality(q: Quality) {
        quality = q
    }

    fun updateDataSaver(enabled: Boolean) {
        dataSaver = enabled
    }

    fun effectiveQuality(): Quality = if (dataSaver) Quality.LOW else quality

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
                headers["Viewport-Width"] = "412"
            }
            Quality.MEDIUM -> {
                headers["DPR"] = "1.5"
            }
            Quality.HIGH -> { }
            Quality.OFF -> {
                headers["Save-Data"] = "on"
            }
        }
        return headers
    }

    fun applyTo(settings: WebSettings) {
        settings.loadsImagesAutomatically = effectiveQuality() != Quality.OFF
        settings.blockNetworkImage = effectiveQuality() == Quality.OFF
    }
}