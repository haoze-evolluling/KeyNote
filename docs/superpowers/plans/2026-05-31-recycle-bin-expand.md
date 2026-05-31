# 回收站扩展实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将回收站从仅支持笔记扩展为支持笔记、账单、日程三种类型

**Architecture:** 沿用现有软删除模式，给 BillEntity 和 ScheduleEntity 各加 isDeleted/deletedAt 字段，扩展 DAO/Repository/ViewModel/UI 层

**Tech Stack:** Kotlin, Room, Jetpack Compose, StateFlow

---

## Task 1: Entity 改动 — 加 isDeleted/deletedAt 字段

**Files:**
- Modify: `app/src/main/java/com/haoze/keynote/data/db/entity/BillEntity.kt`
- Modify: `app/src/main/java/com/haoze/keynote/data/db/entity/ScheduleEntity.kt`

- [ ] **Step 1: 修改 BillEntity，加 isDeleted 和 deletedAt 字段**

```kotlin
package com.haoze.keynote.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bills",
    indices = [Index("categoryId")]
)
data class BillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val item: String,
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    val categoryId: Long? = null,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)
```

- [ ] **Step 2: 修改 ScheduleEntity，加 isDeleted 和 deletedAt 字段**

```kotlin
package com.haoze.keynote.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val date: Long,
    val endDate: Long? = null,
    val location: String? = null,
    val description: String? = null,
    val noteId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)
```

- [ ] **Step 3: 提交**

```bash
git add app/src/main/java/com/haoze/keynote/data/db/entity/BillEntity.kt app/src/main/java/com/haoze/keynote/data/db/entity/ScheduleEntity.kt
git commit -m "feat: 给 BillEntity 和 ScheduleEntity 加 isDeleted/deletedAt 字段"
```

---

## Task 2: Room Migration — 数据库版本升级

**Files:**
- Modify: `app/src/main/java/com/haoze/keynote/data/db/NoteDatabase.kt:21-126`

- [ ] **Step 1: 修改 NoteDatabase，版本号从 9 升到 10，加 MIGRATION_9_10**

在 `version = 9` 改为 `version = 10`。

在 `MIGRATION_8_9` 之后添加：

```kotlin
private val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE bills ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE bills ADD COLUMN deletedAt INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE schedules ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE schedules ADD COLUMN deletedAt INTEGER DEFAULT NULL")
    }
}
```

在 `.addMigrations(...)` 中追加 `MIGRATION_9_10`。

- [ ] **Step 2: 提交**

```bash
git add app/src/main/java/com/haoze/keynote/data/db/NoteDatabase.kt
git commit -m "feat: Room Migration 9->10，bills 和 schedules 加软删除字段"
```

---

## Task 3: DAO 改动 — BillDao 加软删除方法 + 现有查询加过滤

**Files:**
- Modify: `app/src/main/java/com/haoze/keynote/data/db/dao/BillDao.kt`

- [ ] **Step 1: 修改 BillDao**

需要做两件事：
1. 所有现有查询（获取活跃数据的）加 `WHERE isDeleted = 0` 或 `AND isDeleted = 0`
2. 新增 4 个软删除方法

具体改动：

`getAllBills()` 改为：
```kotlin
@Query("SELECT * FROM bills WHERE isDeleted = 0 ORDER BY date DESC")
fun getAllBills(): Flow<List<BillEntity>>
```

`getBillsWithCategory()` 改为：
```kotlin
@Query("""
    SELECT b.id as bill_id, b.item as bill_item, b.amount as bill_amount, b.date as bill_date, b.categoryId as bill_categoryId,
           c.id as cat_id, c.name as cat_name, c.isDefault as cat_isDefault
    FROM bills b LEFT JOIN categories c ON b.categoryId = c.id
    WHERE b.isDeleted = 0
    ORDER BY b.date DESC
""")
fun getBillsWithCategory(): Flow<List<BillWithCategoryRaw>>
```

`getTotalSpending()` 改为：
```kotlin
@Query("SELECT SUM(amount) FROM bills WHERE isDeleted = 0")
fun getTotalSpending(): Flow<Double?>
```

`getMonthlySpending()` 改为：
```kotlin
@Query("SELECT SUM(amount) FROM bills WHERE isDeleted = 0 AND date >= :startOfMonth")
fun getMonthlySpending(startOfMonth: Long): Flow<Double?>
```

`getYearlySpending()` 改为：
```kotlin
@Query("SELECT SUM(amount) FROM bills WHERE isDeleted = 0 AND date >= :startOfYear")
fun getYearlySpending(startOfYear: Long): Flow<Double?>
```

`getBillCount()` 改为：
```kotlin
@Query("SELECT COUNT(*) FROM bills WHERE isDeleted = 0")
fun getBillCount(): Flow<Int>
```

`getSpendingByCategory()` 改为：
```kotlin
@Query("""
    SELECT categoryId,
           COALESCE(c.name, '未分类') as categoryName,
           SUM(b.amount) as total
    FROM bills b LEFT JOIN categories c ON b.categoryId = c.id
    WHERE b.isDeleted = 0 AND b.date BETWEEN :start AND :end
    GROUP BY categoryId
""")
fun getSpendingByCategory(start: Long, end: Long): Flow<List<CategorySpending>>
```

`getMonthlySpendingTrend()` 改为：
```kotlin
@Query("""
    SELECT strftime('%Y-%m', b.date / 1000, 'unixepoch', 'localtime') as month,
           SUM(b.amount) as total
    FROM bills b
    WHERE b.isDeleted = 0 AND b.date BETWEEN :start AND :end
    GROUP BY month
    ORDER BY month
""")
fun getMonthlySpendingTrend(start: Long, end: Long): Flow<List<MonthlySpending>>
```

`getSpendingInRange()` 改为：
```kotlin
@Query("SELECT SUM(amount) FROM bills WHERE isDeleted = 0 AND date BETWEEN :start AND :end")
fun getSpendingInRange(start: Long, end: Long): Flow<Double?>
```

`getBillCountInRange()` 改为：
```kotlin
@Query("SELECT COUNT(*) FROM bills WHERE isDeleted = 0 AND date BETWEEN :start AND :end")
fun getBillCountInRange(start: Long, end: Long): Flow<Int>
```

`getBillsByDateRange()` 改为：
```kotlin
@Query("SELECT * FROM bills WHERE isDeleted = 0 AND date BETWEEN :start AND :end ORDER BY date DESC")
fun getBillsByDateRange(start: Long, end: Long): Flow<List<BillEntity>>
```

`getBillsByDateRangeAndCategory()` 改为：
```kotlin
@Query("SELECT * FROM bills WHERE isDeleted = 0 AND date BETWEEN :start AND :end AND categoryId IN (:categoryIds) ORDER BY date DESC")
fun getBillsByDateRangeAndCategory(start: Long, end: Long, categoryIds: List<Long>): Flow<List<BillEntity>>
```

`getDailySpending()` 改为：
```kotlin
@Query("""
    SELECT strftime('%Y-%m-%d', b.date / 1000, 'unixepoch', 'localtime') as day,
           SUM(b.amount) as total
    FROM bills b
    WHERE b.isDeleted = 0 AND b.date BETWEEN :start AND :end
    GROUP BY day
    ORDER BY day
""")
fun getDailySpending(start: Long, end: Long): Flow<List<DailySpending>>
```

`getWeeklySpending()` 改为：
```kotlin
@Query("""
    SELECT strftime('%Y-W%W', b.date / 1000, 'unixepoch', 'localtime') as week,
           SUM(b.amount) as total
    FROM bills b
    WHERE b.isDeleted = 0 AND b.date BETWEEN :start AND :end
    GROUP BY week
    ORDER BY week
""")
fun getWeeklySpending(start: Long, end: Long): Flow<List<WeeklySpending>>
```

新增 4 个方法：
```kotlin
@Query("UPDATE bills SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
suspend fun softDeleteBill(id: Long, deletedAt: Long = System.currentTimeMillis())

@Query("UPDATE bills SET isDeleted = 0, deletedAt = NULL WHERE id = :id")
suspend fun restoreBill(id: Long)

@Query("SELECT * FROM bills WHERE isDeleted = 1 ORDER BY deletedAt DESC")
fun getAllDeletedBills(): Flow<List<BillEntity>>

@Query("DELETE FROM bills WHERE isDeleted = 1 AND deletedAt < :expireTime")
suspend fun deleteExpiredTrashBills(expireTime: Long)
```

- [ ] **Step 2: 提交**

```bash
git add app/src/main/java/com/haoze/keynote/data/db/dao/BillDao.kt
git commit -m "feat: BillDao 加软删除方法，现有查询加 isDeleted 过滤"
```

---

## Task 4: DAO 改动 — ScheduleDao 加软删除方法 + 现有查询加过滤

**Files:**
- Modify: `app/src/main/java/com/haoze/keynote/data/db/dao/ScheduleDao.kt`

- [ ] **Step 1: 修改 ScheduleDao**

`getAllSchedules()` 改为：
```kotlin
@Query("SELECT * FROM schedules WHERE isDeleted = 0 ORDER BY date ASC")
fun getAllSchedules(): Flow<List<ScheduleEntity>>
```

`getSchedulesByDateRange()` 改为：
```kotlin
@Query("SELECT * FROM schedules WHERE isDeleted = 0 AND date BETWEEN :start AND :end ORDER BY date ASC")
fun getSchedulesByDateRange(start: Long, end: Long): Flow<List<ScheduleEntity>>
```

新增 4 个方法：
```kotlin
@Query("UPDATE schedules SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
suspend fun softDeleteSchedule(id: Long, deletedAt: Long = System.currentTimeMillis())

@Query("UPDATE schedules SET isDeleted = 0, deletedAt = NULL WHERE id = :id")
suspend fun restoreSchedule(id: Long)

@Query("SELECT * FROM schedules WHERE isDeleted = 1 ORDER BY deletedAt DESC")
fun getAllDeletedSchedules(): Flow<List<ScheduleEntity>>

@Query("DELETE FROM schedules WHERE isDeleted = 1 AND deletedAt < :expireTime")
suspend fun deleteExpiredTrashSchedules(expireTime: Long)
```

- [ ] **Step 2: 提交**

```bash
git add app/src/main/java/com/haoze/keynote/data/db/dao/ScheduleDao.kt
git commit -m "feat: ScheduleDao 加软删除方法，现有查询加 isDeleted 过滤"
```

---

## Task 5: Repository 改动 — BillRepository 和 ScheduleRepository

**Files:**
- Modify: `app/src/main/java/com/haoze/keynote/data/repository/BillRepository.kt:29-30`
- Modify: `app/src/main/java/com/haoze/keynote/data/repository/ScheduleRepository.kt:19-20`

- [ ] **Step 1: 修改 BillRepository，替换 deleteBill 为 softDelete，加新方法**

将 `suspend fun deleteBill(bill: BillEntity) = billDao.deleteBill(bill)` 替换为：

```kotlin
suspend fun softDelete(bill: BillEntity) = billDao.softDeleteBill(bill.id)

suspend fun restore(bill: BillEntity) = billDao.restoreBill(bill.id)

suspend fun permanentlyDelete(bill: BillEntity) = billDao.deleteBill(bill)

fun getAllDeletedBills(): Flow<List<BillEntity>> = billDao.getAllDeletedBills()
```

- [ ] **Step 2: 修改 ScheduleRepository，替换 deleteSchedule 为 softDelete，加新方法**

将 `suspend fun deleteSchedule(schedule: ScheduleEntity) = scheduleDao.deleteSchedule(schedule)` 替换为：

```kotlin
suspend fun softDelete(schedule: ScheduleEntity) = scheduleDao.softDeleteSchedule(schedule.id)

suspend fun restore(schedule: ScheduleEntity) = scheduleDao.restoreSchedule(schedule.id)

suspend fun permanentlyDelete(schedule: ScheduleEntity) = scheduleDao.deleteSchedule(schedule)

fun getAllDeletedSchedules(): Flow<List<ScheduleEntity>> = scheduleDao.getAllDeletedSchedules()
```

- [ ] **Step 3: 提交**

```bash
git add app/src/main/java/com/haoze/keynote/data/repository/BillRepository.kt app/src/main/java/com/haoze/keynote/data/repository/ScheduleRepository.kt
git commit -m "feat: BillRepository/ScheduleRepository 加软删除和恢复方法"
```

---

## Task 6: ViewModel 层 — BillViewModel 和 ScheduleViewModel 删除调用改为软删除

**Files:**
- Modify: `app/src/main/java/com/haoze/keynote/ui/bill/BillViewModel.kt:36-38`
- Modify: `app/src/main/java/com/haoze/keynote/ui/schedule/ScheduleViewModel.kt:52-54`

- [ ] **Step 1: 修改 BillViewModel.deleteBill 调用 softDelete**

```kotlin
fun deleteBill(bill: BillEntity) {
    viewModelScope.launch { repository.softDelete(bill) }
}
```

- [ ] **Step 2: 修改 ScheduleViewModel.deleteSchedule 调用 softDelete**

```kotlin
fun deleteSchedule(schedule: ScheduleEntity) {
    viewModelScope.launch { scheduleRepository.softDelete(schedule) }
}
```

- [ ] **Step 3: 提交**

```bash
git add app/src/main/java/com/haoze/keynote/ui/bill/BillViewModel.kt app/src/main/java/com/haoze/keynote/ui/schedule/ScheduleViewModel.kt
git commit -m "feat: BillViewModel/ScheduleViewModel 删除改为软删除"
```

---

## Task 7: TrashViewModel — 扩展支持三种类型

**Files:**
- Modify: `app/src/main/java/com/haoze/keynote/ui/trash/TrashViewModel.kt`

- [ ] **Step 1: 重写 TrashViewModel**

```kotlin
package com.haoze.keynote.ui.trash

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.haoze.keynote.data.db.NoteDatabase
import com.haoze.keynote.data.db.entity.BillEntity
import com.haoze.keynote.data.db.entity.NoteWithTags
import com.haoze.keynote.data.db.entity.ScheduleEntity
import com.haoze.keynote.data.repository.BillRepository
import com.haoze.keynote.data.repository.NoteRepository
import com.haoze.keynote.data.repository.ScheduleRepository
import com.haoze.keynote.util.PreferencesManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class TrashItem {
    abstract val deletedAt: Long

    data class TrashNote(val data: NoteWithTags) : TrashItem() {
        override val deletedAt: Long get() = data.note.deletedAt ?: data.note.updatedAt
    }

    data class TrashBill(val data: BillEntity) : TrashItem() {
        override val deletedAt: Long get() = data.deletedAt ?: 0
    }

    data class TrashSchedule(val data: ScheduleEntity) : TrashItem() {
        override val deletedAt: Long get() = data.deletedAt ?: 0
    }
}

class TrashViewModel(application: Application) : AndroidViewModel(application) {

    private val noteRepository: NoteRepository
    private val billRepository: BillRepository
    private val scheduleRepository: ScheduleRepository

    private val _trashItems = MutableStateFlow<List<TrashItem>>(emptyList())
    val trashItems: StateFlow<List<TrashItem>> = _trashItems.asStateFlow()

    init {
        val db = NoteDatabase.getDatabase(application)
        noteRepository = NoteRepository(db.noteDao(), db.tagDao(), PreferencesManager(application))
        billRepository = BillRepository(db.billDao(), db.categoryDao())
        scheduleRepository = ScheduleRepository(db.scheduleDao())

        viewModelScope.launch {
            combine(
                noteRepository.getAllDeletedNotes(),
                billRepository.getAllDeletedBills(),
                scheduleRepository.getAllDeletedSchedules()
            ) { notes, bills, schedules ->
                val items = mutableListOf<TrashItem>()
                notes.mapTo(items) { TrashItem.TrashNote(it) }
                bills.mapTo(items) { TrashItem.TrashBill(it) }
                schedules.mapTo(items) { TrashItem.TrashSchedule(it) }
                items.sortedByDescending { it.deletedAt }
            }.collect { _trashItems.value = it }
        }
    }

    fun restore(item: TrashItem) {
        viewModelScope.launch {
            when (item) {
                is TrashItem.TrashNote -> noteRepository.restoreNote(item.data.note)
                is TrashItem.TrashBill -> billRepository.restore(item.data)
                is TrashItem.TrashSchedule -> scheduleRepository.restore(item.data)
            }
        }
    }

    fun permanentlyDelete(item: TrashItem) {
        viewModelScope.launch {
            when (item) {
                is TrashItem.TrashNote -> noteRepository.permanentlyDeleteNote(item.data.note)
                is TrashItem.TrashBill -> billRepository.permanentlyDelete(item.data)
                is TrashItem.TrashSchedule -> scheduleRepository.permanentlyDelete(item.data)
            }
        }
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add app/src/main/java/com/haoze/keynote/ui/trash/TrashViewModel.kt
git commit -m "feat: TrashViewModel 扩展支持账单和日程"
```

---

## Task 8: TrashScreen — 混合列表渲染三种卡片

**Files:**
- Modify: `app/src/main/java/com/haoze/keynote/ui/trash/TrashScreen.kt`

- [ ] **Step 1: 重写 TrashScreen**

```kotlin
package com.haoze.keynote.ui.trash

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.haoze.keynote.ui.theme.LocalAppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    drawerState: DrawerState,
    scope: CoroutineScope,
    viewModel: TrashViewModel = viewModel()
) {
    val colors = LocalAppColors.current
    val trashItems by viewModel.trashItems.collectAsState()
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("回收站") },
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Default.Menu, contentDescription = "菜单")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (trashItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "回收站为空",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.outline
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(trashItems, key = {
                    when (it) {
                        is TrashItem.TrashNote -> "note_${it.data.note.id}"
                        is TrashItem.TrashBill -> "bill_${it.data.id}"
                        is TrashItem.TrashSchedule -> "schedule_${it.data.id}"
                    }
                }) { item ->
                    val remainingDays = maxOf(0, 30 - TimeUnit.MILLISECONDS.toDays(
                        System.currentTimeMillis() - item.deletedAt
                    ).toInt())

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, colors.outlineVariant, CardDefaults.shape),
                        colors = CardDefaults.cardColors(containerColor = colors.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            when (item) {
                                is TrashItem.TrashNote -> NoteTrashContent(item, remainingDays, dateFormat)
                                is TrashItem.TrashBill -> BillTrashContent(item, remainingDays, dateFormat)
                                is TrashItem.TrashSchedule -> ScheduleTrashContent(item, remainingDays, dateFormat)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            TrashActionButtons(
                                onRestore = { viewModel.restore(item) },
                                onDelete = { viewModel.permanentlyDelete(item) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteTrashContent(item: TrashItem.TrashNote, remainingDays: Int, SimpleDateFormat: SimpleDateFormat) {
    val colors = LocalAppColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Default.Description,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = colors.outline
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = item.data.note.title.ifBlank { "无标题" },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(modifier = Modifier.width(8.dp))
        RemainingDaysBadge(remainingDays)
    }
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = SimpleDateFormat.format(Date(item.data.note.updatedAt)),
        style = MaterialTheme.typography.bodySmall,
        color = colors.outline
    )
}

@Composable
private fun BillTrashContent(item: TrashItem.TrashBill, remainingDays: Int, SimpleDateFormat: SimpleDateFormat) {
    val colors = LocalAppColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Default.Receipt,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = colors.outline
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "¥%.2f".format(item.data.amount),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = colors.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = item.data.item,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(modifier = Modifier.width(8.dp))
        RemainingDaysBadge(remainingDays)
    }
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = SimpleDateFormat.format(Date(item.data.date)),
        style = MaterialTheme.typography.bodySmall,
        color = colors.outline
    )
}

@Composable
private fun ScheduleTrashContent(item: TrashItem.TrashSchedule, remainingDays: Int, SimpleDateFormat: SimpleDateFormat) {
    val colors = LocalAppColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Default.Event,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = colors.outline
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = item.data.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(modifier = Modifier.width(8.dp))
        RemainingDaysBadge(remainingDays)
    }
    Spacer(modifier = Modifier.height(4.dp))
    val dateText = buildString {
        append(SimpleDateFormat.format(Date(item.data.date)))
        item.data.endDate?.let { append(" - ${SimpleDateFormat.format(Date(it))}") }
    }
    Text(
        text = dateText,
        style = MaterialTheme.typography.bodySmall,
        color = colors.outline
    )
    item.data.location?.let { location ->
        if (location.isNotBlank()) {
            Text(
                text = location,
                style = MaterialTheme.typography.bodySmall,
                color = colors.outline
            )
        }
    }
}

@Composable
private fun RemainingDaysBadge(remainingDays: Int) {
    val colors = LocalAppColors.current
    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = colors.errorContainer
    ) {
        Text(
            text = "${remainingDays}天",
            style = MaterialTheme.typography.labelSmall,
            color = colors.onErrorContainer,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun TrashActionButtons(onRestore: () -> Unit, onDelete: () -> Unit) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onRestore) {
            Icon(
                Icons.Default.Restore,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("恢复")
        }
        Spacer(modifier = Modifier.width(8.dp))
        TextButton(onClick = onDelete) {
            Icon(
                Icons.Default.DeleteForever,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = colors.error
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("永久删除", color = colors.error)
        }
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add app/src/main/java/com/haoze/keynote/ui/trash/TrashScreen.kt
git commit -m "feat: TrashScreen 支持混合列表展示笔记、账单、日程"
```

---

## Task 9: TrashCleanupReceiver — 扩展自动清理

**Files:**
- Modify: `app/src/main/java/com/haoze/keynote/receiver/TrashCleanupReceiver.kt`

- [ ] **Step 1: 修改 TrashCleanupReceiver，在 try 块中添加 bills 和 schedules 清理**

```kotlin
package com.haoze.keynote.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.haoze.keynote.data.db.NoteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class TrashCleanupReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = NoteDatabase.getDatabase(context)
                val expireTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
                db.noteDao().deleteExpiredTrashNotes(expireTime)
                db.billDao().deleteExpiredTrashBills(expireTime)
                db.scheduleDao().deleteExpiredTrashSchedules(expireTime)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add app/src/main/java/com/haoze/keynote/receiver/TrashCleanupReceiver.kt
git commit -m "feat: TrashCleanupReceiver 扩展清理账单和日程"
```

---

## Task 10: 编译验证

- [ ] **Step 1: 编译 debug APK**

Run: `.\gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 如果编译失败，修复错误并重新编译**

检查是否有遗漏的 `deleteBill` / `deleteSchedule` 调用点未改为 `softDelete`。

- [ ] **Step 3: 提交最终版本**

```bash
git add -A
git commit -m "feat: 回收站扩展支持账单和日程 v1.23.0"
```
