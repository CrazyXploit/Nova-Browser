package com.nova.browser.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColors = darkColorScheme(
    primary = NovaPurple,
    onPrimary = Color.White,
    secondary = NovaBlue,
    tertiary = NovaCyan,
    background = NovaBg,
    surface = NovaSurface,
    surfaceVariant = NovaSurfaceHigh,
    onSurface = Color(0xFFEDEDED),
    onSurfaceVariant = Color(0xFFB0B0B0),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF6D28D9),
    secondary = Color(0xFF2563EB),
    tertiary = Color(0xFF0891B2),
)

@Composable
fun NovaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val scheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            try {
                if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
            } catch (e: Throwable) {
                if (darkTheme) DarkColors else LightColors
            }
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = scheme,
        typography = NovaTypography,
        content = content,
    )
}