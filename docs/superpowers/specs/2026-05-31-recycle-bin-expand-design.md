# 回收站扩展设计：支持账单和日程

## 概述

当前回收站仅支持笔记（软删除机制）。账单和日程删除后是永久删除，无法恢复。本次改动将回收站扩展为支持三种数据类型：笔记、账单、日程。

## 设计方案

采用方案 A：沿用现有软删除模式，给 BillEntity 和 ScheduleEntity 各加 `isDeleted` / `deletedAt` 字段，与笔记完全一致。

## 数据层改动

### Entity 改动

**BillEntity** 新增字段：
- `isDeleted: Boolean = false`
- `deletedAt: Long? = null`

**ScheduleEntity** 新增字段：
- `isDeleted: Boolean = false`
- `deletedAt: Long? = null`

### Room Migration

两个表各需要一次 Migration（ALTER TABLE ADD COLUMN），数据库版本号升一级。

### DAO 改动

每个 DAO 新增 4 个方法：

**BillDao：**
- `softDeleteBill(id: Long, deletedAt: Long)` — UPDATE bills SET isDeleted=1, deletedAt=:deletedAt
- `restoreBill(id: Long)` — UPDATE bills SET isDeleted=0, deletedAt=NULL
- `getAllDeletedBills(): Flow<List<BillEntity>>` — SELECT WHERE isDeleted=1 ORDER BY deletedAt DESC
- `deleteExpiredTrashBills(expireTime: Long)` — DELETE WHERE isDeleted=1 AND deletedAt < expireTime

**ScheduleDao：**
- `softDeleteSchedule(id: Long, deletedAt: Long)` — UPDATE schedules SET isDeleted=1, deletedAt=:deletedAt
- `restoreSchedule(id: Long)` — UPDATE schedules SET isDeleted=0, deletedAt=NULL
- `getAllDeletedSchedules(): Flow<List<ScheduleEntity>>` — SELECT WHERE isDeleted=1 ORDER BY deletedAt DESC
- `deleteExpiredTrashSchedules(expireTime: Long)` — DELETE WHERE isDeleted=1 AND deletedAt < expireTime

### Repository 改动

BillRepository 和 ScheduleRepository 各新增：
- `softDelete(entity)` — 调用 DAO 的 softDelete
- `restore(entity)` — 调用 DAO 的 restore
- `permanentlyDelete(entity)` — 调用 DAO 的硬删除
- `getAllDeleted()` — 调用 DAO 的 getAllDeleted

## ViewModel 改动

TrashViewModel 扩展：

- 引入 BillRepository 和 ScheduleRepository
- 定义 TrashItem sealed class：

```kotlin
sealed class TrashItem {
    data class TrashNote(val data: NoteWithTags) : TrashItem()
    data class TrashBill(val data: BillEntity) : TrashItem()
    data class TrashSchedule(val data: ScheduleEntity) : TrashItem()
}
```

- 合并三种数据流，按 deletedAt 降序排列，输出 `StateFlow<List<TrashItem>>`
- `restore(item: TrashItem)` — 根据类型分发到对应 Repository
- `permanentlyDelete(item: TrashItem)` — 根据类型分发到对应 Repository

## UI 改动（TrashScreen）

混合列表按删除时间排序，每种类型用不同卡片样式区分：

**笔记卡片**（保持现有样式）：
- 标题 + 剩余天数标签 + 更新时间 + 恢复/永久删除按钮

**账单卡片**：
- 金额（醒目显示）+ 消费项目 + 剩余天数标签 + 删除时间 + 恢复/永久删除按钮

**日程卡片**：
- 标题 + 日期范围 + 地点（如有）+ 剩余天数标签 + 恢复/永久删除按钮

每种卡片标题旁用小图标区分类型：笔记用 Description，账单用 Receipt，日程用 Event。

## 删除逻辑改动

### 删除触发点

| 页面 | 当前行为 | 改为 |
|------|---------|------|
| BillScreen 删除账单 | `BillRepository.deleteBill()` (硬删除) | `BillRepository.softDelete()` |
| ScheduleScreen 删除日程 | `ScheduleRepository.deleteSchedule()` (硬删除) | `ScheduleRepository.softDelete()` |
| EditNoteViewModel 空笔记 | 硬删除（不走回收站） | 保持不变 |

### 自动清理

TrashCleanupReceiver 扩展：
- 现有：`noteDao.deleteExpiredTrashNotes(expireTime)`
- 新增：`billDao.deleteExpiredTrashBills(expireTime)` + `scheduleDao.deleteExpiredTrashSchedules(expireTime)`

统一 30 天过期策略。

### 永久删除确认

账单和日程复用笔记现有的确认弹窗模式。

## 影响范围

改动文件清单：
- `BillEntity.kt` — 加字段
- `ScheduleEntity.kt` — 加字段
- `NoteDatabase.kt` — Migration + 版本升级
- `BillDao.kt` — 加 4 个方法
- `ScheduleDao.kt` — 加 4 个方法
- `BillRepository.kt` — 加 4 个方法
- `ScheduleRepository.kt` — 加 4 个方法
- `TrashViewModel.kt` — 引入新 Repository + TrashItem sealed class + 合并数据流
- `TrashScreen.kt` — 混合列表渲染三种卡片
- `TrashCleanupReceiver.kt` — 加 bills 和 schedules 清理
- `BillScreen.kt` — 删除调用改为 softDelete
- `ScheduleScreen.kt` — 删除调用改为 softDelete
