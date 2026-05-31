package com.haoze.keynote.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(8.dp)
)

@Composable
fun KeyNoteTheme(
    darkModeManager: DarkModeManager = DarkModeManager(),
    content: @Composable () -> Unit
) {
    val darkTheme = darkModeManager.isDarkMode()
    val colors = if (darkTheme) AppColorPalette.Dark else AppColorPalette.Light

    CompositionLocalProvider(
        LocalAppColors provides colors,
        LocalDarkModeManager provides darkModeManager
    ) {
        MaterialTheme(
            colorScheme = colors.toMaterialColorScheme(),
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}
