package com.nova.browser.ui.browser

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nova.browser.data.AdBlocker
import com.nova.browser.data.BookmarkDao
import com.nova.browser.data.BookmarkEntity
import com.nova.browser.data.DownloadDao
import com.nova.browser.data.DownloadEntity
import com.nova.browser.data.HistoryDao
import com.nova.browser.data.HistoryEntity
import com.nova.browser.data.TabDao
import com.nova.browser.data.TabEntity
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

data class BrowserUiState(
    val tabs: List<TabEntity> = emptyList(),
    val activeTabId: String? = null,
    val urlInput: String = "",
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val showTabSwitcher: Boolean = false,
    val showSearchOverlay: Boolean = false,
    val showMoreTools: Boolean = false,
    val isIncognito: Boolean = false,
    val adBlockEnabled: Boolean = true,
    val isCurrentUrlBookmarked: Boolean = false,
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

    // Lazy — only collects when a screen subscribes
    val history: StateFlow<List<HistoryEntity>> = historyDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarks: StateFlow<List<BookmarkEntity>> = bookmarkDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloads: StateFlow<List<DownloadEntity>> = downloadDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        AdBlocker.enabled = true
        viewModelScope.launch {
            tabDao.observeAll().collect { tabs ->
                val current = _state.value
                val active = current.activeTabId
                    ?.takeIf { id -> tabs.any { it.id == id } }
                    ?: tabs.firstOrNull()?.id
                if (current.tabs != tabs || current.activeTabId != active) {
                    _state.update { it.copy(tabs = tabs, activeTabId = active) }
                }
            }
        }
        viewModelScope.launch {
            if (tabDao.observeAll().first().isEmpty()) {
                createTab("https://duckduckgo.com")
            }
        }
    }

    // ── Tab actions ────────────────────────────────────────
    val newTab: (String) -> Unit = { url ->
        viewModelScope.launch { createTab(url) }
    }

    val closeTab: (String) -> Unit = { id ->
        viewModelScope.launch {
            tabDao.delete(id)
            val remaining = _state.value.tabs.filterNot { it.id == id }
            if (_state.value.activeTabId == id) {
                val next = remaining.firstOrNull()
                _state.update {
                    it.copy(
                        activeTabId = next?.id,
                        urlInput = next?.url.orEmpty(),
                    )
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

    // ── UI toggles ─────────────────────────────────────────
    val toggleTabSwitcher: () -> Unit = {
        _state.update { it.copy(showTabSwitcher = !it.showTabSwitcher) }
    }

    val openSearchOverlay: () -> Unit = {
        _state.update {
            it.copy(showSearchOverlay = true, urlInput = "")
        }
    }

    val closeSearchOverlay: () -> Unit = {
        _state.update { it.copy(showSearchOverlay = false) }
    }

    val toggleMoreTools: () -> Unit = {
        _state.update { it.copy(showMoreTools = !it.showMoreTools) }
    }

    val toggleAdBlock: () -> Unit = {
        AdBlocker.enabled = !AdBlocker.enabled
        _state.update { it.copy(adBlockEnabled = AdBlocker.enabled) }
    }

    val toggleIncognito: () -> Unit = {
        viewModelScope.launch {
            val incognito = !_state.value.isIncognito
            _state.update { it.copy(isIncognito = incognito) }
            createTab("https://duckduckgo.com")
        }
    }

    // ── URL input ──────────────────────────────────────────
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
                        showSearchOverlay = false,
                    )
                }
            }
        }
    }

    // ── Page events ────────────────────────────────────────
    val onPageStarted: () -> Unit = {
        if (!_state.value.isLoading) {
            _state.update { it.copy(isLoading = true) }
        }
    }

    val onProgress: (Int) -> Unit = { p ->
        val current = _state.value.progress
        val currentBucket = current / 10
        val newBucket = p / 10
        if (currentBucket != newBucket || p == 100) {
            _state.update { it.copy(progress = p) }
        }
    }

    val onPageFinished: (String, String, String?) -> Unit = { url, title, favicon ->
        viewModelScope.launch {
            val id = _state.value.activeTabId
            if (id != null) {
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
                val state = _state.value
                if (state.isLoading || state.progress != 100 ||
                    state.isCurrentUrlBookmarked != (bm != null)) {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            progress = 100,
                            isCurrentUrlBookmarked = bm != null,
                        )
                    }
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

    // ── History ────────────────────────────────────────────
    val clearHistory: () -> Unit = {
        viewModelScope.launch { historyDao.clear() }
    }

    // ── Downloads ──────────────────────────────────────────
    val onDownloadStart: (String, String?, String?, String?, Long) -> Unit =
        { url, userAgent, contentDisposition, mimeType, contentLength ->
            viewModelScope.launch {
                val fileName = android.webkit.URLUtil.guessFileName(
                    url, contentDisposition, mimeType
                )
                downloadDao.insert(
                    DownloadEntity(
                        url = url,
                        fileName = fileName,
                        mimeType = mimeType,
                        contentLength = contentLength,
                        status = "QUEUED",
                    )
                )
                val ctx = getApplication<Application>()
                com.nova.browser.data.DownloadManagerHelper.enqueue(
                    context = ctx,
                    url = url,
                    userAgent = userAgent,
                    contentDisposition = contentDisposition,
                    mimeType = mimeType,
                )
            }
        }

    val removeDownload: (Long) -> Unit = { id ->
        viewModelScope.launch { downloadDao.delete(id) }
    }

    private suspend fun createTab(url: String) {
        val id = UUID.randomUUID().toString()
        tabDao.upsert(
            TabEntity(
                id = id,
                url = url,
                title = "New Tab",
                faviconUrl = null,
                lastActive = System.currentTimeMillis(),
                isIncognito = _state.value.isIncognito,
            )
        )
        _state.update {
            it.copy(
                activeTabId = id,
                urlInput = url,
                showSearchOverlay = false,
            )
        }
    }
}