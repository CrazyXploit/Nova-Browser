package com.nova.browser.ui.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class LongPressAction(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
)

@Composable
fun LongPressMenu(
    visible: Boolean,
    title: String,
    subtitle: String?,
    actions: List<LongPressAction>,
    onDismiss: () -> Unit,
) {
    if (!visible || actions.isEmpty()) return

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
                .heightIn(max = 520.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable { /* consume */ }
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(Modifier.size(4.dp))
                    Text(
                        text = subtitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.size(4.dp))

            actions.forEach { action ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            action.onClick()
                            onDismiss()
                        }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                ) {
                    Icon(
                        action.icon, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(14.dp))
                    Text(
                        action.label,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                    )
                }
            }
        }
    }
}

object LongPressActions {
    fun forLink(url: String, onOpenNewTab: () -> Unit, onCopyLink: () -> Unit, onShare: () -> Unit, onDownload: () -> Unit): List<LongPressAction> = listOf(
        LongPressAction(Icons.Default.OpenInNew, "Open in new tab", onOpenNewTab),
        LongPressAction(Icons.Default.ContentCopy, "Copy link", onCopyLink),
        LongPressAction(Icons.Default.Share, "Share link", onShare),
        LongPressAction(Icons.Default.Download, "Download link", onDownload),
    )

    fun forImage(url: String, onOpenImage: () -> Unit, onCopyUrl: () -> Unit, onDownload: () -> Unit): List<LongPressAction> = listOf(
        LongPressAction(Icons.Default.OpenInNew, "Open image", onOpenImage),
        LongPressAction(Icons.Default.ContentCopy, "Copy image URL", onCopyUrl),
        LongPressAction(Icons.Default.Download, "Download image", onDownload),
    )

    fun forText(text: String, onCopy: () -> Unit, onSearch: () -> Unit, onShare: () -> Unit): List<LongPressAction> = listOf(
        LongPressAction(Icons.Default.ContentCopy, "Copy text", onCopy),
        LongPressAction(Icons.Default.Search, "Search web", onSearch),
        LongPressAction(Icons.Default.Share, "Share text", onShare),
    )

    fun forPhone(number: String, onCall: () -> Unit, onCopy: () -> Unit): List<LongPressAction> = listOf(
        LongPressAction(Icons.Default.OpenInBrowser, "Call $number", onCall),
        LongPressAction(Icons.Default.ContentCopy, "Copy number", onCopy),
    )

    fun forEmail(email: String, onSend: () -> Unit, onCopy: () -> Unit): List<LongPressAction> = listOf(
        LongPressAction(Icons.Default.Person, "Email $email", onSend),
        LongPressAction(Icons.Default.ContentCopy, "Copy email", onCopy),
    )
}