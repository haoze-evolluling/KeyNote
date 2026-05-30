package com.haoze.keynote.ui.home

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.haoze.keynote.data.db.NoteDatabase
import com.haoze.keynote.data.db.entity.NoteWithTags
import com.haoze.keynote.data.repository.BillRepository
import com.haoze.keynote.data.repository.NoteRepository
import com.haoze.keynote.util.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportDataScreen(
    drawerState: DrawerState,
    drawerScope: CoroutineScope,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isExporting by remember { mutableStateOf(false) }

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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "选择要导出的数据类型",
                style = MaterialTheme.typography.titleMedium
            )

            if (isExporting) {
                CircularProgressIndicator()
            }

            Button(
                onClick = {
                    scope.launch {
                        isExporting = true
                        try {
                            exportNotes(context)
                            snackbarHostState.showSnackbar("笔记导出成功")
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("导出失败: ${e.message}")
                        } finally {
                            isExporting = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isExporting
            ) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("导出笔记 (Markdown)")
            }

            OutlinedButton(
                onClick = {
                    scope.launch {
                        isExporting = true
                        try {
                            exportBills(context)
                            snackbarHostState.showSnackbar("账单导出成功")
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("导出失败: ${e.message}")
                        } finally {
                            isExporting = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isExporting
            ) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("导出账单 (CSV)")
            }

            Text(
                "文件将保存到 Downloads/KeyNote/",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

private fun writeToDownloads(context: Context, fileName: String, mimeType: String, content: ByteArray) {
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

private suspend fun exportNotes(context: Context) {
    val db = NoteDatabase.getDatabase(context)
    val repository = NoteRepository(db.noteDao(), db.tagDao(), PreferencesManager(context))
    val notes: List<NoteWithTags> = repository.getAllActiveNotesWithTags().first()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault())

    notes.forEach { noteWithTags ->
        val safeTitle = noteWithTags.note.title
            .ifBlank { "无标题" }
            .replace(Regex("[/\\\\:*?\"<>|]"), "_")
        val fileName = "${safeTitle}-${dateFormat.format(Date(noteWithTags.note.createdAt))}.md"

        val content = buildString {
            appendLine("# ${noteWithTags.note.title}")
            appendLine()
            if (noteWithTags.tags.isNotEmpty()) {
                appendLine("标签: ${noteWithTags.tags.joinToString(" ") { "#${it.name}" }}")
                appendLine()
            }
            appendLine(noteWithTags.note.content)
        }

        writeToDownloads(context, fileName, "text/markdown", content.toByteArray(Charsets.UTF_8))
    }
}

private suspend fun exportBills(context: Context) {
    val db = NoteDatabase.getDatabase(context)
    val repository = BillRepository(db.billDao(), db.categoryDao())
    val bills = repository.getAllBillsList()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    val csvContent = buildString {
        appendLine("消费项目,金额,时间")
        bills.forEach { bill ->
            val item = bill.item.replace("\"", "\"\"")
            appendLine("\"${item}\",${bill.amount},${dateFormat.format(Date(bill.date))}")
        }
    }

    val fileName = "账单导出-${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}.csv"

    writeToDownloads(context, fileName, "text/csv", csvContent.toByteArray(Charsets.UTF_8))
}
