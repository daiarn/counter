package com.daiarn.counter.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val roundedSans = FontFamily.SansSerif
val Typography = Typography(
    headlineLarge = TextStyle(fontFamily = roundedSans, fontWeight = FontWeight.Bold,
        fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-1).sp),
    headlineSmall = TextStyle(fontFamily = roundedSans, fontWeight = FontWeight.Bold,
        fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-.4).sp),
    titleLarge = TextStyle(fontFamily = roundedSans, fontWeight = FontWeight.SemiBold,
        fontSize = 21.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = roundedSans, fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontFamily = roundedSans, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = roundedSans, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = roundedSans, fontSize = 12.sp, lineHeight = 18.sp)
)
