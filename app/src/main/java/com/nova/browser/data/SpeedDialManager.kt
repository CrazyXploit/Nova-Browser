package com.nova.browser.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * Speed Dial / Top Sites — tracks how many times each host is visited.
 * Auto-ranks by visit count. Persists top 8.
 */
object SpeedDialManager {

    data class Site(
        val host: String,
        val url: String,
        val title: String,
        val faviconUrl: String,
        val visitCount: Int,
        val lastVisit: Long,
    )

    private const val PREFS = "nova_speed_dial"
    private const val KEY_DATA = "data"

    private val visits = ConcurrentHashMap<String, VisitEntry>()

    private data class VisitEntry(
        var url: String,
        var title: String,
        var faviconUrl: String,
        var count: Int,
        var lastVisit: Long,
    )

    private val _topSites = MutableStateFlow<List<Site>>(emptyList())
    val topSites: StateFlow<List<Site>> = _topSites.asStateFlow()

    @Volatile private var loaded = false

    fun load(context: Context) {
        if (loaded) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val stored = prefs.getString(KEY_DATA, null) ?: return
        try {
            stored.split("§").forEach { entry ->
                val parts = entry.split("|")
                if (parts.size >= 5) {
                    val host = parts[0]
                    visits[host] = VisitEntry(
                        url = parts[1],
                        title = parts[2],
                        faviconUrl = parts[3],
                        count = parts[4].toIntOrNull() ?: 0,
                        lastVisit = parts.getOrNull(5)?.toLongOrNull() ?: 0L,
                    )
                }
            }
            rebuild()
        } catch (_: Exception) { }
        loaded = true
    }

    fun save(context: Context) {
        val serialized = visits.entries.joinToString("§") { (host, v) ->
            "$host|${v.url}|${v.title}|${v.faviconUrl}|${v.count}|${v.lastVisit}"
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_DATA, serialized)
            .apply()
    }

    fun recordVisit(url: String, title: String, faviconUrl: String?) {
        if (url.isBlank() ||
            url.startsWith("about:") ||
            url.startsWith("data:") ||
            url.startsWith("https://appassets.")) return

        val host = try {
            java.net.URI(url).host ?: return
        } catch (_: Exception) { return }

        val entry = visits[host]
        if (entry == null) {
            visits[host] = VisitEntry(
                url = url,
                title = title.ifBlank { host },
                faviconUrl = faviconUrl ?: "https://$host/favicon.ico",
                count = 1,
                lastVisit = System.currentTimeMillis(),
            )
        } else {
            entry.count += 1
            entry.lastVisit = System.currentTimeMillis()
            if (title.isNotBlank() && title != host) entry.title = title
            if (!faviconUrl.isNullOrBlank()) entry.faviconUrl = faviconUrl
        }
        rebuild()
    }

    fun remove(host: String, context: Context) {
        visits.remove(host)
        rebuild()
        save(context)
    }

    private fun rebuild() {
        val sorted = visits.entries
            .sortedByDescending { it.value.count * 1000L - (System.currentTimeMillis() - it.value.lastVisit) / 60000L }
            .take(12)
            .map { (host, v) ->
                Site(
                    host = host,
                    url = v.url,
                    title = v.title,
                    faviconUrl = v.faviconUrl,
                    visitCount = v.count,
                    lastVisit = v.lastVisit,
                )
            }
        _topSites.value = sorted
    }
}