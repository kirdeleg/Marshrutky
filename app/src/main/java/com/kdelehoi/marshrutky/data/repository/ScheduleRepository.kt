package com.kdelehoi.marshrutky.data.repository

import android.content.Context
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
 * Джерело розкладів. Показуємо те, що вже є на пристрої, а свіже підвантажуємо з GitHub у фоні.
 * Порядок пошуку локальних даних: кеш попереднього завантаження, а якщо його ще немає —
 * знімок, вшитий в assets на випадок першого запуску без інтернету.
 */
class ScheduleRepository(
    private val context: Context,
    private val cache: RoutesCache,
    private val remote: RoutesRemoteDataSource,
    private val json: Json
) {

    suspend fun loadLocalRoutes(): List<Route> = withContext(Dispatchers.IO) {
        val cached = cache.read().map { it.fileName to it.content }
        val files = cached.ifEmpty { readBundled() }

        files.toRoutes()
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

    private fun readBundled(): List<Pair<String, String>> =
        context.assets.list(BUNDLED_DIR).orEmpty()
            .filter { it.endsWith(JSON_SUFFIX) }
            .mapNotNull { fileName ->
                runCatching {
                    fileName to context.assets.open("$BUNDLED_DIR/$fileName")
                        .bufferedReader()
                        .use { it.readText() }
                }.getOrNull()
            }

    private fun List<Pair<String, String>>.toRoutes(): List<Route> =
        mapNotNull { (fileName, content) -> parseRoute(fileName, content) }
            .sortedWith(compareBy({ it.number.toIntOrNull() ?: Int.MAX_VALUE }, { it.number }))

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
        const val BUNDLED_DIR = "routes"
        const val JSON_SUFFIX = ".json"
        const val TAG = "ScheduleRepository"
    }
}
