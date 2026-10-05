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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Gif
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.browser.data.MediaSniffer

@Composable
fun MediaPanel(
    visible: Boolean,
    items: List<MediaSniffer.MediaItem>,
    onPlay: (MediaSniffer.MediaItem) -> Unit,
    onDownload: (MediaSniffer.MediaItem) -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) return

    val navBarPadding = WindowInsets.navigationBars.asPaddingValues()
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val panelWidth = (screenWidth - 32).coerceAtMost(480).dp

    var filter by remember { mutableStateOf<MediaSniffer.MediaKind?>(null) }

    val filtered = remember(items, filter) {
        if (filter == null) items
        else items.filter { it.kind == filter }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f))
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
                .width(panelWidth)
                .heightIn(max = 600.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable { }
                .padding(vertical = 12.dp),
        ) {
            // ── Header ─────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Media on page",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "${items.size}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.width(6.dp))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Close")
                }
            }

            // ── Filter chips ───────────────────────────────
            if (items.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FilterChip(
                        label = "All",
                        count = items.size,
                        selected = filter == null,
                        onClick = { filter = null },
                    )
                    val vc = items.count { it.kind == MediaSniffer.MediaKind.VIDEO }
                    if (vc > 0) FilterChip(
                        label = "Video",
                        count = vc,
                        selected = filter == MediaSniffer.MediaKind.VIDEO,
                        onClick = { filter = MediaSniffer.MediaKind.VIDEO },
                    )
                    val ac = items.count { it.kind == MediaSniffer.MediaKind.AUDIO }
                    if (ac > 0) FilterChip(
                        label = "Audio",
                        count = ac,
                        selected = filter == MediaSniffer.MediaKind.AUDIO,
                        onClick = { filter = MediaSniffer.MediaKind.AUDIO },
                    )
                    val ic = items.count {
                        it.kind == MediaSniffer.MediaKind.IMAGE ||
                            it.kind == MediaSniffer.MediaKind.GIF
                    }
                    if (ic > 0) FilterChip(
                        label = "Image",
                        count = ic,
                        selected = filter == MediaSniffer.MediaKind.IMAGE,
                        onClick = { filter = MediaSniffer.MediaKind.IMAGE },
                    )
                }
            }

            // ── List ───────────────────────────────────────
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            Icons.Default.Movie, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(40.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "No media detected yet",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                        )
                        Text(
                            "Play a video or open images",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                        )
                    }
                }
            } else if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "No ${filter?.name?.lowercase() ?: ""} items",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 440.dp),
                ) {
                    items(filtered, key = { it.url }) { item ->
                        MediaCard(
                            item = item,
                            onPlay = { onPlay(item) },
                            onDownload = { onDownload(item) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            "$label $count",
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun MediaCard(
    item: MediaSniffer.MediaItem,
    onPlay: () -> Unit,
    onDownload: () -> Unit,
) {
    val (icon, tint) = iconAndTintFor(item.kind)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // ── Icon block ────────────────────────────────
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(tint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp),
            )
        }

        Spacer(Modifier.width(12.dp))

        // ── Info column ───────────────────────────────
        Column(Modifier.weight(1f)) {
            Text(
                text = item.fileName,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(2.dp))

            // Type · Size · Duration · Dimensions
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.kindLabel,
                    color = tint,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = " · ${item.displaySize}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                )
                if (item.displayDuration.isNotBlank()) {
                    Text(
                        text = " · ${item.displayDuration}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
                if (item.displayDimensions.isNotBlank()) {
                    Text(
                        text = " · ${item.displayDimensions}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }

            // MIME type (small)
            item.contentType?.let { ct ->
                Text(
                    text = ct,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // ── Actions ───────────────────────────────────
        IconButton(onClick = onPlay, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.Default.PlayArrow, "Play",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
        }
        IconButton(onClick = onDownload, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.Default.Download, "Download",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

private fun iconAndTintFor(kind: MediaSniffer.MediaKind): Pair<ImageVector, Color> {
    return when (kind) {
        MediaSniffer.MediaKind.VIDEO -> Icons.Default.Movie to Color(0xFFEF4444)
        MediaSniffer.MediaKind.AUDIO -> Icons.Default.MusicNote to Color(0xFF8B5CF6)
        MediaSniffer.MediaKind.IMAGE -> Icons.Default.Image to Color(0xFF3B82F6)
        MediaSniffer.MediaKind.GIF -> Icons.Default.Gif to Color(0xFFF59E0B)
        MediaSniffer.MediaKind.STREAM -> Icons.Default.Podcasts to Color(0xFF10B981)
        MediaSniffer.MediaKind.DOCUMENT -> Icons.Default.InsertDriveFile to Color(0xFF6B7280)
        else -> Icons.Default.Movie to Color(0xFF6B7280)
    }
}