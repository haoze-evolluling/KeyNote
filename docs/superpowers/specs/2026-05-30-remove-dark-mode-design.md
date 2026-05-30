# 移除深色模式支持设计文档

## 1. 概述

### 1.1 目标
完全移除应用中的深色模式支持，使应用始终使用浅色模式。同时移除主题色选择功能，使用固定的默认主题色（紫色），并移除Material You动态取色支持。

### 1.2 背景
用户希望简化应用的主题系统，移除不必要的复杂性，使代码更简洁易维护。

### 1.3 范围
- 完全移除深色模式支持
- 移除主题色选择功能
- 移除Material You动态取色支持
- 将硬编码颜色统一改为MaterialTheme颜色

## 2. 设计方案

### 2.1 核心原则
1. **简化优先**：移除所有深色模式相关代码，简化主题系统
2. **一致性**：所有颜色统一使用MaterialTheme，提高代码一致性
3. **向后兼容**：保留现有的浅色模式外观，不影响用户体验

### 2.2 架构变更

#### 2.2.1 主题系统简化
**当前架构**：
```
用户选择 → PreferencesManager → MainActivity → KeyNoteTheme → MaterialTheme
   |              |                  |              |
   v              v                  v              v
SettingsScreen  is_dark_mode key   决定 isDark    选择 ColorScheme
               "system"/"light"/"dark"   true/false    light or dark
```

**目标架构**：
```
固定配置 → KeyNoteTheme → MaterialTheme
   |            |
   v            v
PurpleSeed   固定浅色方案
```

#### 2.2.2 文件变更清单

| 文件 | 变更类型 | 说明 |
|------|----------|------|
| `Theme.kt` | 重构 | 移除深色模式逻辑，简化为固定浅色方案 |
| `ColorGenerator.kt` | 删除 | 不再需要动态生成配色方案 |
| `Color.kt` | 简化 | 保留PurpleSeed，移除其他种子色 |
| `PreferencesManager.kt` | 简化 | 移除isDarkMode和themeColor相关代码 |
| `SettingsViewModel.kt` | 简化 | 移除setDarkMode和setThemeColor方法 |
| `MainActivity.kt` | 简化 | 移除深色模式判断逻辑，强制使用浅色模式 |
| `SettingsScreen.kt` | 简化 | 移除外观模式和主题色选择UI |
| `AIChatScreen.kt` | 修复 | 将硬编码颜色改为MaterialTheme颜色 |
| `LineChart.kt` | 修复 | 将硬编码Color.White改为MaterialTheme.colorScheme.surface |
| `BarChart.kt` | 修复 | 将硬编码颜色改为MaterialTheme颜色 |
| `DonutChart.kt` | 修复 | 将硬编码色表改为MaterialTheme颜色 |
| `EditNoteScreen.kt` | 修复 | 移除isSystemInDarkTheme()调用 |

### 2.3 详细设计

#### 2.3.1 主题系统重构

**Theme.kt 重构**：
```kotlin
@Composable
fun KeyNoteTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = lightColorScheme(
        primary = PurpleSeed,
        // 其他颜色使用Material3默认浅色方案
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
```

**Color.kt 简化**：
```kotlin
package com.haoze.keynote.ui.theme

import androidx.compose.ui.graphics.Color

val PurpleSeed = Color(0xFF6750A4)
```

#### 2.3.2 配置与持久化简化

**PreferencesManager.kt 简化**：
- 移除 `IS_DARK_MODE` 和 `THEME_COLOR` 常量
- 移除 `isDarkMode` 和 `themeColor` Flow
- 移除 `saveIsDarkMode()` 和 `saveThemeColor()` 方法

**SettingsViewModel.kt 简化**：
- 移除 `isDarkMode` 和 `themeColor` StateFlow
- 移除 `setDarkMode()` 和 `setThemeColor()` 方法

**MainActivity.kt 简化**：
```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SideEffect {
                val controller = WindowInsetsControllerCompat(window, window.decorView)
                controller.isAppearanceLightStatusBars = true
            }
            KeyNoteTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()
                }
            }
        }
    }
}
```

#### 2.3.3 设置界面简化

**SettingsScreen.kt 简化**：
- 移除"外观模式"部分（第66-78行）
- 移除"主题色"部分（第79-124行）
- 保留"正文字体大小"和"AI配置"部分

#### 2.3.4 UI组件修复

**AIChatScreen.kt 修复**：
- 将硬编码的 `Color.White` 和浅蓝色改为 `MaterialTheme.colorScheme.surface`
- 将硬编码的 `Color.Black.copy(alpha = 0.1f)` 改为 `MaterialTheme.colorScheme.outline`

**LineChart.kt 修复**：
- 将 `drawCircle(Color.White, ...)` 改为 `drawCircle(MaterialTheme.colorScheme.surface, ...)`

**BarChart.kt 修复**：
- 将硬编码的柱状图颜色改为 `MaterialTheme.colorScheme.primary`
- 将硬编码的文字颜色改为 `MaterialTheme.colorScheme.onSurface`

**DonutChart.kt 修复**：
- 将硬编码的色表改为使用 `MaterialTheme.colorScheme` 中的颜色

**EditNoteScreen.kt 修复**：
- 移除 `isDark = isSystemInDarkTheme()` 调用
- 如果 `MarkdownPreview` 需要 `isDark` 参数，传入 `false`

## 3. 实施计划

### 3.1 阶段一：主题系统简化
1. 简化 `Theme.kt`，移除深色模式逻辑
2. 简化 `Color.kt`，只保留PurpleSeed
3. 删除 `ColorGenerator.kt`

### 3.2 阶段二：配置与持久化简化
1. 简化 `PreferencesManager.kt`
2. 简化 `SettingsViewModel.kt`
3. 简化 `MainActivity.kt`

### 3.3 阶段三：设置界面简化
1. 简化 `SettingsScreen.kt`，移除外观模式和主题色选择UI

### 3.4 阶段四：UI组件修复
1. 修复 `AIChatScreen.kt` 中的硬编码颜色
2. 修复 `LineChart.kt` 中的硬编码颜色
3. 修复 `BarChart.kt` 中的硬编码颜色
4. 修复 `DonutChart.kt` 中的硬编码颜色
5. 修复 `EditNoteScreen.kt` 中的 `isSystemInDarkTheme()` 调用

## 4. 验证标准

### 4.1 功能验证
- [ ] 应用始终使用浅色模式
- [ ] 设置界面不再显示外观模式和主题色选择
- [ ] 所有UI组件颜色正确使用MaterialTheme
- [ ] 应用启动和运行正常

### 4.2 代码验证
- [ ] 所有深色模式相关代码已移除
- [ ] 所有硬编码颜色已改为MaterialTheme颜色
- [ ] 代码编译无错误
- [ ] 无未使用的导入和变量

## 5. 风险与缓解

### 5.1 风险
1. **用户体验变化**：用户可能习惯深色模式
2. **代码遗漏**：可能遗漏某些深色模式相关代码
3. **兼容性问题**：某些UI组件可能依赖深色模式

### 5.2 缓解措施
1. **用户沟通**：提前告知用户此变更
2. **代码审查**：仔细检查所有相关文件
3. **测试验证**：全面测试所有UI组件

## 6. 总结

本设计文档详细说明了如何完全移除应用中的深色模式支持。通过简化主题系统、移除配置选项、修复硬编码颜色，可以使代码更简洁易维护，同时保持现有的浅色模式外观。