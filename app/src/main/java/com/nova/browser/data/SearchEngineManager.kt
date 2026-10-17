package com.nova.browser.data

import android.content.Context

/**
 * Manages search engine choice. Persists across restarts.
 * Default: Google (modern).
 */
object SearchEngineManager {

    data class Engine(
        val id: String,
        val name: String,
        val searchUrl: String,
        val homepage: String,
        val suggestUrl: String? = null,
    )

    val engines: List<Engine> = listOf(
        Engine(
            id = "google",
            name = "Google",
            searchUrl = "https://www.google.com/search?q=%s&ie=UTF-8",
            homepage = "https://www.google.com",
            suggestUrl = "https://suggestqueries.google.com/complete/search?client=firefox&q=%s",
        ),
        Engine(
            id = "duckduckgo",
            name = "DuckDuckGo",
            searchUrl = "https://duckduckgo.com/?q=%s&ia=web",
            homepage = "https://duckduckgo.com",
        ),
        Engine(
            id = "bing",
            name = "Bing",
            searchUrl = "https://www.bing.com/search?q=%s",
            homepage = "https://www.bing.com",
        ),
        Engine(
            id = "brave",
            name = "Brave Search",
            searchUrl = "https://search.brave.com/search?q=%s",
            homepage = "https://search.brave.com",
        ),
        Engine(
            id = "ecosia",
            name = "Ecosia",
            searchUrl = "https://www.ecosia.org/search?q=%s",
            homepage = "https://www.ecosia.org",
        ),
        Engine(
            id = "startpage",
            name = "Startpage",
            searchUrl = "https://www.startpage.com/sp/search?query=%s",
            homepage = "https://www.startpage.com",
        ),
        Engine(
            id = "yandex",
            name = "Yandex",
            searchUrl = "https://yandex.com/search/?text=%s",
            homepage = "https://yandex.com",
        ),
    )

    private const val PREFS = "nova_search_prefs"
    private const val KEY_ENGINE_ID = "engine_id"

    @Volatile var currentId: String = "google"

    val current: Engine
        get() = engines.firstOrNull { it.id == currentId } ?: engines[0]

    val homepage: String get() = current.homepage

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        currentId = prefs.getString(KEY_ENGINE_ID, "google") ?: "google"
        // Sanity: if stored id doesn't exist, fallback to google
        if (engines.none { it.id == currentId }) currentId = "google"
    }

    fun save(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ENGINE_ID, currentId)
            .apply()
    }

    fun select(engineId: String) {
        if (engines.any { it.id == engineId }) currentId = engineId
    }

    fun buildSearchUrl(query: String): String {
        val encoded = try {
            java.net.URLEncoder.encode(query, "UTF-8")
        } catch (_: Exception) { query }
        return current.searchUrl.replace("%s", encoded)
    }
}