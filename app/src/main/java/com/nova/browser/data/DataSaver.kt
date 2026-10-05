package com.nova.browser.data

import android.content.Context
import java.util.concurrent.atomic.AtomicLong

/**
 * Tracks data saved by:
 * - blocked trackers/ads (avg 50KB each)
 * - image quality reduction (approximate)
 * - image blocking
 */
object DataSaver {

    private const val PREFS = "nova_datasave_prefs"
    private const val KEY_SAVED_BYTES = "saved_bytes"

    // Average bytes saved per blocked resource
    private const val BYTES_PER_BLOCK = 50_000L           // 50 KB
    private const val BYTES_PER_IMAGE_SKIP = 120_000L     // 120 KB avg image

    private val _savedBytes = AtomicLong(0)

    val savedBytes: Long get() = _savedBytes.get()

    @Volatile private var loaded = false

    fun load(context: Context) {
        if (loaded) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _savedBytes.set(prefs.getLong(KEY_SAVED_BYTES, 0))
        loaded = true
    }

    fun save(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_SAVED_BYTES, _savedBytes.get())
            .apply()
    }

    /**
     * Called when a tracker/ad is blocked.
     */
    fun onBlockedResource() {
        _savedBytes.addAndGet(BYTES_PER_BLOCK)
    }

    /**
     * Called when an image is skipped.
     */
    fun onBlockedImage() {
        _savedBytes.addAndGet(BYTES_PER_IMAGE_SKIP)
    }

    /**
     * Called when a resource was served with reduced quality.
     * Approximate 60% savings on remaining bytes.
     */
    fun onCompressedResource(originalSize: Long) {
        if (originalSize > 0) {
            _savedBytes.addAndGet((originalSize * 60L) / 100L)
        }
    }

    fun reset(context: Context) {
        _savedBytes.set(0)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().clear().apply()
    }
}