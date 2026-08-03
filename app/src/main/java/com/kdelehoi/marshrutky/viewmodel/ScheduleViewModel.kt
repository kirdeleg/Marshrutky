package com.kdelehoi.marshrutky.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kdelehoi.marshrutky.data.remote.NetworkMonitor
import com.kdelehoi.marshrutky.data.repository.PreferencesRepository
import com.kdelehoi.marshrutky.data.repository.RefreshResult
import com.kdelehoi.marshrutky.data.repository.ScheduleRepository
import com.kdelehoi.marshrutky.domain.DepartureCalculator
import com.kdelehoi.marshrutky.domain.model.DayType
import com.kdelehoi.marshrutky.domain.model.Route
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime

enum class SyncStatus {
    IDLE,
    IN_PROGRESS,
    FAILED
}

data class ScheduleUiState(
    val isLoading: Boolean = true,
    val routes: List<Route> = emptyList(),
    val favoriteRouteIds: List<String> = emptyList(),
    val now: LocalDateTime = LocalDateTime.now(),
    val syncStatus: SyncStatus = SyncStatus.IDLE,
    val lastSyncedAt: Instant? = null,
    val selectedStop: String? = null
) {
    /** Саме в порядку, який задав користувач, а не в тому, у якому маршрути лежать у файлах. */
    val favoriteRoutes: List<Route>
        get() = favoriteRouteIds.mapNotNull { id -> routes.firstOrNull { it.id == id } }

    val today: DayType
        get() = DepartureCalculator.dayTypeOf(now.toLocalDate())

    fun routeById(routeId: String): Route? = routes.firstOrNull { it.id == routeId }
}

class ScheduleViewModel(
    private val scheduleRepository: ScheduleRepository,
    private val preferencesRepository: PreferencesRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val syncStatus = MutableStateFlow(SyncStatus.IDLE)

    /**
     * Поточний час із точністю до хвилини. Холодний Flow, тож він працює рівно доти, доки на
     * екран хтось дивиться: щойно застосунок згорнули, підписник відвалюється і годинник
     * зупиняється сам. Прокидання рахуємо щоразу від поточного часу, тому дрейф не накопичується.
     */
    private val minutes: Flow<LocalDateTime> = flow {
        while (true) {
            val now = LocalDateTime.now()
            emit(now)
            delay(millisUntilNextMinute(now))
        }
    }

    /**
     * `WhileSubscribed` — це те, що робить «нічого не робити у фоні» властивістю конструкції, а
     * не домовленістю. Пауза перед зупинкою потрібна, щоб поворот екрана чи короткий перехід між
     * екранами не перезапускали весь ланцюжок дарма.
     */
    val state: StateFlow<ScheduleUiState> = combine(
        scheduleRepository.routes,
        preferencesRepository.preferences,
        minutes,
        syncStatus
    ) { routes, preferences, now, sync ->
        ScheduleUiState(
            isLoading = routes == null,
            routes = routes.orEmpty(),
            favoriteRouteIds = preferences.favoriteRouteIds,
            now = now,
            syncStatus = sync,
            lastSyncedAt = preferences.lastSyncedAt,
            selectedStop = preferences.selectedStop
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(UNSUBSCRIBE_DELAY_MILLIS),
        initialValue = ScheduleUiState()
    )

    init {
        loadAndSync()
    }

    fun toggleFavorite(routeId: String) {
        viewModelScope.launch { preferencesRepository.toggleFavorite(routeId) }
    }

    fun saveFavoriteOrder(routeIds: List<String>) {
        viewModelScope.launch { preferencesRepository.saveFavoriteOrder(routeIds) }
    }

    fun selectStop(stopName: String) {
        viewModelScope.launch { preferencesRepository.saveSelectedStop(stopName) }
    }

    /** Кнопка «Оновити зараз»: іде по мережу незалежно від того, коли синхронізувалися востаннє. */
    fun refresh() = sync(isManual = true)

    /** Спершу показуємо те, що вже є на пристрої, і аж потім вирішуємо, чи йти по свіже. */
    private fun loadAndSync() {
        viewModelScope.launch {
            scheduleRepository.loadCached()

            val lastSyncedAt = preferencesRepository.preferences.first().lastSyncedAt
            val isFresh = lastSyncedAt != null &&
                Duration.between(lastSyncedAt, Instant.now()) < SYNC_INTERVAL
            // Розклади міняються раз на місяці, тож ходити по мережу на кожен запуск нема сенсу.
            if (!isFresh) sync(isManual = false)
        }
    }

    private fun sync(isManual: Boolean) {
        if (syncStatus.value == SyncStatus.IN_PROGRESS) return

        viewModelScope.launch {
            if (!networkMonitor.isOnline) {
                // Про невдачу повідомляємо, лише якщо оновлення попросили руками: інакше це просто
                // марно розбуджений радіомодуль і червоний напис нізащо.
                if (isManual) syncStatus.value = SyncStatus.FAILED
                return@launch
            }

            syncStatus.value = SyncStatus.IN_PROGRESS
            when (scheduleRepository.refresh()) {
                RefreshResult.Updated, RefreshResult.UpToDate -> {
                    syncStatus.value = SyncStatus.IDLE
                    preferencesRepository.saveLastSyncedAt(Instant.now())
                }

                RefreshResult.Failed -> syncStatus.value = SyncStatus.FAILED
            }
        }
    }

    private companion object {
        const val UNSUBSCRIBE_DELAY_MILLIS = 5_000L
        val SYNC_INTERVAL: Duration = Duration.ofHours(6)
    }
}

/** Скільки лишилося до наступної рівної хвилини. Винесено окремо, бо на межі доби легко схибити. */
internal fun millisUntilNextMinute(now: LocalDateTime): Long =
    60_000L - now.second * 1_000L - now.nano / 1_000_000
