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

            DateRangeSelector(
                startDate = startDate,
                endDate = endDate,
                onStartChange = { startDate = it },
                onEndChange = { endDate = it }
            )

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
