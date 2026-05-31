package com.haoze.keynote.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.haoze.keynote.data.remote.AiApiManager
import com.haoze.keynote.data.remote.AiProvider
import com.haoze.keynote.util.KeyObfuscator
import com.haoze.keynote.util.PreferencesManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.util.Log
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.haoze.keynote.ui.theme.DarkModePreference
import com.haoze.keynote.ui.theme.toDarkModePreference
import com.haoze.keynote.ui.theme.toInt

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesManager = PreferencesManager(application)
    private val apiManager = AiApiManager(preferencesManager)

    val activeProviderId: StateFlow<String> = preferencesManager.activeProviderId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "built-in")

    private val _providers = MutableStateFlow<List<AiProvider>>(emptyList())
    val providers: StateFlow<List<AiProvider>> = _providers.asStateFlow()

    val builtinUnlocked: StateFlow<Boolean> = preferencesManager.builtinUnlocked
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val noteFontSize: StateFlow<Int> = preferencesManager.noteFontSize
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 16)

    val darkModePreference: StateFlow<DarkModePreference> = preferencesManager.darkModePreference
        .map { it.toDarkModePreference() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DarkModePreference.SYSTEM)

    init {
        viewModelScope.launch {
            preferencesManager.providersJson
                .map { raw ->
                    Log.d("SettingsVM", "providersJson emitted: ${raw.take(100)}")
                    try {
                        val type = object : TypeToken<List<AiProvider>>() {}.type
                        val parsed = Gson().fromJson<List<AiProvider>>(raw, type)
                        if (parsed.isEmpty()) {
                            Log.d("SettingsVM", "Parsed empty, seeding defaults")
                            defaultProviders()
                        } else {
                            Log.d("SettingsVM", "Parsed ${parsed.size} providers: ${parsed.map { "${it.id}:${it.name}" }}")
                            parsed
                        }
                    } catch (e: Exception) {
                        Log.e("SettingsVM", "Parse error", e)
                        defaultProviders()
                    }
                }
                .collect {
                    Log.d("SettingsVM", "Updating _providers with ${it.size} items")
                    _providers.value = it
                }
        }
    }

    fun getActiveProvider(): AiProvider? {
        val id = activeProviderId.value
        return providers.value.find { it.id == id }
    }

    fun selectProvider(id: String) {
        viewModelScope.launch { preferencesManager.saveActiveProviderId(id) }
    }

    fun updateProvider(updated: AiProvider) {
        val list = _providers.value.toMutableList()
        val idx = list.indexOfFirst { it.id == updated.id }
        if (idx >= 0) {
            list[idx] = updated
            _providers.value = list
            viewModelScope.launch {
                Log.d("SettingsVM", "Saving providers: ${list.map { "${it.id}:${it.name}:${it.baseUrl}" }}")
                apiManager.saveProviders(list)
                Log.d("SettingsVM", "Providers saved to DataStore")
            }
        } else {
            Log.d("SettingsVM", "Provider not found: ${updated.id}, current list: ${_providers.value.map { it.id }}")
        }
    }

    fun addCustomProvider(name: String, baseUrl: String, modelName: String, apiKey: String) {
        viewModelScope.launch {
            try {
                val list = _providers.value.toMutableList()
                val id = "custom_${System.currentTimeMillis()}"
                val newProvider = AiProvider(id, name, baseUrl, apiKey = apiKey, modelName = modelName)
                list.add(newProvider)
                apiManager.saveProviders(list)
                _providers.value = list
                preferencesManager.saveActiveProviderId(id)
                Log.d("SettingsVM", "Custom provider added successfully: $id")
            } catch (e: Exception) {
                Log.e("SettingsVM", "Failed to add custom provider", e)
            }
        }
    }

    fun deleteCustomProvider(id: String) {
        val list = _providers.value.toMutableList()
        list.removeAll { it.id == id }
        _providers.value = list
        viewModelScope.launch {
            apiManager.saveProviders(list)
            if (activeProviderId.value == id) {
                preferencesManager.saveActiveProviderId("built-in")
            }
        }
    }

    fun setNoteFontSize(sp: Int) {
        viewModelScope.launch { preferencesManager.saveNoteFontSize(sp) }
    }

    fun setDarkMode(preference: DarkModePreference) {
        viewModelScope.launch { preferencesManager.saveDarkModePreference(preference.toInt()) }
    }

    fun sealZidaipass(plain: String): String = KeyObfuscator.sealUserZidaipass(plain)
    fun openZidaipass(sealed: String): String = KeyObfuscator.openUserZidaipass(sealed)

    private fun defaultProviders(): List<AiProvider> = listOf(
        AiProvider("built-in", "预设", "https://api.deepseek.com", isBuiltin = true)
    )
}
