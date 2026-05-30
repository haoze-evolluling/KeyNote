package com.haoze.keynote.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.haoze.keynote.data.db.NoteDatabase
import com.haoze.keynote.data.db.entity.CategoryEntity
import com.haoze.keynote.data.repository.BillRepository
import com.haoze.keynote.data.repository.NoteRepository
import com.haoze.keynote.data.repository.ScheduleRepository
import com.haoze.keynote.data.remote.AiApiManager
import com.haoze.keynote.data.remote.DeepSeekRequest
import com.haoze.keynote.data.remote.Message
import com.haoze.keynote.util.PreferencesManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: Long,
    val role: String,
    val content: String,
    val isBillRelated: Boolean = false,
    val billJson: String? = null,
    val isScheduleRelated: Boolean = false,
    val scheduleJson: String? = null
)

data class PendingBill(
    val item: String,
    val amount: Double,
    val date: Long,
    val suggestedCategory: String?
)

class AIChatViewModel(application: Application) : AndroidViewModel(application) {

    private var messageCounter = 0L

    private val preferencesManager = PreferencesManager(application)
    private val apiManager = AiApiManager(preferencesManager)
    private val noteRepository: NoteRepository
    private val billRepository: BillRepository
    private val scheduleRepository: ScheduleRepository

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isCreatingBill = MutableStateFlow(false)
    val isCreatingBill: StateFlow<Boolean> = _isCreatingBill.asStateFlow()

    private val _createdNoteId = MutableSharedFlow<Long>()
    val createdNoteId: SharedFlow<Long> = _createdNoteId.asSharedFlow()

    private val _categories = MutableStateFlow<List<CategoryEntity>>(emptyList())
    val categories: StateFlow<List<CategoryEntity>> = _categories.asStateFlow()

    private val _pendingBill = MutableStateFlow<PendingBill?>(null)
    val pendingBill: StateFlow<PendingBill?> = _pendingBill.asStateFlow()

    init {
        val db = NoteDatabase.getDatabase(application)
        noteRepository = NoteRepository(db.noteDao(), db.tagDao(), preferencesManager)
        billRepository = BillRepository(db.billDao(), db.categoryDao())
        scheduleRepository = ScheduleRepository(db.scheduleDao())

        viewModelScope.launch {
            billRepository.getAllCategories().collect { _categories.value = it }
        }
    }

    fun sendMessage(content: String) {
        if (content.isBlank() || _isLoading.value) return

        val userMessage = ChatMessage(role = "user", content = content, id = messageCounter++)
        _messages.value = _messages.value + userMessage
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val provider = apiManager.getActiveProvider()
                val apiKey = apiManager.resolveApiKey(provider)

                if (apiKey.isBlank()) {
                    _messages.value = _messages.value + ChatMessage(
                        role = "assistant",
                        content = "请先在设置中配置 API Key",
                        id = messageCounter++
                    )
                    return@launch
                }

                val api = apiManager.createApi()
                val historyMessages = _messages.value.map { msg ->
                    Message(role = msg.role, content = msg.content)
                }
                val currentTime = java.text.SimpleDateFormat(
                    "yyyy-MM-dd HH:mm", java.util.Locale.getDefault()
                ).format(java.util.Date())
                val categoryNames = _categories.value.joinToString("、") { it.name }
                val systemPrompt = """
你是个人智能助理，管理笔记、账单、日程。当前时间：$currentTime。
可用类别：$categoryNames
必须始终回复内容，不能为空。

【账单识别】消息中同时出现金额+消费场景时（吃饭、打车、购物、充值、买、花了、消费、支出等），回复格式：
[BILL]
{"item":"消费项目","amount":金额数字,"date":"yyyy-MM-dd HH:mm","category":"最匹配类别"}
友好回复内容

规则：amount用数字（10或99.9）；未提时间则用当前时间；item从上下文推断（"吃了午餐"→"午餐"）；category匹配现有类别，无合适则新建；口语化也算（"打车20"、"充100话费"）；仅提金额无消费场景（"我有12块"）则正常回复，不加[BILL]。

【日程识别】消息中提到时间相关安排时（明天开会、周五去医院等），回复格式：
[SCHEDULE]
{"title":"日程标题","date":"yyyy-MM-dd HH:mm","endDate":"","location":"","description":""}
友好回复内容

规则：title从上下文推断；只说日期默认09:00；endDate/location/description提到则填，否则留空；仅提时间无安排（"明天是周五"）则正常回复，不加[SCHEDULE]。

同时匹配账单和日程时，优先识别为账单。
""".trimIndent()
                val request = DeepSeekRequest(
                    model = provider.modelName,
                    messages = listOf(
                        Message(role = "system", content = systemPrompt)
                    ) + historyMessages,
                    maxTokens = 500
                )
                val response = api.generateTags(auth = "Bearer $apiKey", request = request)
                val rawReply = response.choices.firstOrNull()?.message?.content ?: "无响应"
                val billTagIndex = rawReply.indexOf("[BILL]")
                val isBillRelated = billTagIndex >= 0

                var billJson: String? = null
                var reply = rawReply.trim()

                if (isBillRelated) {
                    val afterBill = rawReply.substring(billTagIndex + 6).trim()
                    val lines = afterBill.lines()
                    val jsonLine = lines.firstOrNull { it.trim().startsWith("{") && it.trim().endsWith("}") }
                    if (jsonLine != null) {
                        billJson = jsonLine.trim()
                        val beforeBill = rawReply.substring(0, billTagIndex).trim()
                        val otherLines = lines.filter { it.trim() != jsonLine.trim() }
                            .joinToString("\n").trim()
                        reply = listOf(beforeBill, otherLines).filter { it.isNotBlank() }
                            .joinToString("\n\n")
                    } else {
                        val beforeBill = rawReply.substring(0, billTagIndex).trim()
                        reply = listOf(beforeBill, afterBill).filter { it.isNotBlank() }
                            .joinToString("\n\n")
                    }
                }

                val isScheduleRelated = !isBillRelated && rawReply.indexOf("[SCHEDULE]") >= 0
                var scheduleJson: String? = null

                if (isScheduleRelated) {
                    val scheduleTagIndex = rawReply.indexOf("[SCHEDULE]")
                    val afterSchedule = rawReply.substring(scheduleTagIndex + 10).trim()
                    val lines = afterSchedule.lines()
                    val jsonLine = lines.firstOrNull { it.trim().startsWith("{") && it.trim().endsWith("}") }
                    if (jsonLine != null) {
                        scheduleJson = jsonLine.trim()
                        val beforeSchedule = rawReply.substring(0, scheduleTagIndex).trim()
                        val otherLines = lines.filter { it.trim() != jsonLine.trim() }
                            .joinToString("\n").trim()
                        reply = listOf(beforeSchedule, otherLines).filter { it.isNotBlank() }
                            .joinToString("\n\n")
                    } else {
                        val beforeSchedule = rawReply.substring(0, scheduleTagIndex).trim()
                        reply = listOf(beforeSchedule, afterSchedule).filter { it.isNotBlank() }
                            .joinToString("\n\n")
                    }
                }

                _messages.value = _messages.value + ChatMessage(
                    role = "assistant", content = reply, id = messageCounter++,
                    isBillRelated = isBillRelated, billJson = billJson,
                    isScheduleRelated = isScheduleRelated, scheduleJson = scheduleJson
                )
            } catch (e: Exception) {
                _messages.value = _messages.value + ChatMessage(
                    role = "assistant",
                    content = "请求失败: ${e.message ?: "未知错误"}",
                    id = messageCounter++
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createNoteFromContent(content: String) {
        viewModelScope.launch {
            try {
                val noteId = noteRepository.createNote("", content)
                preferencesManager.saveMarkdownMode(noteId, true)
                _createdNoteId.emit(noteId)
            } catch (_: Exception) {
                _createdNoteId.emit(-1L)
            }
        }
    }

    fun createNoteFromMessages() {
        viewModelScope.launch {
            try {
                val markdown = buildString {
                    _messages.value.forEach { message ->
                        if (message.role == "system") return@forEach
                        if (message.content.isBlank()) return@forEach
                        val label = if (message.role == "user") "**用户：**" else "**AI：**"
                        val displayContent = if (message.isBillRelated || message.isScheduleRelated) {
                            message.content.lines()
                                .filter { line ->
                                    !line.trimStart().startsWith("[BILL]") &&
                                    !line.trimStart().startsWith("[SCHEDULE]")
                                }
                                .joinToString("\n")
                                .trim()
                        } else {
                            message.content
                        }
                        if (displayContent.isBlank()) return@forEach
                        if (isNotEmpty()) append("\n\n")
                        append(label).append(" ").append(displayContent)
                    }
                }
                val noteId = noteRepository.createNote("", markdown)
                preferencesManager.saveMarkdownMode(noteId, true)
                _createdNoteId.emit(noteId)
            } catch (_: Exception) {
                _createdNoteId.emit(-1L)
            }
        }
    }

    fun prepareBillFromAI(billJson: String?) {
        if (billJson.isNullOrBlank()) return
        try {
            val jsonStr = billJson
                .removePrefix("```json").removePrefix("```")
                .removeSuffix("```")
                .trim()
            val json = org.json.JSONObject(jsonStr)
            val item = json.getString("item")
            val amount = json.getDouble("amount")
            val dateStr = json.optString("date", "")
            val date = if (dateStr.isNotBlank()) {
                try {
                    java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                        .parse(dateStr)?.time ?: System.currentTimeMillis()
                } catch (_: Exception) { System.currentTimeMillis() }
            } else {
                System.currentTimeMillis()
            }
            val category = json.optString("category", "").ifBlank { null }
            _pendingBill.value = PendingBill(item, amount, date, category)
        } catch (_: Exception) {
            _messages.value = _messages.value + ChatMessage(
                role = "assistant",
                content = "❌ 无法解析账单信息，请重试",
                id = messageCounter++
            )
        }
    }

    fun confirmCreateBill(categoryId: Long?) {
        val bill = _pendingBill.value ?: return
        _pendingBill.value = null
        _isCreatingBill.value = true
        viewModelScope.launch {
            try {
                val finalCategoryId = categoryId ?: run {
                    val suggested = bill.suggestedCategory
                    if (!suggested.isNullOrBlank()) {
                        val existing = _categories.value.find { it.name == suggested }
                        if (existing != null) {
                            existing.id
                        } else {
                            billRepository.insertCategory(suggested)
                        }
                    } else null
                }
                billRepository.insertBill(bill.item, bill.amount, bill.date, finalCategoryId)
                val catName = finalCategoryId?.let { id ->
                    _categories.value.find { it.id == id }?.name
                } ?: "未分类"
                _messages.value = _messages.value + ChatMessage(
                    role = "assistant",
                    content = "✅ 账单已创建：${bill.item} - ¥${String.format("%.2f", bill.amount)}（$catName）",
                    id = messageCounter++
                )
            } catch (e: Exception) {
                _messages.value = _messages.value + ChatMessage(
                    role = "assistant",
                    content = "❌ 账单创建失败：${e.message ?: "未知错误"}",
                    id = messageCounter++
                )
            } finally {
                _isCreatingBill.value = false
            }
        }
    }

    fun dismissPendingBill() {
        _pendingBill.value = null
    }

    fun createScheduleFromAI(scheduleJson: String?) {
        viewModelScope.launch {
            if (scheduleJson.isNullOrBlank()) {
                _messages.value = _messages.value + ChatMessage(
                    role = "assistant",
                    content = "❌ 日程创建失败：未找到日程信息",
                    id = messageCounter++
                )
                return@launch
            }

            try {
                val jsonStr = scheduleJson
                    .removePrefix("```json").removePrefix("```")
                    .removeSuffix("```")
                    .trim()
                val json = org.json.JSONObject(jsonStr)
                val title = json.getString("title")

                val dateStr = json.optString("date", "")
                val date = if (dateStr.isNotBlank()) {
                    try {
                        java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                            .parse(dateStr)?.time ?: System.currentTimeMillis()
                    } catch (_: Exception) { System.currentTimeMillis() }
                } else {
                    System.currentTimeMillis()
                }

                val endDateStr = json.optString("endDate", "")
                val endDate = if (endDateStr.isNotBlank()) {
                    try {
                        java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                            .parse(endDateStr)?.time
                    } catch (_: Exception) { null }
                } else null

                val location = json.optString("location", "").ifBlank { null }
                val description = json.optString("description", "").ifBlank { null }

                scheduleRepository.insertSchedule(title, date, endDate, location, description)
                _messages.value = _messages.value + ChatMessage(
                    role = "assistant",
                    content = "✅ 日程已创建：${title}",
                    id = messageCounter++
                )
            } catch (e: org.json.JSONException) {
                _messages.value = _messages.value + ChatMessage(
                    role = "assistant",
                    content = "❌ 日程创建失败：无法解析日程信息",
                    id = messageCounter++
                )
            } catch (e: Exception) {
                _messages.value = _messages.value + ChatMessage(
                    role = "assistant",
                    content = "❌ 日程创建失败：${e.message ?: "未知错误"}",
                    id = messageCounter++
                )
            }
        }
    }

    fun clearMessages() {
        _messages.value = emptyList()
    }
}
