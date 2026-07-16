package com.rohan.mbtool.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("mbtool_prefs")

class HistoryRepository(private val ctx: Context) {

    companion object {
        val KEY_HISTORY = stringPreferencesKey("history")
        val KEY_THEME   = stringPreferencesKey("theme_mode")
        private const val MAX_ENTRIES = 50
    }

    val historyFlow: Flow<List<HistoryEntry>> = ctx.dataStore.data.map { prefs ->
        prefs[KEY_HISTORY]
            ?.split("\n")
            ?.filter { it.isNotBlank() }
            ?.mapNotNull { HistoryEntry.fromRecord(it) }
            ?.sortedByDescending { it.timestamp }
            ?: emptyList()
    }

    val themeModeFlow: Flow<String> = ctx.dataStore.data.map { prefs ->
        prefs[KEY_THEME] ?: "SYSTEM"
    }

    suspend fun addEntry(entry: HistoryEntry) {
        ctx.dataStore.edit { prefs ->
            val existing = prefs[KEY_HISTORY]
                ?.split("\n")
                ?.filter { it.isNotBlank() }
                ?: emptyList()

            val updated = (listOf(entry.toRecord()) + existing).take(MAX_ENTRIES)
            prefs[KEY_HISTORY] = updated.joinToString("\n")
        }
    }

    suspend fun clearHistory() {
        ctx.dataStore.edit { it.remove(KEY_HISTORY) }
    }

    suspend fun setThemeMode(mode: String) {
        ctx.dataStore.edit { it[KEY_THEME] = mode }
    }
}
