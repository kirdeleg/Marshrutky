package com.kdelehoi.marshrutky.data.repository

import com.kdelehoi.marshrutky.domain.model.Route
import kotlinx.coroutines.flow.StateFlow

sealed interface RefreshResult {
    data object Updated : RefreshResult
    data object UpToDate : RefreshResult
    data object Failed : RefreshResult
}

/**
 * Звідки застосунок бере маршрути. Реалізація одна, тож інтерфейс тут не заради підміни джерела: він
 * потрібен як шов для тестів. Правила «коли йти по свіже», «що казати при невдачі» й «що робити з
 * двома тапами по «Оновити зараз» живуть у ViewModel, а перевірити їх, поки поруч справжній диск і
 * справжня мережа, неможливо.
 */
interface ScheduleRepository {

    /** `null`, поки кеш ще не прочитано — це відрізняє «завантажуємо» від «маршрутів немає». */
    val routes: StateFlow<List<Route>?>

    /** Читає кеш один раз за життя процесу: далі список уже в пам'яті. */
    suspend fun loadCached()

    suspend fun refresh(): RefreshResult
}
