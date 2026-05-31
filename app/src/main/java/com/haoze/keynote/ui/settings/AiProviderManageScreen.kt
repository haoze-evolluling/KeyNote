package com.haoze.keynote.ui.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.haoze.keynote.data.remote.AiProvider
import com.haoze.keynote.ui.theme.AppAlertDialog
import com.haoze.keynote.ui.theme.LocalAppColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AiProviderManageScreen(
    onNavigateBack: () -> Unit,
    providers: List<AiProvider>,
    activeProviderId: String,
    onSelectProvider: (String) -> Unit,
    onUpdateProvider: (AiProvider) -> Unit,
    onDeleteProvider: (String) -> Unit,
    onAddProvider: (String, String, String, String) -> Unit,
    sealKey: (String) -> String,
    openKey: (String) -> String
) {
    val colors = LocalAppColors.current
    var editingProvider by remember { mutableStateOf<AiProvider?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI 厂商管理") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(providers, key = { it.id }) { provider ->
                val isActive = provider.id == activeProviderId
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { onSelectProvider(provider.id) },
                            onLongClick = {
                                editingProvider = provider
                            }
                        ),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = if (isActive) colors.primaryContainer
                                        else colors.surfaceVariant
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
                                provider.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isActive) colors.onPrimaryContainer
                                       else colors.onSurface
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                provider.baseUrl,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isActive) colors.onPrimaryContainer.copy(alpha = 0.7f)
                                       else colors.outline
                            )
                            if (provider.modelName.isNotBlank()) {
                                Text(
                                    provider.modelName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isActive) colors.onPrimaryContainer.copy(alpha = 0.7f)
                                           else colors.outline
                                )
                            }
                        }
                        if (isActive) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = colors.primary
                            ) {
                                Text(
                                    "当前",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.onPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                        }
                        if (!provider.isBuiltin) {
                            IconButton(onClick = { onDeleteProvider(provider.id) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "删除",
                                    tint = colors.error
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        editingProvider = AiProvider("", "", "", modelName = "")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("添加自定义厂商")
                }
            }
        }
    }

    editingProvider?.let { provider ->
        val placeholder = "••••••••••••••••"
        ProviderEditDialog(
            provider = provider,
            openKey = openKey,
            onDismiss = { editingProvider = null },
            onSave = { name, baseUrl, modelName, apiKey ->
                if (provider.id.isEmpty()) {
                    val sealedKey = if (apiKey.isNotBlank() && apiKey != placeholder) sealKey(apiKey) else ""
                    onAddProvider(name, baseUrl, modelName, sealedKey)
                } else {
                    val sealedKey = when {
                        apiKey.isBlank() || apiKey == placeholder -> provider.apiKey
                        else -> sealKey(apiKey)
                    }
                    onUpdateProvider(provider.copy(
                        name = name,
                        baseUrl = baseUrl,
                        modelName = modelName,
                        apiKey = sealedKey
                    ))
                    onSelectProvider(provider.id)
                }
                editingProvider = null
            }
        )
    }
}

@Composable
private fun ProviderEditDialog(
    provider: AiProvider,
    openKey: (String) -> String,
    onDismiss: () -> Unit,
    onSave: (name: String, baseUrl: String, modelName: String, apiKey: String) -> Unit
) {
    val colors = LocalAppColors.current
    val isNewMode = provider.id.isEmpty()
    var name by remember(provider.id) { mutableStateOf(provider.name) }
    var baseUrl by remember(provider.id) { mutableStateOf(provider.baseUrl) }
    var modelName by remember(provider.id) { mutableStateOf(provider.modelName) }
    val decryptedKey = if (provider.apiKey.isNotBlank() && !isNewMode) openKey(provider.apiKey) else ""
    var apiKey by remember(provider.id) { mutableStateOf(decryptedKey) }
    var showKey by remember { mutableStateOf(false) }
    val isFullyLocked = provider.isBuiltin && provider.id == "built-in"

    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNewMode) "添加自定义厂商" else "编辑厂商") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("厂商名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("基础地址") },
                    singleLine = true,
                    enabled = !isFullyLocked,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("https://api.example.com/v1") }
                )
                OutlinedTextField(
                    value = modelName,
                    onValueChange = { modelName = it },
                    label = { Text("模型名称") },
                    singleLine = true,
                    enabled = !isFullyLocked,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!isFullyLocked) {
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("API Key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showKey = !showKey }) {
                                Icon(
                                    if (showKey) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (showKey) "隐藏" else "显示"
                                )
                            }
                        }
                    )
                }
                if (isFullyLocked) {
                    Text(
                        "预设厂商参数不可修改",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.outline
                    )
                }
            }
        },
        confirmButton = {
            if (isFullyLocked) {
                TextButton(onClick = onDismiss) { Text("关闭") }
            } else {
                TextButton(
                    onClick = { onSave(name, baseUrl, modelName, apiKey) },
                    enabled = name.isNotBlank() && baseUrl.isNotBlank()
                ) { Text("保存") }
            }
        },
        dismissButton = {
            if (!isFullyLocked) {
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        }
    )
}
