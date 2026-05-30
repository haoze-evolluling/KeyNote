package com.haoze.keynote.ui.edit

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit

import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

import androidx.compose.material3.*
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.haoze.keynote.viewmodel.EditNoteViewModel
import kotlinx.coroutines.launch
import com.haoze.keynote.ui.theme.LocalAppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditNoteScreen(
    noteId: Long,
    onNavigateBack: () -> Unit,
    viewModel: EditNoteViewModel = viewModel()
) {
    val colors = LocalAppColors.current
    val title by viewModel.title.collectAsState()
    val content by viewModel.content.collectAsState()
    val tags by viewModel.tags.collectAsState()
    val isGenerating by viewModel.isGeneratingTags.collectAsState()
    val isGeneratingTitle by viewModel.isGeneratingTitle.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val summaries by viewModel.summaries.collectAsState()
    val isSummarizing by viewModel.isSummarizing.collectAsState()
    val isPolishing by viewModel.isPolishing.collectAsState()
    val isPreview by viewModel.isPreview.collectAsState()
    val noteFontSize by viewModel.noteFontSize.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val polishedText by viewModel.polishedText.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(noteId) {
        viewModel.loadNote(noteId)
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    BackHandler {
        viewModel.saveNote { onNavigateBack() }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.deleteIfEmpty()
            viewModel.saveNote {}
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("编辑笔记") },
                    navigationIcon = {
                        IconButton(onClick = {
                            viewModel.saveNote { onNavigateBack() }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    },
                    actions = {
                        // Undo
                        IconButton(
                            onClick = { viewModel.undo() },
                            enabled = canUndo
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Undo,
                                    contentDescription = "撤回",
                                    modifier = Modifier.size(18.dp),
                                    tint = if (canUndo) colors.onSurface
                                           else colors.onSurface.copy(alpha = 0.38f)
                                )
                                Text(
                                    "撤回",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (canUndo) colors.onSurface
                                            else colors.onSurface.copy(alpha = 0.38f)
                                )
                            }
                        }
                        // Save
                        IconButton(onClick = { viewModel.saveNoteWithFeedback() }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Save, contentDescription = "保存", modifier = Modifier.size(18.dp))
                                Text("保存", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        // Preview
                        IconButton(onClick = { viewModel.togglePreview() }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = if (isPreview) Icons.Default.Edit else Icons.Default.Visibility,
                                    contentDescription = if (isPreview) "编辑" else "预览",
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(if (isPreview) "编辑" else "预览", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        // Menu
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "更多")
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                // Share
                                val context = LocalContext.current
                                DropdownMenuItem(
                                    text = { Text("分享") },
                                    onClick = {
                                        val text = buildString {
                                            if (title.isNotBlank()) append(title).append("\n\n")
                                            append(content)
                                        }
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, text)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "分享笔记"))
                                        showMenu = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                )
                                HorizontalDivider()
                                // AI Summary
                                DropdownMenuItem(
                                    text = { Text("AI摘要") },
                                    onClick = {
                                        viewModel.summarizeNote()
                                        showMenu = false
                                    },
                                    leadingIcon = {
                                        if (isSummarizing) {
                                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                        } else {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                        }
                                    },
                                    enabled = !isSummarizing
                                )
                                // AI Tags
                                DropdownMenuItem(
                                    text = { Text("AI标签") },
                                    onClick = {
                                        viewModel.generateTags()
                                        showMenu = false
                                    },
                                    leadingIcon = {
                                        if (isGenerating) {
                                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                        } else {
                                            Icon(Icons.Default.Label, contentDescription = null, modifier = Modifier.size(18.dp))
                                        }
                                    },
                                    enabled = !isGenerating
                                )
                                // AI Polish
                                DropdownMenuItem(
                                    text = { Text("AI润色") },
                                    onClick = {
                                        viewModel.polishNote()
                                        showMenu = false
                                    },
                                    leadingIcon = {
                                        if (isPolishing) {
                                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                        } else {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                        }
                                    },
                                    enabled = !isPolishing
                                )
                                HorizontalDivider()
                                // Delete
                                DropdownMenuItem(
                                    text = { Text("删除", color = colors.error) },
                                    onClick = {
                                        showDeleteDialog = true
                                        showMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = colors.error
                                        )
                                    }
                                )
                            }
                        }
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
            val isInPreview = isPreview && content.isNotBlank()
            if (isInPreview) {
                // Preview layout: non-scrolling, WebView takes remaining space
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp)
                        .imePadding()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { viewModel.onTitleChanged(it) },
                            label = { Text("标题") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    MarkdownPreview(
                        content = content,
                        isDark = false,
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Tags section in preview
                    if (tags.isNotEmpty()) {
                        Text(
                            "标签",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.outline
                        )
                        TagChipRow(
                            tags = tags,
                            onRemoveTag = { viewModel.removeTag(it) }
                        )
                    }
                }
            } else {
                // Edit layout: scrolling
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { viewModel.onTitleChanged(it) },
                            label = { Text("标题") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            trailingIcon = {
                                if (title.isBlank() && content.isNotBlank()) {
                                    if (isGeneratingTitle) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        IconButton(onClick = { viewModel.generateTitleFromContent() }) {
                                            Icon(
                                                Icons.Default.AutoAwesome,
                                                contentDescription = "AI生成标题"
                                            )
                                        }
                                    }
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = content,
                        onValueChange = { viewModel.onContentChanged(it) },
                        label = { Text("正文") },
                        textStyle = TextStyle(fontSize = noteFontSize.sp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 300.dp)
                            .imePadding(),
                        maxLines = Int.MAX_VALUE
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Summary cards
                    summaries.forEachIndexed { index, summaryText ->
                        var showEditDialog by remember { mutableStateOf(false) }
                        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .combinedClickable(
                                    onClick = {
                                        clipboardManager.setPrimaryClip(ClipData.newPlainText("summary", summaryText))
                                        coroutineScope.launch { snackbarHostState.showSnackbar("已复制到剪贴板") }
                                    },
                                    onLongClick = { showEditDialog = true }
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = colors.primaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    val circledNumbers = listOf("\u2460", "\u2461", "\u2462", "\u2463", "\u2464", "\u2465", "\u2466", "\u2467", "\u2468", "\u2469")
                                    val label = if (index < 10) "AI \u6458\u8981 ${circledNumbers[index]}" else "AI \u6458\u8981 ${index + 1}"
                                    Text(
                                        label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = colors.onPrimaryContainer
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = summaryText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colors.onPrimaryContainer
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.removeSummary(index) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "关闭摘要",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        if (showEditDialog) {
                            var editedText by remember { mutableStateOf(summaryText) }
                            AlertDialog(
                                onDismissRequest = { showEditDialog = false },
                                title = { Text("编辑摘要") },
                                text = {
                                    OutlinedTextField(
                                        value = editedText,
                                        onValueChange = { editedText = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        maxLines = 5
                                    )
                                },
                                confirmButton = {
                                    TextButton(onClick = {
                                        viewModel.updateSummary(index, editedText)
                                        showEditDialog = false
                                    }) { Text("保存") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showEditDialog = false }) { Text("取消") }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "标签",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.outline
                    )
                    TagChipRow(
                        tags = tags,
                        onRemoveTag = { viewModel.removeTag(it) }
                    )

                    var newTag by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = newTag,
                        onValueChange = { newTag = it },
                        label = { Text("添加标签") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    if (newTag.isNotBlank()) {
                                        viewModel.addTag(newTag.trim().removePrefix("#"))
                                        newTag = ""
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "添加标签")
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (newTag.isNotBlank()) {
                                    viewModel.addTag(newTag.trim().removePrefix("#"))
                                    newTag = ""
                                }
                            }
                        )
                    )
                }
            }
        }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除笔记") },
            text = { Text("确定要删除这篇笔记吗？删除后可在回收站中恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteNote()
                    showDeleteDialog = false
                    onNavigateBack()
                }) {
                    Text("删除", color = colors.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (polishedText != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissPolishedText() },
            title = { Text("AI润色结果") },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = LocalConfiguration.current.screenHeightDp.dp * 0.6f)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(polishedText!!)
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.applyPolishedText() }) {
                    Text("替换")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissPolishedText() }) {
                    Text("取消")
                }
            }
        )
    }
}
