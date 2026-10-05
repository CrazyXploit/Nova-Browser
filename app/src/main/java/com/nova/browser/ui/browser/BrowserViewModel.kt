package com.nova.browser.ui.browser

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nova.browser.data.AdBlocker
import com.nova.browser.data.BookmarkDao
import com.nova.browser.data.BookmarkEntity
import com.nova.browser.data.DownloadDao
import com.nova.browser.data.DownloadEntity
import com.nova.browser.data.DownloadManagerHelper
import com.nova.browser.data.ErudaManager
import com.nova.browser.data.HistoryDao
import com.nova.browser.data.HistoryEntity
import com.nova.browser.data.MyIpFetcher
import com.nova.browser.data.TabDao
import com.nova.browser.data.TabEntity
import com.nova.browser.data.UserAgentManager
import com.nova.browser.util.UrlUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

private const val HOME_URL = "https://www.google.com"

data class BrowserUiState(
    val tabs: List<TabEntity> = emptyList(),
    val activeTabId: String? = null,
    val urlInput: String = "",              // ← what the popup is editing
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val showTabSwitcher: Boolean = false,
    val showUrlPopup: Boolean = false,
    val urlMaximized: Boolean = false,
    val showOverlayMenu: Boolean = false,
    val showSiteInfo: Boolean = false,
    val showIpOverlay: Boolean = false,
    val showUserAgentPicker: Boolean = false,
    val isIncognito: Boolean = false,
    val adBlockEnabled: Boolean = true,
    val erudaEnabled: Boolean = true,
    val erudaReady: Boolean = false,
    val desktopMode: Boolean = false,
    val userAgentMode: UserAgentManager.Mode = UserAgentManager.Mode.MOBILE,
    val isCurrentUrlBookmarked: Boolean = false,
    val myIp: String = "",
    val myIpLoading: Boolean = false,
) {
    val activeTab: TabEntity? get() = tabs.firstOrNull { it.id == activeTabId }
}

@HiltViewModel
class BrowserViewModel @Inject constructor(
    application: Application,
    private val tabDao: TabDao,
    private val historyDao: HistoryDao,
    private val bookmarkDao: BookmarkDao,
    private val downloadDao: DownloadDao,
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(BrowserUiState())
    val state: StateFlow<BrowserUiState> = _state.asStateFlow()

    val history: StateFlow<List<HistoryEntity>> = historyDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarks: StateFlow<List<BookmarkEntity>> = bookmarkDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloads: StateFlow<List<DownloadEntity>> = downloadDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @Volatile private var webViewRef: android.webkit.WebView? = null

    fun attachWebView(wv: android.webkit.WebView) {
        webViewRef = wv
    }

    val goBack: () -> Unit = {
        webViewRef?.let { if (it.canGoBack()) it.goBack() }
    }
    val goForward: () -> Unit = {
        webViewRef?.let { if (it.canGoForward()) it.goForward() }
    }
    val reload: () -> Unit = {
        webViewRef?.reload()
    }
    val canGoBack: () -> Boolean = { webViewRef?.canGoBack() == true }
    val canGoForward: () -> Boolean = { webViewRef?.canGoForward() == true }

    init {
        AdBlocker.enabled = true
        ErudaManager.enabled = true

        val ctx = getApplication<Application>()
        UserAgentManager.load(ctx)
        _state.update {
            it.copy(
                userAgentMode = UserAgentManager.mode,
                desktopMode = UserAgentManager.mode == UserAgentManager.Mode.DESKTOP,
            )
        }

        viewModelScope.launch {
            val ok = ErudaManager.ensureDownloaded(ctx)
            _state.update { it.copy(erudaReady = ok) }
        }

        viewModelScope.launch {
            tabDao.observeAll().collect { tabs ->
                val cur = _state.value
                val active = cur.activeTabId
                    ?.takeIf { id -> tabs.any { it.id == id } }
                    ?: tabs.firstOrNull()?.id
                if (cur.tabs != tabs || cur.activeTabId != active) {
                    _state.update { it.copy(tabs = tabs, activeTabId = active) }
                }
            }
        }
        viewModelScope.launch {
            if (tabDao.observeAll().first().isEmpty()) {
                createTab(HOME_URL)
            }
        }
    }

    // ── Tabs ───────────────────────────────────────────────
    val newTab: (String) -> Unit = { url -> viewModelScope.launch { createTab(url) } }

    val closeTab: (String) -> Unit = { id ->
        viewModelScope.launch {
            tabDao.delete(id)
            val remaining = _state.value.tabs.filterNot { it.id == id }
            if (_state.value.activeTabId == id) {
                val next = remaining.firstOrNull()
                _state.update {
                    it.copy(activeTabId = next?.id, urlInput = next?.url.orEmpty())
                }
            }
        }
    }

    val selectTab: (String) -> Unit = { id ->
        val tab = _state.value.tabs.firstOrNull { it.id == id }
        _state.update {
            it.copy(
                activeTabId = id,
                urlInput = tab?.url.orEmpty(),
                showTabSwitcher = false,
            )
        }
        viewModelScope.launch {
            tab?.let { tabDao.upsert(it.copy(lastActive = System.currentTimeMillis())) }
        }
    }

    // ── URL popup ──────────────────────────────────────────
    val openUrlPopup: () -> Unit = {
        _state.update {
            it.copy(
                showUrlPopup = true,
                urlInput = it.activeTab?.url.orEmpty(),
                urlMaximized = false,
            )
        }
    }

    val closeUrlPopup: () -> Unit = {
        _state.update { it.copy(showUrlPopup = false, urlMaximized = false) }
    }

    val toggleUrlMaximize: () -> Unit = {
        _state.update { it.copy(urlMaximized = !it.urlMaximized) }
    }

    // Called on every keystroke by BasicTextField — state stays in VM
    val onUrlInputChange: (String) -> Unit = { input ->
        _state.update { it.copy(urlInput = input) }
    }

    val navigate: (String) -> Unit = { input ->
        val id = _state.value.activeTabId
        if (id != null && input.isNotBlank()) {
            viewModelScope.launch {
                val normalized = UrlUtils.normalize(input)
                tabDao.upsert(
                    TabEntity(
                        id = id,
                        url = normalized,
                        title = UrlUtils.displayHost(normalized),
                        faviconUrl = null,
                        lastActive = System.currentTimeMillis(),
                    )
                )
                _state.update {
                    it.copy(
                        urlInput = normalized,
                        isLoading = true,
                        progress = 0,
                        showUrlPopup = false,
                        urlMaximized = false,
                    )
                }
            }
        }
    }

    // Navigate using the current urlInput value (called by popup's Go button)
    val navigateFromPopup: () -> Unit = {
        navigate(_state.value.urlInput)
    }

    // ── Overlay toggles ────────────────────────────────────
    val toggleTabSwitcher: () -> Unit = {
        _state.update { it.copy(showTabSwitcher = !it.showTabSwitcher) }
    }
    val toggleOverlayMenu: () -> Unit = {
        _state.update { it.copy(showOverlayMenu = !it.showOverlayMenu) }
    }
    val closeOverlayMenu: () -> Unit = {
        _state.update { it.copy(showOverlayMenu = false) }
    }
    val toggleSiteInfo: () -> Unit = {
        _state.update { it.copy(showSiteInfo = !it.showSiteInfo) }
    }
    val closeSiteInfo: () -> Unit = {
        _state.update { it.copy(showSiteInfo = false) }
    }
    val openIpOverlay: () -> Unit = {
        _state.update { it.copy(showIpOverlay = true, myIpLoading = true, myIp = "") }
        fetchMyIp()
    }
    val closeIpOverlay: () -> Unit = {
        _state.update { it.copy(showIpOverlay = false) }
    }
    val openUserAgentPicker: () -> Unit = {
        _state.update { it.copy(showUserAgentPicker = true) }
    }
    val closeUserAgentPicker: () -> Unit = {
        _state.update { it.copy(showUserAgentPicker = false) }
    }

    val toggleAdBlock: () -> Unit = {
        AdBlocker.enabled = !AdBlocker.enabled
        _state.update { it.copy(adBlockEnabled = AdBlocker.enabled) }
    }
    val toggleEruda: () -> Unit = {
        ErudaManager.enabled = !ErudaManager.enabled
        _state.update { it.copy(erudaEnabled = ErudaManager.enabled) }
    }
    val toggleIncognito: () -> Unit = {
        viewModelScope.launch {
            val incognito = !_state.value.isIncognito
            _state.update { it.copy(isIncognito = incognito) }
            createTab(HOME_URL)
        }
    }

    val toggleDesktopMode: () -> Unit = {
        val newDesktop = !_state.value.desktopMode
        val newMode = if (newDesktop) UserAgentManager.Mode.DESKTOP
        else UserAgentManager.Mode.MOBILE
        UserAgentManager.setMode(newMode)
        UserAgentManager.save(getApplication())
        _state.update {
            it.copy(
                desktopMode = newDesktop,
                userAgentMode = newMode,
                showOverlayMenu = false,
            )
        }
        reload()
    }

    val setUserAgent: (String) -> Unit = { ua ->
        val newMode = when (ua) {
            "mobile" -> UserAgentManager.Mode.MOBILE
            "desktop" -> UserAgentManager.Mode.DESKTOP
            else -> UserAgentManager.Mode.CUSTOM
        }
        UserAgentManager.setMode(newMode, if (newMode == UserAgentManager.Mode.CUSTOM) ua else null)
        UserAgentManager.save(getApplication())
        _state.update {
            it.copy(
                showUserAgentPicker = false,
                userAgentMode = newMode,
                desktopMode = newMode == UserAgentManager.Mode.DESKTOP,
            )
        }
        reload()
    }

    val goHome: () -> Unit = { navigate(HOME_URL) }

    val redownloadEruda: () -> Unit = {
        viewModelScope.launch {
            val ok = ErudaManager.forceRedownload(getApplication())
            _state.update { it.copy(erudaReady = ok) }
        }
    }

    // ── Page events ────────────────────────────────────────
    val onPageStarted: () -> Unit = {
        if (!_state.value.isLoading) _state.update { it.copy(isLoading = true) }
    }

    val onProgress: (Int) -> Unit = { p ->
        val c = _state.value.progress
        if (c / 10 != p / 10 || p == 100) {
            _state.update { it.copy(progress = p) }
        }
    }

    val onPageFinished: (String, String, String?) -> Unit = { url, title, favicon ->
        viewModelScope.launch {
            val id = _state.value.activeTabId ?: return@launch
            val existing = _state.value.tabs.firstOrNull { it.id == id }
            val newFavicon = favicon ?: existing?.faviconUrl

            if (existing?.url != url ||
                existing.title != title ||
                existing.faviconUrl != newFavicon
            ) {
                tabDao.upsert(
                    TabEntity(
                        id = id,
                        url = url,
                        title = title.ifBlank { UrlUtils.displayHost(url) },
                        faviconUrl = newFavicon,
                        lastActive = existing?.lastActive ?: System.currentTimeMillis(),
                        isIncognito = _state.value.isIncognito,
                    )
                )
                if (!_state.value.isIncognito) {
                    historyDao.insert(HistoryEntity(url = url, title = title))
                }
            }

            val bm = bookmarkDao.findByUrl(url)
            val s = _state.value
            if (s.isLoading || s.progress != 100 ||
                s.isCurrentUrlBookmarked != (bm != null)
            ) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        progress = 100,
                        isCurrentUrlBookmarked = bm != null,
                        // Only sync urlInput if popup is NOT open
                        urlInput = if (s.showUrlPopup) s.urlInput else url,
                    )
                }
            }
        }
    }

    // ── Bookmarks ──────────────────────────────────────────
    val toggleBookmark: () -> Unit = {
        viewModelScope.launch {
            val tab = _state.value.activeTab ?: return@launch
            val existing = bookmarkDao.findByUrl(tab.url)
            if (existing != null) {
                bookmarkDao.delete(existing.id)
                _state.update { it.copy(isCurrentUrlBookmarked = false) }
            } else {
                bookmarkDao.insert(
                    BookmarkEntity(
                        url = tab.url,
                        title = tab.title.ifBlank { UrlUtils.displayHost(tab.url) },
                    )
                )
                _state.update { it.copy(isCurrentUrlBookmarked = true) }
            }
        }
    }

    val removeBookmark: (Long) -> Unit = { id ->
        viewModelScope.launch { bookmarkDao.delete(id) }
    }

    val clearHistory: () -> Unit = {
        viewModelScope.launch { historyDao.clear() }
    }

    // ── Downloads ──────────────────────────────────────────
    val onDownloadStart: (String, String?, String?, String?, Long) -> Unit =
        { url, ua, cd, mime, len ->
            viewModelScope.launch {
                val ctx = getApplication<Application>()
                val fileName = android.webkit.URLUtil.guessFileName(url, cd, mime)
                val dmId = DownloadManagerHelper.enqueue(ctx, url, ua, cd, mime)
                downloadDao.insert(
                    DownloadEntity(
                        id = if (dmId > 0) dmId else System.currentTimeMillis(),
                        url = url,
                        fileName = fileName,
                        mimeType = mime,
                        contentLength = len,
                        status = if (dmId > 0) "DOWNLOADING" else "FAILED",
                    )
                )
            }
        }

    val removeDownload: (Long) -> Unit = { id ->
        viewModelScope.launch { downloadDao.delete(id) }
    }

    val openDownload: (Long) -> Unit = { id ->
        viewModelScope.launch {
            val dl = downloads.value.firstOrNull { it.id == id }
            if (dl != null) {
                DownloadManagerHelper.openFile(getApplication(), dl.fileName)
            }
        }
    }

    private fun fetchMyIp() {
        viewModelScope.launch {
            val ip = MyIpFetcher.fetch()
            _state.update { it.copy(myIp = ip, myIpLoading = false) }
        }
    }

    private suspend fun createTab(url: String) {
        val id = UUID.randomUUID().toString()
        tabDao.upsert(
            TabEntity(
                id = id, url = url, title = "New Tab",
                faviconUrl = null, lastActive = System.currentTimeMillis(),
                isIncognito = _state.value.isIncognito,
            )
        )
        _state.update {
            it.copy(activeTabId = id, urlInput = url, showUrlPopup = false)
        }
    }
}