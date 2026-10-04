package com.nova.browser.ui.browser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nova.browser.data.HistoryDao
import com.nova.browser.data.HistoryEntity
import com.nova.browser.data.TabDao
import com.nova.browser.data.TabEntity
import com.nova.browser.util.UrlUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
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

    // ── Lambdas (clean from Compose) ──────────────────────
    val newTab: (String) -> Unit = { url -> viewModelScope.launch { createTab(url) } }

    val closeTab: (String) -> Unit = { id ->
        viewModelScope.launch {
            tabDao.delete(id)
            val remaining = _state.value.tabs.filterNot { it.id == id }
            if (_state.value.activeTabId == id) {
                _state.update { it.copy(activeTabId = remaining.firstOrNull()?.id) }
            }
        }
    }

    val selectTab: (String) -> Unit = { id ->
        _state.update { s ->
            s.copy(
                activeTabId = id,
                urlInput = s.tabs.firstOrNull { it.id == id }?.url.orEmpty(),
                showTabSwitcher = false,
            )
        }
        viewModelScope.launch {
            _state.value.tabs.firstOrNull { it.id == id }?.let {
                tabDao.upsert(it.copy(lastActive = System.currentTimeMillis()))
            }
        }
    }

    val toggleTabSwitcher: () -> Unit = {
        _state.update { it.copy(showTabSwitcher = !it.showTabSwitcher) }
    }

    val onUrlInputChange: (String) -> Unit = { input ->
        _state.update { it.copy(urlInput = input) }
    }

    // ☕ Uses Java util for URL normalization
    val navigate: (String) -> Unit = { input ->
        val id = _state.value.activeTabId ?: return@navigate
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
            _state.update { it.copy(urlInput = normalized, isLoading = true, progress = 0) }
        }
    }

    val onPageStarted: () -> Unit = { _state.update { it.copy(isLoading = true, progress = 0) } }

    val onProgress: (Int) -> Unit = { p -> _state.update { it.copy(progress = p) } }

    val onPageFinished: (String, String, String?) -> Unit = { url, title, favicon ->
        viewModelScope.launch {
            val id = _state.value.activeTabId ?: return@launch
            tabDao.upsert(
                TabEntity(
                    id = id,
                    url = url,
                    title = title.ifBlank { UrlUtils.displayHost(url) },
                    faviconUrl = favicon,
                    lastActive = System.currentTimeMillis(),
                )
            )
            historyDao.insert(HistoryEntity(url = url, title = title))
            _state.update { it.copy(isLoading = false, progress = 100, urlInput = url) }
        }
    }

    private suspend fun createTab(url: String) {
        val id = UUID.randomUUID().toString()
        tabDao.upsert(TabEntity(id, url, "New Tab", null, System.currentTimeMillis()))
        _state.update { it.copy(activeTabId = id, urlInput = url) }
    }
}