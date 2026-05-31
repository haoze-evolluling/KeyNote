package com.haoze.keynote.ui.schedule

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.haoze.keynote.data.db.entity.NoteWithTags
import com.haoze.keynote.data.db.entity.ScheduleEntity
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.haoze.keynote.ui.theme.AppAlertDialog
import com.haoze.keynote.ui.theme.AppDatePickerDialog
import com.haoze.keynote.ui.theme.LocalAppColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ScheduleScreen(
    drawerState: DrawerState,
    scope: CoroutineScope,
    viewModel: ScheduleViewModel
) {
    val colors = LocalAppColors.current
    val schedules by viewModel.schedules.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val aiGeneratedContent by viewModel.aiGeneratedContent.collectAsState()
    val isGeneratingNote by viewModel.isGeneratingNote.collectAsState()
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    var showCreateDialog by remember { mutableStateOf(false) }
    var showActionDialogForSchedule by remember { mutableStateOf<Long?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<ScheduleEntity?>(null) }
    var showEditDialogForSchedule by remember { mutableStateOf<ScheduleEntity?>(null) }
    var showLinkNoteDialog by remember { mutableStateOf<Long?>(null) }
    var pendingAiScheduleId by remember { mutableStateOf<Long?>(null) }

    val groupedSchedules = remember(schedules) {
        schedules.groupBy { schedule ->
            val cal = Calendar.getInstance().apply { timeInMillis = schedule.date }
            "${cal.get(Calendar.YEAR)}年${cal.get(Calendar.MONTH) + 1}月"
        }.toSortedMap(Comparator.reverseOrder())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("日程") },
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Default.Menu, contentDescription = "菜单")
                    }
                }
            )
        },
        floatingActionButton = {
            Box(modifier = Modifier.padding(bottom = 16.dp)) {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "新建日程")
                }
            }
        }
    ) { innerPadding ->
        if (schedules.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = colors.outline
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("暂无日程", style = MaterialTheme.typography.bodyLarge, color = colors.outline)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                groupedSchedules.forEach { (month, monthSchedules) ->
                    item {
                        Text(
                            text = month,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(monthSchedules, key = { it.id }) { schedule ->
                        val linkedNote = notes.find { it.note.id == schedule.noteId }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .border(1.dp, colors.outlineVariant, CardDefaults.shape)
                                .combinedClickable(onClick = {}, onLongClick = { showActionDialogForSchedule = schedule.id }),
                            colors = CardDefaults.cardColors(containerColor = colors.surface)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(schedule.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                                    Spacer(Modifier.height(4.dp))
                                    Text(dateFormat.format(Date(schedule.date)), style = MaterialTheme.typography.bodySmall, color = colors.outline)
                                    if (schedule.location != null) {
                                        Spacer(Modifier.height(2.dp))
                                        Text("📍 ${schedule.location}", style = MaterialTheme.typography.bodySmall, color = colors.outline)
                                    }
                                    if (linkedNote != null) {
                                        Spacer(Modifier.height(2.dp))
                                        Text("关联: ${linkedNote.note.title.ifBlank { "无标题" }}", style = MaterialTheme.typography.bodySmall, color = colors.primary)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }
    }

    if (showActionDialogForSchedule != null) {
        val currentSchedule = schedules.find { it.id == showActionDialogForSchedule }
        if (currentSchedule != null) {
            val hasLink = currentSchedule.noteId != null
            AppAlertDialog(
                onDismissRequest = { showActionDialogForSchedule = null },
                title = { Text(currentSchedule.title) },
                text = {
                    Column {
                        ScheduleActionRow(
                            icon = Icons.Default.Edit,
                            label = "编辑日程",
                            onClick = {
                                showEditDialogForSchedule = currentSchedule
                                showActionDialogForSchedule = null
                            }
                        )
                        if (hasLink) {
                            ScheduleActionRow(
                                icon = Icons.Default.LinkOff,
                                label = "取消关联笔记",
                                onClick = {
                                    viewModel.unlinkNote(currentSchedule.id)
                                    showActionDialogForSchedule = null
                                }
                            )
                        } else {
                            ScheduleActionRow(
                                icon = Icons.Default.Link,
                                label = "关联笔记",
                                onClick = {
                                    showLinkNoteDialog = currentSchedule.id
                                    showActionDialogForSchedule = null
                                }
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        ScheduleActionRow(
                            icon = Icons.Default.AutoAwesome,
                            label = if (isGeneratingNote) "生成中..." else "AI 生成笔记",
                            onClick = {
                                pendingAiScheduleId = currentSchedule.id
                                viewModel.aiGenerateNote(currentSchedule.id)
                                showActionDialogForSchedule = null
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        ScheduleActionRow(
                            icon = Icons.Default.Delete,
                            label = "删除日程",
                            isDestructive = true,
                            onClick = {
                                showDeleteConfirm = currentSchedule
                                showActionDialogForSchedule = null
                            }
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showActionDialogForSchedule = null }) { Text("取消") }
                }
            )
        }
    }

    showDeleteConfirm?.let { schedule ->
        AppAlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("删除日程") },
            text = { Text("确定要删除「${schedule.title}」吗？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSchedule(schedule)
                    showDeleteConfirm = null
                }) { Text("删除", color = colors.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) { Text("取消") }
            }
        )
    }

    if (showCreateDialog) {
        ScheduleDialog(
            title = "新建日程",
            onDismiss = { showCreateDialog = false },
            onConfirm = { title, date, endDate, location, description, noteId ->
                viewModel.createSchedule(title, date, endDate, location, description, noteId)
                showCreateDialog = false
            },
            notes = notes
        )
    }

    showEditDialogForSchedule?.let { schedule ->
        ScheduleDialog(
            title = "编辑日程",
            initialTitle = schedule.title,
            initialDate = schedule.date,
            initialEndDate = schedule.endDate,
            initialLocation = schedule.location,
            initialDescription = schedule.description,
            initialNoteId = schedule.noteId,
            onDismiss = { showEditDialogForSchedule = null },
            onConfirm = { title, date, endDate, location, description, noteId ->
                viewModel.updateSchedule(schedule.copy(title = title, date = date, endDate = endDate, location = location, description = description, noteId = noteId))
                showEditDialogForSchedule = null
            },
            notes = notes
        )
    }

    showLinkNoteDialog?.let { scheduleId ->
        AppAlertDialog(
            onDismissRequest = { showLinkNoteDialog = null },
            title = { Text("选择关联笔记") },
            text = {
                if (notes.isEmpty()) {
                    Text("暂无笔记", color = colors.outline)
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        notes.forEach { noteWithTags ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = noteWithTags.note.title.ifBlank { "无标题" },
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = {
                                    viewModel.linkNote(scheduleId, noteWithTags.note.id)
                                    showLinkNoteDialog = null
                                }) { Text("选择") }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLinkNoteDialog = null }) { Text("取消") }
            }
        )
    }

    aiGeneratedContent?.let { content ->
        var editedContent by remember(content) { mutableStateOf(content) }
        AppAlertDialog(
            onDismissRequest = {
                viewModel.discardAiGeneratedNote()
                pendingAiScheduleId = null
            },
            title = { Text("AI 生成笔记预览") },
            text = {
                OutlinedTextField(
                    value = editedContent,
                    onValueChange = { editedContent = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 400.dp),
                    maxLines = Int.MAX_VALUE
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingAiScheduleId?.let { scheduleId ->
                        viewModel.saveAiGeneratedNote(scheduleId, editedContent)
                    }
                    pendingAiScheduleId = null
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.discardAiGeneratedNote()
                    pendingAiScheduleId = null
                }) { Text("取消") }
            }
        )
    }

}

@Composable
private fun ScheduleActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    val colors = LocalAppColors.current
    val contentColor = if (isDestructive) colors.error
                       else colors.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clickable { onClick() }
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = contentColor, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = contentColor)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ScheduleDialog(
    title: String,
    initialTitle: String = "",
    initialDate: Long = System.currentTimeMillis(),
    initialEndDate: Long? = null,
    initialLocation: String? = null,
    initialDescription: String? = null,
    initialNoteId: Long? = null,
    onDismiss: () -> Unit,
    onConfirm: (title: String, date: Long, endDate: Long?, location: String?, description: String?, noteId: Long?) -> Unit,
    notes: List<NoteWithTags>
) {
    val colors = LocalAppColors.current
    var editTitle by remember { mutableStateOf(initialTitle) }
    var editDate by remember { mutableStateOf(initialDate) }
    var editEndDate by remember { mutableStateOf(initialEndDate) }
    var editLocation by remember { mutableStateOf(initialLocation ?: "") }
    var editDescription by remember { mutableStateOf(initialDescription ?: "") }
    var editNoteId by remember { mutableStateOf(initialNoteId) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var pendingTime by remember { mutableStateOf(false) }
    var pendingEndTime by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    LaunchedEffect(pendingTime) {
        if (pendingTime) {
            kotlinx.coroutines.delay(350)
            showTimePicker = true
            pendingTime = false
        }
    }

    LaunchedEffect(pendingEndTime) {
        if (pendingEndTime) {
            kotlinx.coroutines.delay(350)
            showEndTimePicker = true
            pendingEndTime = false
        }
    }

    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = editTitle,
                    onValueChange = { editTitle = it },
                    label = { Text("日程标题") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                // 开始时间
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = dateFormat.format(Date(editDate)),
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text("开始时间") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = colors.onSurface,
                            disabledBorderColor = colors.outline,
                            disabledLabelColor = colors.onSurfaceVariant,
                        )
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .padding(start = 48.dp)
                            .clickable { showDatePicker = true }
                    )
                }
                // 结束时间（可选）
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = if (editEndDate != null) dateFormat.format(Date(editEndDate!!)) else "",
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text("结束时间（可选）") },
                        placeholder = { Text("点击选择结束时间") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = colors.onSurface,
                            disabledBorderColor = colors.outline,
                            disabledLabelColor = colors.onSurfaceVariant,
                        )
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .padding(start = 48.dp)
                            .clickable { showEndDatePicker = true }
                    )
                }
                // 地点
                OutlinedTextField(
                    value = editLocation,
                    onValueChange = { editLocation = it },
                    label = { Text("地点（可选）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                // 任务事项
                OutlinedTextField(
                    value = editDescription,
                    onValueChange = { editDescription = it },
                    label = { Text("任务事项（可选）") },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    maxLines = 4
                )
                val linkedNote = notes.find { it.note.id == editNoteId }
                if (linkedNote != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("关联: ${linkedNote.note.title.ifBlank { "无标题" }}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        TextButton(onClick = { editNoteId = null }) { Text("取消关联") }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (editTitle.isNotBlank()) onConfirm(
                        editTitle, editDate, editEndDate,
                        editLocation.ifBlank { null },
                        editDescription.ifBlank { null },
                        editNoteId
                    )
                },
                enabled = editTitle.isNotBlank()
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = editDate)
        AppDatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val localCal = Calendar.getInstance().apply {
                            timeInMillis = it
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        editDate = localCal.timeInMillis
                        showDatePicker = false
                        pendingTime = true
                    }
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    if (showTimePicker) {
        val cal = remember { Calendar.getInstance().apply { timeInMillis = editDate } }
        val timePickerState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            is24Hour = true
        )
        AppAlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("选择时间") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    val c = Calendar.getInstance().apply {
                        timeInMillis = editDate
                        set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        set(Calendar.MINUTE, timePickerState.minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    editDate = c.timeInMillis
                    showTimePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("取消") }
            }
        )
    }

    if (showEndDatePicker) {
        val endDatePickerState = rememberDatePickerState(initialSelectedDateMillis = editEndDate ?: System.currentTimeMillis())
        AppDatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    endDatePickerState.selectedDateMillis?.let {
                        val localCal = Calendar.getInstance().apply {
                            timeInMillis = it
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        editEndDate = localCal.timeInMillis
                        showEndDatePicker = false
                        pendingEndTime = true
                    }
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) { Text("取消") }
            }
        ) { DatePicker(state = endDatePickerState) }
    }

    if (showEndTimePicker) {
        val cal = remember { Calendar.getInstance().apply { timeInMillis = editEndDate ?: System.currentTimeMillis() } }
        val endTimePickerState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            is24Hour = true
        )
        AppAlertDialog(
            onDismissRequest = { showEndTimePicker = false },
            title = { Text("选择结束时间") },
            text = { TimePicker(state = endTimePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    val c = Calendar.getInstance().apply {
                        timeInMillis = editEndDate ?: System.currentTimeMillis()
                        set(Calendar.HOUR_OF_DAY, endTimePickerState.hour)
                        set(Calendar.MINUTE, endTimePickerState.minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    editEndDate = c.timeInMillis
                    showEndTimePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showEndTimePicker = false }) { Text("取消") }
            }
        )
    }
}
