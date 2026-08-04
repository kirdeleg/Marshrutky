package com.kdelehoi.marshrutky.data.remote

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest

/**
 * Індекс — єдине, з чого застосунок дізнається, які файли є в теці. Якщо його забути перебудувати,
 * новий маршрут не з'явиться ні в кого, а видалений почне віддавати 404 і валити все оновлення. Ця
 * перевірка тримає індекс і теку в одному стані, тож несвіжий індекс валить збірку, а не чужий
 * застосунок.
 */
class RoutesIndexTest {

    private val routes = repositoryRoot().resolve(ROUTES_DIRECTORY)

    @Test
    fun `the index lists exactly the files in the directory`() {
        val files = routes.list()
            .orEmpty()
            .filter { it.endsWith(ROUTE_FILE_SUFFIX) && it != INDEX_FILE_NAME }
            .sorted()

        assertTrue("У теці немає розкладів — тест шукає не там", files.isNotEmpty())
        assertEquals("Перебудуй індекс: python3 tools/build_index.py", files, index().map { it.fileName })
    }

    @Test
    fun `every hash in the index matches its file`() {
        index().forEach { entry ->
            assertEquals(
                "Хеш ${entry.fileName} застарів: python3 tools/build_index.py",
                blobSha(routes.resolve(entry.fileName)),
                entry.sha
            )
        }
    }

    private fun index(): List<RemoteRoute> = RoutesRemoteDataSource(Json, RoutesSource.Default)
        .parseIndex(routes.resolve(INDEX_FILE_NAME).readText())

    /** Той самий SHA-1, що його рахує `git hash-object`, — саме він лежить в індексі. */
    private fun blobSha(file: File): String {
        val content = file.readBytes()
        val digest = MessageDigest.getInstance("SHA-1")
        digest.update("blob ${content.size}\u0000".toByteArray())
        digest.update(content)
        return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }

    /** Тести можуть запускатися і з теки модуля, і з кореня, тож теку розкладів шукаємо вгору. */
    private fun repositoryRoot(): File = generateSequence(File("").absoluteFile) { it.parentFile }
        .first { File(it, ROUTES_DIRECTORY).isDirectory }

    private companion object {
        const val ROUTES_DIRECTORY = "routes"
    }
}
