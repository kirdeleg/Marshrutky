package com.kdelehoi.marshrutky.data.repository

import android.util.Log
import com.kdelehoi.marshrutky.data.local.CachedRoute
import com.kdelehoi.marshrutky.data.local.RoutesCache
import com.kdelehoi.marshrutky.data.remote.RoutesRemoteDataSource
import com.kdelehoi.marshrutky.domain.model.Route
import com.kdelehoi.marshrutky.domain.model.RouteFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.IOException

sealed interface RefreshResult {
    data class Updated(val routes: List<Route>) : RefreshResult
    data object UpToDate : RefreshResult
    data object Failed : RefreshResult
}

/**
 * Джерело розкладів. Усі дані живуть у репозиторії на GitHub: показуємо збережене з минулого
 * разу, а свіже підтягуємо у фоні. У самому застосунку розкладів немає, тож першому запуску
 * потрібна мережа.
 */
class ScheduleRepository(
    private val cache: RoutesCache,
    private val remote: RoutesRemoteDataSource,
    private val json: Json
) {

    suspend fun loadLocalRoutes(): List<Route> = withContext(Dispatchers.IO) {
        cache.read().map { it.fileName to it.content }.toRoutes()
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

            val fresh = published.map { route ->
                cached[route.fileName]?.takeIf { it.sha == route.sha }
                    ?: CachedRoute(route.fileName, route.sha, remote.download(route))
            }
            cache.replaceWith(fresh)

            RefreshResult.Updated(fresh.map { it.fileName to it.content }.toRoutes())
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
        const val TAG = "ScheduleRepository"
    }
}
