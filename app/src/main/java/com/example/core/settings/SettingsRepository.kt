package com.example.core.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "smartboost_settings")

class SettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val AUTO_SCAN_DAILY = booleanPreferencesKey("auto_scan_daily")
        val AUTO_REMIND_CLEANUP = booleanPreferencesKey("auto_remind_cleanup")
        val THEME = stringPreferencesKey("app_theme")
        val LANGUAGE = stringPreferencesKey("app_language")
        val ANIMATIONS_ENABLED = booleanPreferencesKey("animations_enabled")
        val PRIVACY_MODE = booleanPreferencesKey("privacy_mode")
    }

    val autoScanDaily: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.AUTO_SCAN_DAILY] ?: true
    }

    val autoRemindCleanup: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.AUTO_REMIND_CLEANUP] ?: true
    }

    val theme: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.THEME] ?: "SYSTEM"
    }

    val language: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.LANGUAGE] ?: "en"
    }

    val animationsEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.ANIMATIONS_ENABLED] ?: true
    }

    val privacyMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.PRIVACY_MODE] ?: false
    }

    suspend fun setAutoScanDaily(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_SCAN_DAILY] = enabled
        }
    }

    suspend fun setAutoRemindCleanup(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_REMIND_CLEANUP] = enabled
        }
    }

    suspend fun setTheme(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME] = theme
        }
    }

    suspend fun setLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LANGUAGE] = language
        }
    }

    suspend fun setAnimationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ANIMATIONS_ENABLED] = enabled
        }
    }

    suspend fun setPrivacyMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PRIVACY_MODE] = enabled
        }
    }
}
