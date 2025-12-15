package com.furka.music.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Simple DataStore manager for Furka's onboarding + source selection.
 *
 * SOURCE_MODE:
 *  - "AUDIOPHILE" → user‑selected folder (tree URI required)
 *  - "CASUAL"     → scan entire device via MediaStore
 *
 * SELECTED_URI:
 *  - Persisted tree URI when SOURCE_MODE == "AUDIOPHILE"
 */

enum class SourceMode {
    AUDIOPHILE, // Specific folder
    CASUAL      // Scan all (MediaStore)
}

data class AppSettings(
    val isSetupComplete: Boolean = false,
    val sourceMode: SourceMode = SourceMode.CASUAL,
    val selectedUri: String? = null
)

private const val DATASTORE_NAME = "furka_settings"

private val Context.dataStore by preferencesDataStore(name = DATASTORE_NAME)

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val IS_SETUP_COMPLETE = booleanPreferencesKey("is_setup_complete")
        val SOURCE_MODE = stringPreferencesKey("source_mode")
        val SELECTED_URI = stringPreferencesKey("selected_uri")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs: Preferences ->
        val modeString = prefs[Keys.SOURCE_MODE]
        val mode = when (modeString) {
            SourceMode.AUDIOPHILE.name -> SourceMode.AUDIOPHILE
            SourceMode.CASUAL.name -> SourceMode.CASUAL
            else -> SourceMode.CASUAL
        }

        AppSettings(
            isSetupComplete = prefs[Keys.IS_SETUP_COMPLETE] ?: false,
            sourceMode = mode,
            selectedUri = prefs[Keys.SELECTED_URI]
        )
    }

    suspend fun setSourceMode(mode: SourceMode, selectedUri: String?) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SOURCE_MODE] = mode.name
            if (selectedUri != null) {
                prefs[Keys.SELECTED_URI] = selectedUri
            } else {
                prefs.remove(Keys.SELECTED_URI)
            }
        }
    }

    suspend fun markSetupComplete() {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_SETUP_COMPLETE] = true
        }
    }
}


