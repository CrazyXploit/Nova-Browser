package com.nova.browser.ui.browser

import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private const val HOME_URL = "https://www.google.com"

@Composable
fun BrowserScreen(
    vm: BrowserViewModel = hiltViewModel(),
    onOpenBookmarks: () -> Unit = {},
    onOpenDownloads: () -> Unit = {},
) {
    val state by vm.state.collectAsStateWithLifecycle()

    val webViewRef = remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }

    LaunchedEffect(state.progress, state.activeTabId) {
        val wv = webViewRef.value
        canGoBack = wv?.canGoBack() == true
        canGoForward = wv?.canGoForward() == true
    }

    BackHandler(enabled = state.showTabSwitcher) { vm.toggleTabSwitcher() }
    BackHandler(enabled = state.showOverlayMenu) { vm.closeOverlayMenu() }
    BackHandler(enabled = state.showSiteInfo) { vm.closeSiteInfo() }
    BackHandler(enabled = state.showIpOverlay) { vm.closeIpOverlay() }
    BackHandler(
        enabled = !state.showTabSwitcher &&
            !state.showSearchOverlay &&
            webViewRef.value?.canGoBack() == true
    ) {
        webViewRef.value?.goBack()
        canGoBack = webViewRef.value?.canGoBack() == true
        canGoForward = webViewRef.value?.canGoForward() == true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                UrlBar(
                    url = state.activeTab?.url.orEmpty(),
                    loading = state.isLoading,
                    progress = state.progress,
                    isIncognito = state.isIncognito,
                    isBookmarked = state.isCurrentUrlBookmarked,
                    onBarClick = vm.openSearchOverlay,
                    onBookmarkClick = vm.toggleBookmark,
                    onShieldClick = vm.toggleSiteInfo,
                    onMoreClick = vm.toggleOverlayMenu,
                    onTabsClick = vm.toggleTabSwitcher,
                    tabCount = state.tabs.size,
                )
            },
            bottomBar = {
                BottomBar(
                    canGoBack = canGoBack,
                    canGoForward = canGoForward,
                    tabCount = state.tabs.size,
                    onBack = {
                        webViewRef.value?.goBack()
                        canGoBack = webViewRef.value?.canGoBack() == true
                        canGoForward = webViewRef.value?.canGoForward() == true
                    },
                    onForward = {
                        webViewRef.value?.goForward()
                        canGoBack = webViewRef.value?.canGoBack() == true
                        canGoForward = webViewRef.value?.canGoForward() == true
                    },
                    onHome = { vm.goHome() },
                    onTabs = vm.toggleTabSwitcher,
                    onMore = vm.toggleOverlayMenu,
                    onLongPressMore = { vm.toggleOverlayMenu() },
                )
            },
        ) { padding ->
            Column(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                WebViewContainer(
                    initialUrl = state.activeTab?.url.orEmpty(),
                    isIncognito = state.isIncognito,
                    userAgentMode = state.userAgentMode,
                    onPageStarted = vm.onPageStarted,
                    onProgress = vm.onProgress,
                    onPageFinished = vm.onPageFinished,
                    onDownloadStart = vm.onDownloadStart,
                )
            }
        }

        if (state.showSearchOverlay) {
            val history by vm.history.collectAsStateWithLifecycle()
            SearchOverlay(
                history = history,
                currentUrl = state.activeTab?.url.orEmpty(),
                onNavigate = vm.navigate,
                onDismiss = vm.closeSearchOverlay,
                onClearHistory = vm.clearHistory,
            )
        }

        if (state.showSiteInfo) {
            SiteInfoOverlay(
                url = state.activeTab?.url.orEmpty(),
                onDismiss = vm.closeSiteInfo,
            )
        }

        if (state.showIpOverlay) {
            MyIpOverlay(
                ip = state.myIp,
                loading = state.myIpLoading,
                onDismiss = vm.closeIpOverlay,
            )
        }

        if (state.showUserAgentPicker) {
            UserAgentPicker(
                current = state.userAgentMode,
                onPick = vm.setUserAgent,
                onDismiss = vm.closeUserAgentPicker,
            )
        }
    }

    OverlayMenu(
        visible = state.showOverlayMenu,
        adBlockEnabled = state.adBlockEnabled,
        erudaEnabled = state.erudaEnabled,
        desktopMode = state.desktopMode,
        isIncognito = state.isIncognito,
        onToggleAdBlock = vm.toggleAdBlock,
        onToggleEruda = vm.toggleEruda,
        onToggleDesktop = vm.toggleDesktopMode,
        onToggleIncognito = vm.toggleIncognito,
        onUserAgent = {
            vm.closeOverlayMenu()
            vm.openUserAgentPicker()
        },
        onMyIp = {
            vm.closeOverlayMenu()
            vm.openIpOverlay()
        },
        onDismiss = vm.closeOverlayMenu,
    )

    if (state.showTabSwitcher) {
        TabSwitcherSheet(
            tabs = state.tabs,
            activeId = state.activeTabId,
            onSelect = vm.selectTab,
            onClose = vm.closeTab,
            onNew = { vm.newTab(HOME_URL) },
            onDismiss = vm.toggleTabSwitcher,
        )
    }
}