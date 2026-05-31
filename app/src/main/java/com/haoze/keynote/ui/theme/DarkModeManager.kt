package com.haoze.keynote.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 深色模式偏好枚举
 */
enum class DarkModePreference {
    SYSTEM,  // 跟随系统
    LIGHT,   // 始终浅色
    DARK     // 始终深色
}

/**
 * 深色模式管理器，封装主题切换逻辑
 */
@Immutable
data class DarkModeManager(
    val preference: DarkModePreference = DarkModePreference.SYSTEM
) {
    /**
     * 根据当前偏好判断是否使用深色模式
     */
    @Composable
    fun isDarkMode(): Boolean {
        return when (preference) {
            DarkModePreference.SYSTEM -> isSystemInDarkTheme()
            DarkModePreference.LIGHT -> false
            DarkModePreference.DARK -> true
        }
    }
}

/**
 * CompositionLocal 提供 DarkModeManager 实例
 */
val LocalDarkModeManager = staticCompositionLocalOf<DarkModeManager> {
    DarkModeManager()
}

/**
 * 将 Int 偏好值转换为 DarkModePreference 枚举
 */
fun Int.toDarkModePreference(): DarkModePreference {
    return when (this) {
        1 -> DarkModePreference.LIGHT
        2 -> DarkModePreference.DARK
        else -> DarkModePreference.SYSTEM
    }
}

/**
 * 将 DarkModePreference 枚举转换为 Int 偏好值
 */
fun DarkModePreference.toInt(): Int {
    return when (this) {
        DarkModePreference.SYSTEM -> 0
        DarkModePreference.LIGHT -> 1
        DarkModePreference.DARK -> 2
    }
}
