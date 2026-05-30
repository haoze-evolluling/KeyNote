# 移除深色模式支持实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 完全移除应用中的深色模式支持，使应用始终使用浅色模式，同时移除主题色选择功能，使用固定的默认主题色（紫色）

**Architecture:** 简化主题系统，移除所有深色模式相关代码，将硬编码颜色统一改为MaterialTheme颜色

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, DataStore

---

## 文件结构

### 主题系统核心文件
- `app/src/main/java/com/haoze/keynote/ui/theme/Theme.kt` - 主题入口，简化为固定浅色方案
- `app/src/main/java/com/haoze/keynote/ui/theme/Color.kt` - 颜色定义，只保留PurpleSeed
- `app/src/main/java/com/haoze/keynote/ui/theme/ColorGenerator.kt` - 删除此文件

### 配置与持久化文件
- `app/src/main/java/com/haoze/keynote/util/PreferencesManager.kt` - 移除深色模式和主题色偏好
- `app/src/main/java/com/haoze/keynote/viewmodel/SettingsViewModel.kt` - 移除相关状态和方法
- `app/src/main/java/com/haoze/keynote/MainActivity.kt` - 简化为主题初始化

### 设置界面文件
- `app/src/main/java/com/haoze/keynote/ui/settings/SettingsScreen.kt` - 移除外观模式和主题色选择UI

### UI组件文件
- `app/src/main/java/com/haoze/keynote/ui/chat/AIChatScreen.kt` - 修复硬编码颜色
- `app/src/main/java/com/haoze/keynote/ui/bill/LineChart.kt` - 修复硬编码颜色
- `app/src/main/java/com/haoze/keynote/ui/bill/BarChart.kt` - 修复硬编码颜色
- `app/src/main/java/com/haoze/keynote/ui/bill/DonutChart.kt` - 修复硬编码颜色
- `app/src/main/java/com/haoze/keynote/ui/edit/EditNoteScreen.kt` - 移除isSystemInDarkTheme()调用

---

## 任务1：简化主题系统

**文件：**
- 修改: `app/src/main/java/com/haoze/keynote/ui/theme/Theme.kt`
- 修改: `app/src/main/java/com/haoze/keynote/ui/theme/Color.kt`
- 删除: `app/src/main/java/com/haoze/keynote/ui/theme/ColorGenerator.kt`

- [ ] **步骤1：简化Color.kt**

```kotlin
package com.haoze.keynote.ui.theme

import androidx.compose.ui.graphics.Color

val PurpleSeed = Color(0xFF6750A4)
```

- [ ] **步骤2：简化Theme.kt**

```kotlin
package com.haoze.keynote.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
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
    content: @Composable () -> Unit
) {
    val colorScheme = lightColorScheme(
        primary = PurpleSeed,
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
```

- [ ] **步骤3：删除ColorGenerator.kt**

```bash
rm app/src/main/java/com/haoze/keynote/ui/theme/ColorGenerator.kt
```

- [ ] **步骤4：验证编译**

```bash
./gradlew assembleDebug
```

- [ ] **步骤5：提交更改**

```bash
git add app/src/main/java/com/haoze/keynote/ui/theme/Theme.kt app/src/main/java/com/haoze/keynote/ui/theme/Color.kt
git rm app/src/main/java/com/haoze/keynote/ui/theme/ColorGenerator.kt
git commit -m "refactor: 简化主题系统，移除深色模式支持"
```

---

## 任务2：简化配置与持久化

**文件：**
- 修改: `app/src/main/java/com/haoze/keynote/util/PreferencesManager.kt`
- 修改: `app/src/main/java/com/haoze/keynote/viewmodel/SettingsViewModel.kt`
- 修改: `app/src/main/java/com/haoze/keynote/MainActivity.kt`

- [ ] **步骤1：简化PreferencesManager.kt**

移除以下内容：
- 第20-21行：`IS_DARK_MODE` 和 `THEME_COLOR` 常量
- 第37-43行：`isDarkMode` 和 `themeColor` Flow
- 第61-67行：`saveIsDarkMode()` 和 `saveThemeColor()` 方法

- [ ] **步骤2：简化SettingsViewModel.kt**

移除以下内容：
- 第30-34行：`isDarkMode` 和 `themeColor` StateFlow
- 第118-124行：`setDarkMode()` 和 `setThemeColor()` 方法

- [ ] **步骤3：简化MainActivity.kt**

```kotlin
package com.haoze.keynote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.core.view.WindowInsetsControllerCompat
import com.haoze.keynote.ui.navigation.AppNavigation
import com.haoze.keynote.ui.theme.KeyNoteTheme

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

- [ ] **步骤4：验证编译**

```bash
./gradlew assembleDebug
```

- [ ] **步骤5：提交更改**

```bash
git add app/src/main/java/com/haoze/keynote/util/PreferencesManager.kt app/src/main/java/com/haoze/keynote/viewmodel/SettingsViewModel.kt app/src/main/java/com/haoze/keynote/MainActivity.kt
git commit -m "refactor: 简化配置与持久化，移除深色模式相关代码"
```

---

## 任务3：简化设置界面

**文件：**
- 修改: `app/src/main/java/com/haoze/keynote/ui/settings/SettingsScreen.kt`

- [ ] **步骤1：移除外观模式和主题色选择UI**

移除第66-124行，包括：
- "外观模式"部分（第66-78行）
- "主题色"部分（第79-124行）
- 相关的导入和变量

- [ ] **步骤2：验证编译**

```bash
./gradlew assembleDebug
```

- [ ] **步骤3：提交更改**

```bash
git add app/src/main/java/com/haoze/keynote/ui/settings/SettingsScreen.kt
git commit -m "refactor: 简化设置界面，移除外观模式和主题色选择UI"
```

---

## 任务4：修复UI组件硬编码颜色

**文件：**
- 修改: `app/src/main/java/com/haoze/keynote/ui/chat/AIChatScreen.kt`
- 修改: `app/src/main/java/com/haoze/keynote/ui/bill/LineChart.kt`
- 修改: `app/src/main/java/com/haoze/keynote/ui/bill/BarChart.kt`
- 修改: `app/src/main/java/com/haoze/keynote/ui/bill/DonutChart.kt`
- 修改: `app/src/main/java/com/haoze/keynote/ui/edit/EditNoteScreen.kt`

- [ ] **步骤1：修复AIChatScreen.kt**

将硬编码颜色改为MaterialTheme颜色：
- 背景渐变：`Color.White` 和浅蓝色改为 `MaterialTheme.colorScheme.surface`
- 输入框背景：`Color.White` 改为 `MaterialTheme.colorScheme.surface`
- 阴影颜色：`Color.Black.copy(alpha = 0.1f)` 改为 `MaterialTheme.colorScheme.outline`

- [ ] **步骤2：修复LineChart.kt**

将 `drawCircle(Color.White, ...)` 改为 `drawCircle(MaterialTheme.colorScheme.surface, ...)`

- [ ] **步骤3：修复BarChart.kt**

- 柱状图颜色：`Color(0xFF6750A4)` 改为 `MaterialTheme.colorScheme.primary`
- 文字颜色：`android.graphics.Color.parseColor("#888888")` 改为 `MaterialTheme.colorScheme.onSurface`

- [ ] **步骤4：修复DonutChart.kt**

将硬编码的色表改为使用 `MaterialTheme.colorScheme` 中的颜色

- [ ] **步骤5：修复EditNoteScreen.kt**

移除 `isDark = isSystemInDarkTheme()` 调用，如果 `MarkdownPreview` 需要 `isDark` 参数，传入 `false`

- [ ] **步骤6：验证编译**

```bash
./gradlew assembleDebug
```

- [ ] **步骤7：提交更改**

```bash
git add app/src/main/java/com/haoze/keynote/ui/chat/AIChatScreen.kt app/src/main/java/com/haoze/keynote/ui/bill/LineChart.kt app/src/main/java/com/haoze/keynote/ui/bill/BarChart.kt app/src/main/java/com/haoze/keynote/ui/bill/DonutChart.kt app/src/main/java/com/haoze/keynote/ui/edit/EditNoteScreen.kt
git commit -m "refactor: 修复UI组件硬编码颜色，统一使用MaterialTheme"
```

---

## 任务5：最终验证与清理

- [ ] **步骤1：完整编译验证**

```bash
./gradlew clean assembleDebug
```

- [ ] **步骤2：运行测试（如果有）**

```bash
./gradlew test
```

- [ ] **步骤3：检查未使用的导入**

检查所有修改的文件，移除未使用的导入

- [ ] **步骤4：最终提交**

```bash
git add .
git commit -m "refactor: 完成移除深色模式支持，应用始终使用浅色模式"
```

---

## 验证清单

- [ ] 应用始终使用浅色模式
- [ ] 设置界面不再显示外观模式和主题色选择
- [ ] 所有UI组件颜色正确使用MaterialTheme
- [ ] 应用启动和运行正常
- [ ] 所有深色模式相关代码已移除
- [ ] 所有硬编码颜色已改为MaterialTheme颜色
- [ ] 代码编译无错误
- [ ] 无未使用的导入和变量