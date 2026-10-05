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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.nova.browser.data.AdBlocker
import com.nova.browser.data.ErudaInjector

private const val TAG = "NovaWebView"

class WebViewHolder {
    var webView: WebView? = null
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewContainer(
    initialUrl: String,
    isIncognito: Boolean,
    onPageStarted: () -> Unit,
    onProgress: (Int) -> Unit,
    onPageFinished: (String, String, String?) -> Unit,
    onDownloadStart: (String, String?, String?, String?, Long) -> Unit,
) {
    val holder = remember { WebViewHolder() }

    val currentOnPageStarted by rememberUpdatedState(onPageStarted)
    val currentOnProgress by rememberUpdatedState(onProgress)
    val currentOnPageFinished by rememberUpdatedState(onPageFinished)
    val currentOnDownloadStart by rememberUpdatedState(onDownloadStart)

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            WebView(ctx).apply {
                setLayerType(WebView.LAYER_TYPE_HARDWARE, null)
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                overScrollMode = WebView.OVER_SCROLL_NEVER

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    loadsImagesAutomatically = true
                    setSupportZoom(false)
                    builtInZoomControls = false
                    displayZoomControls = false
                    mediaPlaybackRequiresUserGesture = true
                    useWideViewPort = true
                    loadWithOverviewMode = true

                    // === LOW-END PERF: cache everything ===
                    cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                    allowFileAccess = true                    // needed for file:///android_asset/
                    allowContentAccess = false
                    blockNetworkImage = false
                    blockNetworkLoads = false

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        safeBrowsingEnabled = false               // save RAM
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        forceDark = WebSettings.FORCE_DARK_AUTO
                    }

                    mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

                    // === LOW-END PERF: aggressive rendering ===
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        offscreenPreRaster = false
                    }
                }

                if (isIncognito) {
                    val baseUa = settings.userAgentString ?: ""
                    settings.userAgentString = "$baseUa NovaIncognito/1.0"
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?,
                    ): WebResourceResponse? {
                        val url = request?.url?.toString()
                        if (AdBlocker.shouldBlock(url)) {
                            return AdBlocker.blockedResponse()
                        }
                        return super.shouldInterceptRequest(view, request)
                    }

                    override fun onPageStarted(
                        view: WebView?,
                        url: String?,
                        favicon: Bitmap?,
                    ) {
                        currentOnPageStarted()
                        if (ErudaInjector.enabled && view != null) {
                            view.evaluateJavascript(
                                ErudaInjector.buildInitScript(),
                                null,
                            )
                        }
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        if (ErudaInjector.enabled && view != null) {
                            view.evaluateJavascript(
                                ErudaInjector.buildInitScript(),
                                null,
                            )
                        }

                        val faviconUrl = url?.let { pageUrl ->
                            try {
                                val uri = java.net.URI(pageUrl)
                                val host = uri.host ?: return@let null
                                "${uri.scheme ?: "https"}://$host/favicon.ico"
                            } catch (_: Exception) {
                                null
                            }
                        }
                        currentOnPageFinished(
                            url ?: "",
                            view?.title ?: "",
                            faviconUrl,
                        )
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        currentOnProgress(newProgress)
                    }

                    override fun onConsoleMessage(
                        consoleMessage: ConsoleMessage?,
                    ): Boolean {
                        Log.d(
                            TAG,
                            "[${consoleMessage?.messageLevel()}] ${consoleMessage?.message()}",
                        )
                        return true
                    }
                }

                setDownloadListener(
                    DownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
                        currentOnDownloadStart(
                            url,
                            userAgent,
                            contentDisposition,
                            mimeType,
                            contentLength,
                        )
                    }
                )

                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance()
                    .setAcceptThirdPartyCookies(this, !isIncognito)

                tag = initialUrl
                holder.webView = this
                loadUrl(initialUrl)
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
                wv.loadUrl(initialUrl)
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
        onDispose {
            holder.webView?.stopLoading()
        }
    }
}