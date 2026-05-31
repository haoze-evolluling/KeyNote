# 导出数据 UI 重构与功能扩展 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 重构导出页面为卡片式 UI，新增日程导出（iCal + CSV），所有数据类型支持筛选

**Architecture:** 方案 A — 在现有 `ExportDataScreen.kt` 基础上重构 UI，导出逻辑提取到新建的 `ExportHelper.kt`，不引入 ViewModel

**Tech Stack:** Kotlin, Jetpack Compose (Material 3), Room, Android PdfDocument API

---

## 文件结构

| 操作 | 文件 | 职责 |
|------|------|------|
| 修改 | `app/src/main/java/com/haoze/keynote/data/db/dao/NoteDao.kt` | 新增日期范围查询 |
| 修改 | `app/src/main/java/com/haoze/keynote/data/db/dao/ScheduleDao.kt` | 新增日期范围查询 |
| 修改 | `app/src/main/java/com/haoze/keynote/data/repository/NoteRepository.kt` | 新增筛选导出方法 |
| 修改 | `app/src/main/java/com/haoze/keynote/data/repository/ScheduleRepository.kt` | 新增筛选导出方法 |
| 新建 | `app/src/main/java/com/haoze/keynote/util/ExportHelper.kt` | 所有导出逻辑（笔记/账单/日程） |
| 重构 | `app/src/main/java/com/haoze/keynote/ui/home/ExportDataScreen.kt` | 卡片式 UI + BottomSheet 筛选 |

---

### Task 1: 添加 DAO 日期范围查询

**Files:**
- Modify: `app/src/main/java/com/haoze/keynote/data/db/dao/NoteDao.kt`
- Modify: `app/src/main/java/com/haoze/keynote/data/db/dao/ScheduleDao.kt`

- [ ] **Step 1: NoteDao 添加日期范围查询**

在 `NoteDao.kt` 的 `getNotesByTagId` 方法之后添加：

```kotlin
@Transaction
@Query("""
    SELECT * FROM notes
    WHERE isDeleted = 0 AND createdAt BETWEEN :start AND :end
    ORDER BY createdAt DESC
""")
fun getActiveNotesByDateRange(start: Long, end: Long): Flow<List<NoteWithTags>>

@Transaction
@Query("""
    SELECT * FROM notes
    WHERE isDeleted = 0
    AND createdAt BETWEEN :start AND :end
    AND id IN (
        SELECT noteId FROM note_tag_cross_ref WHERE tagId IN (:tagIds)
    )
    ORDER BY createdAt DESC
""")
fun getActiveNotesByDateRangeAndTags(start: Long, end: Long, tagIds: List<Long>): Flow<List<NoteWithTags>>
```

- [ ] **Step 2: ScheduleDao 添加日期范围查询**

在 `ScheduleDao.kt` 的 `deleteSchedule` 方法之后添加：

```kotlin
@Query("SELECT * FROM schedules WHERE date BETWEEN :start AND :end ORDER BY date ASC")
fun getSchedulesByDateRange(start: Long, end: Long): Flow<List<ScheduleEntity>>
```

- [ ] **Step 3: 验证编译**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/haoze/keynote/data/db/dao/NoteDao.kt app/src/main/java/com/haoze/keynote/data/db/dao/ScheduleDao.kt
git commit -m "feat: add date range queries to NoteDao and ScheduleDao"
```

---

### Task 2: 添加 Repository 筛选方法

**Files:**
- Modify: `app/src/main/java/com/haoze/keynote/data/repository/NoteRepository.kt`
- Modify: `app/src/main/java/com/haoze/keynote/data/repository/ScheduleRepository.kt`
- Modify: `app/src/main/java/com/haoze/keynote/data/repository/BillRepository.kt`

- [ ] **Step 1: NoteRepository 添加筛选方法**

在 `NoteRepository.kt` 的 `getNotesByTagId` 方法之后添加：

```kotlin
fun getActiveNotesByDateRange(start: Long, end: Long): Flow<List<NoteWithTags>> =
    noteDao.getActiveNotesByDateRange(start, end)

fun getActiveNotesByDateRangeAndTags(start: Long, end: Long, tagIds: List<Long>): Flow<List<NoteWithTags>> =
    noteDao.getActiveNotesByDateRangeAndTags(start, end, tagIds)
```

- [ ] **Step 2: ScheduleRepository 添加筛选方法**

在 `ScheduleRepository.kt` 的 `deleteSchedule` 方法之后添加：

```kotlin
fun getSchedulesByDateRange(start: Long, end: Long): Flow<List<ScheduleEntity>> =
    scheduleDao.getSchedulesByDateRange(start, end)

suspend fun getAllSchedulesList(): List<ScheduleEntity> {
    var result = emptyList<ScheduleEntity>()
    scheduleDao.getAllSchedules().first().let { result = it }
    return result
}

suspend fun getSchedulesByDateRangeList(start: Long, end: Long): List<ScheduleEntity> {
    var result = emptyList<ScheduleEntity>()
    scheduleDao.getSchedulesByDateRange(start, end).first().let { result = it }
    return result
}
```

需要在文件顶部添加 import：`import kotlinx.coroutines.flow.first`

- [ ] **Step 3: BillRepository 添加筛选方法**

在 `BillRepository.kt` 的 `getAllBillsList` 方法之后添加：

```kotlin
suspend fun getBillsByDateRange(start: Long, end: Long): List<BillEntity> {
    var result = emptyList<BillEntity>()
    billDao.getBillsByDateRange(start, end).first().let { result = it }
    return result
}

suspend fun getBillsByDateRangeAndCategory(start: Long, end: Long, categoryIds: List<Long>): List<BillEntity> {
    var result = emptyList<BillEntity>()
    billDao.getBillsByDateRangeAndCategory(start, end, categoryIds).first().let { result = it }
    return result
}
```

- [ ] **Step 4: BillDao 添加对应查询**

在 `BillDao.kt` 的 `getBillCountInRange` 方法之后添加：

```kotlin
@Query("SELECT * FROM bills WHERE date BETWEEN :start AND :end ORDER BY date DESC")
fun getBillsByDateRange(start: Long, end: Long): Flow<List<BillEntity>>

@Query("SELECT * FROM bills WHERE date BETWEEN :start AND :end AND categoryId IN (:categoryIds) ORDER BY date DESC")
fun getBillsByDateRangeAndCategory(start: Long, end: Long, categoryIds: List<Long>): Flow<List<BillEntity>>
```

需要在 `BillRepository.kt` 顶部添加 import：`import kotlinx.coroutines.flow.first`

- [ ] **Step 5: 验证编译**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/haoze/keynote/data/db/dao/BillDao.kt app/src/main/java/com/haoze/keynote/data/repository/NoteRepository.kt app/src/main/java/com/haoze/keynote/data/repository/ScheduleRepository.kt app/src/main/java/com/haoze/keynote/data/repository/BillRepository.kt
git commit -m "feat: add filtered query methods to repositories"
```

---

### Task 3: 创建 ExportHelper.kt

**Files:**
- Create: `app/src/main/java/com/haoze/keynote/util/ExportHelper.kt`

- [ ] **Step 1: 创建 ExportHelper.kt 基础结构**

```kotlin
package com.haoze.keynote.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.provider.MediaStore
import com.haoze.keynote.data.db.NoteDatabase
import com.haoze.keynote.data.db.entity.BillEntity
import com.haoze.keynote.data.db.entity.NoteWithTags
import com.haoze.keynote.data.db.entity.ScheduleEntity
import com.haoze.keynote.data.repository.BillRepository
import com.haoze.keynote.data.repository.NoteRepository
import com.haoze.keynote.data.repository.ScheduleRepository
import kotlinx.coroutines.flow.first
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportHelper {

    fun writeToDownloads(context: Context, fileName: String, mimeType: String, content: ByteArray) {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/KeyNote")
        }
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw Exception("无法创建文件: $fileName")
        context.contentResolver.openOutputStream(uri)?.use { it.write(content) }
            ?: throw Exception("无法写入文件: $fileName")
    }

    // ========== 笔记导出 ==========

    enum class NoteExportFormat { MARKDOWN, TXT, PDF }

    suspend fun exportNotes(
        context: Context,
        startDate: Long? = null,
        endDate: Long? = null,
        tagIds: List<Long>? = null,
        format: NoteExportFormat = NoteExportFormat.MARKDOWN
    ): Int {
        val db = NoteDatabase.getDatabase(context)
        val repository = NoteRepository(db.noteDao(), db.tagDao(), PreferencesManager(context))

        val notes: List<NoteWithTags> = when {
            startDate != null && endDate != null && !tagIds.isNullOrEmpty() ->
                repository.getActiveNotesByDateRangeAndTags(startDate, endDate, tagIds).first()
            startDate != null && endDate != null ->
                repository.getActiveNotesByDateRange(startDate, endDate).first()
            else ->
                repository.getAllActiveNotesWithTags().first()
        }

        if (notes.isEmpty()) throw Exception("没有符合条件的笔记")

        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault())
        notes.forEach { noteWithTags ->
            val safeTitle = noteWithTags.note.title
                .ifBlank { "无标题" }
                .replace(Regex("[/\\\\:*?\"<>|]"), "_")
            val timestamp = dateFormat.format(Date(noteWithTags.note.createdAt))

            when (format) {
                NoteExportFormat.MARKDOWN -> {
                    val fileName = "$safeTitle-$timestamp.md"
                    val content = buildMarkdown(noteWithTags)
                    writeToDownloads(context, fileName, "text/markdown", content.toByteArray(Charsets.UTF_8))
                }
                NoteExportFormat.TXT -> {
                    val fileName = "$safeTitle-$timestamp.txt"
                    val content = buildTxt(noteWithTags)
                    writeToDownloads(context, fileName, "text/plain", content.toByteArray(Charsets.UTF_8))
                }
                NoteExportFormat.PDF -> {
                    val fileName = "$safeTitle-$timestamp.pdf"
                    val content = generatePdf(noteWithTags)
                    writeToDownloads(context, fileName, "application/pdf", content)
                }
            }
        }
        return notes.size
    }

    private fun buildMarkdown(noteWithTags: NoteWithTags): String = buildString {
        appendLine("# ${noteWithTags.note.title}")
        appendLine()
        if (noteWithTags.tags.isNotEmpty()) {
            appendLine("标签: ${noteWithTags.tags.joinToString(" ") { "#${it.name}" }}")
            appendLine()
        }
        appendLine(noteWithTags.note.content)
    }

    private fun buildTxt(noteWithTags: NoteWithTags): String = buildString {
        appendLine(noteWithTags.note.title)
        appendLine()
        if (noteWithTags.tags.isNotEmpty()) {
            appendLine("标签: ${noteWithTags.tags.joinToString(", ") { it.name }}")
            appendLine()
        }
        appendLine(noteWithTags.note.content)
    }

    private fun generatePdf(noteWithTags: NoteWithTags): ByteArray {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
        var page = document.startPage(pageInfo)
        var canvas: Canvas = page.canvas
        val paint = Paint().apply {
            textSize = 12f
            isAntiAlias = true
        }
        val titlePaint = Paint().apply {
            textSize = 18f
            isAntiAlias = true
            isFakeBoldText = true
        }
        val tagPaint = Paint().apply {
            textSize = 10f
            isAntiAlias = true
            color = android.graphics.Color.GRAY
        }

        var y = 40f
        val margin = 40f
        val maxWidth = 595f - margin * 2

        // Title
        canvas.drawText(noteWithTags.note.title, margin, y, titlePaint)
        y += 30f

        // Tags
        if (noteWithTags.tags.isNotEmpty()) {
            val tagText = "标签: ${noteWithTags.tags.joinToString(", ") { it.name }}"
            canvas.drawText(tagText, margin, y, tagPaint)
            y += 20f
        }
        y += 10f

        // Content - split by lines and wrap
        val lines = noteWithTags.note.content.split("\n")
        for (line in lines) {
            if (y > 800f) {
                document.finishPage(page)
                val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, document.pages.size + 1).create()
                page = document.startPage(newPageInfo)
                canvas = page.canvas
                y = 40f
            }
            if (line.isBlank()) {
                y += 14f
                continue
            }
            // Simple word wrap
            val chars = line.toList()
            var currentLine = ""
            for (ch in chars) {
                val testLine = currentLine + ch
                if (paint.measureText(testLine) > maxWidth) {
                    canvas.drawText(currentLine, margin, y, paint)
                    y += 16f
                    currentLine = ch.toString()
                    if (y > 800f) {
                        document.finishPage(page)
                        val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, document.pages.size + 1).create()
                        page = document.startPage(newPageInfo)
                        canvas = page.canvas
                        y = 40f
                    }
                } else {
                    currentLine = testLine
                }
            }
            if (currentLine.isNotBlank()) {
                canvas.drawText(currentLine, margin, y, paint)
                y += 16f
            }
        }

        document.finishPage(page)
        val outputStream = ByteArrayOutputStream()
        document.writeTo(outputStream)
        document.close()
        return outputStream.toByteArray()
    }

    // ========== 账单导出 ==========

    suspend fun exportBills(
        context: Context,
        startDate: Long? = null,
        endDate: Long? = null,
        categoryIds: List<Long>? = null
    ): Int {
        val db = NoteDatabase.getDatabase(context)
        val repository = BillRepository(db.billDao(), db.categoryDao())

        val bills: List<BillEntity> = when {
            startDate != null && endDate != null && !categoryIds.isNullOrEmpty() ->
                repository.getBillsByDateRangeAndCategory(startDate, endDate, categoryIds)
            startDate != null && endDate != null ->
                repository.getBillsByDateRange(startDate, endDate)
            else ->
                repository.getAllBillsList()
        }

        if (bills.isEmpty()) throw Exception("没有符合条件的账单")

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val csvContent = buildString {
            appendLine("消费项目,金额,时间")
            bills.forEach { bill ->
                val item = bill.item.replace("\"", "\"\"")
                appendLine("\"${item}\",${bill.amount},${dateFormat.format(Date(bill.date))}")
            }
        }

        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val fileName = "账单导出-$dateStr.csv"
        writeToDownloads(context, fileName, "text/csv", csvContent.toByteArray(Charsets.UTF_8))
        return bills.size
    }

    // ========== 日程导出 ==========

    enum class ScheduleExportFormat { ICS, CSV }

    suspend fun exportSchedules(
        context: Context,
        startDate: Long? = null,
        endDate: Long? = null,
        format: ScheduleExportFormat = ScheduleExportFormat.ICS
    ): Int {
        val db = NoteDatabase.getDatabase(context)
        val repository = ScheduleRepository(db.scheduleDao())

        val schedules: List<ScheduleEntity> = when {
            startDate != null && endDate != null ->
                repository.getSchedulesByDateRangeList(startDate, endDate)
            else ->
                repository.getAllSchedulesList()
        }

        if (schedules.isEmpty()) throw Exception("没有符合条件的日程")

        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        when (format) {
            ScheduleExportFormat.ICS -> {
                val fileName = "日程导出-$dateStr.ics"
                val content = generateIcs(schedules)
                writeToDownloads(context, fileName, "text/calendar", content.toByteArray(Charsets.UTF_8))
            }
            ScheduleExportFormat.CSV -> {
                val fileName = "日程导出-$dateStr.csv"
                val content = generateScheduleCsv(schedules)
                writeToDownloads(context, fileName, "text/csv", content.toByteArray(Charsets.UTF_8))
            }
        }
        return schedules.size
    }

    private fun generateIcs(schedules: List<ScheduleEntity>): String = buildString {
        val dateTimeFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.US)
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.US)
        appendLine("BEGIN:VCALENDAR")
        appendLine("VERSION:2.0")
        appendLine("PRODID:-//KeyNote//Export//CN")
        appendLine("CALSCALE:GREGORIAN")
        schedules.forEach { schedule ->
            appendLine("BEGIN:VEVENT")
            appendLine("DTSTART:${dateTimeFormat.format(Date(schedule.date))}")
            if (schedule.endDate != null) {
                appendLine("DTEND:${dateTimeFormat.format(Date(schedule.endDate))}")
            }
            appendLine("SUMMARY:${escapeIcs(schedule.title)}")
            if (!schedule.location.isNullOrBlank()) {
                appendLine("LOCATION:${escapeIcs(schedule.location)}")
            }
            if (!schedule.description.isNullOrBlank()) {
                appendLine("DESCRIPTION:${escapeIcs(schedule.description)}")
            }
            appendLine("UID:keynote-${schedule.id}@haoze")
            appendLine("DTSTAMP:${dateTimeFormat.format(Date())}")
            appendLine("END:VEVENT")
        }
        appendLine("END:VCALENDAR")
    }

    private fun escapeIcs(text: String): String {
        return text.replace("\\", "\\\\")
            .replace(",", "\\,")
            .replace(";", "\\;")
            .replace("\n", "\\n")
    }

    private fun generateScheduleCsv(schedules: List<ScheduleEntity>): String = buildString {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        appendLine("标题,开始时间,结束时间,地点,描述")
        schedules.forEach { schedule ->
            val title = schedule.title.replace("\"", "\"\"")
            val start = dateFormat.format(Date(schedule.date))
            val end = if (schedule.endDate != null) dateFormat.format(Date(schedule.endDate)) else ""
            val location = (schedule.location ?: "").replace("\"", "\"\"")
            val desc = (schedule.description ?: "").replace("\"", "\"\"")
            appendLine("\"$title\",\"$start\",\"$end\",\"$location\",\"$desc\"")
        }
    }
}
```

- [ ] **Step 2: 验证编译**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/haoze/keynote/util/ExportHelper.kt
git commit -m "feat: add ExportHelper with notes/bills/schedules export logic"
```

---

### Task 4: 重构 ExportDataScreen.kt UI

**Files:**
- Refactor: `app/src/main/java/com/haoze/keynote/ui/home/ExportDataScreen.kt`

- [ ] **Step 1: 重写 ExportDataScreen.kt**

完整替换 `ExportDataScreen.kt` 内容：

```kotlin
package com.haoze.keynote.ui.home

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.haoze.keynote.data.db.NoteDatabase
import com.haoze.keynote.data.db.entity.CategoryEntity
import com.haoze.keynote.data.db.entity.TagEntity
import com.haoze.keynote.util.ExportHelper
import com.haoze.keynote.util.ExportHelper.NoteExportFormat
import com.haoze.keynote.util.ExportHelper.ScheduleExportFormat
import com.haoze.keynote.ui.theme.LocalAppColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportDataScreen(
    drawerState: DrawerState,
    drawerScope: CoroutineScope,
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isExporting by remember { mutableStateOf(false) }

    var showNoteSheet by remember { mutableStateOf(false) }
    var showBillSheet by remember { mutableStateOf(false) }
    var showScheduleSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("导出数据") },
                navigationIcon = {
                    IconButton(onClick = { drawerScope.launch { drawerState.open() } }) {
                        Icon(Icons.Default.Menu, contentDescription = "菜单")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ExportCard(
                icon = Icons.Default.Edit,
                title = "笔记",
                description = "导出为 Markdown / TXT / PDF",
                onClick = { showNoteSheet = true },
                enabled = !isExporting
            )

            ExportCard(
                icon = Icons.Default.AccountBalance,
                title = "账单",
                description = "导出为 CSV",
                onClick = { showBillSheet = true },
                enabled = !isExporting
            )

            ExportCard(
                icon = Icons.Default.CalendarMonth,
                title = "日程",
                description = "导出为 iCal / CSV",
                onClick = { showScheduleSheet = true },
                enabled = !isExporting
            )

            if (isExporting) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            Text(
                "文件将保存到 Downloads/KeyNote/",
                style = MaterialTheme.typography.bodySmall,
                color = colors.outline
            )
        }
    }

    // 笔记筛选 BottomSheet
    if (showNoteSheet) {
        NoteExportSheet(
            context = context,
            onDismiss = { showNoteSheet = false },
            onExport = { startDate, endDate, tagIds, format ->
                showNoteSheet = false
                scope.launch {
                    isExporting = true
                    try {
                        val count = ExportHelper.exportNotes(context, startDate, endDate, tagIds, format)
                        snackbarHostState.showSnackbar("成功导出 ${count} 篇笔记")
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("导出失败: ${e.message}")
                    } finally {
                        isExporting = false
                    }
                }
            }
        )
    }

    // 账单筛选 BottomSheet
    if (showBillSheet) {
        BillExportSheet(
            context = context,
            onDismiss = { showBillSheet = false },
            onExport = { startDate, endDate, categoryIds ->
                showBillSheet = false
                scope.launch {
                    isExporting = true
                    try {
                        val count = ExportHelper.exportBills(context, startDate, endDate, categoryIds)
                        snackbarHostState.showSnackbar("成功导出 ${count} 条账单")
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("导出失败: ${e.message}")
                    } finally {
                        isExporting = false
                    }
                }
            }
        )
    }

    // 日程筛选 BottomSheet
    if (showScheduleSheet) {
        ScheduleExportSheet(
            context = context,
            onDismiss = { showScheduleSheet = false },
            onExport = { startDate, endDate, format ->
                showScheduleSheet = false
                scope.launch {
                    isExporting = true
                    try {
                        val count = ExportHelper.exportSchedules(context, startDate, endDate, format)
                        snackbarHostState.showSnackbar("成功导出 ${count} 个日程")
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("导出失败: ${e.message}")
                    } finally {
                        isExporting = false
                    }
                }
            }
        )
    }
}

@Composable
private fun ExportCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        enabled = enabled
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(description, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Default.Download, contentDescription = "导出")
        }
    }
}

// ========== 笔记筛选面板 ==========

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteExportSheet(
    context: Context,
    onDismiss: () -> Unit,
    onExport: (startDate: Long?, endDate: Long?, tagIds: List<Long>?, format: NoteExportFormat) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var startDate by remember { mutableStateOf<Long?>(null) }
    var endDate by remember { mutableStateOf<Long?>(null) }
    var selectedTagIds by remember { mutableStateOf(setOf<Long>()) }
    var selectedFormat by remember { mutableStateOf(NoteExportFormat.MARKDOWN) }
    val tags = remember { mutableStateListOf<TagEntity>() }

    LaunchedEffect(Unit) {
        val db = NoteDatabase.getDatabase(context)
        tags.clear()
        tags.addAll(db.tagDao().getActiveTags().first())
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("导出笔记", style = MaterialTheme.typography.titleLarge)

            // 日期范围
            DateRangeSelector(
                startDate = startDate,
                endDate = endDate,
                onStartChange = { startDate = it },
                onEndChange = { endDate = it }
            )

            // 标签多选
            if (tags.isNotEmpty()) {
                Text("标签筛选", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    tags.forEach { tag ->
                        FilterChip(
                            selected = tag.id in selectedTagIds,
                            onClick = {
                                selectedTagIds = if (tag.id in selectedTagIds) {
                                    selectedTagIds - tag.id
                                } else {
                                    selectedTagIds + tag.id
                                }
                            },
                            label = { Text(tag.name) }
                        )
                    }
                }
            }

            // 格式选择
            Text("导出格式", style = MaterialTheme.typography.titleSmall)
            NoteExportFormat.entries.forEach { format ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = selectedFormat == format,
                        onClick = { selectedFormat = format }
                    )
                    Text(
                        when (format) {
                            NoteExportFormat.MARKDOWN -> "Markdown"
                            NoteExportFormat.TXT -> "纯文本 (TXT)"
                            NoteExportFormat.PDF -> "PDF"
                        }
                    )
                }
            }

            Button(
                onClick = {
                    onExport(
                        startDate,
                        endDate,
                        if (selectedTagIds.isEmpty()) null else selectedTagIds.toList(),
                        selectedFormat
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("确认导出")
            }
        }
    }
}

// ========== 账单筛选面板 ==========

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BillExportSheet(
    context: Context,
    onDismiss: () -> Unit,
    onExport: (startDate: Long?, endDate: Long?, categoryIds: List<Long>?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var startDate by remember { mutableStateOf<Long?>(null) }
    var endDate by remember { mutableStateOf<Long?>(null) }
    var selectedCategoryIds by remember { mutableStateOf(setOf<Long>()) }
    val categories = remember { mutableStateListOf<CategoryEntity>() }

    LaunchedEffect(Unit) {
        val db = NoteDatabase.getDatabase(context)
        categories.clear()
        categories.addAll(db.categoryDao().getAllCategories().first())
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("导出账单", style = MaterialTheme.typography.titleLarge)

            DateRangeSelector(
                startDate = startDate,
                endDate = endDate,
                onStartChange = { startDate = it },
                onEndChange = { endDate = it }
            )

            if (categories.isNotEmpty()) {
                Text("分类筛选", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.forEach { category ->
                        FilterChip(
                            selected = category.id in selectedCategoryIds,
                            onClick = {
                                selectedCategoryIds = if (category.id in selectedCategoryIds) {
                                    selectedCategoryIds - category.id
                                } else {
                                    selectedCategoryIds + category.id
                                }
                            },
                            label = { Text(category.name) }
                        )
                    }
                }
            }

            Button(
                onClick = {
                    onExport(
                        startDate,
                        endDate,
                        if (selectedCategoryIds.isEmpty()) null else selectedCategoryIds.toList()
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("确认导出")
            }
        }
    }
}

// ========== 日程筛选面板 ==========

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleExportSheet(
    context: Context,
    onDismiss: () -> Unit,
    onExport: (startDate: Long?, endDate: Long?, format: ScheduleExportFormat) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var startDate by remember { mutableStateOf<Long?>(null) }
    var endDate by remember { mutableStateOf<Long?>(null) }
    var selectedFormat by remember { mutableStateOf(ScheduleExportFormat.ICS) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("导出日程", style = MaterialTheme.typography.titleLarge)

            DateRangeSelector(
                startDate = startDate,
                endDate = endDate,
                onStartChange = { startDate = it },
                onEndChange = { endDate = it }
            )

            Text("导出格式", style = MaterialTheme.typography.titleSmall)
            ScheduleExportFormat.entries.forEach { format ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = selectedFormat == format,
                        onClick = { selectedFormat = format }
                    )
                    Text(
                        when (format) {
                            ScheduleExportFormat.ICS -> "iCal (.ics)"
                            ScheduleExportFormat.CSV -> "CSV"
                        }
                    )
                }
            }

            Button(
                onClick = { onExport(startDate, endDate, selectedFormat) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("确认导出")
            }
        }
    }
}

// ========== 日期范围选择器 ==========

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangeSelector(
    startDate: Long?,
    endDate: Long?,
    onStartChange: (Long?) -> Unit,
    onEndChange: (Long?) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    Text("日期范围", style = MaterialTheme.typography.titleSmall)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { showStartPicker = true }, modifier = Modifier.weight(1f)) {
            Text(
                if (startDate != null) dateFormat.format(Date(startDate)) else "开始日期",
                maxLines = 1
            )
        }
        OutlinedButton(onClick = { showEndPicker = true }, modifier = Modifier.weight(1f)) {
            Text(
                if (endDate != null) dateFormat.format(Date(endDate)) else "结束日期",
                maxLines = 1
            )
        }
    }
    if (startDate != null || endDate != null) {
        TextButton(onClick = { onStartChange(null); onEndChange(null) }) {
            Text("清除日期")
        }
    }

    if (showStartPicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = startDate)
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onStartChange(datePickerState.selectedDateMillis)
                    showStartPicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showStartPicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEndPicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = endDate)
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onEndChange(datePickerState.selectedDateMillis)
                    showEndPicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showEndPicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
```

- [ ] **Step 2: 验证编译**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/haoze/keynote/ui/home/ExportDataScreen.kt
git commit -m "feat: refactor ExportDataScreen with card UI and filter BottomSheets"
```

---

### Task 5: 最终验证

- [ ] **Step 1: 完整编译**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Commit 最终版本**

```bash
git add -A
git commit -m "feat: export data UI refactor with schedule support and filtering v1.20.0"
```
