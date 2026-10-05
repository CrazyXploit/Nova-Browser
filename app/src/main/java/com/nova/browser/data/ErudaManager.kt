package com.nova.browser.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Manages Eruda DevTools script.
 *
 * On first launch:
 *  - Downloads eruda.js from CDN
 *  - Saves to /data/data/com.nova.browser/files/eruda/eruda.js
 *
 * Subsequent launches:
 *  - Uses the local file (no download)
 *
 * If download fails, falls back to CDN loading.
 */
object ErudaManager {

    private const val TAG = "ErudaManager"

    // Where we save the file locally
    private const val ERUDA_DIR = "eruda"
    private const val ERUDA_FILE = "eruda.js"

    // Where we download from
    private const val ERUDA_CDN_URL = "https://cdn.jsdelivr.net/npm/eruda@3.0.1/eruda.js"

    // URL that WebViewAssetLoader will serve our local file from
    const val ERUDA_LOCAL_URL = "https://appassets.androidplatform.net/files/eruda/eruda.js"

    // Whether Eruda is enabled by user
    @Volatile
    var enabled: Boolean = true

    // Whether Eruda script is downloaded and available locally
    @Volatile
    private var localReady: Boolean = false

    val isLocalReady: Boolean get() = localReady

    /**
     * Call once at app startup. Downloads Eruda if not present.
     * Safe to call multiple times — only downloads once.
     */
    suspend fun ensureDownloaded(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.filesDir, ERUDA_DIR)
            if (!dir.exists()) dir.mkdirs()

            val file = File(dir, ERUDA_FILE)

            // Already downloaded and valid?
            if (file.exists() && file.length() > 10_000) {
                Log.d(TAG, "Eruda already downloaded: ${file.length()} bytes")
                localReady = true
                return@withContext true
            }

            Log.d(TAG, "Downloading Eruda from $ERUDA_CDN_URL")

            val conn = (URL(ERUDA_CDN_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 20_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Mozilla/5.0 NovaBrowser")
            }

            val code = conn.responseCode
            if (code !in 200..299) {
                Log.e(TAG, "Download failed: HTTP $code")
                return@withContext false
            }

            conn.inputStream.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            conn.disconnect()

            val size = file.length()
            Log.d(TAG, "Eruda downloaded: $size bytes")

            if (size < 10_000) {
                Log.e(TAG, "Downloaded file too small — deleting")
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

    /**
     * Force re-download (for updates).
     */
    suspend fun forceRedownload(context: Context): Boolean = withContext(Dispatchers.IO) {
        val file = File(File(context.filesDir, ERUDA_DIR), ERUDA_FILE)
        if (file.exists()) file.delete()
        localReady = false
        ensureDownloaded(context)
    }

    /**
     * Builds the JS injection script.
     * Uses local file if ready, otherwise falls back to CDN.
     */
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

                function runEruda() {
                    try {
                        if (typeof eruda === 'undefined') {
                            console.log('Nova: eruda object missing after load');
                            return;
                        }
                        eruda.init({
                            defaults: {
                                displaySize: 40,
                                transparency: 0.9,
                                theme: 'Dark'
                            },
                            tool: ['console', 'elements', 'network', 'resources', 'info']
                        });
                        eruda.show();
                        console.log('Nova: Eruda loaded OK from $srcUrl');
                    } catch (e) {
                        console.log('Nova: Eruda init failed', e);
                    }
                }

                var script = document.createElement('script');
                script.src = '$srcUrl';
                script.onload = runEruda;
                script.onerror = function() {
                    console.log('Nova: Eruda failed to load from ' + script.src);
                };
                (document.head || document.documentElement).appendChild(script);
            })();
        """.trimIndent()
    }
}