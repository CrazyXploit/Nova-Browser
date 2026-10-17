package com.nova.browser.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object ErudaManager {

    private const val TAG = "ErudaManager"
    private const val ERUDA_DIR = "eruda"
    private const val ERUDA_FILE = "eruda.js"
    private const val ERUDA_CDN_URL = "https://cdn.jsdelivr.net/npm/eruda@3.0.1/eruda.js"

    const val ERUDA_LOCAL_URL =
        "https://appassets.androidplatform.net/files/eruda/eruda.js"

    @Volatile var enabled: Boolean = true
    @Volatile private var localReady: Boolean = false

    val isLocalReady: Boolean get() = localReady

    suspend fun ensureDownloaded(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.filesDir, ERUDA_DIR)
            if (!dir.exists()) dir.mkdirs()

            val file = File(dir, ERUDA_FILE)
            if (file.exists() && file.length() > 10_000) {
                localReady = true
                return@withContext true
            }

            val conn = (URL(ERUDA_CDN_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 20_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Mozilla/5.0 NovaBrowser")
            }

            val code = conn.responseCode
            if (code !in 200..299) {
                conn.disconnect()
                return@withContext false
            }

            conn.inputStream.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            conn.disconnect()

            val size = file.length()
            if (size < 10_000) {
                file.delete()
                return@withContext false
            }

            localReady = true
            true
        } catch (t: Throwable) {
            Log.e(TAG, "Eruda download error", t)
            false
        }
    }

    suspend fun forceRedownload(context: Context): Boolean = withContext(Dispatchers.IO) {
        val file = File(File(context.filesDir, ERUDA_DIR), ERUDA_FILE)
        if (file.exists()) file.delete()
        localReady = false
        ensureDownloaded(context)
    }

    /**
     * Checks if Eruda should be injected on this URL.
     * Skips heavy sites for speed.
     */
    fun shouldInject(url: String): Boolean {
        if (!enabled) return false
        if (url.isBlank()) return false
        if (url.startsWith("about:") ||
            url.startsWith("data:") ||
            url.startsWith("chrome:") ||
            url.contains("appassets.androidplatform.net")) return false

        val host = try {
            java.net.URI(url).host?.lowercase() ?: return true
        } catch (_: Exception) { return true }

        // Skip on known-heavy sites
        return SpeedTuning.SKIP_ERUDA_DOMAINS.none { host.endsWith(it) }
    }

    fun buildInitScript(): String {
        val srcUrl = if (localReady) ERUDA_LOCAL_URL else ERUDA_CDN_URL
        return """
            (function() {
                if (window.__novaErudaLoaded) return;
                if (location.href.startsWith('about:') ||
                    location.href.startsWith('data:') ||
                    location.href.startsWith('chrome:') ||
                    location.href.includes('appassets.androidplatform.net')) {
                    return;
                }
                window.__novaErudaLoaded = true;
                function initEruda() {
                    try {
                        if (typeof eruda === 'undefined') {
                            setTimeout(initEruda, 200);
                            return;
                        }
                        if (window.__novaErudaInited) return;
                        eruda.init({
                            defaults: { displaySize: 40, transparency: 0.9, theme: 'Dark' },
                            tool: ['console', 'elements', 'network']
                        });
                        window.__novaErudaInited = true;
                        eruda.show();
                    } catch(e) {}
                }
                function loadScript() {
                    var s = document.createElement('script');
                    s.src = '$srcUrl';
                    s.onload = initEruda;
                    s.onerror = function() {
                        var s2 = document.createElement('script');
                        s2.src = '$ERUDA_CDN_URL';
                        s2.onload = initEruda;
                        document.head.appendChild(s2);
                    };
                    (document.head || document.documentElement).appendChild(s);
                }
                if (document.head) loadScript();
                else document.addEventListener('DOMContentLoaded', loadScript);
            })();
        """.trimIndent()
    }
}