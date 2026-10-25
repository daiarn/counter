package com.daiarn.counter.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// A consistent blue-and-gold identity, including when the device uses light mode.
private val CounterColors = darkColorScheme(
    primary = Gold, onPrimary = DeepInk,
    primaryContainer = CardBlue, onPrimaryContainer = Cream,
    secondary = Mist, onSecondary = DeepInk,
    secondaryContainer = CardBlue, onSecondaryContainer = Cream,
    background = Navy, onBackground = Cream,
    surface = CardBlue, onSurface = Cream,
    surfaceVariant = Color(0xFF476E8B), onSurfaceVariant = Mist,
    surfaceContainer = CardBlue, surfaceContainerHigh = Color(0xFF305976),
    surfaceContainerHighest = Color(0xFF476E8B),
    outline = Color(0xFF9CB3C4), outlineVariant = Color(0xFF557993),
    error = Color(0xFFFFB4AB), onError = Color(0xFF601410),
    inverseSurface = Cream, inverseOnSurface = DeepInk, inversePrimary = Navy
)

@Composable
fun CounterTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CounterColors,
        typography = Typography,
        shapes = Shapes(
            small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(22.dp), extraLarge = RoundedCornerShape(28.dp)
        ),
        content = content
    )
}
