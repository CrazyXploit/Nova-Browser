package com.nova.browser.ui.browser

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import com.nova.browser.data.AdBlocker
import com.nova.browser.data.DataSaver
import com.nova.browser.data.ErudaManager
import com.nova.browser.data.ImageQualityManager
import com.nova.browser.data.MediaSniffer
import com.nova.browser.data.UserAgentManager

private const val TAG = "NovaWebView"
private const val HOME_PLACEHOLDER = "about:home"

class WebViewHolder {
    var webView: WebView? = null
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewContainer(
    initialUrl: String,
    isIncognito: Boolean,
    userAgentMode: UserAgentManager.Mode,
    imageQuality: ImageQualityManager.Quality,
    dataSaver: Boolean,
    onPageStarted: () -> Unit,
    onProgress: (Int) -> Unit,
    onPageFinished: (String, String, String?) -> Unit,
    onDownloadStart: (String, String?, String?, String?, Long) -> Unit,
    onWebViewCreated: (WebView) -> Unit = {},
) {
    val holder = remember { WebViewHolder() }

    val currentOnPageStarted by rememberUpdatedState(onPageStarted)
    val currentOnProgress by rememberUpdatedState(onProgress)
    val currentOnPageFinished by rememberUpdatedState(onPageFinished)
    val currentOnDownloadStart by rememberUpdatedState(onDownloadStart)
    val currentOnWebViewCreated by rememberUpdatedState(onWebViewCreated)

    LaunchedEffect(userAgentMode) {
        val wv = holder.webView ?: return@LaunchedEffect
        UserAgentManager.applyTo(wv.settings)
        if (isIncognito) {
            wv.settings.userAgentString = "${wv.settings.userAgentString} NovaIncognito/1.0"
        }
    }

    LaunchedEffect(imageQuality, dataSaver) {
        val wv = holder.webView ?: return@LaunchedEffect
        ImageQualityManager.applyTo(wv.settings)
        if (ImageQualityManager.dataSaver) {
            try {
                wv.evaluateJavascript(ImageQualityManager.buildLowQualityJs(), null)
            } catch (_: Exception) { }
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val assetLoader = WebViewAssetLoader.Builder()
                .addPathHandler(
                    "/files/",
                    WebViewAssetLoader.InternalStoragePathHandler(ctx, ctx.filesDir),
                )
                .addPathHandler(
                    "/assets/",
                    WebViewAssetLoader.AssetsPathHandler(ctx),
                )
                .build()

            WebView(ctx).apply {
                setLayerType(WebView.LAYER_TYPE_HARDWARE, null)
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                overScrollMode = WebView.OVER_SCROLL_NEVER

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    setSupportZoom(false)
                    builtInZoomControls = false
                    displayZoomControls = false
                    mediaPlaybackRequiresUserGesture = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                    allowFileAccess = false
                    allowContentAccess = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) safeBrowsingEnabled = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        forceDark = WebSettings.FORCE_DARK_AUTO
                    }
                    mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                }

                UserAgentManager.captureDefault(settings)
                UserAgentManager.applyTo(settings)
                if (isIncognito) {
                    settings.userAgentString = "${settings.userAgentString} NovaIncognito/1.0"
                }

                ImageQualityManager.applyTo(settings)
                setOnCreateContextMenuListener(null)

                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?,
                    ): WebResourceResponse? {
                        val uri = request?.url ?: return super.shouldInterceptRequest(view, request)

                        val intercepted = assetLoader.shouldInterceptRequest(uri)
                        if (intercepted != null) return intercepted

                        if (AdBlocker.shouldBlock(uri.toString())) {
                            return AdBlocker.blockedResponse()
                        }

                        // === MEDIA SNIFF with content-length + type ===
                        if (request.method == "GET") {
                            val headers = request.requestHeaders ?: emptyMap()
                            val ct = headers["Accept"]?.let { null } ?: null

                            // WebResourceRequest doesn't expose response headers — we sniff URL + hint CT
                            val hinted = guessContentType(uri.toString())
                            MediaSniffer.sniff(uri.toString(), hinted, 0L)
                        }

                        if (ImageQualityManager.shouldBlockImages()) {
                            val accept = request.requestHeaders["Accept"]
                            if (accept?.contains("image/") == true) {
                                DataSaver.onBlockedImage()
                                return AdBlocker.blockedResponse()
                            }
                        }

                        return super.shouldInterceptRequest(view, request)
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        currentOnPageStarted()
                        if (url != null && url != HOME_PLACEHOLDER) {
                            try {
                                val uri = java.net.URI(url)
                                MediaSniffer.setPageHost(uri.host ?: "")
                            } catch (_: Exception) { }
                        }
                        if (ErudaManager.enabled && url != HOME_PLACEHOLDER) injectEruda(view)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        if (ErudaManager.enabled && url != HOME_PLACEHOLDER) injectEruda(view)
                        if (ImageQualityManager.dataSaver && view != null) {
                            try {
                                view.evaluateJavascript(ImageQualityManager.buildLowQualityJs(), null)
                            } catch (_: Exception) { }
                        }

                        // Probe media metadata in page
                        if (view != null) injectMediaProbe(view)

                        val faviconUrl = url?.let { pageUrl ->
                            try {
                                val uri = java.net.URI(pageUrl)
                                val host = uri.host ?: return@let null
                                "${uri.scheme ?: "https"}://$host/favicon.ico"
                            } catch (_: Exception) { null }
                        }
                        currentOnPageFinished(url ?: "", view?.title ?: "", faviconUrl)
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        currentOnProgress(newProgress)
                    }
                    override fun onConsoleMessage(c: ConsoleMessage?): Boolean {
                        val msg = c?.message() ?: ""
                        if (msg.startsWith("NOVA_MEDIA:")) {
                            parseMediaProbe(msg.removePrefix("NOVA_MEDIA:"))
                            return true
                        }
                        if (msg.contains("Nova:") || msg.contains("Eruda")) {
                            Log.d(TAG, "[Nova] $msg")
                        }
                        return true
                    }
                }

                setDownloadListener(
                    DownloadListener { url, ua, cd, mime, len ->
                        currentOnDownloadStart(url, ua, cd, mime, len)
                    }
                )

                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance()
                    .setAcceptThirdPartyCookies(this, !isIncognito)

                tag = initialUrl
                holder.webView = this
                currentOnWebViewCreated(this)

                if (initialUrl == HOME_PLACEHOLDER || initialUrl.isBlank()) {
                    loadUrl("about:blank")
                } else {
                    loadUrl(initialUrl, ImageQualityManager.extraHeaders())
                }
            }
        },
        update = { wv ->
            val lastUrl = wv.tag as? String
            if (initialUrl.isNotBlank() &&
                lastUrl != initialUrl &&
                !initialUrl.startsWith("data:")
            ) {
                wv.tag = initialUrl
                if (initialUrl == HOME_PLACEHOLDER) {
                    wv.loadUrl("about:blank")
                } else if (!initialUrl.startsWith("about:")) {
                    wv.loadUrl(initialUrl, ImageQualityManager.extraHeaders())
                }
            }
        },
        onRelease = { wv ->
            wv.stopLoading()
            wv.loadUrl("about:blank")
            wv.destroy()
            holder.webView = null
        },
    )

    DisposableEffect(Unit) {
        onDispose { holder.webView?.stopLoading() }
    }
}

private fun guessContentType(url: String): String? {
    val lower = url.lowercase()
    return when {
        lower.endsWith(".mp4") -> "video/mp4"
        lower.endsWith(".webm") -> "video/webm"
        lower.endsWith(".mkv") -> "video/x-matroska"
        lower.endsWith(".mov") -> "video/quicktime"
        lower.endsWith(".m3u8") -> "application/vnd.apple.mpegurl"
        lower.endsWith(".mpd") -> "application/dash+xml"
        lower.endsWith(".mp3") -> "audio/mpeg"
        lower.endsWith(".m4a") -> "audio/mp4"
        lower.endsWith(".ogg") -> "audio/ogg"
        lower.endsWith(".wav") -> "audio/wav"
        lower.endsWith(".flac") -> "audio/flac"
        lower.endsWith(".gif") -> "image/gif"
        lower.endsWith(".png") -> "image/png"
        lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> "image/jpeg"
        lower.endsWith(".webp") -> "image/webp"
        lower.endsWith(".svg") -> "image/svg+xml"
        lower.endsWith(".avif") -> "image/avif"
        lower.endsWith(".pdf") -> "application/pdf"
        else -> null
    }
}

private fun injectEruda(view: WebView?) {
    if (view == null) return
    try {
        view.evaluateJavascript(ErudaManager.buildInitScript(), null)
    } catch (_: Exception) { }
}

/**
 * Probes DOM for <video>, <audio>, <img> sizes/durations + Content-Length via fetch HEAD.
 * Emits NOVA_MEDIA:<json> messages we parse in onConsoleMessage.
 */
private fun injectMediaProbe(view: WebView) {
    val js = """
        (function() {
            if (window.__novaMediaProbe) return;
            window.__novaMediaProbe = true;

            function fmt(ms) { return Math.floor(ms); }

            function probeVideo(v) {
                try {
                    var src = v.currentSrc || v.src;
                    if (!src) return;
                    var duration = isFinite(v.duration) ? fmt(v.duration * 1000) : 0;
                    var w = v.videoWidth || 0;
                    var h = v.videoHeight || 0;
                    console.log('NOVA_MEDIA:' + JSON.stringify({
                        url: src, durationMs: duration, width: w, height: h
                    }));
                } catch(e) {}
            }

            function probeAudio(a) {
                try {
                    var src = a.currentSrc || a.src;
                    if (!src) return;
                    var duration = isFinite(a.duration) ? fmt(a.duration * 1000) : 0;
                    console.log('NOVA_MEDIA:' + JSON.stringify({
                        url: src, durationMs: duration, width: 0, height: 0
                    }));
                } catch(e) {}
            }

            function probeImage(img) {
                try {
                    var src = img.currentSrc || img.src;
                    if (!src) return;
                    var w = img.naturalWidth || 0;
                    var h = img.naturalHeight || 0;
                    if (w === 0) return;
                    console.log('NOVA_MEDIA:' + JSON.stringify({
                        url: src, durationMs: 0, width: w, height: h
                    }));
                } catch(e) {}
            }

            function probeAll() {
                try {
                    document.querySelectorAll('video').forEach(probeVideo);
                    document.querySelectorAll('audio').forEach(probeAudio);
                    document.querySelectorAll('img').forEach(probeImage);
                } catch(e) {}
            }

            // Initial scan
            probeAll();

            // Watch for dynamic media
            try {
                var obs = new MutationObserver(function() {
                    probeAll();
                });
                obs.observe(document.documentElement, { childList: true, subtree: true });
            } catch(e) {}
        })();
    """.trimIndent()

    try {
        view.evaluateJavascript(js, null)
    } catch (_: Exception) { }
}

private fun parseMediaProbe(json: String) {
    try {
        val trimmed = json.trim()
        if (!trimmed.startsWith("{")) return

        val url = extractString(trimmed, "url") ?: return
        val durationMs = extractLong(trimmed, "durationMs") ?: 0L
        val width = extractInt(trimmed, "width") ?: 0
        val height = extractInt(trimmed, "height") ?: 0

        MediaSniffer.updateMetadata(
            url = url,
            durationMs = durationMs,
            widthPx = width,
            heightPx = height,
        )
    } catch (_: Exception) { }
}

// Tiny JSON extractors — avoid pulling in a parser just for this
private fun extractString(json: String, key: String): String? {
    val pattern = "\"$key\"\\s*:\\s*\"([^\"]*)\"".toRegex()
    return pattern.find(json)?.groupValues?.get(1)
}

private fun extractLong(json: String, key: String): Long? {
    val pattern = "\"$key\"\\s*:\\s*(-?\\d+)".toRegex()
    return pattern.find(json)?.groupValues?.get(1)?.toLongOrNull()
}

private fun extractInt(json: String, key: String): Int? {
    val pattern = "\"$key\"\\s*:\\s*(-?\\d+)".toRegex()
    return pattern.find(json)?.groupValues?.get(1)?.toIntOrNull()
}