package com.kdelehoi.marshrutky.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Один файл розкладу так, як його бачить GitHub. */
data class RemoteRoute(
    val fileName: String,
    val sha: String,
    val downloadUrl: String
)

/** Тягне розклади з теки, яку задає [source]. */
class RoutesRemoteDataSource(private val json: Json, private val source: RoutesSource) {

    suspend fun listRoutes(): List<RemoteRoute> = withContext(Dispatchers.IO) {
        val raw = fetch(source.contentsUrl, ACCEPT_GITHUB_JSON)

        json.decodeFromString<List<ContentsEntry>>(raw)
            .filter { it.type == TYPE_FILE && it.name.endsWith(ROUTE_FILE_SUFFIX) }
            .mapNotNull { entry ->
                val downloadUrl = entry.downloadUrl ?: return@mapNotNull null
                RemoteRoute(fileName = entry.name, sha = entry.sha, downloadUrl = downloadUrl)
            }
    }

    suspend fun download(route: RemoteRoute): String = withContext(Dispatchers.IO) {
        fetch(route.downloadUrl, ACCEPT_PLAIN)
    }

    /**
     * Свідомо без `disconnect()`: він рве сокет, а нам за один прохід треба забрати десяток файлів
     * з того самого хоста. Дочитаний до кінця потік повертає з'єднання в пул, і наступний файл іде
     * без нового рукостискання TLS — інакше перший запуск коштує стільки рукостискань, скільки
     * маршрутів, і стільки ж часу з увімкненим радіомодулем.
     */
    private fun fetch(url: String, accept: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MILLIS
            readTimeout = TIMEOUT_MILLIS
            setRequestProperty("Accept", accept)
            // GitHub відмовляє запитам без User-Agent.
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

    @Serializable
    private data class ContentsEntry(
        val name: String,
        val type: String,
        val sha: String,
        @SerialName("download_url") val downloadUrl: String? = null
    )

    private companion object {
        const val ACCEPT_GITHUB_JSON = "application/vnd.github+json"
        const val ACCEPT_PLAIN = "text/plain"
        const val USER_AGENT = "Marshrutky-Android"
        const val TIMEOUT_MILLIS = 15_000
        const val TYPE_FILE = "file"
        val HTTP_OK_RANGE = 200..299
    }
}
