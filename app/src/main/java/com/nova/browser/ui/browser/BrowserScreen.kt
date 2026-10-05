package com.nova.browser.ui.browser

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

    BackHandler(enabled = state.showUrlPopup) { vm.closeUrlPopup() }
    BackHandler(enabled = state.showTabSwitcher) { vm.toggleTabSwitcher() }
    BackHandler(enabled = state.showOverlayMenu) { vm.closeOverlayMenu() }
    BackHandler(enabled = state.showSiteInfo) { vm.closeSiteInfo() }
    BackHandler(enabled = state.showIpOverlay) { vm.closeIpOverlay() }
    BackHandler(enabled = state.showUserAgentPicker) { vm.closeUserAgentPicker() }
    BackHandler(enabled = vm.canGoBack()) { vm.goBack() }

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
                    onBarClick = vm.openUrlPopup,
                    onBookmarkClick = vm.toggleBookmark,
                    onRefreshClick = vm.reload,
                    onShieldClick = vm.toggleSiteInfo,
                    onMoreClick = vm.toggleOverlayMenu,
                    onTabsClick = vm.toggleTabSwitcher,
                    tabCount = state.tabs.size,
                )
            },
            bottomBar = {
                BottomBar(
                    canGoBack = vm.canGoBack(),
                    canGoForward = vm.canGoForward(),
                    tabCount = state.tabs.size,
                    onBack = vm.goBack,
                    onForward = vm.goForward,
                    onHome = vm.goHome,
                    onTabs = vm.toggleTabSwitcher,
                    onMore = vm.toggleOverlayMenu,
                    onLongPressMore = vm.toggleOverlayMenu,
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
                    onWebViewCreated = { wv -> vm.attachWebView(wv) },
                )
            }
        }

        // URL popup — small popup above URL bar (top-aligned)
        UrlPopup(
            visible = state.showUrlPopup,
            url = state.urlInput,
            maximized = state.urlMaximized,
            onUrlChange = vm.onUrlInputChange,
            onNavigate = { vm.navigate(state.urlInput) },
            onRefresh = vm.reload,
            onToggleMaximize = vm.toggleUrlMaximize,
            onDismiss = vm.closeUrlPopup,
        )

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