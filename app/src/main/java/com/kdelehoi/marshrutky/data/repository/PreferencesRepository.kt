package com.kdelehoi.marshrutky.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesRepository(private val context: Context) {

    val selection: Flow<Selection> = context.dataStore.data.map { preferences ->
        Selection(
            routeId = preferences[ROUTE_ID_KEY],
            directionIndex = preferences[DIRECTION_INDEX_KEY] ?: 0
        )
    }

    suspend fun saveSelection(routeId: String, directionIndex: Int) {
        context.dataStore.edit { preferences ->
            preferences[ROUTE_ID_KEY] = routeId
            preferences[DIRECTION_INDEX_KEY] = directionIndex
        }
    }

    data class Selection(
        val routeId: String?,
        val directionIndex: Int
    )

    companion object {
        private val ROUTE_ID_KEY = stringPreferencesKey("selected_route_id")
        private val DIRECTION_INDEX_KEY = intPreferencesKey("selected_direction_index")
    }
}
