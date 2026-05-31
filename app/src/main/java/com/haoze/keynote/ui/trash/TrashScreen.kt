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
private fun NoteTrashContent(item: TrashItem.TrashNote, remainingDays: Int, dateFormat: SimpleDateFormat) {
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
        text = dateFormat.format(Date(item.data.note.updatedAt)),
        style = MaterialTheme.typography.bodySmall,
        color = colors.outline
    )
}

@Composable
private fun BillTrashContent(item: TrashItem.TrashBill, remainingDays: Int, dateFormat: SimpleDateFormat) {
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
        text = dateFormat.format(Date(item.data.date)),
        style = MaterialTheme.typography.bodySmall,
        color = colors.outline
    )
}

@Composable
private fun ScheduleTrashContent(item: TrashItem.TrashSchedule, remainingDays: Int, dateFormat: SimpleDateFormat) {
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
        append(dateFormat.format(Date(item.data.date)))
        item.data.endDate?.let { append(" - ${dateFormat.format(Date(it))}") }
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
