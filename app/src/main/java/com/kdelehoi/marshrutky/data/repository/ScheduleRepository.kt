package com.kdelehoi.marshrutky.data.repository

import android.content.Context
import android.util.Log
import com.kdelehoi.marshrutky.domain.model.Route
import com.kdelehoi.marshrutky.domain.model.RouteFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class ScheduleRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Кожен маршрут — окремий файл у `assets/routes`. Нового маршруту достатньо просто
     * покласти туди, ніякого списку-індексу вести не треба.
     */
    suspend fun loadRoutes(): List<Route> = withContext(Dispatchers.IO) {
        val fileNames = context.assets.list(ROUTES_DIR).orEmpty()

        fileNames
            .filter { it.endsWith(JSON_SUFFIX) }
            .mapNotNull { fileName -> readRoute(fileName) }
            .sortedWith(compareBy({ it.number.toIntOrNull() ?: Int.MAX_VALUE }, { it.number }))
    }

    private fun readRoute(fileName: String): Route? = try {
        val raw = context.assets.open("$ROUTES_DIR/$fileName").bufferedReader().use { it.readText() }
        val parsed = json.decodeFromString<RouteFile>(raw)
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
        const val ROUTES_DIR = "routes"
        const val JSON_SUFFIX = ".json"
        const val TAG = "ScheduleRepository"
    }
}
