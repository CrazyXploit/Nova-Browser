package com.nova.browser.ui.browser

import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest

@Composable
fun BrowserScreen(vm: BrowserViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val webViewRef = remember { mutableStateOf<WebView?>(null) }

    // Predictive back: close tab switcher first, then WebView history
    BackHandler(enabled = state.showTabSwitcher) { vm.toggleTabSwitcher() }
    BackHandler(enabled = !state.showTabSwitcher && webViewRef.value?.canGoBack() == true) {
        webViewRef.value?.goBack()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            UrlBar(
                url = state.urlInput,
                loading = state.isLoading,
                progress = state.progress,
                onUrlChange = vm.onUrlInputChange,
                onNavigate = vm.navigate,
                onBack = { if (webViewRef.value?.canGoBack() == true) webViewRef.value?.goBack() },
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

            // Web content — animated page transition
            AnimatedContent(
                targetState = state.activeTab,
                transitionSpec = {
                    (fadeIn(tween(220)) + scaleIn(initialScale = 0.98f, animationSpec = tween(220)))
                        .togetherWith(fadeOut(tween(180)))
                },
                label = "page",
                modifier = Modifier.fillMaxSize(),
            ) { tab ->
                WebViewContainer(
                    tab = tab,
                    onPageStarted = vm.onPageStarted,
                    onProgress = vm.onProgress,
                    onPageFinished = vm.onPageFinished,
                    webViewRef = webViewRef,
                )
            }
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