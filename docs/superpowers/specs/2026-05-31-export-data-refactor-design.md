# 导出数据 UI 重构与功能扩展设计

## 概述

重构 `ExportDataScreen`，将现有简陋的双按钮布局改为卡片式 UI，新增日程导出支持（iCal + CSV），并为所有数据类型添加筛选功能。

## 方案选择

采用方案 A（最小改动）：在现有文件基础上重构 UI，导出逻辑提取到独立工具类，不引入 ViewModel，保持与现有代码风格一致。

## 页面布局

页面结构为三张卡片纵向排列：

- **笔记卡片**：图标 + 名称 + 格式描述（Markdown / TXT / PDF）+ 导出按钮
- **账单卡片**：图标 + 名称 + 格式描述（CSV）+ 导出按钮
- **日程卡片**：图标 + 名称 + 格式描述（iCal / CSV）+ 导出按钮

底部保留提示文字：`文件将保存到 Downloads/KeyNote/`

点击"导出"按钮弹出 `ModalBottomSheet` 进行筛选和格式选择。

## 筛选面板（BottomSheet）

### 笔记筛选

- 日期范围选择器（开始 ~ 结束，默认全部）
- 标签多选（Chip 形式，列出已有标签）
- 格式选择：Markdown / TXT / PDF（Radio）
- [确认导出] 按钮

### 账单筛选

- 日期范围选择器（默认全部）
- 分类多选（Chip 形式，列出已有分类）
- 格式：CSV（固定）
- [确认导出] 按钮

### 日程筛选

- 日期范围选择器（默认全部）
- 格式选择：iCal / CSV（Radio）
- [确认导出] 按钮

所有筛选面板预设默认值为"全部数据"，用户可直接点确认快速导出。

## 导出格式

### 笔记

| 格式 | 说明 | 文件名 |
|------|------|--------|
| Markdown | 每个笔记一个 `.md` 文件，含标题、标签、正文 | `{标题}-{创建时间}.md` |
| TXT | 纯文本，去掉 Markdown 语法 | `{标题}-{创建时间}.txt` |
| PDF | 使用 Android `PdfDocument` API，含标题、标签、正文 | `{标题}-{创建时间}.pdf` |

### 账单

| 格式 | 说明 | 文件名 |
|------|------|--------|
| CSV | 单文件，列为：消费项目、金额、时间 | `账单导出-{导出日期}.csv` |

### 日程

| 格式 | 说明 | 文件名 |
|------|------|--------|
| iCal | 标准 `.ics`，每个日程一个 VEVENT（SUMMARY/DTSTART/DTEND/LOCATION/DESCRIPTION） | `日程导出-{导出日期}.ics` |
| CSV | 列为：标题、开始时间、结束时间、地点、描述 | `日程导出-{导出日期}.csv` |

## 代码结构

### `ExportDataScreen.kt`（重构）

- 三张卡片 UI 组件
- 三个 BottomSheet（可抽取为复用组件 `ExportFilterSheet`，通过参数区分数据类型）
- 筛选状态用 `remember` / `mutableStateOf` 管理
- 导出过程显示 `CircularProgressIndicator`，完成后 Snackbar 提示

### `ExportHelper.kt`（新建）

从 `ExportDataScreen.kt` 提取所有导出逻辑：

- `exportNotes(context, startDate?, endDate?, selectedTags?, format)`
- `exportBills(context, startDate?, endDate?, selectedCategories?)`
- `exportSchedules(context, startDate?, endDate?, format)`
- `writeToDownloads(context, fileName, mimeType, content)`（复用）
- `generateIcs(schedules)` — iCal 格式生成
- `generatePdf(note)` — PDF 生成

### 依赖

- 复用现有 `NoteRepository`、`BillRepository`、`ScheduleRepository`
- 不新增 ViewModel、不新增 DAO

## 筛选逻辑

### 日期范围

- 通过 `ScheduleDao` / `NoteDao` / `BillDao` 已有的查询方法，或在 Repository 中添加带日期参数的查询
- 需要在 DAO 中新增带日期范围参数的查询方法（如 `getNotesByDateRange`、`getBillsByDateRange`、`getSchedulesByDateRange`）

### 标签筛选（笔记）

- 通过 `NoteDao` 的 `getNotesByTagId` 或类似方法
- 标签列表从 `TagDao.getAllTags()` 获取

### 分类筛选（账单）

- 通过 `BillDao` 的 `getBillsByCategoryId` 或类似方法
- 分类列表从 `CategoryDao.getAllCategories()` 获取

## 测试场景

1. **笔记导出**：选择日期范围 + 标签 → 分别导出 Markdown/TXT/PDF → 验证文件内容正确
2. **账单导出**：选择日期范围 + 分类 → 导出 CSV → 验证筛选结果和文件内容
3. **日程导出**：选择日期范围 → 分别导出 iCal/CSV → 验证文件格式和内容
4. **快速导出**：不修改任何筛选条件直接确认 → 验证导出全部数据
5. **导出状态**：导出中显示加载指示器 → 完成后 Snackbar 提示
6. **错误处理**：无数据时的提示、文件写入失败时的错误信息
