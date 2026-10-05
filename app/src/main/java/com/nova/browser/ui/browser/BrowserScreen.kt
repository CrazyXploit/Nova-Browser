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
import com.nova.browser.ui.home.HomeDashboard

private const val HOME_PLACEHOLDER = "about:home"

@Composable
fun BrowserScreen(
    vm: BrowserViewModel = hiltViewModel(),
    onOpenBookmarks: () -> Unit = {},
    onOpenDownloads: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
) {
    val state by vm.state.collectAsStateWithLifecycle()

    BackHandler(enabled = state.longPressTarget != null) { vm.closeLongPress() }
    BackHandler(enabled = state.showInPageFind) { vm.closeFind() }
    BackHandler(enabled = state.showMediaPanel) { vm.closeMediaPanel() }
    BackHandler(enabled = state.showUrlPopup) { vm.closeUrlPopup() }
    BackHandler(enabled = state.showTabSwitcher) { vm.toggleTabSwitcher() }
    BackHandler(enabled = state.showOverlayMenu) { vm.closeOverlayMenu() }
    BackHandler(enabled = state.showSiteInfo) { vm.closeSiteInfo() }
    BackHandler(enabled = state.showIpOverlay) { vm.closeIpOverlay() }
    BackHandler(enabled = state.showUserAgentPicker) { vm.closeUserAgentPicker() }
    BackHandler(enabled = state.showImageQualityPicker) { vm.closeImageQualityPicker() }
    BackHandler(enabled = state.showSearchEnginePicker) { vm.closeSearchEnginePicker() }

    // System back → go to home when not already there
    BackHandler(enabled = !state.isHomeVisible) { vm.goBackOrHome() }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                UrlBar(
                    url = if (state.isHomeVisible) "" else state.activeTab?.url.orEmpty(),
                    loading = state.isLoading,
                    progress = state.progress,
                    isIncognito = state.isIncognito,
                    isBookmarked = state.isCurrentUrlBookmarked,
                    trackersBlocked = state.trackersBlocked,
                    onBarClick = vm.openUrlPopup,
                    onBookmarkClick = vm.toggleBookmark,
                    onRefreshClick = {
                        if (state.isLoading) vm.stopLoading() else vm.reload()
                    },
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
                    onBack = vm.goBackOrHome,
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
                if (state.isHomeVisible) {
                    HomeDashboard(
                        engineName = state.searchEngineName,
                        trackersBlocked = state.statsTrackers,
                        adsBlocked = state.statsAds,
                        timeSavedMs = state.statsTimeSavedMs,
                        timeSpentMs = state.statsTimeSpentMs,
                        dataSavedBytes = state.statsDataSavedBytes,
                        onSearch = vm.searchFromHome,
                    )
                } else {
                    WebViewContainer(
                        initialUrl = state.activeTab?.url.orEmpty(),
                        isIncognito = state.isIncognito,
                        userAgentMode = state.userAgentMode,
                        imageQuality = state.imageQuality,
                        dataSaver = state.dataSaver,
                        onPageStarted = vm.onPageStarted,
                        onProgress = vm.onProgress,
                        onPageFinished = vm.onPageFinished,
                        onDownloadStart = vm.onDownloadStart,
                        onWebViewCreated = { wv -> vm.attachWebView(wv) },
                    )
                }
            }
        }

        UrlPopup(
            visible = state.showUrlPopup,
            url = state.urlInput,
            maximized = state.urlMaximized,
            onUrlChange = vm.onUrlInputChange,
            onNavigate = vm.navigateFromPopup,
            onRefresh = vm.reload,
            onToggleMaximize = vm.toggleUrlMaximize,
            onDismiss = vm.closeUrlPopup,
        )

        MediaPanel(
            visible = state.showMediaPanel,
            items = state.mediaItems,
            onPlay = vm.playMedia,
            onDownload = vm.downloadMedia,
            onDismiss = vm.closeMediaPanel,
        )

        if (state.showSiteInfo) {
            SiteInfoOverlay(
                url = state.activeTab?.url.orEmpty(),
                trackersBlocked = state.trackersBlocked,
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

        if (state.showImageQualityPicker) {
            ImageQualityPicker(
                current = state.imageQuality,
                dataSaver = state.dataSaver,
                onPick = vm.setImageQuality,
                onToggleDataSaver = vm.toggleDataSaver,
                onDismiss = vm.closeImageQualityPicker,
            )
        }

        if (state.showSearchEnginePicker) {
            SearchEnginePicker(
                currentId = state.searchEngineId,
                onPick = vm.selectSearchEngine,
                onDismiss = vm.closeSearchEnginePicker,
            )
        }

        InPageFind(
            visible = state.showInPageFind,
            matchCount = state.findMatchCount,
            currentMatch = state.findCurrentMatch,
            onSearch = vm.findText,
            onNext = vm.findNext,
            onPrev = vm.findPrev,
            onDismiss = vm.closeFind,
        )

        val target = state.longPressTarget
        LongPressMenu(
            visible = target != null,
            title = when (target?.type) {
                "link" -> "Link"
                "image" -> "Image"
                "phone" -> "Phone number"
                "email" -> "Email"
                else -> "Options"
            },
            subtitle = target?.url,
            actions = when (target?.type) {
                "link" -> LongPressActions.forLink(
                    url = target.url,
                    onOpenNewTab = { vm.openInNewTab(target.url) },
                    onCopyLink = { vm.copyToClipboard(target.url) },
                    onShare = { vm.shareText(target.url) },
                    onDownload = { vm.downloadUrl(target.url) },
                )
                "image" -> LongPressActions.forImage(
                    url = target.url,
                    onOpenImage = { vm.openInNewTab(target.url) },
                    onCopyUrl = { vm.copyToClipboard(target.url) },
                    onDownload = { vm.downloadUrl(target.url) },
                )
                "phone" -> LongPressActions.forPhone(
                    number = target.url,
                    onCall = { vm.shareText(target.url) },
                    onCopy = { vm.copyToClipboard(target.url) },
                )
                "email" -> LongPressActions.forEmail(
                    email = target.url,
                    onSend = { vm.shareText(target.url) },
                    onCopy = { vm.copyToClipboard(target.url) },
                )
                else -> emptyList()
            },
            onDismiss = vm.closeLongPress,
        )
    }

    OverlayMenu(
        visible = state.showOverlayMenu,
        adBlockEnabled = state.adBlockEnabled,
        erudaEnabled = state.erudaEnabled,
        desktopMode = state.desktopMode,
        isIncognito = state.isIncognito,
        imageQualityLabel = state.effectiveQualityLabel,
        dataSaver = state.dataSaver,
        onToggleAdBlock = vm.toggleAdBlock,
        onToggleEruda = vm.toggleEruda,
        onToggleDesktop = vm.toggleDesktopMode,
        onToggleIncognito = vm.toggleIncognito,
        onUserAgent = { vm.closeOverlayMenu(); vm.openUserAgentPicker() },
        onMyIp = { vm.closeOverlayMenu(); vm.openIpOverlay() },
        onImageQuality = { vm.closeOverlayMenu(); vm.openImageQualityPicker() },
        onFind = { vm.closeOverlayMenu(); vm.openFind() },
        onSearchEngine = { vm.closeOverlayMenu(); vm.openSearchEnginePicker() },
        onMedia = { vm.closeOverlayMenu(); vm.openMediaPanel() },
        onHistory = { vm.closeOverlayMenu(); onOpenHistory() },
        onBookmarks = { vm.closeOverlayMenu(); onOpenBookmarks() },
        onDownloads = { vm.closeOverlayMenu(); onOpenDownloads() },
        onDismiss = vm.closeOverlayMenu,
    )

    if (state.showTabSwitcher) {
        TabSwitcherSheet(
            tabs = state.tabs,
            activeId = state.activeTabId,
            onSelect = vm.selectTab,
            onClose = vm.closeTab,
            onNew = { vm.newTab(HOME_PLACEHOLDER) },
            onDismiss = vm.toggleTabSwitcher,
        )
    }
}