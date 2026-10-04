package com.nova.browser.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val NovaTypography = Typography(
    titleLarge = Typography().titleLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
    bodyMedium = Typography().bodyMedium.copy(fontFamily = FontFamily.SansSerif),
)