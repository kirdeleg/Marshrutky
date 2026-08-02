package com.kdelehoi.marshrutky.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kdelehoi.marshrutky.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesRepository(private val context: Context) {

    val favoriteRouteIds: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[FAVORITES_KEY].orEmpty()
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        preferences[THEME_MODE_KEY]
            ?.let { stored -> ThemeMode.entries.firstOrNull { it.name == stored } }
            ?: ThemeMode.SYSTEM
    }

    suspend fun toggleFavorite(routeId: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[FAVORITES_KEY].orEmpty()
            preferences[FAVORITES_KEY] = if (routeId in current) {
                current - routeId
            } else {
                current + routeId
            }
        }
    }

    suspend fun saveThemeMode(themeMode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = themeMode.name
        }
    }

    private companion object {
        val FAVORITES_KEY = stringSetPreferencesKey("favorite_route_ids")
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
    }
}
