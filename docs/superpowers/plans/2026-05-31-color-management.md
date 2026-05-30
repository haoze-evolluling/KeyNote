# 颜色管理系统实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 KeyNote 应用建立统一的颜色管理系统，支持深色/浅色模式切换，消除硬编码颜色值。

**Architecture:** 创建独立的颜色管理模块，包含 AppColors 数据类、AppColorPalette 主题定义、CompositionLocal 提供颜色访问。通过扩展函数保持向后兼容。

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, CompositionLocal

---

## 文件结构

```
ui/theme/
├── Color.kt          # 颜色定义（保留，扩展）
├── AppColors.kt      # AppColors 数据类
├── AppColorPalette.kt # 浅色/深色主题定义
├── Theme.kt          # 主题配置（修改）
├── Type.kt           # 字体（不变）
└── LocalAppColors.kt # CompositionLocal 定义
```

## 任务分解

### Task 1: 创建 AppColors 数据类

**Files:**
- Create: `app/src/main/java/com/haoze/keynote/ui/theme/AppColors.kt`

- [ ] **Step 1: 创建 AppColors 数据类**

```kotlin
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
    
    // 中性色
    val outline: Color,
    val outlineVariant: Color,
    val background: Color,
    val onBackground: Color,
    
    // 图表专用色
    val chartColors: List<Color>
)
```

- [ ] **Step 2: 验证编译通过**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 提交代码**

```bash
git add app/src/main/java/com/haoze/keynote/ui/theme/AppColors.kt
git commit -m "feat: 创建 AppColors 数据类"
```

### Task 2: 创建 LocalAppColors CompositionLocal

**Files:**
- Create: `app/src/main/java/com/haoze/keynote/ui/theme/LocalAppColors.kt`

- [ ] **Step 1: 创建 CompositionLocal 定义**

```kotlin
package com.haoze.keynote.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf

val LocalAppColors = staticCompositionLocalOf<AppColors> {
    error("No AppColors provided")
}
```

- [ ] **Step 2: 验证编译通过**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 提交代码**

```bash
git add app/src/main/java/com/haoze/keynote/ui/theme/LocalAppColors.kt
git commit -m "feat: 创建 LocalAppColors CompositionLocal"
```

### Task 3: 创建 AppColorPalette 主题定义

**Files:**
- Create: `app/src/main/java/com/haoze/keynote/ui/theme/AppColorPalette.kt`

- [ ] **Step 1: 创建浅色主题定义**

```kotlin
package com.haoze.keynote.ui.theme

import androidx.compose.ui.graphics.Color

object AppColorPalette {
    val Light = AppColors(
        // 品牌色
        primary = Color(0xFF0593FF),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD1E4FF),
        onPrimaryContainer = Color(0xFF001D36),
        
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
```

- [ ] **Step 2: 验证编译通过**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 提交代码**

```bash
git add app/src/main/java/com/haoze/keynote/ui/theme/AppColorPalette.kt
git commit -m "feat: 创建 AppColorPalette 主题定义"
```

### Task 4: 添加颜色转换函数

**Files:**
- Modify: `app/src/main/java/com/haoze/keynote/ui/theme/AppColorPalette.kt`

- [ ] **Step 1: 添加 toMaterialColorScheme 扩展函数**

```kotlin
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme

fun AppColors.toMaterialColorScheme() = if (this == AppColorPalette.Light) {
    lightColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
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
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        outline = outline,
        outlineVariant = outlineVariant,
        background = background,
        onBackground = onBackground
    )
}
```

- [ ] **Step 2: 验证编译通过**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 提交代码**

```bash
git add app/src/main/java/com/haoze/keynote/ui/theme/AppColorPalette.kt
git commit -m "feat: 添加颜色转换函数"
```

### Task 5: 修改 Theme.kt 集成新颜色系统

**Files:**
- Modify: `app/src/main/java/com/haoze/keynote/ui/theme/Theme.kt`

- [ ] **Step 1: 修改 KeyNoteTheme 函数**

```kotlin
package com.haoze.keynote.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
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
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) AppColorPalette.Dark else AppColorPalette.Light
    
    CompositionLocalProvider(LocalAppColors provides colors) {
        MaterialTheme(
            colorScheme = colors.toMaterialColorScheme(),
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}
```

- [ ] **Step 2: 验证编译通过**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 提交代码**

```bash
git add app/src/main/java/com/haoze/keynote/ui/theme/Theme.kt
git commit -m "feat: 修改 Theme.kt 集成新颜色系统"
```

### Task 6: 创建向后兼容扩展函数

**Files:**
- Create: `app/src/main/java/com/haoze/keynote/ui/theme/ThemeExtensions.kt`

- [ ] **Step 1: 创建扩展函数**

```kotlin
package com.haoze.keynote.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

val MaterialTheme.appColors: AppColors
    @Composable
    get() = LocalAppColors.current
```

- [ ] **Step 2: 验证编译通过**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 提交代码**

```bash
git add app/src/main/java/com/haoze/keynote/ui/theme/ThemeExtensions.kt
git commit -m "feat: 创建向后兼容扩展函数"
```

### Task 7: 迁移 AIChatScreen.kt 中的颜色使用

**Files:**
- Modify: `app/src/main/java/com/haoze/keynote/ui/chat/AIChatScreen.kt`

- [ ] **Step 1: 添加导入语句**

```kotlin
import com.haoze.keynote.ui.theme.LocalAppColors
```

- [ ] **Step 2: 替换 MaterialTheme.colorScheme 为 LocalAppColors.current**

在文件开头添加：
```kotlin
val colors = LocalAppColors.current
```

然后替换所有 `MaterialTheme.colorScheme.xxx` 为 `colors.xxx`。

- [ ] **Step 3: 验证编译通过**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 提交代码**

```bash
git add app/src/main/java/com/haoze/keynote/ui/chat/AIChatScreen.kt
git commit -m "refactor: 迁移 AIChatScreen.kt 中的颜色使用"
```

### Task 8: 迁移其他UI文件中的颜色使用

**Files:**
- Modify: `app/src/main/java/com/haoze/keynote/ui/navigation/AppDrawer.kt`
- Modify: `app/src/main/java/com/haoze/keynote/ui/bill/BarChart.kt`
- Modify: `app/src/main/java/com/haoze/keynote/ui/bill/LineChart.kt`
- Modify: `app/src/main/java/com/haoze/keynote/ui/bill/DonutChart.kt`
- Modify: `app/src/main/java/com/haoze/keynote/ui/settings/SettingsScreen.kt`
- Modify: `app/src/main/java/com/haoze/keynote/ui/edit/EditNoteScreen.kt`
- Modify: `app/src/main/java/com/haoze/keynote/ui/trash/TrashScreen.kt`
- Modify: `app/src/main/java/com/haoze/keynote/ui/tag/TagNotesScreen.kt`
- Modify: `app/src/main/java/com/haoze/keynote/ui/layout/PushDrawerLayout.kt`

- [ ] **Step 1: 迁移 AppDrawer.kt**

在文件开头添加导入语句：
```kotlin
import com.haoze.keynote.ui.theme.LocalAppColors
```

在 Composable 函数开头添加：
```kotlin
val colors = LocalAppColors.current
```

替换所有 `MaterialTheme.colorScheme.xxx` 为 `colors.xxx`。

- [ ] **Step 2: 迁移 BarChart.kt**

在文件开头添加导入语句：
```kotlin
import com.haoze.keynote.ui.theme.LocalAppColors
```

在 Composable 函数开头添加：
```kotlin
val colors = LocalAppColors.current
```

替换所有 `MaterialTheme.colorScheme.xxx` 为 `colors.xxx`。

- [ ] **Step 3: 迁移 LineChart.kt**

在文件开头添加导入语句：
```kotlin
import com.haoze.keynote.ui.theme.LocalAppColors
```

在 Composable 函数开头添加：
```kotlin
val colors = LocalAppColors.current
```

替换所有 `MaterialTheme.colorScheme.xxx` 为 `colors.xxx`。

- [ ] **Step 4: 迁移 DonutChart.kt**

在文件开头添加导入语句：
```kotlin
import com.haoze.keynote.ui.theme.LocalAppColors
```

在 Composable 函数开头添加：
```kotlin
val colors = LocalAppColors.current
```

替换所有 `MaterialTheme.colorScheme.xxx` 为 `colors.xxx`。

- [ ] **Step 5: 迁移 SettingsScreen.kt**

在文件开头添加导入语句：
```kotlin
import com.haoze.keynote.ui.theme.LocalAppColors
```

在 Composable 函数开头添加：
```kotlin
val colors = LocalAppColors.current
```

替换所有 `MaterialTheme.colorScheme.xxx` 为 `colors.xxx`。

- [ ] **Step 6: 迁移 EditNoteScreen.kt**

在文件开头添加导入语句：
```kotlin
import com.haoze.keynote.ui.theme.LocalAppColors
```

在 Composable 函数开头添加：
```kotlin
val colors = LocalAppColors.current
```

替换所有 `MaterialTheme.colorScheme.xxx` 为 `colors.xxx`。

- [ ] **Step 7: 迁移 TrashScreen.kt**

在文件开头添加导入语句：
```kotlin
import com.haoze.keynote.ui.theme.LocalAppColors
```

在 Composable 函数开头添加：
```kotlin
val colors = LocalAppColors.current
```

替换所有 `MaterialTheme.colorScheme.xxx` 为 `colors.xxx`。

- [ ] **Step 8: 迁移 TagNotesScreen.kt**

在文件开头添加导入语句：
```kotlin
import com.haoze.keynote.ui.theme.LocalAppColors
```

在 Composable 函数开头添加：
```kotlin
val colors = LocalAppColors.current
```

替换所有 `MaterialTheme.colorScheme.xxx` 为 `colors.xxx`。

- [ ] **Step 9: 迁移 PushDrawerLayout.kt**

在文件开头添加导入语句：
```kotlin
import com.haoze.keynote.ui.theme.LocalAppColors
```

在 Composable 函数开头添加：
```kotlin
val colors = LocalAppColors.current
```

替换所有 `MaterialTheme.colorScheme.xxx` 为 `colors.xxx`。

- [ ] **Step 10: 验证编译通过**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 11: 提交代码**

```bash
git add app/src/main/java/com/haoze/keynote/ui/
git commit -m "refactor: 迁移所有UI文件中的颜色使用"
```

### Task 9: 清理 colors.xml 中未使用的颜色

**Files:**
- Modify: `app/src/main/res/values/colors.xml`

- [ ] **Step 1: 检查哪些颜色被使用**

运行以下命令搜索代码中对 `R.color.xxx` 的引用：
```bash
grep -r "R.color\." app/src/main/java/
```

记录所有被引用的颜色名称。

- [ ] **Step 2: 删除未使用的颜色**

删除未被引用的颜色定义。

- [ ] **Step 3: 验证编译通过**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 提交代码**

```bash
git add app/src/main/res/values/colors.xml
git commit -m "refactor: 清理 colors.xml 中未使用的颜色"
```

### Task 10: 添加单元测试

**Files:**
- Create: `app/src/test/java/com/haoze/keynote/ui/theme/AppColorsTest.kt`

- [ ] **Step 1: 创建 AppColors 单元测试**

```kotlin
package com.haoze.keynote.ui.theme

import org.junit.Test
import org.junit.Assert.*

class AppColorsTest {
    
    @Test
    fun `light theme should have correct primary color`() {
        val colors = AppColorPalette.Light
        assertEquals(0xFF0593FF.toInt(), colors.primary.hashCode())
    }
    
    @Test
    fun `dark theme should have correct primary color`() {
        val colors = AppColorPalette.Dark
        assertEquals(0xFF9ECAFF.toInt(), colors.primary.hashCode())
    }
    
    @Test
    fun `light and dark themes should have different colors`() {
        val light = AppColorPalette.Light
        val dark = AppColorPalette.Dark
        assertNotEquals(light.primary, dark.primary)
        assertNotEquals(light.surface, dark.surface)
    }
    
    @Test
    fun `chart colors should have 5 items`() {
        val light = AppColorPalette.Light
        assertEquals(5, light.chartColors.size)
    }
}
```

- [ ] **Step 2: 运行测试**

Run: `./gradlew :app:testDebugUnitTest`
Expected: ALL TESTS PASSED

- [ ] **Step 3: 提交代码**

```bash
git add app/src/test/java/com/haoze/keynote/ui/theme/AppColorsTest.kt
git commit -m "test: 添加 AppColors 单元测试"
```

### Task 11: 验证主题切换功能

**Files:**
- Modify: `app/src/main/java/com/haoze/keynote/MainActivity.kt` (如果需要添加主题切换按钮)

- [ ] **Step 1: 手动测试深色模式**

1. 在设备上启用深色模式（设置 -> 显示 -> 深色模式）
2. 运行应用
3. 验证颜色正确切换：
   - 主色从 #0593FF 变为 #9ECAFF
   - 表面色从浅色变为深色
   - 文字颜色从深色变为浅色

- [ ] **Step 2: 验证所有UI组件颜色正确**

检查所有页面的颜色是否符合预期。

- [ ] **Step 3: 提交最终代码**

```bash
git add .
git commit -m "feat: 完成颜色管理系统实现"
```

## 验证清单

- [ ] 所有颜色定义集中在 AppColorPalette 中
- [ ] 代码中不再有硬编码颜色值（除了 Color.Transparent 等系统颜色）
- [ ] 支持深色/浅色模式切换
- [ ] 颜色命名符合语义化规范
- [ ] 现有功能不受影响，UI 保持一致
- [ ] 单元测试通过
- [ ] 编译通过，无警告