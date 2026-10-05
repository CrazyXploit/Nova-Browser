package com.nova.browser.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object UsageStats {

    private const val PREFS = "nova_stats_prefs"
    private const val KEY_TOTAL_MS = "total_ms"
    private const val KEY_TRACKERS = "trackers_blocked"
    private const val KEY_ADS = "ads_blocked"

    // Estimated time saved per blocked resource (~80ms)
    private const val MS_SAVED_PER_BLOCK = 80L

    data class Stats(
        val totalTimeMs: Long = 0,
        val trackersBlocked: Int = 0,
        val adsBlocked: Int = 0,
        val currentSessionMs: Long = 0,
        val dataSavedBytes: Long = 0,
    ) {
        val timeSavedMs: Long
            get() = (trackersBlocked + adsBlocked) * MS_SAVED_PER_BLOCK
    }

    private val _stats = MutableStateFlow(Stats())
    val stats: StateFlow<Stats> = _stats.asStateFlow()

    @Volatile private var loaded = false

    fun load(context: Context) {
        if (loaded) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val total = prefs.getLong(KEY_TOTAL_MS, 0)
        val trackers = prefs.getInt(KEY_TRACKERS, 0)
        val ads = prefs.getInt(KEY_ADS, 0)
        // Load saved bytes from DataSaver
        DataSaver.load(context)
        _stats.value = Stats(total, trackers, ads, 0, DataSaver.savedBytes)
        loaded = true
    }

    fun save(context: Context) {
        val s = _stats.value
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_TOTAL_MS, s.totalTimeMs)
            .putInt(KEY_TRACKERS, s.trackersBlocked)
            .putInt(KEY_ADS, s.adsBlocked)
            .apply()
        DataSaver.save(context)
    }

    fun addSessionTime(ms: Long) {
        if (ms <= 0) return
        val s = _stats.value
        _stats.value = s.copy(
            totalTimeMs = s.totalTimeMs + ms,
            currentSessionMs = s.currentSessionMs + ms,
        )
    }

    fun syncBlockedCounts() {
        val s = _stats.value
        val t = AdBlocker.trackersBlocked
        val a = AdBlocker.adsBlocked
        val d = DataSaver.savedBytes
        if (s.trackersBlocked != t || s.adsBlocked != a || s.dataSavedBytes != d) {
            _stats.value = s.copy(
                trackersBlocked = t,
                adsBlocked = a,
                dataSavedBytes = d,
            )
        }
    }

    fun reset(context: Context) {
        _stats.value = Stats()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().clear().apply()
        DataSaver.reset(context)
    }
}