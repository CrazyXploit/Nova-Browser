package com.nova.browser.ui.browser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nova.browser.data.HistoryDao
import com.nova.browser.data.HistoryEntity
import com.nova.browser.data.TabDao
import com.nova.browser.data.TabEntity
import com.nova.browser.util.UrlUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class BrowserUiState(
    val tabs: List<TabEntity> = emptyList(),
    val activeTabId: String? = null,
    val urlInput: String = "",
    val currentUrl: String = "",
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val showTabSwitcher: Boolean = false,
) {
    val activeTab: TabEntity? get() = tabs.firstOrNull { it.id == activeTabId }
}

@HiltViewModel
class BrowserViewModel @Inject constructor(
    private val tabDao: TabDao,
    private val historyDao: HistoryDao,
) : ViewModel() {

    private val _state = MutableStateFlow(BrowserUiState())
    val state: StateFlow<BrowserUiState> = _state.asStateFlow()

    init {
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

    val newTab: (String) -> Unit = { url -> viewModelScope.launch { createTab(url) } }

    val closeTab: (String) -> Unit = { id ->
        viewModelScope.launch {
            tabDao.delete(id)
            val remaining = _state.value.tabs.filterNot { it.id == id }
            if (_state.value.activeTabId == id) {
                val next = remaining.firstOrNull()
                _state.update {
                    it.copy(
                        activeTabId = next?.id,
                        currentUrl = next?.url ?: "",
                        urlInput = next?.url ?: "",
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
                currentUrl = tab?.url.orEmpty(),
                showTabSwitcher = false,
            )
        }
        viewModelScope.launch {
            tab?.let { tabDao.upsert(it.copy(lastActive = System.currentTimeMillis())) }
        }
    }

    val toggleTabSwitcher: () -> Unit = {
        _state.update { it.copy(showTabSwitcher = !it.showTabSwitcher) }
    }

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
                        currentUrl = normalized,
                        isLoading = true,
                        progress = 0,
                    )
                }
            }
        }
    }

    val onPageStarted: () -> Unit = {
        _state.update { it.copy(isLoading = true) }
    }

    val onProgress: (Int) -> Unit = { p ->
        _state.update { it.copy(progress = p) }
    }

    // IMPORTANT: no longer update urlInput here → stops reload loop
    val onPageFinished: (String, String, String?) -> Unit = { url, title, favicon ->
        viewModelScope.launch {
            val id = _state.value.activeTabId
            if (id != null) {
                val existing = _state.value.tabs.firstOrNull { it.id == id }
                // Only update if URL or title actually changed
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
                    historyDao.insert(HistoryEntity(url = url, title = title))
                }
                _state.update {
                    it.copy(
                        isLoading = false,
                        progress = 100,
                        currentUrl = url,
                    )
                }
            }
        }
    }

    private suspend fun createTab(url: String) {
        val id = UUID.randomUUID().toString()
        tabDao.upsert(TabEntity(id, url, "New Tab", null, System.currentTimeMillis()))
        _state.update {
            it.copy(
                activeTabId = id,
                urlInput = url,
                currentUrl = url,
            )
        }
    }
}