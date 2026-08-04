package com.kdelehoi.marshrutky.viewmodel

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.kdelehoi.marshrutky.data.remote.NetworkMonitor
import com.kdelehoi.marshrutky.data.repository.PreferencesRepository
import com.kdelehoi.marshrutky.data.repository.RefreshResult
import com.kdelehoi.marshrutky.data.repository.ScheduleRepository
import com.kdelehoi.marshrutky.domain.model.Route
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

/**
 * Правила походів по мережу коштують акумулятора й нервів, а перевірити їх руками майже неможливо:
 * потрібен то порожній кеш, то шість годин очікування, то вимкнений Wi-Fi у слушний момент. Тому
 * годинник, мережа й самі розклади тут підставні, а налаштування — справжні: саме через них їде час
 * останньої синхронізації, від якого залежить, чи піде наступний запуск по дані.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleViewModelTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val dispatcher = UnconfinedTestDispatcher()
    private val scope = TestScope(dispatcher)

    @Before
    fun replaceMainDispatcher() {
        // viewModelScope працює на Main, тож без підміни ViewModel у тесті нічого не запустить.
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun restoreMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun `a cache synced an hour ago does not go to the network`() = scope.runTest {
        val settings = settings()
        settings.saveLastSyncedAt(NOW.minus(Duration.ofHours(1)))
        val schedules = FakeScheduleRepository(cached = listOf(ROUTE))

        val viewModel = start(schedules, settings)
        advanceUntilIdle()

        // Розклади міняються раз на місяці: кожен запуск будити радіомодуль нема за чим.
        assertEquals(0, schedules.refreshCount)
        assertEquals(RoutesState.Ready(listOf(ROUTE)), viewModel.state.value.routes)
    }

    @Test
    fun `a cache older than the interval goes to the network and remembers the success`() = scope.runTest {
        val settings = settings()
        settings.saveLastSyncedAt(NOW.minus(Duration.ofHours(7)))
        val schedules = FakeScheduleRepository(cached = listOf(ROUTE))

        val viewModel = start(schedules, settings)
        advanceUntilIdle()

        assertEquals(1, schedules.refreshCount)
        assertEquals(NOW, settings.preferences.first().lastSyncedAt)
        assertEquals(SyncStatus.IDLE, viewModel.state.value.syncStatus)
    }

    @Test
    fun `the very first launch has nothing to compare with, so it goes to the network`() = scope.runTest {
        val schedules = FakeScheduleRepository()

        start(schedules, settings())
        advanceUntilIdle()

        assertEquals(1, schedules.refreshCount)
    }

    @Test
    fun `an answer without changes counts as a successful sync too`() = scope.runTest {
        val settings = settings()
        val schedules = FakeScheduleRepository(cached = listOf(ROUTE), result = RefreshResult.UpToDate)

        val viewModel = start(schedules, settings)
        advanceUntilIdle()

        // Інакше «нічого не змінилося» відкладало б наступну спробу на потім і ходило б щоразу.
        assertEquals(NOW, settings.preferences.first().lastSyncedAt)
        assertEquals(SyncStatus.IDLE, viewModel.state.value.syncStatus)
    }

    @Test
    fun `a failed update leaves the last synced time alone`() = scope.runTest {
        val settings = settings()
        val schedules = FakeScheduleRepository(result = RefreshResult.Failed)

        val viewModel = start(schedules, settings)
        advanceUntilIdle()

        assertEquals(SyncStatus.FAILED, viewModel.state.value.syncStatus)
        // Час ставимо лише після успіху, інакше невдача відкладала б наступну спробу на шість годин.
        assertNull(settings.preferences.first().lastSyncedAt)
    }

    @Test
    fun `a launch without network keeps quiet`() = scope.runTest {
        val schedules = FakeScheduleRepository()

        val viewModel = start(schedules, settings(), online = false)
        advanceUntilIdle()

        assertEquals(0, schedules.refreshCount)
        // Червоний напис нізащо: людина застосунок щойно відкрила, а не просила оновлення.
        assertEquals(SyncStatus.IDLE, viewModel.state.value.syncStatus)
        assertEquals(RoutesState.Empty, viewModel.state.value.routes)
    }

    @Test
    fun `a manual refresh without network says so`() = scope.runTest {
        val viewModel = start(FakeScheduleRepository(cached = listOf(ROUTE)), settings(), online = false)
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(SyncStatus.FAILED, viewModel.state.value.syncStatus)
    }

    @Test
    fun `two quick taps on refresh make one request`() = scope.runTest {
        val settings = settings()
        settings.saveLastSyncedAt(NOW)
        val schedules = FakeScheduleRepository(cached = listOf(ROUTE), holdsUpdate = true)
        val viewModel = start(schedules, settings)
        advanceUntilIdle()

        viewModel.refresh()
        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(1, schedules.refreshCount)
        assertEquals(SyncStatus.IN_PROGRESS, viewModel.state.value.syncStatus)

        schedules.finishUpdate()
        advanceUntilIdle()

        assertEquals(SyncStatus.IDLE, viewModel.state.value.syncStatus)
    }

    @Test
    fun `an empty cache during the first sync reads as loading, not as an empty list`() = scope.runTest {
        val schedules = FakeScheduleRepository(downloaded = listOf(ROUTE), holdsUpdate = true)

        val viewModel = start(schedules, settings())
        advanceUntilIdle()

        // Саме тут колись блимало «Маршрутів немає» на першому запуску.
        assertEquals(RoutesState.Loading, viewModel.state.value.routes)

        schedules.finishUpdate()
        advanceUntilIdle()

        assertEquals(RoutesState.Ready(listOf(ROUTE)), viewModel.state.value.routes)
    }

    @Test
    fun `a favorite added on another tab reaches the state`() = scope.runTest {
        val viewModel = start(FakeScheduleRepository(cached = listOf(ROUTE)), settings())
        advanceUntilIdle()

        viewModel.toggleFavorite(ROUTE.id)
        advanceUntilIdle()

        assertEquals(listOf(ROUTE.id), viewModel.state.value.favoriteRouteIds)
    }

    /**
     * Стан живе, поки на нього хтось дивиться, тож у тесті потрібен свій підписник — інакше
     * `state.value` назавжди лишиться початковим значенням.
     */
    private fun TestScope.start(
        schedules: ScheduleRepository,
        settings: PreferencesRepository,
        online: Boolean = true
    ): ScheduleViewModel {
        val viewModel = ScheduleViewModel(
            scheduleRepository = schedules,
            preferencesRepository = settings,
            networkMonitor = FakeNetworkMonitor(online),
            clock = Clock.fixed(NOW, ZoneOffset.UTC)
        )
        backgroundScope.launch { viewModel.state.collect {} }
        return viewModel
    }

    private fun settings() = PreferencesRepository(
        PreferenceDataStoreFactory.create(scope = scope.backgroundScope) {
            File(folder.root, "settings.preferences_pb")
        }
    )

    private companion object {
        val NOW: Instant = Instant.parse("2026-08-04T18:00:00Z")
        val ROUTE = Route(id = "199-komarivka-kharkiv", number = "199", name = "Комарівка — Харків", directions = emptyList())
    }
}

private class FakeNetworkMonitor(override val isOnline: Boolean) : NetworkMonitor

private class FakeScheduleRepository(
    private val cached: List<Route> = emptyList(),
    private val downloaded: List<Route> = cached,
    private val result: RefreshResult = RefreshResult.Updated,
    /** Тримає оновлення в польоті: так перевіряється те, що видно саме під час завантаження. */
    private val holdsUpdate: Boolean = false
) : ScheduleRepository {

    private val _routes = MutableStateFlow<List<Route>?>(null)
    override val routes: StateFlow<List<Route>?> = _routes.asStateFlow()

    private val update = CompletableDeferred<Unit>()

    var refreshCount = 0
        private set

    override suspend fun loadCached() {
        if (_routes.value == null) _routes.value = cached
    }

    override suspend fun refresh(): RefreshResult {
        refreshCount++
        if (holdsUpdate) update.await()
        if (result == RefreshResult.Updated) _routes.value = downloaded
        return result
    }

    fun finishUpdate() {
        update.complete(Unit)
    }
}
