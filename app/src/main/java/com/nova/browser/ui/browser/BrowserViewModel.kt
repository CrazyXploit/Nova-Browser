package com.nova.browser.ui.browser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nova.browser.data.AdBlocker
import com.nova.browser.data.BookmarkDao
import com.nova.browser.data.BookmarkEntity
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
) {
    val activeTab: TabEntity? get() = tabs.firstOrNull { it.id == activeTabId }
}

@HiltViewModel
class BrowserViewModel @Inject constructor(
    private val tabDao: TabDao,
    private val historyDao: HistoryDao,
    private val bookmarkDao: BookmarkDao,
) : ViewModel() {

    private val _state = MutableStateFlow(BrowserUiState())
    val state: StateFlow<BrowserUiState> = _state.asStateFlow()

    val history: StateFlow<List<HistoryEntity>> = historyDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val bookmarks: StateFlow<List<BookmarkEntity>> = bookmarkDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        AdBlocker.enabled = true

        viewModelScope.launch {
            tabDao.observeAll().collect { tabs ->
                val current = _state.value
                val active = current.activeTabId
                    ?.takeIf { id -> tabs.any { it.id == id } }
                    ?: tabs.firstOrNull()?.id
                _state.update { it.copy(tabs = tabs, activeTabId = active) }
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
        _state.update { it.copy(showSearchOverlay = true, urlInput = "") }
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
        if (id != null) {
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
        _state.update { it.copy(isLoading = true) }
    }

    val onProgress: (Int) -> Unit = { p ->
        _state.update { it.copy(progress = p) }
    }

    val onPageFinished: (String, String, String?) -> Unit = { url, title, favicon ->
        viewModelScope.launch {
            val id = _state.value.activeTabId
            if (id != null) {
                val existing = _state.value.tabs.firstOrNull { it.id == id }
                if (existing?.url != url || existing.title != title) {
                    tabDao.upsert(
                        TabEntity(
                            id = id,
                            url = url,
                            title = title.ifBlank { UrlUtils.displayHost(url) },
                            faviconUrl = favicon,
                            lastActive = existing?.lastActive ?: System.currentTimeMillis(),
                        )
                    )
                    if (!_state.value.isIncognito) {
                        historyDao.insert(HistoryEntity(url = url, title = title))
                    }
                }
                _state.update {
                    it.copy(isLoading = false, progress = 100)
                }
            }
        }
    }

    // ── Bookmarks ──────────────────────────────────────────
    val addBookmark: () -> Unit = {
        viewModelScope.launch {
            val tab = _state.value.activeTab ?: return@launch
            bookmarkDao.insert(
                BookmarkEntity(
                    url = tab.url,
                    title = tab.title.ifBlank { UrlUtils.displayHost(tab.url) },
                )
            )
        }
    }

    val removeBookmark: (Long) -> Unit = { id ->
        viewModelScope.launch { bookmarkDao.delete(id) }
    }

    // ── History ────────────────────────────────────────────
    val clearHistory: () -> Unit = {
        viewModelScope.launch { historyDao.clear() }
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
        _state.update { it.copy(activeTabId = id, urlInput = url) }
    }
}