package com.nova.browser.ui.browser

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.nova.browser.data.TabEntity

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewContainer(
    tab: TabEntity?,
    onPageStarted: () -> Unit,
    onProgress: (Int) -> Unit,
    onPageFinished: (String, String, String?) -> Unit,
    webViewRef: MutableState<WebView?>,
) {
    if (tab == null) return

    // Key by tab id → new WebView per tab (avoids reload flicker)
    key(tab.id) {
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
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
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
                    webChromeClient = object : android.webkit.WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            onProgress(newProgress)
                        }
                    }
                    webViewRef.value = this
                    loadUrl(tab.url)
                }
            },
            update = { wv ->
                if (wv.url != tab.url) wv.loadUrl(tab.url)
            },
        )
    }
}