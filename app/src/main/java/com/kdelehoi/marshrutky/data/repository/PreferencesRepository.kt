package com.kdelehoi.marshrutky.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.kdelehoi.marshrutky.domain.model.AppLanguage
import com.kdelehoi.marshrutky.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

/**
 * Сховище налаштувань на пристрої. Ім'я файлу те саме, що й раніше, — інакше після оновлення
 * застосунку обране й тема опинилися б у новому порожньому файлі.
 */
fun createSettingsDataStore(context: Context): DataStore<Preferences> =
    PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("settings") }

/** Усе, що застосунок пам'ятає між запусками. */
data class UserPreferences(
    /** Обране в порядку, який задав користувач. */
    val favoriteRouteIds: List<String> = emptyList(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val language: AppLanguage = AppLanguage.SYSTEM,
    /** Коли востаннє вдалося дістати розклади з мережі. `null` — ще жодного разу. */
    val lastSyncedAt: Instant? = null,
    /** Зупинка на вкладці «Найближчі». `null` — користувач ще нічого не вибирав. */
    val selectedStop: String? = null
)

/**
 * Тут, на відміну від [ScheduleRepository], інтерфейс не потрібен: шов уже є — саме сховище заходить
 * параметром, тож у тестах працює справжній репозиторій над файлом у тимчасовій теці. Підставний
 * тільки приховав би формат, який уже раз мінявся.
 */
class PreferencesRepository(private val dataStore: DataStore<Preferences>) {

    /**
     * Один потік на всі налаштування: чотири окремі `map` над тим самим DataStore означали б
     * чотирьох читачів одного файлу замість одного.
     */
    val preferences: Flow<UserPreferences> = dataStore.data.map { stored ->
        UserPreferences(
            favoriteRouteIds = readFavorites(stored),
            themeMode = stored[THEME_MODE_KEY]
                ?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
                ?: ThemeMode.SYSTEM,
            language = stored[LANGUAGE_KEY]
                ?.let { name -> AppLanguage.entries.firstOrNull { it.name == name } }
                ?: AppLanguage.SYSTEM,
            lastSyncedAt = stored[LAST_SYNCED_AT_KEY]?.let(Instant::ofEpochSecond),
            selectedStop = stored[SELECTED_STOP_KEY]
        )
    }

    /** Нове обране стає в кінець списку — туди, де користувач його й шукатиме. */
    suspend fun toggleFavorite(routeId: String) {
        dataStore.edit { preferences ->
            val current = readFavorites(preferences)
            writeFavorites(
                preferences,
                if (routeId in current) current - routeId else current + routeId
            )
        }
    }

    suspend fun saveFavoriteOrder(routeIds: List<String>) {
        dataStore.edit { preferences -> writeFavorites(preferences, routeIds) }
    }

    suspend fun saveThemeMode(themeMode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = themeMode.name
        }
    }

    suspend fun saveLanguage(language: AppLanguage) {
        dataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = language.name
        }
    }

    suspend fun saveLastSyncedAt(instant: Instant) {
        dataStore.edit { preferences ->
            preferences[LAST_SYNCED_AT_KEY] = instant.epochSecond
        }
    }

    suspend fun saveSelectedStop(stopName: String) {
        dataStore.edit { preferences ->
            preferences[SELECTED_STOP_KEY] = stopName
        }
    }

    private fun readFavorites(preferences: Preferences): List<String> =
        preferences[FAVORITES_ORDER_KEY]
            ?.split(SEPARATOR)
            ?.filter { it.isNotBlank() }
            ?: preferences[LEGACY_FAVORITES_KEY].orEmpty().sorted()

    private fun writeFavorites(preferences: MutablePreferences, routeIds: List<String>) {
        preferences[FAVORITES_ORDER_KEY] = routeIds.joinToString(SEPARATOR)
        // Стару множину прибираємо, щоб вона не перебивала порядок після перевстановлення.
        preferences.remove(LEGACY_FAVORITES_KEY)
    }

    private companion object {
        /** Ідентифікатор маршруту — це ім'я файлу, тож перенесення рядка в ньому не буде. */
        const val SEPARATOR = "\n"

        val FAVORITES_ORDER_KEY = stringPreferencesKey("favorite_route_ids_ordered")
        val LEGACY_FAVORITES_KEY = stringSetPreferencesKey("favorite_route_ids")
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        val LANGUAGE_KEY = stringPreferencesKey("language")
        val LAST_SYNCED_AT_KEY = longPreferencesKey("last_synced_at")
        val SELECTED_STOP_KEY = stringPreferencesKey("selected_stop")
    }
}
