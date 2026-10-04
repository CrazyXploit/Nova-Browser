package com.nova.browser.ui.browser

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.browser.data.TabEntity

@Composable
fun TabStrip(
    tabs: List<TabEntity>,
    activeId: String?,
    onSelect: (String) -> Unit,
    onClose: (String) -> Unit,
) {
    AnimatedVisibility(
        visible = tabs.size > 1,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
        ) {
            items(tabs, key = { it.id }) { tab ->
                TabChip(
                    tab = tab,
                    active = tab.id == activeId,
                    onSelect = { onSelect(tab.id) },
                    onClose = { onClose(tab.id) },
                )
            }
        }
    }
}

@Composable
private fun TabChip(
    tab: TabEntity,
    active: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit,
) {
    val bg by animateColorAsState(
        targetValue = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        animationSpec = tween(250),
        label = "bg",
    )
    val border by animateColorAsState(
        targetValue = if (active) MaterialTheme.colorScheme.primary
        else Color.Transparent,
        animationSpec = tween(250),
        label = "border",
    )

    Row(
        modifier = Modifier
            .height(34.dp)
            .widthIn(min = 120.dp, max = 200.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable { onSelect() }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = tab.title.ifBlank { "New Tab" },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 12.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onClose, modifier = Modifier.size(20.dp)) {
            Icon(Icons.Default.Close, "Close", modifier = Modifier.size(12.dp))
        }
    }
}