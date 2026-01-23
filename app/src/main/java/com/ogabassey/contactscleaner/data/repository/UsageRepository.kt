package com.ogabassey.contactscleaner.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.usageDataStore: DataStore<Preferences> by preferencesDataStore(name = "usage_prefs")

@Singleton
class UsageRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val FREE_ACTIONS_USED = intPreferencesKey("free_actions_used")
    }

    val freeActionsUsed: Flow<Int> = context.usageDataStore.data.map { preferences ->
        preferences[PreferencesKeys.FREE_ACTIONS_USED] ?: 0
    }

    suspend fun incrementFreeActions() {
        context.usageDataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.FREE_ACTIONS_USED] ?: 0
            preferences[PreferencesKeys.FREE_ACTIONS_USED] = current + 1
        }
    }
}
