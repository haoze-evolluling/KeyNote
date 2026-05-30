# 颜色系统重构实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 重构 KeyNote 应用的颜色系统，移除主题色选择功能，固定主题色为 #0593ff，统一所有背景颜色

**Architecture:** 使用 Material3 HCT 色彩系统从固定种子色生成配色方案，移除动态颜色和主题色选择逻辑，简化 Theme 和 Settings 模块

**Tech Stack:** Kotlin, Jetpack Compose, Material3, DataStore

---

## 文件结构

| 文件 | 职责 | 操作 |
|------|------|------|
| `Color.kt` | 定义种子颜色 | 修改：移除所有种子色，仅保留 PrimarySeed |
| `ColorGenerator.kt` | 从种子色生成 Material3 配色方案 | 保持不变 |
| `Theme.kt` | 应用主题配置 | 修改：移除 colorSchemes Map 和动态颜色 |
| `SettingsScreen.kt` | 设置界面 | 修改：移除主题色选择区域 |
| `SettingsViewModel.kt` | 设置业务逻辑 | 修改：移除 themeColor 相关逻辑 |
| `PreferencesManager.kt` | 数据持久化 | 修改：移除 themeColor 相关代码 |
| `MainActivity.kt` | 应用入口 | 修改：移除 themeColorPref |
| `AIChatScreen.kt` | AI 对话界面 | 修改：深色模式移除渐变，使用纯色背景 |

---

### Task 1: 修改 Color.kt - 简化种子颜色定义

**Files:**
- Modify: `app/src/main/java/com/haoze/keynote/ui/theme/Color.kt`

- [ ] **Step 1: 替换 Color.kt 内容**

```kotlin
package com.haoze.keynote.ui.theme

import androidx.compose.ui.graphics.Color

val PrimarySeed = Color(0xFF0593FF)
```

- [ ] **Step 2: 验证编译**

Run: `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/haoze/keynote/ui/theme/Color.kt
git commit -m "refactor: simplify Color.kt, keep only PrimarySeed"
```

---

### Task 2: 修改 Theme.kt - 移除动态颜色和主题色选择

**Files:**
- Modify: `app/src/main/java/com/haoze/keynote/ui/theme/Theme.kt`

- [ ] **Step 1: 替换 Theme.kt 内容**

```kotlin
package com.haoze.keynote.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
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
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = generateColorScheme(PrimarySeed.toArgb(), darkTheme)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
```

- [ ] **Step 2: 验证编译**

Run: `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/haoze/keynote/ui/theme/Theme.kt
git commit -m "refactor: remove dynamic color and theme color selection from Theme"
```

---

### Task 3: 修改 MainActivity.kt - 移除 themeColorPref

**Files:**
- Modify: `app/src/main/java/com/haoze/keynote/MainActivity.kt`

- [ ] **Step 1: 替换 MainActivity.kt 内容**

```kotlin
package com.haoze.keynote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.view.WindowInsetsControllerCompat
import com.haoze.keynote.ui.navigation.AppNavigation
import com.haoze.keynote.ui.theme.KeyNoteTheme
import com.haoze.keynote.util.PreferencesManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val preferencesManager = remember { PreferencesManager(applicationContext) }
            val darkModePref by preferencesManager.isDarkMode.collectAsState(initial = "system")
            val isDark = when (darkModePref) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }
            SideEffect {
                val controller = WindowInsetsControllerCompat(window, window.decorView)
                controller.isAppearanceLightStatusBars = !isDark
            }
            KeyNoteTheme(darkTheme = isDark) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()
                }
            }
        }
    }
}
```

- [ ] **Step 2: 验证编译**

Run: `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/haoze/keynote/MainActivity.kt
git commit -m "refactor: remove themeColorPref from MainActivity"
```

---

### Task 4: 修改 PreferencesManager.kt - 移除 themeColor 相关代码

**Files:**
- Modify: `app/src/main/java/com/haoze/keynote/util/PreferencesManager.kt`

- [ ] **Step 1: 移除 THEME_COLOR 相关代码**

移除以下内容：
- 第 21 行：`private val THEME_COLOR = stringPreferencesKey("theme_color")`
- 第 41-43 行：`val themeColor: Flow<String>...`
- 第 65-67 行：`suspend fun saveThemeColor(color: String)...`

- [ ] **Step 2: 验证编译**

Run: `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/haoze/keynote/util/PreferencesManager.kt
git commit -m "refactor: remove themeColor from PreferencesManager"
```

---

### Task 5: 修改 SettingsViewModel.kt - 移除 themeColor 逻辑

**Files:**
- Modify: `app/src/main/java/com/haoze/keynote/viewmodel/SettingsViewModel.kt`

- [ ] **Step 1: 移除 themeColor 相关代码**

移除以下内容：
- 第 33-34 行：`val themeColor: StateFlow<String>...`
- 第 122-124 行：`fun setThemeColor(color: String)...`

- [ ] **Step 2: 验证编译**

Run: `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/haoze/keynote/viewmodel/SettingsViewModel.kt
git commit -m "refactor: remove themeColor from SettingsViewModel"
```

---

### Task 6: 修改 SettingsScreen.kt - 移除主题色选择 UI

**Files:**
- Modify: `app/src/main/java/com/haoze/keynote/ui/settings/SettingsScreen.kt`

- [ ] **Step 1: 移除主题色选择区域**

移除以下内容：
- 第 40 行：`val themeColor by viewModel.themeColor.collectAsState()`
- 第 79-124 行：整个主题色选择区域（从 `Text("主题色")` 到 `FlowRow` 结束）

- [ ] **Step 2: 验证编译**

Run: `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/haoze/keynote/ui/settings/SettingsScreen.kt
git commit -m "refactor: remove theme color selector from SettingsScreen"
```

---

### Task 7: 修改 AIChatScreen.kt - 深色模式移除渐变

**Files:**
- Modify: `app/src/main/java/com/haoze/keynote/ui/chat/AIChatScreen.kt`

- [ ] **Step 1: 修改渐变背景逻辑**

将第 70-89 行的渐变背景代码修改为：

```kotlin
    val isDark = isSystemInDarkTheme()
    val gradientBrush = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1C1B1F),
                Color(0xFF1C1B1F),
                Color(0xFF1C1B1F),
                Color(0xFF1C1B1F)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color.White,
                Color.White,
                Color(0xFFE3F2FD).copy(alpha = 0.5f),
                Color(0xFFBBDEFB).copy(alpha = 0.6f)
            )
        )
    }
```

- [ ] **Step 2: 验证编译**

Run: `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/haoze/keynote/ui/chat/AIChatScreen.kt
git commit -m "refactor: remove gradient effect in dark mode for AIChatScreen"
```

---

### Task 8: 最终验证和提交

- [ ] **Step 1: 完整构建验证**

Run: `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 推送到远程**

```bash
git push
```

---

## 验收检查

完成所有任务后，验证以下内容：

1. [ ] 设置页面不再显示主题色选择器
2. [ ] 应用全局使用固定的 #0593ff 主题色
3. [ ] 浅色模式背景为纯白
4. [ ] 深色模式背景为深灰
5. [ ] 侧边栏和标题栏背景与主背景一致
6. [ ] AI 对话页面深色模式无渐变效果
7. [ ] 所有文字在对应背景下清晰可读
8. [ ] 浅色/深色模式切换正常
