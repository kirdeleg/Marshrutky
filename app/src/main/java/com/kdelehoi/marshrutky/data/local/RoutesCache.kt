package com.kdelehoi.marshrutky.data.local

import android.content.Context
import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/** Файл розкладу, який уже лежить на пристрої. `sha` — те, що віддав GitHub на момент завантаження. */
data class CachedRoute(
    val fileName: String,
    val sha: String,
    val content: String
)

/**
 * Знімок `routes/` на пристрої. Тримаємо його разом із sha кожного файлу, щоб під час оновлення
 * не перекачувати те, що не змінилося.
 */
class RoutesCache(context: Context, private val json: Json) {

    private val directory = File(context.filesDir, DIRECTORY)
    private val staging = File(context.filesDir, STAGING_DIRECTORY)

    fun read(): List<CachedRoute> {
        val index = readIndex() ?: return emptyList()

        return index.shaByFile.mapNotNull { (fileName, sha) ->
            val content = runCatching { File(directory, fileName).readText() }.getOrNull()
            content?.let { CachedRoute(fileName = fileName, sha = sha, content = it) }
        }
    }

    /**
     * Новий знімок збираємо поруч і лише зібраний ставимо на місце старого. Писати одразу в робочу
     * теку означало б, що вбитий посеред запису процес лишає по собі половину розкладів з індексом
     * від цілого набору — і застосунок вважає цю половину повними даними.
     */
    fun replaceWith(routes: List<CachedRoute>) {
        try {
            staging.deleteRecursively()
            staging.mkdirs()
            routes.forEach { route -> File(staging, route.fileName).writeText(route.content) }
            File(staging, INDEX_FILE).writeText(
                json.encodeToString(Index.serializer(), Index(routes.associate { it.fileName to it.sha }))
            )

            directory.deleteRecursively()
            staging.renameTo(directory)
        } catch (e: Exception) {
            // Кеш — не джерело правди: якщо не записався, наступного разу просто перекачаємо.
            Log.e(TAG, "Не вдалося зберегти кеш розкладів", e)
        }
    }

    private fun readIndex(): Index? = try {
        val file = File(directory, INDEX_FILE)
        if (file.exists()) json.decodeFromString<Index>(file.readText()) else null
    } catch (e: Exception) {
        Log.e(TAG, "Кеш розкладів пошкоджений", e)
        null
    }

    @Serializable
    private data class Index(val shaByFile: Map<String, String> = emptyMap())

    private companion object {
        const val DIRECTORY = "routes"
        const val STAGING_DIRECTORY = "routes.new"
        const val INDEX_FILE = "index.json"
        const val TAG = "RoutesCache"
    }
}
