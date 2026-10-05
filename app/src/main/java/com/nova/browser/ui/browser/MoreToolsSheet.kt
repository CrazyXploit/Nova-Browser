package com.nova.browser.ui.browser

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreToolsSheet(
    adBlockEnabled: Boolean,
    erudaEnabled: Boolean,
    erudaReady: Boolean,
    isIncognito: Boolean,
    onToggleAdBlock: () -> Unit,
    onToggleEruda: () -> Unit,
    onToggleIncognito: () -> Unit,
    onRedownloadEruda: () -> Unit,
    onBookmarks: () -> Unit,
    onDownloads: () -> Unit,
    onDismiss: () -> Unit,
) {
    // Deprecated — tools are now in the OverlayMenu
    onDismiss()
}