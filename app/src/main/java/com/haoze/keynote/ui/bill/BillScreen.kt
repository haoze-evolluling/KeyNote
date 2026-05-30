package com.haoze.keynote.ui.bill

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.haoze.keynote.data.db.entity.BillEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BillScreen(
    drawerState: DrawerState,
    scope: CoroutineScope,
    viewModel: BillViewModel = viewModel()
) {
    val bills by viewModel.bills.collectAsState()
    val billsWithCategory by viewModel.billsWithCategory.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val context = LocalContext.current

    val categoryMap = remember(billsWithCategory) {
        billsWithCategory.associateBy { it.billId }
    }

    var showCreateDialog by remember { mutableStateOf(false) }
    var newBillItem by remember { mutableStateOf("") }
    var newBillAmount by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf<Long?>(null) }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }
    var pendingTimeSelection by remember { mutableStateOf(false) }

    var showActionDialogForBill by remember { mutableStateOf<Long?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<BillEntity?>(null) }
    var showEditDialogForBill by remember { mutableStateOf<BillEntity?>(null) }
    var showBillDetailsForBill by remember { mutableStateOf<BillEntity?>(null) }


    LaunchedEffect(pendingTimeSelection) {
        if (pendingTimeSelection) {
            delay(350)
            showTimePickerDialog = true
            pendingTimeSelection = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("账单") },
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Default.Menu, contentDescription = "菜单")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "新建账单")
            }
        }
    ) { innerPadding ->
        if (bills.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "暂无账单记录",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.outline
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
                items(bills, key = { it.id }) { bill ->
                    val rawBill = categoryMap[bill.id]
                    BillCard(
                        bill = bill,
                        categoryName = rawBill?.catName,
                        dateFormat = dateFormat,
                        onLongClick = { showActionDialogForBill = bill.id }
                    )
                }
            }
        }
    }

    if (showActionDialogForBill != null) {
        val currentBill = bills.find { it.id == showActionDialogForBill }
        if (currentBill != null) {
            BillActionBottomSheet(
                billItem = currentBill.item,
                billAmount = currentBill.amount,
                onEdit = {
                    showEditDialogForBill = currentBill
                    showActionDialogForBill = null
                },
                onViewDetails = {
                    showBillDetailsForBill = currentBill
                    showActionDialogForBill = null
                },
                onCopyItem = {
                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                    clipboard?.setPrimaryClip(ClipData.newPlainText("KeyNote", currentBill.item))
                    showActionDialogForBill = null
                },
                onCopyAmount = {
                    val amountText = String.format("%.2f", currentBill.amount)
                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                    clipboard?.setPrimaryClip(ClipData.newPlainText("KeyNote", amountText))
                    showActionDialogForBill = null
                },
                onDelete = {
                    showDeleteConfirm = currentBill
                    showActionDialogForBill = null
                },
                onDismiss = { showActionDialogForBill = null }
            )
        }
    }

    if (showDeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("删除账单") },
            text = { Text("确定要删除这条账单记录吗？") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm?.let { viewModel.deleteBill(it) }
                    showDeleteConfirm = null
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) { Text("取消") }
            }
        )
    }

    if (showBillDetailsForBill != null) {
        val bill = showBillDetailsForBill
        val rawBill = bill?.let { categoryMap[it.id] }
        if (bill != null) {
            AlertDialog(
                onDismissRequest = { showBillDetailsForBill = null },
                title = { Text("账单详情") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("消费项目", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                        Text(bill.item, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        if (!rawBill?.catName.isNullOrBlank()) {
                            Text("类别", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                            Text(rawBill!!.catName!!, style = MaterialTheme.typography.bodyMedium)
                        }
                        Text("金额", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                        Text("¥${String.format("%.2f", bill.amount)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("时间", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                        Text(dateFormat.format(Date(bill.date)), style = MaterialTheme.typography.bodyMedium)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showBillDetailsForBill = null }) { Text("关闭") }
                }
            )
        }
    }

    if (showEditDialogForBill != null) {
        val bill = showEditDialogForBill
        var editItem by remember(bill) { mutableStateOf(bill?.item ?: "") }
        var editAmount by remember(bill) { mutableStateOf(if (bill != null) String.format("%.2f", bill.amount) else "") }
        var editDate by remember(bill) { mutableStateOf(bill?.date) }
        var editCategoryId by remember(bill) { mutableStateOf(bill?.categoryId) }
        var showEditDatePicker by remember { mutableStateOf(false) }
        var showEditTimePicker by remember { mutableStateOf(false) }
        var pendingEditTime by remember { mutableStateOf(false) }

        LaunchedEffect(pendingEditTime) {
            if (pendingEditTime) {
                delay(350)
                showEditTimePicker = true
                pendingEditTime = false
            }
        }

        AlertDialog(
            onDismissRequest = { showEditDialogForBill = null },
            title = { Text("编辑账单") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CategoryChipRow(
                        categories = categories,
                        selectedCategoryId = editCategoryId,
                        onSelectCategory = { editCategoryId = it },
                        onAddCategory = { viewModel.addCategory(it) }
                    )
                    OutlinedTextField(
                        value = editItem,
                        onValueChange = { editItem = it },
                        label = { Text("消费项目") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editAmount,
                        onValueChange = { editAmount = it },
                        label = { Text("金额") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showEditDatePicker = true }
                    ) {
                        OutlinedTextField(
                            value = editDate?.let { dateFormat.format(Date(it)) } ?: "",
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            label = { Text("时间") },
                            placeholder = { Text("点击选择时间") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                            trailingIcon = {
                                Icon(Icons.Default.DateRange, contentDescription = "选择时间")
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val amount = editAmount.toDoubleOrNull()
                        if (editItem.isNotBlank() && amount != null && bill != null) {
                            viewModel.updateBill(bill.copy(
                                item = editItem,
                                amount = amount,
                                date = editDate ?: bill.date,
                                categoryId = editCategoryId
                            ))
                            showEditDialogForBill = null
                        }
                    },
                    enabled = editItem.isNotBlank() && editAmount.toDoubleOrNull() != null
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialogForBill = null }) { Text("取消") }
            }
        )

        if (showEditDatePicker) {
            val initialDate = editDate ?: System.currentTimeMillis()
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDate)
            DatePickerDialog(
                onDismissRequest = { showEditDatePicker = false },
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
                            showEditDatePicker = false
                            pendingEditTime = true
                        }
                    }) { Text("确定") }
                },
                dismissButton = {
                    TextButton(onClick = { showEditDatePicker = false }) { Text("取消") }
                }
            ) { DatePicker(state = datePickerState) }
        }

        if (showEditTimePicker) {
            val cal = remember {
                Calendar.getInstance().apply { editDate?.let { timeInMillis = it } }
            }
            val timePickerState = rememberTimePickerState(
                initialHour = cal.get(Calendar.HOUR_OF_DAY),
                initialMinute = cal.get(Calendar.MINUTE),
                is24Hour = true
            )
            AlertDialog(
                onDismissRequest = { showEditTimePicker = false },
                title = { Text("选择时间") },
                text = { TimePicker(state = timePickerState) },
                confirmButton = {
                    TextButton(onClick = {
                        val c = Calendar.getInstance().apply {
                            editDate?.let { timeInMillis = it }
                            set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            set(Calendar.MINUTE, timePickerState.minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        editDate = c.timeInMillis
                        showEditTimePicker = false
                    }) { Text("确定") }
                },
                dismissButton = {
                    TextButton(onClick = { showEditTimePicker = false }) { Text("取消") }
                }
            )
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = {
                showCreateDialog = false
                newBillItem = ""
                newBillAmount = ""
                selectedDate = null
                selectedCategoryId = null
            },
            title = { Text("新建账单") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CategoryChipRow(
                        categories = categories,
                        selectedCategoryId = selectedCategoryId,
                        onSelectCategory = { selectedCategoryId = it },
                        onAddCategory = { viewModel.addCategory(it) }
                    )
                    OutlinedTextField(
                        value = newBillItem,
                        onValueChange = { newBillItem = it },
                        label = { Text("消费项目") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newBillAmount,
                        onValueChange = { newBillAmount = it },
                        label = { Text("金额") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePickerDialog = true }
                    ) {
                        OutlinedTextField(
                            value = selectedDate?.let { dateFormat.format(Date(it)) } ?: "",
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            label = { Text("时间") },
                            placeholder = { Text("点击选择时间") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                            trailingIcon = {
                                Icon(Icons.Default.DateRange, contentDescription = "选择时间")
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val amount = newBillAmount.toDoubleOrNull()
                        if (newBillItem.isNotBlank() && amount != null) {
                            val date = selectedDate ?: System.currentTimeMillis()
                            viewModel.createBill(newBillItem, amount, date, selectedCategoryId)
                            showCreateDialog = false
                            newBillItem = ""
                            newBillAmount = ""
                            selectedDate = null
                            selectedCategoryId = null
                        }
                    },
                    enabled = newBillItem.isNotBlank() && newBillAmount.toDoubleOrNull() != null
                ) { Text("创建") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCreateDialog = false
                    newBillItem = ""
                    newBillAmount = ""
                    selectedDate = null
                    selectedCategoryId = null
                }) { Text("取消") }
            }
        )
    }

    if (showDatePickerDialog) {
        val initialDate = selectedDate ?: System.currentTimeMillis()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDate)
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    val dateMillis = datePickerState.selectedDateMillis
                    if (dateMillis != null) {
                        val localCal = Calendar.getInstance().apply {
                            timeInMillis = dateMillis
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        selectedDate = localCal.timeInMillis
                        showDatePickerDialog = false
                        pendingTimeSelection = true
                    }
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) { Text("取消") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    if (showTimePickerDialog) {
        val cal = remember {
            Calendar.getInstance().apply { selectedDate?.let { timeInMillis = it } }
        }
        val timePickerState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePickerDialog = false },
            title = { Text("选择时间") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    val c = Calendar.getInstance().apply {
                        selectedDate?.let { timeInMillis = it }
                        set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        set(Calendar.MINUTE, timePickerState.minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    selectedDate = c.timeInMillis
                    showTimePickerDialog = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePickerDialog = false }) { Text("取消") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BillCard(
    bill: BillEntity,
    categoryName: String?,
    dateFormat: SimpleDateFormat,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bill.item,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                if (!categoryName.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    AssistChip(
                        onClick = {},
                        label = { Text(categoryName, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.height(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateFormat.format(Date(bill.date)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Text(
                text = "¥${String.format("%.2f", bill.amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.End,
                modifier = Modifier.padding(end = 8.dp)
            )

        }
    }
}
