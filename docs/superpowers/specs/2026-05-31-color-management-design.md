# 颜色管理系统设计文档

## 概述

本设计文档描述如何为 KeyNote 应用建立统一的颜色管理系统，解决当前颜色定义分散、硬编码颜色值、缺乏主题切换支持等问题。

## 目标

1. **集中定义颜色**：在一个地方定义所有颜色，方便修改和维护
2. **支持主题切换**：支持深色/浅色模式切换
3. **减少硬编码颜色**：消除代码中的硬编码颜色值
4. **建立颜色规范**：创建一套完整的颜色使用规范

## 设计方案

### 方案选择

采用**独立颜色管理模块**方案，创建 `AppColors` 数据类和 `AppColorPalette` 对象，通过 `CompositionLocal` 提供颜色访问。

### 颜色系统架构

#### 1. AppColors 数据类

```kotlin
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

#### 2. 品牌色板（基于 #0593FF）

```
Primary: #0593FF (主色)
Primary Light: #5BC0FF (浅色变体)
Primary Dark: #0066CC (深色变体)
Primary Container: #D1E4FF (容器色)
```

#### 3. 语义化颜色分组

- **品牌色组**：primary, onPrimary, primaryContainer, onPrimaryContainer
- **表面色组**：surface, onSurface, surfaceVariant, onSurfaceVariant
- **功能色组**：error, onError, errorContainer, onErrorContainer
- **中性色组**：outline, outlineVariant, background, onBackground
- **图表色组**：chartColors（包含5种颜色的列表）

### 命名规范

- 使用 camelCase 命名：`primary`, `onSurface`, `errorContainer`
- `on` 前缀表示在对应背景上的文字/图标颜色
- `Container` 后缀表示容器/卡片背景色
- 图表色使用 `chart` + 数字：`chart1`, `chart2`

### 文件结构

```
ui/theme/
├── Color.kt          # 颜色定义（保留，扩展）
├── AppColors.kt      # AppColors 数据类
├── AppColorPalette.kt # 浅色/深色主题定义
├── Theme.kt          # 主题配置（修改）
├── Type.kt           # 字体（不变）
└── LocalAppColors.kt # CompositionLocal 定义
```

### 主题集成

```kotlin
@Composable
fun KeyNoteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) AppColorPalette.Dark else AppColorPalette.Light
    
    CompositionLocal.LocalAppColors provides colors {
        MaterialTheme(
            colorScheme = colors.toMaterialColorScheme(),
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}
```

### 使用方式

```kotlin
// 在 Composable 中使用
val colors = LocalAppColors.current

Text(
    text = "标题",
    color = colors.onSurface
)

Box(
    modifier = Modifier.background(colors.surface)
)
```

## 迁移计划

### 阶段1：创建颜色系统

1. 创建 `AppColors.kt` 定义数据类
2. 创建 `AppColorPalette.kt` 定义浅色/深色主题
3. 创建 `LocalAppColors.kt` 定义 CompositionLocal

### 阶段2：集成到主题

1. 修改 `Theme.kt` 集成新颜色系统
2. 保留向后兼容，同时支持新旧访问方式

### 阶段3：迁移现有代码

1. 逐步替换 `MaterialTheme.colorScheme.xxx` 为 `LocalAppColors.current.xxx`
2. 替换硬编码颜色值（Color.Transparent、Color.Black、Color.White 等系统颜色可保留）
3. 清理 `colors.xml` 中未使用的颜色

### 阶段4：清理和优化

1. 移除旧的颜色定义
2. 更新文档和注释

### 向后兼容策略

在迁移期间，提供扩展函数保持兼容：

```kotlin
// 扩展函数，让现有代码继续工作
val MaterialTheme.appColors: AppColors
    @Composable
    get() = LocalAppColors.current
```

## 错误处理

### 颜色缺失处理

- 如果某个颜色未定义，使用 fallback 颜色（primary）
- 在开发阶段添加日志警告

### 主题切换处理

- 确保深色/浅色模式切换时颜色正确更新
- 处理系统主题变化事件

## 测试策略

### 单元测试

- 测试 `AppColors` 数据类的创建和属性访问
- 测试 `AppColorPalette` 浅色/深色主题定义是否完整
- 测试颜色转换函数（如 `toMaterialColorScheme()`）

### UI 测试

- 测试主题切换功能
- 测试颜色在不同组件中的显示效果
- 测试深色模式下的对比度是否符合无障碍标准

## 文档和工具

### 颜色文档

- 生成颜色预览文档（可选）
- 在代码中添加颜色用途注释

### 开发工具

- 提供颜色预览 Composable（仅 Debug 模式）
- 添加主题切换调试按钮

## 性能考虑

- `AppColors` 数据类使用 `@Immutable` 注解，优化重组性能
- `CompositionLocal` 只在颜色变化时触发重组
- 避免在循环中频繁访问 `LocalAppColors.current`

## 成功标准

1. 所有颜色定义集中在 `AppColorPalette` 中
2. 代码中不再有硬编码颜色值（除了 Color.Transparent 等系统颜色）
3. 支持深色/浅色模式切换
4. 颜色命名符合语义化规范
5. 现有功能不受影响，UI 保持一致