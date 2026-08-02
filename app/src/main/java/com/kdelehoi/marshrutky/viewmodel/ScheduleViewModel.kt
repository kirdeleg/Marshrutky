package com.kdelehoi.marshrutky.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kdelehoi.marshrutky.data.repository.PreferencesRepository
import com.kdelehoi.marshrutky.data.repository.ScheduleRepository
import com.kdelehoi.marshrutky.domain.DepartureCalculator
import com.kdelehoi.marshrutky.domain.model.DayType
import com.kdelehoi.marshrutky.domain.model.Departure
import com.kdelehoi.marshrutky.domain.model.Direction
import com.kdelehoi.marshrutky.domain.model.Route
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDateTime

data class ScheduleUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val routes: List<Route> = emptyList(),
    val selectedRouteId: String? = null,
    val directionIndex: Int = 0,
    val now: LocalDateTime = LocalDateTime.now()
) {
    val selectedRoute: Route?
        get() = routes.firstOrNull { it.id == selectedRouteId } ?: routes.firstOrNull()

    val selectedDirection: Direction?
        get() = selectedRoute?.directions?.getOrNull(directionIndex)

    val today: DayType
        get() = DepartureCalculator.dayTypeOf(now.toLocalDate())

    val upcoming: List<Departure>
        get() = selectedDirection
            ?.let { DepartureCalculator.upcoming(it, now, UPCOMING_LIMIT) }
            .orEmpty()
}

class ScheduleViewModel(
    private val scheduleRepository: ScheduleRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ScheduleUiState())
    val state: StateFlow<ScheduleUiState> = _state.asStateFlow()

    init {
        loadSchedule()
        startClock()
    }

    fun selectRoute(routeId: String) {
        _state.update { current ->
            // У різних маршрутів напрямки свої, тож при зміні маршруту вертаємось до першого.
            current.copy(selectedRouteId = routeId, directionIndex = 0)
        }
        persistSelection()
    }

    fun selectDirection(index: Int) {
        _state.update { it.copy(directionIndex = index) }
        persistSelection()
    }

    fun toggleDirection() {
        val directionCount = _state.value.selectedRoute?.directions?.size ?: return
        if (directionCount < 2) return
        selectDirection((_state.value.directionIndex + 1) % directionCount)
    }

    private fun loadSchedule() {
        viewModelScope.launch {
            val routes = runCatching { scheduleRepository.loadRoutes() }.getOrNull()
            if (routes == null) {
                _state.update { it.copy(isLoading = false, loadFailed = true) }
                return@launch
            }

            val saved = preferencesRepository.selection.first()
            val routeId = routes.firstOrNull { it.id == saved.routeId }?.id ?: routes.firstOrNull()?.id
            val directionCount = routes.firstOrNull { it.id == routeId }?.directions?.size ?: 0

            _state.update {
                it.copy(
                    isLoading = false,
                    routes = routes,
                    selectedRouteId = routeId,
                    directionIndex = saved.directionIndex.coerceIn(0, (directionCount - 1).coerceAtLeast(0)),
                    now = LocalDateTime.now()
                )
            }
        }
    }

    private fun startClock() {
        viewModelScope.launch {
            while (isActive) {
                _state.update { it.copy(now = LocalDateTime.now()) }
                delay(TICK_MILLIS)
            }
        }
    }

    private fun persistSelection() {
        val current = _state.value
        val routeId = current.selectedRoute?.id ?: return
        viewModelScope.launch {
            preferencesRepository.saveSelection(routeId, current.directionIndex)
        }
    }

    private companion object {
        const val TICK_MILLIS = 1_000L
    }
}

private const val UPCOMING_LIMIT = 12
