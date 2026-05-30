package com.haoze.keynote.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme

object AppColorPalette {
    val Light = AppColors(
        // 品牌色
        primary = Color(0xFF0593FF),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD1E4FF),
        onPrimaryContainer = Color(0xFF001D36),
        
        // 次要色
        secondary = Color(0xFF535F70),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFD7E3F7),
        onSecondaryContainer = Color(0xFF101C2B),
        
        // 表面色
        surface = Color(0xFFFDFCFF),
        onSurface = Color(0xFF1A1C1E),
        surfaceVariant = Color(0xFFE7E0EC),
        onSurfaceVariant = Color(0xFF49454F),
        
        // 功能色
        error = Color(0xFFBA1A1A),
        onError = Color.White,
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        
        // 第三色
        tertiary = Color(0xFF7C5800),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFFFDEA1),
        onTertiaryContainer = Color(0xFF261A00),
        
        // 中性色
        outline = Color(0xFF79747E),
        outlineVariant = Color(0xFFCAC4D0),
        background = Color(0xFFFDFCFF),
        onBackground = Color(0xFF1A1C1E),
        
        // 图表专用色
        chartColors = listOf(
            Color(0xFF0593FF), // 主色
            Color(0xFF5BC0FF), // 浅色
            Color(0xFF0066CC), // 深色
            Color(0xFFD1E4FF), // 容器色
            Color(0xFF49454F)  // 中性色
        )
    )
    
    val Dark = AppColors(
        // 品牌色
        primary = Color(0xFF9ECAFF),
        onPrimary = Color(0xFF003258),
        primaryContainer = Color(0xFF00497D),
        onPrimaryContainer = Color(0xFFD1E4FF),
        
        // 次要色
        secondary = Color(0xFFBBC7DB),
        onSecondary = Color(0xFF253140),
        secondaryContainer = Color(0xFF3B4858),
        onSecondaryContainer = Color(0xFFD7E3F7),
        
        // 表面色
        surface = Color(0xFF1A1C1E),
        onSurface = Color(0xFFE3E2E6),
        surfaceVariant = Color(0xFF49454F),
        onSurfaceVariant = Color(0xFFCAC4D0),
        
        // 功能色
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        
        // 第三色
        tertiary = Color(0xFFFFBE2E),
        onTertiary = Color(0xFF3F2E00),
        tertiaryContainer = Color(0xFF5A4300),
        onTertiaryContainer = Color(0xFFFFDEA1),
        
        // 中性色
        outline = Color(0xFF938F99),
        outlineVariant = Color(0xFF49454F),
        background = Color(0xFF1A1C1E),
        onBackground = Color(0xFFE3E2E6),
        
        // 图表专用色
        chartColors = listOf(
            Color(0xFF9ECAFF), // 主色
            Color(0xFF5BC0FF), // 浅色
            Color(0xFF0066CC), // 深色
            Color(0xFF00497D), // 容器色
            Color(0xFF938F99)  // 中性色
        )
    )
}

fun AppColors.toMaterialColorScheme() = if (this == AppColorPalette.Light) {
    lightColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        outline = outline,
        outlineVariant = outlineVariant,
        background = background,
        onBackground = onBackground
    )
} else {
    darkColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        outline = outline,
        outlineVariant = outlineVariant,
        background = background,
        onBackground = onBackground
    )
}