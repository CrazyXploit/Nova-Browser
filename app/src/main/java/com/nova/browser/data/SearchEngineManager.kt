package com.nova.browser.data

import android.content.Context

/**
 * Manages search engine choice. Persists across restarts.
 * If user never chooses, defaults to DuckDuckGo.
 */
object SearchEngineManager {

    data class Engine(
        val id: String,
        val name: String,
        val searchUrl: String,       // %s gets replaced by query
        val homepage: String,        // what to load as "home"
        val suggestUrl: String? = null,
    )

    val engines: List<Engine> = listOf(
        Engine(
            id = "duckduckgo",
            name = "DuckDuckGo",
            searchUrl = "https://duckduckgo.com/?q=%s",
            homepage = "https://duckduckgo.com",
            suggestUrl = "https://duckduckgo.com/ac/?q=%s",
        ),
        Engine(
            id = "google",
            name = "Google",
            searchUrl = "https://www.google.com/search?q=%s",
            homepage = "https://www.google.com",
            suggestUrl = "https://suggestqueries.google.com/complete/search?client=firefox&q=%s",
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
        Engine(
            id = "custom",
            name = "Custom",
            searchUrl = "",
            homepage = "",
        ),
    )

    private const val PREFS = "nova_search_prefs"
    private const val KEY_ENGINE_ID = "engine_id"
    private const val KEY_CUSTOM_SEARCH = "custom_search_url"
    private const val KEY_CUSTOM_HOME = "custom_home_url"

    // Current engine id — survives restarts
    @Volatile var currentId: String = "duckduckgo"
    @Volatile var customSearchUrl: String = ""
    @Volatile var customHomeUrl: String = ""

    val current: Engine
        get() {
            val found = engines.firstOrNull { it.id == currentId }
            if (found != null && found.id != "custom") return found
            return Engine(
                id = "custom",
                name = "Custom",
                searchUrl = customSearchUrl.ifBlank { engines[0].searchUrl },
                homepage = customHomeUrl.ifBlank { engines[0].homepage },
            )
        }

    val homepage: String get() = current.homepage.ifBlank { engines[0].homepage }

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        currentId = prefs.getString(KEY_ENGINE_ID, "duckduckgo") ?: "duckduckgo"
        customSearchUrl = prefs.getString(KEY_CUSTOM_SEARCH, "") ?: ""
        customHomeUrl = prefs.getString(KEY_CUSTOM_HOME, "") ?: ""
    }

    fun save(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ENGINE_ID, currentId)
            .putString(KEY_CUSTOM_SEARCH, customSearchUrl)
            .putString(KEY_CUSTOM_HOME, customHomeUrl)
            .apply()
    }

    fun select(engineId: String, customSearch: String? = null, customHome: String? = null) {
        currentId = engineId
        if (engineId == "custom") {
            if (customSearch != null) customSearchUrl = customSearch
            if (customHome != null) customHomeUrl = customHome
        }
    }

    /**
     * Build search URL for a query.
     */
    fun buildSearchUrl(query: String): String {
        val engine = current
        val encoded = try {
            java.net.URLEncoder.encode(query, "UTF-8")
        } catch (_: Exception) { query }

        return if (engine.searchUrl.contains("%s")) {
            engine.searchUrl.replace("%s", encoded)
        } else {
            // Fallback — treat as homepage and append query
            engine.homepage + "?q=" + encoded
        }
    }
}