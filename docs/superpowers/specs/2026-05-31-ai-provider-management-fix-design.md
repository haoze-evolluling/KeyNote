# AI厂商管理修复设计文档

## 问题描述

1. **保存后数据丢失**：在AI厂商管理中，点击添加自定义厂商，输入参数后点击保存，数据无法正确持久化。
2. **UI重复**：存在"添加自定义厂商"和"编辑厂商"两个独立窗口，需要统一为一个编辑窗口。

## 设计目标

1. 修复保存逻辑，确保数据正确持久化
2. 统一编辑窗口，简化用户交互
3. 保持现有功能完整性（长按编辑、选择厂商、删除厂商等）
4. 改进错误处理，提供更好的用户反馈

## 技术方案

### 1. 保存逻辑修复

**问题分析**：
- 在`SettingsViewModel.addCustomProvider`中，UI更新（`_providers.value = list`）在DataStore保存之前执行
- 如果保存失败，UI已经更新但数据未持久化，导致数据丢失

**解决方案**：
- 将UI更新移到保存成功之后
- 添加try-catch错误处理
- 保存失败时回滚UI状态

**修改文件**：
- `app/src/main/java/com/haoze/keynote/viewmodel/SettingsViewModel.kt`

### 2. 统一编辑窗口

**当前状态**：
- `AiProviderManageScreen`中有两个状态：`editingProvider`和`isAddMode`
- `ProviderEditDialog`根据`isAddMode`显示不同标题和行为

**设计方案**：
- 移除`isAddMode`状态变量
- 使用`editingProvider`是否为null来区分模式：null表示新建，非null表示编辑
- 当`editingProvider.id`为空字符串时，表示新建模式
- 当`editingProvider.id`非空时，表示编辑模式

**修改文件**：
- `app/src/main/java/com/haoze/keynote/ui/settings/AiProviderManageScreen.kt`

### 3. 错误处理改进

**当前状态**：
- 保存操作没有错误处理
- 用户无法知道保存是否成功

**设计方案**：
- 在保存操作中添加try-catch块
- 保存失败时显示Toast或Snackbar提示
- 保存成功时显示确认信息（可选）

## 实现细节

### SettingsViewModel修改

```kotlin
fun addCustomProvider(name: String, baseUrl: String, modelName: String, apiKey: String) {
    viewModelScope.launch {
        try {
            val list = _providers.value.toMutableList()
            val id = "custom_${System.currentTimeMillis()}"
            val newProvider = AiProvider(id, name, baseUrl, apiKey = apiKey, modelName = modelName)
            list.add(newProvider)
            // 先保存到DataStore
            apiManager.saveProviders(list)
            // 保存成功后再更新UI
            _providers.value = list
            preferencesManager.saveActiveProviderId(id)
            Log.d("SettingsVM", "Custom provider added successfully: $id")
        } catch (e: Exception) {
            Log.e("SettingsVM", "Failed to add custom provider", e)
            // 可以在这里添加错误处理，如显示Toast
        }
    }
}
```

### AiProviderManageScreen修改

```kotlin
// 移除 isAddMode 状态
var editingProvider by remember { mutableStateOf<AiProvider?>(null) }

// 添加自定义厂商按钮点击
onClick = {
    editingProvider = AiProvider("", "", "", modelName = "")
}

// 长按厂商卡片
onLongClick = {
    editingProvider = provider
}

// ProviderEditDialog 调用
ProviderEditDialog(
    provider = provider,
    openKey = openKey,
    onDismiss = { editingProvider = null },
    onSave = { name, baseUrl, modelName, apiKey ->
        if (provider.id.isEmpty()) { // 新建模式
            val sealedKey = if (apiKey.isNotBlank() && apiKey != placeholder) sealKey(apiKey) else ""
            onAddProvider(name, baseUrl, modelName, sealedKey)
        } else { // 编辑模式
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
```

### ProviderEditDialog修改

```kotlin
@Composable
private fun ProviderEditDialog(
    provider: AiProvider,
    openKey: (String) -> String,
    onDismiss: () -> Unit,
    onSave: (name: String, baseUrl: String, modelName: String, apiKey: String) -> Unit
) {
    val isNewMode = provider.id.isEmpty()
    // 标题根据模式设置
    title = { Text(if (isNewMode) "添加自定义厂商" else "编辑厂商") }
    // 其余逻辑保持不变
}
```

## 测试场景

1. **新建厂商测试**：
   - 点击"添加自定义厂商"
   - 填写厂商名称、基础地址、模型名称、API Key
   - 点击保存
   - 验证数据是否持久化（退出应用重新打开）

2. **编辑厂商测试**：
   - 长按厂商卡片
   - 修改信息
   - 点击保存
   - 验证修改是否生效

3. **数据丢失测试**：
   - 添加厂商后，立即退出应用
   - 重新打开应用
   - 验证数据是否还在

4. **错误处理测试**：
   - 模拟保存失败（如存储空间不足）
   - 验证错误提示是否正确

## 回归测试

1. 确保原有功能正常：
   - 选择厂商
   - 删除厂商
   - 长按编辑
   - 加密/解密功能

2. 确保预设厂商（built-in）不受影响

3. 确保UI交互符合预期

## 风险与缓解

1. **数据迁移风险**：修改保存逻辑可能影响现有数据
   - 缓解：保持数据格式兼容，不改变AiProvider数据结构

2. **UI状态管理风险**：移除isAddMode可能引入新的bug
   - 缓解：充分测试新建和编辑两种模式

3. **错误处理风险**：添加错误处理可能影响用户体验
   - 缓解：提供清晰的错误信息，允许用户重试