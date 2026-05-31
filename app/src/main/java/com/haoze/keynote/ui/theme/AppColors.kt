package com.haoze.keynote.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class AppColors(
    // 品牌色
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    
    // 次要色
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    
    // 表面色
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    
    // 功能色
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    
    // 第三色
    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    
    // 中性色
    val outline: Color,
    val outlineVariant: Color,
    val background: Color,
    val onBackground: Color,

    // 透明色
    val transparent: Color,

    // 阴影色
    val shadow: Color,

    // 光晕渐变色
    val glowPrimary: Color,
    val glowSecondary: Color,

    // 对话框背景色
    val dialogContainer: Color,

    // 图标原始色（不应用 tint）
    val unspecified: Color,

    // 图表专用色
    val chartColors: List<Color>
)