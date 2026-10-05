package com.nova.browser.ui.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun UrlBar(
    url: String,
    loading: Boolean,
    progress: Int,
    isIncognito: Boolean,
    isBookmarked: Boolean,
    onBarClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onMoreClick: () -> Unit,
    onTabsClick: () -> Unit,
    tabCount: Int,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isIncognito) Color(0xFF1A1A2E)
                else MaterialTheme.colorScheme.background
            )
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Purely display — no input field, no focus issues
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onBarClick() }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (url.startsWith("https"))
                            Icons.Default.Lock
                        else
                            Icons.Default.Language,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (url.startsWith("https"))
                            Color(0xFF34D399)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = url.ifBlank { "Search or type URL" },
                        color = if (url.isBlank())
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else
                            MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            Spacer(Modifier.width(4.dp))

            IconButton(onClick = onBookmarkClick, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = if (isBookmarked)
                        Icons.Default.Bookmark
                    else
                        Icons.Default.BookmarkBorder,
                    contentDescription = "Bookmark",
                    tint = if (isBookmarked)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp),
                )
            }

            Box {
                IconButton(onClick = onTabsClick, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.Default.Language,
                        contentDescription = "Tabs",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp),
                    )
                }
                if (tabCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 2.dp, end = 2.dp)
                            .size(16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            tabCount.toString(),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            IconButton(onClick = onMoreClick, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "More",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        if (progress in 1..99) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Transparent,
            )
        }
    }
}