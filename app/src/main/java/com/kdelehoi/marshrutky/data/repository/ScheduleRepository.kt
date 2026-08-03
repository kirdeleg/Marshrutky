package com.kdelehoi.marshrutky.data.repository

import android.util.Log
import com.kdelehoi.marshrutky.data.local.CachedRoute
import com.kdelehoi.marshrutky.data.local.RoutesCache
import com.kdelehoi.marshrutky.data.remote.RoutesRemoteDataSource
import com.kdelehoi.marshrutky.domain.model.Route
import com.kdelehoi.marshrutky.domain.model.RouteFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.IOException

sealed interface RefreshResult {
    data object Updated : RefreshResult
    data object UpToDate : RefreshResult
    data object Failed : RefreshResult
}

/**
 * Джерело розкладів і власник завантаженого списку маршрутів. Усі дані живуть у репозиторії на
 * GitHub: показуємо збережене з минулого разу, а свіже підтягуємо у фоні. У самому застосунку
 * розкладів немає, тож першому запуску потрібна мережа.
 *
 * Маршрути лежать саме тут, а не у ViewModel, бо ViewModel живе рівно стільки, скільки екран, і
 * при кожному перестворенні довелося б перечитувати кеш з диска.
 */
class ScheduleRepository(
    private val cache: RoutesCache,
    private val remote: RoutesRemoteDataSource,
    private val json: Json
) {

    private val _routes = MutableStateFlow<List<Route>?>(null)

    /** `null`, поки кеш ще не прочитано — це відрізняє «завантажуємо» від «маршрутів немає». */
    val routes: StateFlow<List<Route>?> = _routes.asStateFlow()

    /** Читає кеш один раз за життя процесу: далі список уже в пам'яті. */
    suspend fun loadCached() {
        if (_routes.value != null) return
        _routes.value = withContext(Dispatchers.IO) {
            cache.read().map { it.fileName to it.content }.toRoutes()
        }
    }

    suspend fun refresh(): RefreshResult = withContext(Dispatchers.IO) {
        try {
            val published = remote.listRoutes()
            // Порожня відповідь означає радше проблему з репозиторієм, ніж що маршрутів більше немає.
            if (published.isEmpty()) return@withContext RefreshResult.Failed

            val cached = cache.read().associateBy { it.fileName }
            val isSameSet = published.size == cached.size &&
                published.all { cached[it.fileName]?.sha == it.sha }
            if (isSameSet) return@withContext RefreshResult.UpToDate

            // Послідовне завантаження — це десяток запитів один за одним, тобто радіомодуль
            // увімкнений увесь цей час. Кілька потоків заразом закінчують швидше, але пускати всі
            // одночасно теж не варіант: стільки ж паралельних з'єднань до одного хоста нікому не
            // потрібні, та й GitHub такий сплеск не любить.
            val fresh = coroutineScope {
                val limit = Semaphore(MAX_PARALLEL_DOWNLOADS)
                published.map { route ->
                    async {
                        cached[route.fileName]?.takeIf { it.sha == route.sha }
                            ?: limit.withPermit {
                                CachedRoute(route.fileName, route.sha, remote.download(route))
                            }
                    }
                }.awaitAll()
            }
            cache.replaceWith(fresh)
            _routes.value = fresh.map { it.fileName to it.content }.toRoutes()

            RefreshResult.Updated
        } catch (e: CancellationException) {
            // Скасування — не збій оновлення, а закритий екран. Якщо його проковтнути тут,
            // корутина, яку вже зупинили, вдаватиме, що завершилася сама.
            throw e
        } catch (e: IOException) {
            Log.w(TAG, "Не вдалося оновити розклади", e)
            RefreshResult.Failed
        } catch (e: Exception) {
            Log.e(TAG, "Зламана відповідь від GitHub", e)
            RefreshResult.Failed
        }
    }

    private fun List<Pair<String, String>>.toRoutes(): List<Route> =
        mapNotNull { (fileName, content) -> parseRoute(fileName, content) }
            .sortedWith(compareBy({ it.number?.toIntOrNull() ?: Int.MAX_VALUE }, { it.number ?: it.name }))

    private fun parseRoute(fileName: String, content: String): Route? = try {
        val parsed = json.decodeFromString<RouteFile>(content)
        Route(
            id = fileName.removeSuffix(JSON_SUFFIX),
            number = parsed.number,
            name = parsed.name,
            directions = parsed.directions
        )
    } catch (e: Exception) {
        // Один зіпсований файл не має ламати весь список маршрутів.
        Log.e(TAG, "Не вдалося прочитати маршрут $fileName", e)
        null
    }

    private companion object {
        const val JSON_SUFFIX = ".json"
        const val MAX_PARALLEL_DOWNLOADS = 4
        const val TAG = "ScheduleRepository"
    }
}
