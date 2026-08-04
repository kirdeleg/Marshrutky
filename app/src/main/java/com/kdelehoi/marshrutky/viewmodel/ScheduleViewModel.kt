package com.kdelehoi.marshrutky.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kdelehoi.marshrutky.data.remote.NetworkMonitor
import com.kdelehoi.marshrutky.data.repository.PreferencesRepository
import com.kdelehoi.marshrutky.data.repository.RefreshResult
import com.kdelehoi.marshrutky.data.repository.ScheduleRepository
import com.kdelehoi.marshrutky.domain.model.Route
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime

enum class SyncStatus {
    IDLE,
    IN_PROGRESS,
    FAILED
}

/**
 * Стан розкладів. Трьома випадками, а не списком з прапорцем «завантажуємо»: порожньо буває з двох
 * різних причин — даних ще нема, бо ми їх лише дістаємо, або їх справді немає. Плутати ці випадки —
 * це показати «Маршрутів немає» під час першого завантаження, тобто збрехати рівно тій людині, яка
 * щойно поставила застосунок.
 */
sealed interface RoutesState {

    /** Кеш ще читається або йде перше завантаження з мережі. */
    data object Loading : RoutesState

    /** Дістали все, що могли, і маршрутів немає. */
    data object Empty : RoutesState

    data class Ready(val routes: List<Route>) : RoutesState {

        /** Один раз на кожен новий список замість пошуку по ньому в кожній перекомпозиції. */
        private val byId: Map<String, Route> = routes.associateBy { it.id }

        fun route(routeId: String): Route? = byId[routeId]

        /** Саме в порядку, який задав користувач, а не в тому, у якому маршрути лежать у файлах. */
        fun inOrder(routeIds: List<String>): List<Route> = routeIds.mapNotNull(byId::get)
    }
}

/**
 * Усе, крім поточного часу: він змінюється щохвилини й живе в окремому потоці. Якби він лежав тут,
 * кожна хвилина оголошувала б застарілим увесь стан — і перемальовувалися б навіть «Параметри», де
 * жодного часу немає.
 */
data class ScheduleUiState(
    val routes: RoutesState = RoutesState.Loading,
    val favoriteRouteIds: List<String> = emptyList(),
    val syncStatus: SyncStatus = SyncStatus.IDLE,
    val lastSyncedAt: Instant? = null,
    val selectedStop: String? = null
)

/** Що саме привело нас по мережу: від цього залежить, чи показувати невдачу. */
private enum class SyncTrigger {
    STARTUP,
    MANUAL
}

class ScheduleViewModel(
    private val scheduleRepository: ScheduleRepository,
    private val preferencesRepository: PreferencesRepository,
    private val networkMonitor: NetworkMonitor,
    /** Годинник ззовні, щоб час був даними, а не викликом усередині: інакше це не перевірити. */
    private val clock: Clock
) : ViewModel() {

    private val syncStatus = MutableStateFlow(SyncStatus.IDLE)

    /**
     * Тримається до кінця першого підйому: читання кешу і, якщо треба, перше завантаження. Без
     * цього прапорця між порожнім кешем і стартом синхронізації лишалося б вікно, у якому список
     * порожній, а статус ще IDLE, — і на першому запуску встигало б блимнути «Маршрутів немає».
     */
    private val isStartingUp = MutableStateFlow(true)

    /**
     * Поточний час із точністю до хвилини. Окремий потік, а не поле стану: його читають лише ті
     * екрани, де є час, тож на «Параметрах» чи «Маршрутах» годинник просто зупиняється. Холодний
     * Flow під `WhileSubscribed` робить це властивістю конструкції, а не домовленістю: щойно
     * застосунок згорнули, підписник відвалюється і тікати нема кому. Прокидання рахуємо щоразу
     * від поточного часу, тому дрейф не накопичується.
     */
    val now: StateFlow<LocalDateTime> = flow {
        while (true) {
            val now = LocalDateTime.now(clock)
            emit(now)
            delay(millisUntilNextMinute(now))
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STATE_UNSUBSCRIBE_DELAY_MILLIS),
        initialValue = LocalDateTime.now(clock)
    )

    /**
     * Пауза перед зупинкою потрібна, щоб поворот екрана чи короткий перехід між екранами не
     * перезапускали весь ланцюжок дарма.
     */
    val state: StateFlow<ScheduleUiState> = combine(
        scheduleRepository.routes,
        preferencesRepository.preferences,
        syncStatus,
        isStartingUp
    ) { routes, preferences, sync, startingUp ->
        ScheduleUiState(
            routes = routesState(
                routes = routes,
                isStartingUp = startingUp,
                syncStatus = sync
            ),
            favoriteRouteIds = preferences.favoriteRouteIds,
            syncStatus = sync,
            lastSyncedAt = preferences.lastSyncedAt,
            selectedStop = preferences.selectedStop
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STATE_UNSUBSCRIBE_DELAY_MILLIS),
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
    fun refresh() {
        viewModelScope.launch { sync(SyncTrigger.MANUAL) }
    }

    /** Спершу показуємо те, що вже є на пристрої, і аж потім вирішуємо, чи йти по свіже. */
    private fun loadAndSync() {
        viewModelScope.launch {
            try {
                scheduleRepository.loadCached()

                val lastSyncedAt = preferencesRepository.preferences.first().lastSyncedAt
                val isFresh = lastSyncedAt != null &&
                    Duration.between(lastSyncedAt, Instant.now(clock)) < SYNC_INTERVAL
                // Розклади міняються раз на місяці, тож ходити по мережу на кожен запуск нема сенсу.
                if (!isFresh) sync(SyncTrigger.STARTUP)
            } finally {
                isStartingUp.value = false
            }
        }
    }

    private suspend fun sync(trigger: SyncTrigger) {
        if (!networkMonitor.isOnline) {
            // Про невдачу повідомляємо, лише якщо оновлення попросили руками: інакше це просто
            // марно розбуджений радіомодуль і червоний напис нізащо.
            if (trigger == SyncTrigger.MANUAL) syncStatus.value = SyncStatus.FAILED
            return
        }

        // Перевірити й зайняти статус треба одним рухом: два швидкі тапи по «Оновити зараз» —
        // це два запуски, і роздільна перевірка пропустила б обидва.
        if (syncStatus.getAndUpdate { SyncStatus.IN_PROGRESS } == SyncStatus.IN_PROGRESS) return

        when (scheduleRepository.refresh()) {
            RefreshResult.Updated, RefreshResult.UpToDate -> {
                syncStatus.value = SyncStatus.IDLE
                preferencesRepository.saveLastSyncedAt(Instant.now(clock))
            }

            RefreshResult.Failed -> syncStatus.value = SyncStatus.FAILED
        }
    }

    private companion object {
        val SYNC_INTERVAL: Duration = Duration.ofHours(6)
    }
}

/**
 * Куди віднести те, що дав репозиторій. `null` — кеш ще не читали; порожній список під час першого
 * підйому або синхронізації — це теж «завантажуємо», бо даним ще не було звідки взятися.
 */
internal fun routesState(
    routes: List<Route>?,
    isStartingUp: Boolean,
    syncStatus: SyncStatus
): RoutesState = when {
    routes == null -> RoutesState.Loading
    routes.isNotEmpty() -> RoutesState.Ready(routes)
    isStartingUp || syncStatus == SyncStatus.IN_PROGRESS -> RoutesState.Loading
    else -> RoutesState.Empty
}

/** Скільки лишилося до наступної рівної хвилини. Винесено окремо, бо на межі доби легко схибити. */
internal fun millisUntilNextMinute(now: LocalDateTime): Long =
    60_000L - now.second * 1_000L - now.nano / 1_000_000
