package com.yourapp.whatsappclone.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "wa_clone_session")

/**
 * DataStore-backed persistence for the first-launch disclaimer acceptance and
 * the user's theme choice. (The WhatsApp session itself lives in the
 * WebView's cookie/DOM-storage stores.)
 */
class SessionStore private constructor(private val dataStore: DataStore<Preferences>) {

    suspend fun disclaimerAccepted(): Boolean =
        dataStore.data.first()[KEY_DISCLAIMER_ACCEPTED] ?: false

    suspend fun setDisclaimerAccepted(accepted: Boolean) {
        dataStore.edit { it[KEY_DISCLAIMER_ACCEPTED] = accepted }
    }

    suspend fun themeMode(): String =
        dataStore.data.first()[KEY_THEME_MODE] ?: "SYSTEM"

    suspend fun setThemeMode(mode: String) {
        dataStore.edit { it[KEY_THEME_MODE] = mode }
    }

    companion object {
        private val KEY_DISCLAIMER_ACCEPTED = booleanPreferencesKey("disclaimer_accepted")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")

        @Volatile
        private var instance: SessionStore? = null

        fun get(context: Context): SessionStore = instance ?: synchronized(this) {
            instance ?: SessionStore(context.applicationContext.sessionDataStore)
                .also { instance = it }
        }
    }
}
