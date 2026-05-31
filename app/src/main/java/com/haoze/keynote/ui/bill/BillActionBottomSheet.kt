package com.haoze.keynote.ui.bill

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.haoze.keynote.ui.theme.AppAlertDialog
import com.haoze.keynote.ui.theme.LocalAppColors

@Composable
fun BillActionBottomSheet(
    billItem: String,
    billAmount: Double,
    onEdit: () -> Unit,
    onViewDetails: () -> Unit,
    onCopyItem: () -> Unit,
    onCopyAmount: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = billItem.ifBlank { "无名称" },
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
                ActionRow(Icons.Default.Edit, "编辑账单", onEdit)
                ActionRow(Icons.Default.Info, "查看详情", onViewDetails)

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                ActionRow(Icons.Default.ContentCopy, "复制项目名称", onCopyItem)
                ActionRow(Icons.Default.ContentCopy, "复制金额（¥${"%.2f".format(billAmount)}）", onCopyAmount)

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                ActionRow(Icons.Default.Delete, "删除账单", onDelete, isDestructive = true)
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
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor
        )
    }
}
