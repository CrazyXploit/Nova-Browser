package com.nova.browser.ui.browser

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.nova.browser.data.AdBlocker

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

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            WebView(ctx).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    loadsImagesAutomatically = true
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false
                    mediaPlaybackRequiresUserGesture = false
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    if (isIncognito) {
                        userAgentString = "$userAgentString NovaIncognito/1.0"
                    }
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
                        onPageStarted()
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        // Try to grab the favicon URL from the page
                        val faviconUrl = url?.let { pageUrl ->
                            try {
                                val uri = java.net.URI(pageUrl)
                                "${uri.scheme}://${uri.host}/favicon.ico"
                            } catch (_: Exception) {
                                null
                            }
                        }
                        onPageFinished(
                            url ?: "",
                            view?.title ?: "",
                            faviconUrl,
                        )
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        onProgress(newProgress)
                    }

                    override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                        // Real favicon received
                        view?.url?.let { pageUrl ->
                            val uri = try { java.net.URI(pageUrl) } catch (_: Exception) { null }
                            if (uri != null) {
                                val faviconUrl = "${uri.scheme}://${uri.host}/favicon.ico"
                                onPageFinished(pageUrl, view.title ?: "", faviconUrl)
                            }
                        }
                    }
                }

                setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
                    onDownloadStart(url, userAgent, contentDisposition, mimeType, contentLength)
                })

                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, !isIncognito)

                holder.webView = this
                loadUrl(initialUrl)
            }
        },
        update = { wv ->
            val current = wv.url ?: ""
            if (initialUrl.isNotBlank() &&
                current != initialUrl &&
                !initialUrl.startsWith("about:") &&
                !initialUrl.startsWith("data:")
            ) {
                wv.loadUrl(initialUrl)
            }
        },
    )
}