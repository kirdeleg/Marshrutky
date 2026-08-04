package com.kdelehoi.marshrutky.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Один файл розкладу так, як його описує індекс. */
data class RemoteRoute(
    val fileName: String,
    /**
     * Хеш вмісту — той самий, що видає `git hash-object`, тобто те саме значення, яким колись
     * відповідав Contents API. Завдяки цьому кеші, зібрані попередніми версіями застосунку,
     * лишилися чинними й перехід на індекс нічого повторно не завантажив.
     */
    val sha: String
)

/** Тягне розклади з теки, яку задає [source]. */
class RoutesRemoteDataSource(private val json: Json, private val source: RoutesSource) {

    suspend fun listRoutes(): List<RemoteRoute> = withContext(Dispatchers.IO) {
        parseIndex(fetch(source.indexUrl))
    }

    suspend fun download(route: RemoteRoute): String = withContext(Dispatchers.IO) {
        fetch(source.fileUrl(route.fileName))
    }

    internal fun parseIndex(raw: String): List<RemoteRoute> =
        json.decodeFromString<RouteIndexFile>(raw).routes
            // Індекс не описує ні себе, ні будь-що, з чого маршрут не вийде.
            .filter { it.file != INDEX_FILE_NAME && it.file.endsWith(ROUTE_FILE_SUFFIX) }
            // Двічі згаданий файл означав би, що перелік завжди не збігається з кешем за розміром,
            // тобто тихе повторне завантаження всього набору кожні шість годин.
            .distinctBy { it.file }
            .map { RemoteRoute(fileName = it.file, sha = it.sha) }

    /**
     * Свідомо без `disconnect()`: він рве сокет, а нам за один прохід треба забрати десяток файлів
     * з того самого хоста. Дочитаний до кінця потік повертає з'єднання в пул, і наступний файл іде
     * без нового рукостискання TLS — інакше перший запуск коштує стільки рукостискань, скільки
     * маршрутів, і стільки ж часу з увімкненим радіомодулем.
     */
    private fun fetch(url: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MILLIS
            readTimeout = TIMEOUT_MILLIS
            setRequestProperty("User-Agent", USER_AGENT)
        }

        val code = connection.responseCode
        if (code !in HTTP_OK_RANGE) {
            // Тіло помилки теж треба дочитати, бо недочитане з'єднання в пул не повертається.
            connection.errorStream?.use { it.readBytes() }
            throw IOException("GitHub відповів $code на $url")
        }

        return connection.inputStream.bufferedReader().use { it.readText() }
    }

    private companion object {
        const val USER_AGENT = "Marshrutky-Android"
        const val TIMEOUT_MILLIS = 15_000
        val HTTP_OK_RANGE = 200..299
    }
}

/** Індекс так, як він лежить у репозиторії. */
@Serializable
internal data class RouteIndexFile(val routes: List<Entry> = emptyList()) {

    @Serializable
    data class Entry(val file: String, val sha: String)
}
