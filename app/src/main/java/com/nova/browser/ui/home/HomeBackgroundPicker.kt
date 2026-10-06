package com.nova.browser.ui.home

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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.browser.data.HomeBackgroundManager

@Composable
fun HomeBackgroundPicker(
    current: HomeBackgroundManager.Preset,
    onPick: (HomeBackgroundManager.Preset) -> Unit,
    onDismiss: () -> Unit,
) {
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = navBarPadding.calculateBottomPadding())
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable { }
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Home Background",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Close")
                }
            }

            Spacer(Modifier.height(12.dp))

            HomeBackgroundManager.Preset.values().forEach { p ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (p == current) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                        .clickable { onPick(p) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Preview swatch
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(previewBrush(p)),
                    )
                    Spacer(Modifier.size(12.dp))
                    Text(
                        p.label,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f),
                    )
                    if (p == current) {
                        Icon(
                            Icons.Default.Check, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

private fun previewBrush(preset: HomeBackgroundManager.Preset): Brush = when (preset) {
    HomeBackgroundManager.Preset.NOVA -> Brush.linearGradient(
        listOf(Color(0xFF0A0A0A), Color(0xFF1A0F2E), Color(0xFF0A0A0A))
    )
    HomeBackgroundManager.Preset.OCEAN -> Brush.linearGradient(
        listOf(Color(0xFF0A0A1A), Color(0xFF0A1F2E), Color(0xFF0A0A0A))
    )
    HomeBackgroundManager.Preset.SUNSET -> Brush.linearGradient(
        listOf(Color(0xFF1A0A0A), Color(0xFF2E1A0A), Color(0xFF0A0A0A))
    )
    HomeBackgroundManager.Preset.FOREST -> Brush.linearGradient(
        listOf(Color(0xFF0A1A0A), Color(0xFF0F2E1A), Color(0xFF0A0A0A))
    )
    HomeBackgroundManager.Preset.MONO -> Brush.linearGradient(
        listOf(Color(0xFF0A0A0A), Color(0xFF1A1A1A), Color(0xFF0A0A0A))
    )
    HomeBackgroundManager.Preset.COSMIC -> Brush.linearGradient(
        listOf(Color(0xFF0F0A1A), Color(0xFF2E1A3E), Color(0xFF0A0A0A))
    )
    HomeBackgroundManager.Preset.SOLID -> Brush.linearGradient(
        listOf(Color(0xFF7C5CFF), Color(0xFF7C5CFF))
    )
}