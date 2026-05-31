package com.haoze.keynote.data.remote

import com.haoze.keynote.util.KeyObfuscator
import com.haoze.keynote.util.PreferencesManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first

data class AiProvider(
    val id: String,
    val name: String,
    val baseUrl: String,
    val apiKey: String = "",
    val modelName: String = "deepseek-v4-flash",
    val isBuiltin: Boolean = false
)

class AiApiManager(private val preferencesManager: PreferencesManager) {

    private val gson = Gson()

    suspend fun getActiveProvider(): AiProvider {
        val activeId = preferencesManager.activeProviderId.first()
        val providers = getProviders()
        return providers.find { it.id == activeId } ?: providers.firstOrNull()
            ?: AiProvider("built-in", "预设", "https://api.deepseek.com", isBuiltin = true)
    }

    suspend fun getProviders(): List<AiProvider> {
        val raw = preferencesManager.providersJson.first()
        return try {
            val type = object : TypeToken<List<AiProvider>>() {}.type
            val parsed = gson.fromJson<List<AiProvider>>(raw, type)
            if (parsed.isEmpty()) seedDefaults() else parsed.map { it.sanitize() }
        } catch (_: Exception) {
            seedDefaults()
        }
    }

    private fun AiProvider.sanitize() = AiProvider(
        id = id ?: "",
        name = name ?: "",
        baseUrl = baseUrl ?: "",
        apiKey = apiKey ?: "",
        modelName = modelName ?: "deepseek-v4-flash",
        isBuiltin = isBuiltin
    )

    private suspend fun seedDefaults(): List<AiProvider> {
        val defaults = defaultProviders()
        saveProviders(defaults)
        return defaults
    }

    suspend fun saveProviders(providers: List<AiProvider>) {
        preferencesManager.saveProvidersJson(gson.toJson(providers))
    }

    suspend fun resolveApiKey(provider: AiProvider): String {
        return if (provider.isBuiltin && provider.id == "built-in") {
            KeyObfuscator.builtinZidaipass
        } else {
            val key = provider.apiKey
            if (key.isBlank()) "" else KeyObfuscator.openUserZidaipass(key)
        }
    }

    suspend fun createApi(): DeepSeekApi {
        val provider = getActiveProvider()
        val url = provider.baseUrl.trimEnd('/')
        if (url.isBlank()) throw IllegalStateException("厂商基础地址未配置")
        return DeepSeekApi.create("$url/")
    }

    suspend fun getAuthHeader(): String {
        val provider = getActiveProvider()
        return "Bearer ${resolveApiKey(provider)}"
    }

    suspend fun getModelName(): String {
        return getActiveProvider().modelName
    }

    private fun defaultProviders(): List<AiProvider> = listOf(
        AiProvider("built-in", "预设", "https://api.deepseek.com", isBuiltin = true)
    )
}
