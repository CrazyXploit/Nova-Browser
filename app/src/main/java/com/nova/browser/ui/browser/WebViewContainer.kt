package com.nova.browser.ui.browser

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewContainer(
    initialUrl: String,
    onPageStarted: () -> Unit,
    onProgress: (Int) -> Unit,
    onPageFinished: (String, String, String?) -> Unit,
) {
    val webView = remember { mutableStateOfState<WebView?>(null) }

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
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(
                        view: WebView?,
                        url: String?,
                        favicon: Bitmap?,
                    ) {
                        onPageStarted()
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        onPageFinished(
                            url ?: "",
                            view?.title ?: "",
                            null,
                        )
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        onProgress(newProgress)
                    }
                }

                webView.value = this
                loadUrl(initialUrl)
            }
        },
        update = { wv ->
            // Only load if URL actually differs AND is non-empty
            val current = wv.url
            if (initialUrl.isNotBlank() &&
                current != initialUrl &&
                !initialUrl.startsWith("about:")
            ) {
                wv.loadUrl(initialUrl)
            }
        },
    )
}

// Small helper to keep a nullable WebView reference
private fun <T> mutableOfState(): MutableStateHolder<T> = MutableStateHolder()
private class MutableStateHolder<T> {
    var value: T? = null
}

// Use a simple holder to avoid Compose state import
private fun <T> mutableStateOfState(): MutableStateHolder<T> = MutableStateHolder()