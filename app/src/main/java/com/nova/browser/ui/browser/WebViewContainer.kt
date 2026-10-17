package com.nova.browser.ui.browser

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import android.view.MotionEvent
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
import com.nova.browser.data.NightModeInjector
import com.nova.browser.data.SpeedDialManager
import com.nova.browser.data.UserAgentManager
import kotlin.math.abs

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
    nightModeActive: Boolean,
    isSystemDark: Boolean,
    onPageStarted: () -> Unit,
    onProgress: (Int) -> Unit,
    onPageFinished: (String, String, String?) -> Unit,
    onDownloadStart: (String, String?, String?, String?, Long) -> Unit,
    onSwipeBack: () -> Unit,
    onSwipeForward: () -> Unit,
    onWebViewCreated: (WebView) -> Unit = {},
) {
    val holder = remember { WebViewHolder() }

    val currentOnPageStarted by rememberUpdatedState(onPageStarted)
    val currentOnProgress by rememberUpdatedState(onProgress)
    val currentOnPageFinished by rememberUpdatedState(onPageFinished)
    val currentOnDownloadStart by rememberUpdatedState(onDownloadStart)
    val currentOnSwipeBack by rememberUpdatedState(onSwipeBack)
    val currentOnSwipeForward by rememberUpdatedState(onSwipeForward)
    val currentOnWebViewCreated by rememberUpdatedState(onWebViewCreated)

    LaunchedEffect(userAgentMode) {
        val wv = holder.webView ?: return@LaunchedEffect
        UserAgentManager.applyTo(wv.settings)
    }

    LaunchedEffect(imageQuality, dataSaver) {
        val wv = holder.webView ?: return@LaunchedEffect
        ImageQualityManager.applyTo(wv.settings)
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
                // ── SPEED: hardware acceleration ────────
                setLayerType(WebView.LAYER_TYPE_HARDWARE, null)
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                overScrollMode = WebView.OVER_SCROLL_NEVER

                settings.apply {
                    // ── CORE ────────────────────────────
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    mediaPlaybackRequiresUserGesture = true

                    // ── SPEED: aggressive caching ───────
                    cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                    loadsImagesAutomatically = true

                    // ── SPEED: viewport tuning ──────────
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    setSupportZoom(false)
                    builtInZoomControls = false
                    displayZoomControls = false

                    // ── SPEED: no safe browsing scan ────
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        safeBrowsingEnabled = false
                    }

                    // ── SPEED: force dark (light pages become dark) ──
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        forceDark = WebSettings.FORCE_DARK_ON
                    }

                    // ── SPEED: mixed content (fewer blocks) ──
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                    // ── SPEED: reduce rendering cost ────
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        offscreenPreRaster = false
                    }

                    // ── SPEED: enable HTTP/2 + SPDY ─────
                    // (auto-enabled in modern WebView)

                    // ── SPEED: allow file access for local assets ──
                    allowFileAccess = false
                    allowContentAccess = false
                }

                UserAgentManager.captureDefault(settings)
                UserAgentManager.applyTo(settings)
                if (isIncognito) {
                    settings.userAgentString = "${settings.userAgentString} NovaIncognito/1.0"
                }

                setOnCreateContextMenuListener(null)

                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?,
                    ): WebResourceResponse? {
                        val uri = request?.url ?: return super.shouldInterceptRequest(view, request)
                        val url = uri.toString()

                        // Assets
                        val intercepted = assetLoader.shouldInterceptRequest(uri)
                        if (intercepted != null) return intercepted

                        // Ad block
                        if (AdBlocker.shouldBlock(url)) {
                            return AdBlocker.blockedResponse()
                        }

                        // Media sniff
                        if (request.method == "GET") {
                            val ct = guessContentType(url)
                            if (ct != null) MediaSniffer.sniff(url, ct, 0L)
                        }

                        // Image block
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
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        if (nightModeActive && view != null) {
                            try {
                                view.evaluateJavascript(NightModeInjector.buildCss(), null)
                            } catch (_: Exception) { }
                        }

                        if (ImageQualityManager.dataSaver && view != null) {
                            try {
                                view.evaluateJavascript(ImageQualityManager.buildLowQualityJs(), null)
                            } catch (_: Exception) { }
                        }

                        val faviconUrl = url?.let { pageUrl ->
                            try {
                                val uri = java.net.URI(pageUrl)
                                val host = uri.host ?: return@let null
                                "${uri.scheme ?: "https"}://$host/favicon.ico"
                            } catch (_: Exception) { null }
                        }

                        if (url != null && url != HOME_PLACEHOLDER) {
                            SpeedDialManager.recordVisit(url, view?.title ?: "", faviconUrl)
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
                        return true
                    }
                }

                setDownloadListener(
                    DownloadListener { url, ua, cd, mime, len ->
                        currentOnDownloadStart(url, ua, cd, mime, len)
                    }
                )

                // ── SWIPE GESTURES ──────────────────────
                var startX = 0f
                var startY = 0f
                var startTime = 0L
                var direction = 0

                setOnTouchListener { _, event ->
                    val screenWidth = resources.displayMetrics.widthPixels
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            startX = event.x
                            startY = event.y
                            startTime = System.currentTimeMillis()
                            val fromLeftEdge = startX < 80f
                            val fromRightEdge = startX > (screenWidth - 80f)
                            direction = when {
                                fromLeftEdge -> -1
                                fromRightEdge -> 1
                                else -> 0
                            }
                            false
                        }
                        MotionEvent.ACTION_UP -> {
                            if (direction != 0) {
                                val dx = event.x - startX
                                val dy = event.y - startY
                                val dt = System.currentTimeMillis() - startTime
                                val fastEnough = dt < 600L
                                val horizontalEnough = abs(dy) < 150f
                                if (direction == -1 && dx > 200f && horizontalEnough && fastEnough) {
                                    currentOnSwipeBack()
                                }
                                if (direction == 1 && dx < -200f && horizontalEnough && fastEnough) {
                                    currentOnSwipeForward()
                                }
                            }
                            direction = 0
                            false
                        }
                        MotionEvent.ACTION_CANCEL -> {
                            direction = 0
                            false
                        }
                        else -> false
                    }
                }

                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, !isIncognito)

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
        lower.endsWith(".m3u8") -> "application/vnd.apple.mpegurl"
        lower.endsWith(".mpd") -> "application/dash+xml"
        lower.endsWith(".mp3") -> "audio/mpeg"
        lower.endsWith(".m4a") -> "audio/mp4"
        lower.endsWith(".gif") -> "image/gif"
        lower.endsWith(".png") -> "image/png"
        lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> "image/jpeg"
        lower.endsWith(".webp") -> "image/webp"
        lower.endsWith(".pdf") -> "application/pdf"
        else -> null
    }
}

private fun parseMediaProbe(json: String) {
    try {
        val trimmed = json.trim()
        if (!trimmed.startsWith("{")) return
        val url = extractString(trimmed, "url") ?: return
        val durationMs = extractLong(trimmed, "durationMs") ?: 0L
        val width = extractInt(trimmed, "width") ?: 0
        val height = extractInt(trimmed, "height") ?: 0
        MediaSniffer.updateMetadata(url, durationMs, width, height)
    } catch (_: Exception) { }
}

private fun extractString(json: String, key: String): String? =
    "\"$key\"\\s*:\\s*\"([^\"]*)\"".toRegex().find(json)?.groupValues?.get(1)

private fun extractLong(json: String, key: String): Long? =
    "\"$key\"\\s*:\\s*(-?\\d+)".toRegex().find(json)?.groupValues?.get(1)?.toLongOrNull()

private fun extractInt(json: String, key: String): Int? =
    "\"$key\"\\s*:\\s*(-?\\d+)".toRegex().find(json)?.groupValues?.get(1)?.toIntOrNull()