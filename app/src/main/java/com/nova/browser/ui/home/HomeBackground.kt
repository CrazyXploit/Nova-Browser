package com.nova.browser.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.nova.browser.data.HomeBackgroundManager

@Composable
fun HomeBackground(
    preset: HomeBackgroundManager.Preset,
    customColor: Long,
    content: @Composable () -> Unit,
) {
    val brush = when (preset) {
        HomeBackgroundManager.Preset.NOVA -> Brush.linearGradient(
            listOf(
                Color(0xFF0A0A0A),
                Color(0xFF1A0F2E),
                Color(0xFF0A0A0A),
            )
        )
        HomeBackgroundManager.Preset.OCEAN -> Brush.linearGradient(
            listOf(
                Color(0xFF0A0A1A),
                Color(0xFF0A1F2E),
                Color(0xFF0A0A0A),
            )
        )
        HomeBackgroundManager.Preset.SUNSET -> Brush.linearGradient(
            listOf(
                Color(0xFF1A0A0A),
                Color(0xFF2E1A0A),
                Color(0xFF0A0A0A),
            )
        )
        HomeBackgroundManager.Preset.FOREST -> Brush.linearGradient(
            listOf(
                Color(0xFF0A1A0A),
                Color(0xFF0F2E1A),
                Color(0xFF0A0A0A),
            )
        )
        HomeBackgroundManager.Preset.MONO -> Brush.linearGradient(
            listOf(
                Color(0xFF0A0A0A),
                Color(0xFF1A1A1A),
                Color(0xFF0A0A0A),
            )
        )
        HomeBackgroundManager.Preset.COSMIC -> Brush.linearGradient(
            listOf(
                Color(0xFF0F0A1A),
                Color(0xFF2E1A3E),
                Color(0xFF0A0A0A),
            )
        )
        HomeBackgroundManager.Preset.SOLID -> Brush.linearGradient(
            listOf(
                Color(customColor),
                Color(customColor),
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush),
    ) {
        content()
    }
}