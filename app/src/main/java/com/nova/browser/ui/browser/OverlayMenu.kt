package com.nova.browser.ui.browser

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OverlayMenu(
    visible: Boolean,
    adBlockEnabled: Boolean,
    erudaEnabled: Boolean,
    desktopMode: Boolean,
    isIncognito: Boolean,
    imageQualityLabel: String,
    dataSaver: Boolean,
    nightModeLabel: String,
    readerModeActive: Boolean,
    onToggleAdBlock: () -> Unit,
    onToggleEruda: () -> Unit,
    onToggleDesktop: () -> Unit,
    onToggleIncognito: () -> Unit,
    onCycleNightMode: () -> Unit,
    onToggleReader: () -> Unit,
    onUserAgent: () -> Unit,
    onMyIp: () -> Unit,
    onImageQuality: () -> Unit,
    onFind: () -> Unit,
    onSearchEngine: () -> Unit,
    onMedia: () -> Unit,
    onHistory: () -> Unit,
    onBookmarks: () -> Unit,
    onDownloads: () -> Unit,
    onDismiss: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(180)) + slideInVertically(initialOffsetY = { it }),
        exit = fadeOut(tween(150)) + slideOutVertically(targetOffsetY = { it }),
    ) {
        val navBarPadding = WindowInsets.navigationBars.asPaddingValues()
        val screenWidth = LocalConfiguration.current.screenWidthDp
        val menuWidth = (screenWidth - 32).coerceAtMost(400).dp

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                modifier = Modifier
                    .padding(
                        bottom = navBarPadding.calculateBottomPadding() + 16.dp,
                        start = 16.dp,
                        end = 16.dp,
                    )
                    .width(menuWidth)
                    .heightIn(max = 620.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { }
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 12.dp),
            ) {
                Text(
                    "Quick Tools",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )

                ToggleItem(Icons.Default.Block, "Ad Blocker", adBlockEnabled, onToggleAdBlock)
                ToggleItem(Icons.Default.Code, "Eruda DevTools", erudaEnabled, onToggleEruda)
                ToggleItem(Icons.Default.MenuBook, "Reader Mode", readerModeActive, onToggleReader)
                ToggleItem(Icons.Default.Devices, "Desktop Mode", desktopMode, onToggleDesktop)
                ToggleItem(Icons.Default.Visibility, "Incognito", isIncognito, onToggleIncognito)

                Spacer(Modifier.size(4.dp))

                ActionItem(
                    Icons.Default.DarkMode,
                    "Night Mode",
                    nightModeLabel,
                    onCycleNightMode,
                )
                ActionItem(
                    Icons.Default.Image,
                    "Image Quality",
                    "$imageQualityLabel${if (dataSaver) " · Data Saver" else ""}",
                    onImageQuality,
                )
                ActionItem(Icons.Default.Movie, "Media on page", null, onMedia)
                ActionItem(Icons.Default.Search, "Search Engine", null, onSearchEngine)
                ActionItem(Icons.Default.Search, "Find in page", null, onFind)
                ActionItem(Icons.Default.History, "History", null, onHistory)
                ActionItem(Icons.Default.Bookmark, "Bookmarks", null, onBookmarks)
                ActionItem(Icons.Default.Download, "Downloads", null, onDownloads)
                ActionItem(Icons.Default.Language, "User Agent", null, onUserAgent)
                ActionItem(Icons.Default.Public, "My IP Address", null, onMyIp)
            }
        }
    }
}

@Composable
private fun ToggleItem(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}

@Composable
private fun ActionItem(
    icon: ImageVector,
    label: String,
    sublabel: String?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                label,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
            )
            if (!sublabel.isNullOrBlank()) {
                Text(
                    sublabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
        }
    }
}