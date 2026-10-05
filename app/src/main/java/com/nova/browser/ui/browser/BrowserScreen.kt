package com.nova.browser.ui.browser

import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun BrowserScreen(vm: BrowserViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val webViewRef = remember { mutableStateOf<WebView?>(null) }

    BackHandler(enabled = state.showTabSwitcher) { vm.toggleTabSwitcher() }
    BackHandler(
        enabled = !state.showTabSwitcher && webViewRef.value?.canGoBack() == true
    ) {
        webViewRef.value?.goBack()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            UrlBar(
                url = state.urlInput,
                loading = state.isLoading,
                progress = state.progress,
                onUrlChange = vm.onUrlInputChange,
                onNavigate = vm.navigate,
                onBack = {
                    if (webViewRef.value?.canGoBack() == true) {
                        webViewRef.value?.goBack()
                    }
                },
                onReload = { webViewRef.value?.reload() },
                onHome = { vm.navigate("https://duckduckgo.com") },
                onTabsClick = vm.toggleTabSwitcher,
                tabCount = state.tabs.size,
            )

            TabStrip(
                tabs = state.tabs,
                activeId = state.activeTabId,
                onSelect = vm.selectTab,
                onClose = vm.closeTab,
            )

            // WebView — no AnimatedContent, no key(), stable single instance
            WebViewContainer(
                initialUrl = state.activeTab?.url.orEmpty(),
                onPageStarted = vm.onPageStarted,
                onProgress = vm.onProgress,
                onPageFinished = vm.onPageFinished,
            )
        }
    }

    if (state.showTabSwitcher) {
        TabSwitcherSheet(
            tabs = state.tabs,
            activeId = state.activeTabId,
            onSelect = vm.selectTab,
            onClose = vm.closeTab,
            onNew = { vm.newTab("https://duckduckgo.com") },
            onDismiss = vm.toggleTabSwitcher,
        )
    }
}