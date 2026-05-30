# 颜色系统重构设计文档

## 概述

重构 KeyNote 应用的整套颜色系统，移除主题色选择功能和 Android 原生动态颜色，将主题色固定为 `#0593ff`，统一所有背景颜色、组件颜色、侧边栏颜色和标题栏颜色。

## 设计目标

1. 移除主题色选择功能，简化用户体验
2. 移除 Android 原生动态颜色采集（`dynamicDarkColorScheme`/`dynamicLightColorScheme`）
3. 将主题色固定为 `#0593ff`（与 logo 钥匙颜色一致）
4. 统一浅色/深色模式的背景颜色
5. 适度使用主题色，保持界面简洁

## 颜色规范

### 主题色

- 种子色：`#0593FF`
- 使用 Material3 HCT 色彩系统自动生成完整配色方案
- 使用场景（适度使用）：
  - 选中状态指示器（侧边栏选中项、FilterChip 选中）
  - 主要按钮背景
  - 链接文字
  - 图标选中状态
  - 开关/滑块激活状态

### 浅色模式

| 颜色角色 | 值 | 用途 |
|----------|-----|------|
| background | `#FFFFFF` | 页面背景 |
| surface | `#FFFFFF` | 卡片、弹窗背景 |
| surfaceVariant | `#FFFFFF` | 输入框、组件背景 |
| onSurface | `#1C1B1F` | 主要文字 |
| onSurfaceVariant | `#49454F` | 次要文字 |
| outline | `#79747E` | 占位符、分割线 |
| primary | 由 HCT 生成 | 主题色相关组件 |

### 深色模式

| 颜色角色 | 值 | 用途 |
|----------|-----|------|
| background | `#1C1B1F` | 页面背景 |
| surface | `#1C1B1F` | 卡片、弹窗背景 |
| surfaceVariant | `#2B2930` | 输入框、组件背景 |
| onSurface | `#FFFFFF` | 主要文字 |
| onSurfaceVariant | `#CAC4D0` | 次要文字 |
| outline | `#938F99` | 占位符、分割线 |
| primary | 由 HCT 生成 | 主题色相关组件 |

### 中性色组件

| 组件 | 浅色模式 | 深色模式 |
|------|----------|----------|
| 未选中图标 | onSurfaceVariant | onSurfaceVariant |
| 未选中文字 | onSurface | onSurface |
| 分割线 | outlineVariant | outlineVariant |
| 占位符/次要文字 | outline | outline |
| 禁用状态 | outline.copy(alpha=0.38) | outline.copy(alpha=0.38) |

## 页面适配

### 侧边栏

- 背景色：与主背景一致（浅色纯白，深色深灰）
- 选中项：使用主题色
- 未选中项：使用中性色

### 标题栏（TopAppBar）

- 背景色：与主背景一致（浅色纯白，深色深灰）
- 标题文字：使用 onSurface
- 图标：使用 onSurfaceVariant

### AI 对话页面

**浅色模式：**
- 保持现有设计不变（白色渐变背景）

**深色模式：**
- 移除渐变效果，使用纯色背景 `#1C1B1F`
- 输入框背景：surfaceVariant (`#2B2930`)
- 发送按钮：使用主题色 `#0593ff`
- 欢迎图标/文字：使用对应的深色模式文字颜色

### 设置页面

**移除的内容：**
- 主题色选择区域（"主题色" 标题 + 11 个颜色选项 FilterChip）

**保留的内容：**
- 外观模式选择（系统默认/浅色/深色）
- 正文字体大小滑块
- AI 配置
- 厂商管理

## 技术实现

### 修改的文件

1. **Color.kt**
   - 移除所有种子色（PurpleSeed, RedSeed, GreenSeed 等）
   - 仅保留 `PrimarySeed = Color(0xFF0593FF)`

2. **ColorGenerator.kt**
   - 保持不变，继续使用 HCT 生成配色

3. **Theme.kt**
   - 移除 `colorSchemes` Map
   - 移除动态颜色（`dynamicDarkColorScheme`/`dynamicLightColorScheme`）
   - 固定使用 PrimarySeed 生成颜色方案
   - 简化 `KeyNoteTheme` 函数，移除 `themeColor` 参数

4. **SettingsScreen.kt**
   - 移除主题色选择区域
   - 移除 `chipColors` 和 `colorOptions` 相关代码

5. **SettingsViewModel.kt**
   - 移除 `themeColor` 相关逻辑
   - 移除 `setThemeColor()` 方法

6. **AIChatScreen.kt**
   - 深色模式移除渐变背景，使用纯色
   - 适配深色模式 UI 元素颜色

### 依赖关系

- 保留 `me.tatarka.google.material` 库用于 HCT 色彩生成
- 无新增依赖

## 验收标准

1. 设置页面不再显示主题色选择器
2. 应用全局使用固定的 `#0593ff` 主题色
3. 浅色模式背景为纯白
4. 深色模式背景为深灰
5. 侧边栏和标题栏背景与主背景一致
6. AI 对话页面深色模式无渐变效果
7. 所有文字在对应背景下清晰可读
8. 浅色/深色模式切换正常
