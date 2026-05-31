package com.haoze.keynote.util

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class PreferencesManager(private val context: Context) {

    companion object {
        private val ACTIVE_PROVIDER_ID = stringPreferencesKey("active_provider_id")
        private val PROVIDERS_JSON = stringPreferencesKey("providers_json")
        private val BUILTIN_UNLOCKED = booleanPreferencesKey("builtin_unlocked")
        private val NOTE_FONT_SIZE = intPreferencesKey("note_font_size")
        private val DARK_MODE_PREFERENCE = intPreferencesKey("dark_mode_preference")
    }

    val activeProviderId: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[ACTIVE_PROVIDER_ID] ?: "built-in"
    }

    val providersJson: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[PROVIDERS_JSON] ?: "[]"
    }

    val builtinUnlocked: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[BUILTIN_UNLOCKED] ?: false
    }

    val noteFontSize: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[NOTE_FONT_SIZE] ?: 16
    }

    val darkModePreference: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[DARK_MODE_PREFERENCE] ?: 0
    }

    suspend fun saveActiveProviderId(id: String) {
        context.dataStore.edit { prefs -> prefs[ACTIVE_PROVIDER_ID] = id }
    }

    suspend fun saveProvidersJson(json: String) {
        context.dataStore.edit { prefs -> prefs[PROVIDERS_JSON] = json }
    }

    suspend fun saveBuiltinUnlocked(unlocked: Boolean) {
        context.dataStore.edit { prefs -> prefs[BUILTIN_UNLOCKED] = unlocked }
    }

    suspend fun saveNoteFontSize(sp: Int) {
        context.dataStore.edit { prefs -> prefs[NOTE_FONT_SIZE] = sp }
    }

    suspend fun saveDarkModePreference(mode: Int) {
        context.dataStore.edit { prefs -> prefs[DARK_MODE_PREFERENCE] = mode }
    }

    fun markdownModeForNote(noteId: Long): Flow<Boolean> {
        val key = booleanPreferencesKey("preview_mode_$noteId")
        return context.dataStore.data.map { prefs -> prefs[key] ?: false }
    }

    suspend fun saveMarkdownMode(noteId: Long, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[booleanPreferencesKey("preview_mode_$noteId")] = enabled
        }
    }
}
