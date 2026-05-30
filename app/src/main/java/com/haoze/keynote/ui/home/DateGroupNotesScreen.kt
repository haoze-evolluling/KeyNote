package com.haoze.keynote.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.haoze.keynote.data.db.entity.NoteWithTags
import com.haoze.keynote.ui.home.NoteCard
import com.haoze.keynote.viewmodel.HomeViewModel
import android.content.Intent
import android.content.ClipboardManager
import android.content.ClipData

import java.text.SimpleDateFormat
import java.util.*
import com.haoze.keynote.ui.theme.LocalAppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateGroupNotesScreen(
    drawerState: DrawerState,
    scope: CoroutineScope,
    onNavigateToEdit: (Long) -> Unit,
    onNavigateToTagNotes: (Long, String) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val colors = LocalAppColors.current
    val notes by viewModel.notes.collectAsState()
    var showActionDialogForNote by remember { mutableStateOf<Long?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<Long?>(null) }
    var isAiTagLoadingForNote by remember { mutableStateOf<Long?>(null) }
    var isSummarizingForNote by remember { mutableStateOf<Long?>(null) }
    var isGeneratingTitleForNote by remember { mutableStateOf<Long?>(null) }
    var showAddTagForNote by remember { mutableStateOf<Long?>(null) }
    var showManageTagsForNote by remember { mutableStateOf<Long?>(null) }
    var showNoteDetailsForNote by remember { mutableStateOf<Long?>(null) }

    val context = LocalContext.current

    // Group notes by month
    val groupedNotes = remember(notes) {
        notes.groupBy { noteWithTags ->
            val cal = java.util.Calendar.getInstance().apply {
                timeInMillis = noteWithTags.note.createdAt
            }
            "${cal.get(java.util.Calendar.YEAR)}年${cal.get(java.util.Calendar.MONTH) + 1}月"
        }.toSortedMap(Comparator.reverseOrder()) // newest first
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("按日期查看") },
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Default.Menu, contentDescription = "菜单")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (notes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "暂无笔记",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.outline
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                groupedNotes.forEach { (month, monthNotes) ->
                    item {
                        Text(
                            text = month,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(monthNotes, key = { it.note.id }) { noteWithTags ->
                        NoteCard(
                            noteWithTags = noteWithTags,
                            onClick = { onNavigateToEdit(noteWithTags.note.id) },
                            onTagClick = { tagId, tagName ->
                                onNavigateToTagNotes(tagId, tagName)
                            },
                            onLongClick = {
                                showActionDialogForNote = noteWithTags.note.id
                            }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }

    if (showActionDialogForNote != null) {
        val currentNote = notes.find { it.note.id == showActionDialogForNote }
        if (currentNote != null) {
            NoteActionBottomSheet(
                noteTitle = currentNote.note.title ?: "",
                noteContent = currentNote.note.content ?: "",
                isAiTagLoading = isAiTagLoadingForNote == showActionDialogForNote,
                isSummarizing = isSummarizingForNote == showActionDialogForNote,
                isGeneratingTitle = isGeneratingTitleForNote == showActionDialogForNote,
                onEdit = {
                    showActionDialogForNote?.let { onNavigateToEdit(it) }
                    showActionDialogForNote = null
                },
                onShare = {
                    showActionDialogForNote?.let { noteId ->
                        val note = notes.find { it.note.id == noteId }
                        if (note != null) {
                            val text = buildString {
                                if (note.note.title.isNotBlank()) append(note.note.title).append("\n\n")
                                append(note.note.content)
                            }
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            context.startActivity(Intent.createChooser(intent, "分享笔记"))
                        }
                    }
                    showActionDialogForNote = null
                },
                onAiSummary = {
                    showActionDialogForNote?.let { noteId ->
                        isSummarizingForNote = noteId
                        viewModel.summarizeNote(noteId) { success ->
                            isSummarizingForNote = null
                            if (success) showActionDialogForNote = null
                        }
                    }
                },
                onCopyContent = {
                    showActionDialogForNote?.let { noteId ->
                        val note = notes.find { it.note.id == noteId }
                        if (note != null) {
                            val text = buildString {
                                if (note.note.title.isNotBlank()) append(note.note.title).append("\n\n")
                                append(note.note.content)
                            }
                            val clipboard = context.getSystemService(ClipboardManager::class.java)
                            clipboard?.setPrimaryClip(ClipData.newPlainText("KeyNote", text))
                        }
                    }
                    showActionDialogForNote = null
                },
                onViewDetails = {
                    showNoteDetailsForNote = showActionDialogForNote
                    showActionDialogForNote = null
                },
                onAiGenerateTitle = {
                    showActionDialogForNote?.let { noteId ->
                        isGeneratingTitleForNote = noteId
                        viewModel.aiGenerateTitle(noteId) { success ->
                            isGeneratingTitleForNote = null
                            if (success) showActionDialogForNote = null
                        }
                    }
                },
                onAddTag = {
                    showAddTagForNote = showActionDialogForNote
                    showActionDialogForNote = null
                },
                onManageTags = {
                    showManageTagsForNote = showActionDialogForNote
                    showActionDialogForNote = null
                },
                onAiTag = {
                    showActionDialogForNote?.let { noteId ->
                        isAiTagLoadingForNote = noteId
                        viewModel.aiGenerateTags(noteId) { success ->
                            isAiTagLoadingForNote = null
                            if (success) showActionDialogForNote = null
                        }
                    }
                },
                onDelete = {
                    showDeleteConfirm = showActionDialogForNote
                    showActionDialogForNote = null
                },
                onDismiss = {
                    showActionDialogForNote = null
                    isAiTagLoadingForNote = null
                    isSummarizingForNote = null
                    isGeneratingTitleForNote = null
                }
            )
        }
    }

    // ── 删除确认 ──
    if (showDeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("删除笔记") },
            text = { Text("确定要删除这篇笔记吗？删除后可在回收站中恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm?.let { viewModel.deleteNote(it) }
                    showDeleteConfirm = null
                }) {
                    Text("删除", color = colors.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) { Text("取消") }
            }
        )
    }

    // ── 笔记详情 ──
    if (showNoteDetailsForNote != null) {
        val note = notes.find { it.note.id == showNoteDetailsForNote }
        if (note != null) {
            val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
            AlertDialog(
                onDismissRequest = { showNoteDetailsForNote = null },
                title = { Text("笔记详情") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("标题", style = MaterialTheme.typography.labelMedium, color = colors.outline)
                        Text(note.note.title.ifBlank { "无标题" }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("创建时间", style = MaterialTheme.typography.labelMedium, color = colors.outline)
                        Text(dateFormat.format(Date(note.note.createdAt)), style = MaterialTheme.typography.bodyMedium)
                        Text("更新时间", style = MaterialTheme.typography.labelMedium, color = colors.outline)
                        Text(dateFormat.format(Date(note.note.updatedAt)), style = MaterialTheme.typography.bodyMedium)
                        if (note.tags.isNotEmpty()) {
                            Text("标签", style = MaterialTheme.typography.labelMedium, color = colors.outline)
                            Text(note.tags.joinToString(", ") { "#${it.name}" }, style = MaterialTheme.typography.bodyMedium)
                        }
                        if (note.note.content.isNotBlank()) {
                            Text("内容预览", style = MaterialTheme.typography.labelMedium, color = colors.outline)
                            Text(
                                note.note.content.take(100) + if (note.note.content.length > 100) "..." else "",
                                style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showNoteDetailsForNote = null }) { Text("关闭") }
                }
            )
        }
    }

    // ── 添加标签弹窗 ──
    if (showAddTagForNote != null) {
        var tagName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddTagForNote = null },
            title = { Text("添加标签") },
            text = {
                OutlinedTextField(
                    value = tagName,
                    onValueChange = { tagName = it },
                    label = { Text("标签名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAddTagForNote?.let { noteId -> viewModel.addTagToNote(noteId, tagName) }
                        showAddTagForNote = null
                    },
                    enabled = tagName.isNotBlank()
                ) { Text("添加") }
            },
            dismissButton = {
                TextButton(onClick = { showAddTagForNote = null }) { Text("取消") }
            }
        )
    }

    // ── 管理标签弹窗 ──
    if (showManageTagsForNote != null) {
        val note = notes.find { it.note.id == showManageTagsForNote }
        AlertDialog(
            onDismissRequest = { showManageTagsForNote = null },
            title = { Text("管理标签") },
            text = {
                if (note == null || note.tags.isEmpty()) {
                    Text("暂无标签", color = colors.outline)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        note.tags.forEach { tag ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("#${tag.name}", style = MaterialTheme.typography.bodyMedium)
                                TextButton(onClick = { viewModel.removeTagFromNote(note.note.id, tag.id) }) {
                                    Text("移除", color = colors.error)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showManageTagsForNote = null }) { Text("关闭") }
            }
        )
    }
}
