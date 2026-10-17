package com.nova.browser.data

/**
 * Global speed configuration.
 * Central place for all perf tuning.
 */
object SpeedTuning {

    /** Max tabs before auto-closing oldest */
    const val MAX_TABS = 15

    /** Lazy load images only when scrolled into view */
    const val LAZY_LOAD_IMAGES = true

    /** Prefetch DNS for visited domains */
    const val PREFETCH_DNS = true

    /** Cache max size in MB */
    const val WEBVIEW_CACHE_MB = 200

    /** Preload hint — load pages in background */
    const val PRELOAD_BACKGROUND_PAGES = true

    /** Debounce progress updates — only push to UI every X ms */
    const val PROGRESS_UPDATE_MS = 250L

    /** Debounce URL input → 100ms before lookup */
    const val URL_INPUT_DEBOUNCE_MS = 100L

    /** Don't inject Eruda on these domains (they're slow enough) */
    val SKIP_ERUDA_DOMAINS = setOf(
        "youtube.com", "youtu.be", "m.youtube.com",
        "twitter.com", "x.com",
        "instagram.com",
        "facebook.com", "m.facebook.com",
        "tiktok.com",
        "netflix.com",
        "spotify.com",
    )

    /** Known-fast CDNs — preconnect to these if the page requests them */
    val PRECONNECT_HOSTS = setOf(
        "fonts.googleapis.com",
        "fonts.gstatic.com",
        "ajax.googleapis.com",
        "cdn.jsdelivr.net",
        "unpkg.com",
        "cdnjs.cloudflare.com",
    )
}