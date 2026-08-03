package com.kdelehoi.marshrutky.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kdelehoi.marshrutky.data.repository.PreferencesRepository
import com.kdelehoi.marshrutky.data.repository.RefreshResult
import com.kdelehoi.marshrutky.data.repository.ScheduleRepository
import com.kdelehoi.marshrutky.domain.DepartureCalculator
import com.kdelehoi.marshrutky.domain.model.DayType
import com.kdelehoi.marshrutky.domain.model.Route
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ScheduleUiState())
    val state: StateFlow<ScheduleUiState> = _state.asStateFlow()

    init {
        loadRoutes()
        observeFavorites()
        observeLastSync()
        observeSelectedStop()
        startClock()
    }

    fun toggleFavorite(routeId: String) {
        viewModelScope.launch {
            preferencesRepository.toggleFavorite(routeId)
        }
    }

    fun saveFavoriteOrder(routeIds: List<String>) {
        viewModelScope.launch {
            preferencesRepository.saveFavoriteOrder(routeIds)
        }
    }

    fun selectStop(stopName: String) {
        viewModelScope.launch {
            preferencesRepository.saveSelectedStop(stopName)
        }
    }

    fun refresh() {
        if (_state.value.syncStatus == SyncStatus.IN_PROGRESS) return

        viewModelScope.launch {
            _state.update { it.copy(syncStatus = SyncStatus.IN_PROGRESS) }

            when (val result = scheduleRepository.refresh()) {
                is RefreshResult.Updated -> {
                    _state.update { it.copy(routes = result.routes, syncStatus = SyncStatus.IDLE) }
                    preferencesRepository.saveLastSyncedAt(Instant.now())
                }

                RefreshResult.UpToDate -> {
                    _state.update { it.copy(syncStatus = SyncStatus.IDLE) }
                    preferencesRepository.saveLastSyncedAt(Instant.now())
                }

                RefreshResult.Failed -> _state.update { it.copy(syncStatus = SyncStatus.FAILED) }
            }
        }
    }

    /** Спершу показуємо те, що вже є на пристрої, і аж потім ідемо по свіже. */
    private fun loadRoutes() {
        viewModelScope.launch {
            val routes = scheduleRepository.loadLocalRoutes()
            _state.update { it.copy(isLoading = false, routes = routes, now = LocalDateTime.now()) }
            refresh()
        }
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            preferencesRepository.favoriteRouteIds.collect { ids ->
                _state.update { it.copy(favoriteRouteIds = ids) }
            }
        }
    }

    private fun observeLastSync() {
        viewModelScope.launch {
            preferencesRepository.lastSyncedAt.collect { instant ->
                _state.update { it.copy(lastSyncedAt = instant) }
            }
        }
    }

    private fun observeSelectedStop() {
        viewModelScope.launch {
            preferencesRepository.selectedStop.collect { stopName ->
                _state.update { it.copy(selectedStop = stopName) }
            }
        }
    }

    /**
     * Відлік показує хвилини, тож будимося рівно на межі хвилини, а не щосекунди: інакше
     * застосунок 59 разів на хвилину перебудовує весь екран заради тієї самої цифри.
     * Прокидання рахуємо щоразу від поточного часу, тому дрейф не накопичується.
     */
    private fun startClock() {
        viewModelScope.launch {
            while (isActive) {
                val now = LocalDateTime.now()
                _state.update { it.copy(now = now) }
                delay(millisUntilNextMinute(now))
            }
        }
    }

    private fun millisUntilNextMinute(now: LocalDateTime): Long =
        MINUTE_MILLIS - now.second * 1_000L - now.nano / 1_000_000

    private companion object {
        const val MINUTE_MILLIS = 60_000L
    }
}
