package com.kdelehoi.marshrutky.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kdelehoi.marshrutky.data.repository.PreferencesRepository
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
import java.time.LocalDateTime

data class ScheduleUiState(
    val isLoading: Boolean = true,
    val routes: List<Route> = emptyList(),
    val favoriteRouteIds: Set<String> = emptySet(),
    val now: LocalDateTime = LocalDateTime.now()
) {
    val favoriteRoutes: List<Route>
        get() = routes.filter { it.id in favoriteRouteIds }

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
        startClock()
    }

    fun toggleFavorite(routeId: String) {
        viewModelScope.launch {
            preferencesRepository.toggleFavorite(routeId)
        }
    }

    private fun loadRoutes() {
        viewModelScope.launch {
            val routes = scheduleRepository.loadRoutes()
            _state.update { it.copy(isLoading = false, routes = routes, now = LocalDateTime.now()) }
        }
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            preferencesRepository.favoriteRouteIds.collect { ids ->
                _state.update { it.copy(favoriteRouteIds = ids) }
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

    private companion object {
        const val TICK_MILLIS = 1_000L
    }
}
