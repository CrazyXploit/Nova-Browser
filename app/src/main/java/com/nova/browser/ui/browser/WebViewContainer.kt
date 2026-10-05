package com.nova.browser.ui.browser

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import android.view.ContextMenu
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
import com.nova.browser.data.ErudaManager
import com.nova.browser.data.HttpsEnforcer
import com.nova.browser.data.ImageQualityManager
import com.nova.browser.data.UserAgentManager

private const val TAG = "NovaWebView"

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

    // Apply UA + image quality in place when they change
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
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        safeBrowsingEnabled = false
                    }
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

                // Disable native context menu — we handle long-press ourselves
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

                        // Block images if quality = OFF
                        if (ImageQualityManager.shouldBlockImages()) {
                            val accept = request.requestHeaders["Accept"]
                            if (accept?.contains("image/") == true) {
                                return AdBlocker.blockedResponse()
                            }
                        }

                        return super.shouldInterceptRequest(view, request)
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        currentOnPageStarted()
                        if (ErudaManager.enabled) injectEruda(view)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        if (ErudaManager.enabled) injectEruda(view)
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

                // HTTPS enforcement
                val target = if (HttpsEnforcer.enforceHttps)
                    HttpsEnforcer.upgrade(initialUrl) else initialUrl
                loadUrl(target)
            }
        },
        update = { wv ->
            val lastUrl = wv.tag as? String
            if (initialUrl.isNotBlank() &&
                lastUrl != initialUrl &&
                !initialUrl.startsWith("about:") &&
                !initialUrl.startsWith("data:")
            ) {
                wv.tag = initialUrl
                val target = if (HttpsEnforcer.enforceHttps)
                    HttpsEnforcer.upgrade(initialUrl) else initialUrl
                wv.loadUrl(target)
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

private fun injectEruda(view: WebView?) {
    if (view == null) return
    view.evaluateJavascript(ErudaManager.buildInitScript(), null)
}