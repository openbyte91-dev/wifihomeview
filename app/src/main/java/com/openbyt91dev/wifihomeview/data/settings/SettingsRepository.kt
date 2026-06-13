package com.openbyt91dev.wifihomeview.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    val settings: Flow<AppSettings> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(androidx.datastore.preferences.core.emptyPreferences())
            else throw error
        }
        .map { preferences ->
            AppSettings(
                backgroundAlertsEnabled = preferences[BACKGROUND_ALERTS_ENABLED] ?: true,
                scanIntensity = preferences[SCAN_INTENSITY]?.let { value ->
                    ScanIntensity.entries.firstOrNull { it.name == value }
                } ?: ScanIntensity.DEEP
            )
        }

    suspend fun setBackgroundAlertsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[BACKGROUND_ALERTS_ENABLED] = enabled
        }
    }

    suspend fun setScanIntensity(intensity: ScanIntensity) {
        dataStore.edit { preferences ->
            preferences[SCAN_INTENSITY] = intensity.name
        }
    }

    private companion object {
        val BACKGROUND_ALERTS_ENABLED = booleanPreferencesKey("background_alerts_enabled")
        val SCAN_INTENSITY = stringPreferencesKey("scan_intensity")
    }
}
