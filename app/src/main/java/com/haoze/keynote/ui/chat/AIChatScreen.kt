package com.haoze.keynote.ui.chat

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardReturn
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.haoze.keynote.R
import com.haoze.keynote.ui.theme.AppAlertDialog
import com.haoze.keynote.ui.theme.LocalAppColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIChatScreen(
    drawerState: DrawerState,
    scope: CoroutineScope,
    viewModel: AIChatViewModel = viewModel(),
    onCreateNote: (Long) -> Unit = {}
) {
    val colors = LocalAppColors.current
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isCreatingBill by viewModel.isCreatingBill.collectAsState()
    val pendingBill by viewModel.pendingBill.collectAsState()
    val categories by viewModel.categories.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var isCreatingNote by remember { mutableStateOf(false) }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@LaunchedEffect
            if (lastVisibleItem >= messages.size - 2) {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.createdNoteId.collect { noteId ->
            isCreatingNote = false
            if (noteId > 0) {
                onCreateNote(noteId)
            }
        }
    }

    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            colors.surface,
            colors.surface,
            colors.surfaceVariant.copy(alpha = 0.5f),
            colors.surfaceVariant.copy(alpha = 0.6f)
        )
    )

    // 光晕动画：缓慢非线性消失
    val glowAlpha by animateFloatAsState(
        targetValue = if (messages.isEmpty()) 1f else 0f,
        animationSpec = tween(
            durationMillis = 1500,
            easing = FastOutSlowInEasing
        ),
        label = "glowAlpha"
    )

    Scaffold(
        containerColor = colors.transparent,
        topBar = {
            TopAppBar(
                title = { Text("AI 对话") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.surface
                ),
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Outlined.Menu, contentDescription = "菜单")
                    }
                },
                actions = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = {
                                isCreatingNote = true
                                viewModel.createNoteFromMessages()
                            },
                            enabled = !isCreatingNote
                        ) {
                            Icon(Icons.Outlined.Edit, contentDescription = "创建笔记")
                        }
                        Text(
                            "创建笔记",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { viewModel.clearMessages() }) {
                            Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = "新对话")
                        }
                        Text(
                            "新对话",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 光晕渐变效果：从输入框方向向上扩散，覆盖整个内容区域
            if (messages.isEmpty() || glowAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(alpha = glowAlpha)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    colors.transparent,
                                    colors.glowSecondary.copy(alpha = 0.03f),
                                    colors.glowSecondary,
                                    colors.glowPrimary
                                )
                            )
                        )
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.surface)
                )
            }

            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                if (messages.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            painter = painterResource(R.drawable.ic_logo),
                            contentDescription = "KeyNote Logo",
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            "准备就绪，这就开始",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Medium,
                            color = colors.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "输入消息开始与 AI 对话",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        val isUser = message.role == "user"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!isUser) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(colors.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.Psychology,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = colors.primary
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                            }

                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = if (isUser) colors.primaryContainer
                                        else colors.surfaceVariant,
                                tonalElevation = 1.dp,
                                modifier = Modifier.widthIn(max = 300.dp)
                            ) {
                                Column {
                                    Text(
                                        text = message.content,
                                        modifier = Modifier.padding(12.dp),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (!isUser && (message.isBillRelated || message.isScheduleRelated)) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 12.dp),
                                            color = colors.outlineVariant.copy(alpha = 0.3f)
                                        )
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            if (message.isBillRelated) {
                                                TextButton(
                                                    onClick = { viewModel.prepareBillFromAI(message.billJson) },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                                    enabled = !isCreatingBill
                                                ) {
                                                    Icon(
                                                        Icons.Outlined.Receipt,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("创建账单", style = MaterialTheme.typography.labelMedium)
                                                }
                                            }
                                            if (message.isScheduleRelated) {
                                                TextButton(
                                                    onClick = { viewModel.createScheduleFromAI(message.scheduleJson) },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Outlined.CalendarMonth,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("创建日程", style = MaterialTheme.typography.labelMedium)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (isUser) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(colors.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = colors.primary
                                    )
                                }
                            }
                        }
                    }
                    if (isLoading) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(colors.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.Psychology,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = colors.primary
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = colors.surfaceVariant,
                                    tonalElevation = 1.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = colors.primary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "思考中...",
                                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.outline
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Input bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(28.dp),
                        ambientColor = colors.shadow,
                        spotColor = colors.shadow
                    )
                    .border(
                        width = 1.dp,
                        color = colors.outlineVariant,
                        shape = RoundedCornerShape(28.dp)
                    ),
                shape = RoundedCornerShape(28.dp),
                color = colors.surface,
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { /* TODO: Add attachment functionality */ },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "添加",
                            tint = colors.onSurfaceVariant
                        )
                    }

                    TextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                "问问 KeyNote",
                                color = colors.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        singleLine = true,
                        enabled = !isLoading,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = colors.transparent,
                            unfocusedContainerColor = colors.transparent,
                            disabledContainerColor = colors.transparent,
                            focusedIndicatorColor = colors.transparent,
                            unfocusedIndicatorColor = colors.transparent
                        ),
                        textStyle = MaterialTheme.typography.bodyLarge
                    )

                    FilledIconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                viewModel.sendMessage(inputText)
                                inputText = ""
                            }
                        },
                        enabled = !isLoading && inputText.isNotBlank(),
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = colors.primary,
                            contentColor = colors.onPrimary
                        )
                    ) {
                        Icon(
                            Icons.Default.KeyboardReturn,
                            contentDescription = "发送",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }
        }

        if (pendingBill != null) {
        val bill = pendingBill!!
        var selectedCategoryId by remember(bill) {
            mutableStateOf<Long?>(
                categories.find { it.name == bill.suggestedCategory }?.id
            )
        }

        AppAlertDialog(
            onDismissRequest = { viewModel.dismissPendingBill() },
            title = { Text("确认创建账单") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("消费项目", style = MaterialTheme.typography.labelMedium, color = colors.outline)
                        Text(bill.item, style = MaterialTheme.typography.bodyMedium)
                        Text("金额", style = MaterialTheme.typography.labelMedium, color = colors.outline)
                        Text("¥${String.format("%.2f", bill.amount)}", style = MaterialTheme.typography.bodyMedium)
                        Text("时间", style = MaterialTheme.typography.labelMedium, color = colors.outline)
                        Text(
                            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                                .format(java.util.Date(bill.date)),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Text("类别", style = MaterialTheme.typography.labelMedium, color = colors.outline)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        categories.forEach { category ->
                            FilterChip(
                                selected = selectedCategoryId == category.id,
                                onClick = {
                                    selectedCategoryId = if (selectedCategoryId == category.id) null else category.id
                                },
                                label = { Text(category.name) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmCreateBill(selectedCategoryId) }) {
                    Text("确认创建")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissPendingBill() }) { Text("取消") }
            }
        )
    }
    }
    }
}
