package com.haoze.keynote.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun NoteActionBottomSheet(
    noteTitle: String,
    noteContent: String = "",
    isAiTagLoading: Boolean = false,
    isSummarizing: Boolean = false,
    isGeneratingTitle: Boolean = false,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onAiSummary: () -> Unit,
    onCopyContent: () -> Unit,
    onViewDetails: () -> Unit,
    onAiTag: () -> Unit,
    onAiGenerateTitle: () -> Unit,
    onAddTag: () -> Unit,
    onManageTags: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = noteTitle.ifBlank { "无标题" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                ActionRow(Icons.Default.Edit, "编辑笔记", onEdit)
                ActionRow(Icons.Default.Info, "查看详情", onViewDetails)

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                ActionRow(Icons.Default.Label, "添加标签", onAddTag)
                ActionRow(Icons.Default.Label, "管理标签", onManageTags)

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                ActionRow(
                    icon = Icons.Default.AutoAwesome,
                    label = "AI 摘要",
                    onClick = onAiSummary,
                    isLoading = isSummarizing,
                    loadingLabel = "正在摘要..."
                )
                ActionRow(
                    icon = Icons.Default.AutoAwesome,
                    label = "AI 生成标题",
                    onClick = onAiGenerateTitle,
                    isLoading = isGeneratingTitle,
                    loadingLabel = "生成中..."
                )
                ActionRow(
                    icon = Icons.Default.Label,
                    label = "AI 标签",
                    onClick = onAiTag,
                    isLoading = isAiTagLoading,
                    loadingLabel = "生成中..."
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                ActionRow(Icons.Default.Share, "分享笔记", onShare)
                ActionRow(Icons.Default.ContentCopy, "复制内容", onCopyContent)

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                ActionRow(Icons.Default.Delete, "删除笔记", onDelete, isDestructive = true)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false,
    isLoading: Boolean = false,
    loadingLabel: String = ""
) {
    val contentColor = if (isDestructive) MaterialTheme.colorScheme.error
                       else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clickable(enabled = !isLoading) { onClick() }
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp,
                color = contentColor
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = if (isLoading) loadingLabel else label,
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor
        )
    }
}
