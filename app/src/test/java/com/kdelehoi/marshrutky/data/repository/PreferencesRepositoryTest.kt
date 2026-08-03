package com.kdelehoi.marshrutky.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.kdelehoi.marshrutky.domain.model.ThemeMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Обране легко втратити мовчки: воно зберігається в одному файлі з рештою налаштувань, а формат
 * уже раз змінився — з неупорядкованої множини на список. Ці тести стережуть саме перехід.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PreferencesRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val scope = TestScope(UnconfinedTestDispatcher())

    @Test
    fun `favorites from the old format keep their place`() = scope.runTest {
        val store = dataStore()
        store.edit { it[LEGACY_KEY] = setOf("1611-merefa-kharkiv", "199-komarivka-kharkiv") }

        val saved = PreferencesRepository(store).preferences.first().favoriteRouteIds

        // Порядку в множині не було, тож єдиний передбачуваний варіант — за іменем файлу.
        assertEquals(listOf("1611-merefa-kharkiv", "199-komarivka-kharkiv"), saved)
    }

    @Test
    fun `the order chosen by hand outlives the old key`() = scope.runTest {
        val store = dataStore()
        store.edit { it[LEGACY_KEY] = setOf("a", "b") }
        val repository = PreferencesRepository(store)

        repository.saveFavoriteOrder(listOf("b", "a"))

        assertEquals(listOf("b", "a"), repository.preferences.first().favoriteRouteIds)
        // Стара множина має зникнути, інакше вона перебиватиме порядок при наступному читанні.
        assertEquals(null, store.data.first()[LEGACY_KEY])
    }

    @Test
    fun `a new favorite goes to the end of the list`() = scope.runTest {
        val repository = PreferencesRepository(dataStore())

        repository.toggleFavorite("a")
        repository.toggleFavorite("b")

        assertEquals(listOf("a", "b"), repository.preferences.first().favoriteRouteIds)
    }

    @Test
    fun `toggling twice removes the route and leaves the rest untouched`() = scope.runTest {
        val repository = PreferencesRepository(dataStore())
        repository.saveFavoriteOrder(listOf("a", "b", "c"))

        repository.toggleFavorite("b")

        assertEquals(listOf("a", "c"), repository.preferences.first().favoriteRouteIds)
    }

    @Test
    fun `an unknown theme falls back to the system one`() = scope.runTest {
        val store = dataStore()
        // Так виглядав би файл, збережений новішою версією застосунку.
        store.edit { it[THEME_KEY] = "MIDNIGHT" }

        assertEquals(ThemeMode.SYSTEM, PreferencesRepository(store).preferences.first().themeMode)
    }

    private fun dataStore(): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        scope = scope.backgroundScope
    ) {
        File(folder.root, "settings.preferences_pb")
    }

    private companion object {
        val LEGACY_KEY = stringSetPreferencesKey("favorite_route_ids")
        val THEME_KEY = stringPreferencesKey("theme_mode")
    }
}
