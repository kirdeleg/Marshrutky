package com.kdelehoi.marshrutky.data.remote

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class RoutesSourceTest {

    @Test
    fun `the index and the schedules come from raw, where there is no request limit`() {
        // Contents API дозволяє 60 запитів на годину на IP без токена, і за спільним NAT оператора
        // його вичерпує кілька людей. Тому в жодній з адрес не має бути api.github.com.
        assertEquals(
            "https://raw.githubusercontent.com/kirdeleg/Marshrutky/main/routes/index.json",
            RoutesSource.Default.indexUrl
        )
        assertEquals(
            "https://raw.githubusercontent.com/kirdeleg/Marshrutky/main/routes/1154-merefa-kharkiv.json",
            RoutesSource.Default.fileUrl("1154-merefa-kharkiv.json")
        )
    }
}

class ParseIndexTest {

    private val source = RoutesRemoteDataSource(Json, RoutesSource.Default)

    @Test
    fun `an entry keeps its file name and hash`() {
        val routes = source.parseIndex(
            """{"routes":[{"file":"199-komarivka-kharkiv.json","sha":"abc123"}]}"""
        )

        assertEquals(listOf(RemoteRoute("199-komarivka-kharkiv.json", "abc123")), routes)
    }

    @Test
    fun `the index does not describe itself`() {
        val routes = source.parseIndex(
            """{"routes":[{"file":"index.json","sha":"a"},{"file":"199.json","sha":"b"}]}"""
        )

        // З index.json маршрут не вийде, а помилка розбору виглядала б як зникла маршрутка.
        assertEquals(listOf("199.json"), routes.map { it.fileName })
    }

    @Test
    fun `anything that is not a schedule is skipped`() {
        val routes = source.parseIndex("""{"routes":[{"file":"README.md","sha":"a"}]}""")

        assertEquals(emptyList<RemoteRoute>(), routes)
    }

    @Test
    fun `a file mentioned twice counts once`() {
        val routes = source.parseIndex(
            """{"routes":[{"file":"199.json","sha":"a"},{"file":"199.json","sha":"a"}]}"""
        )

        // Інакше перелік ніколи не збігався б із кешем за розміром, і застосунок тихо
        // перезавантажував би всі розклади кожні шість годин.
        assertEquals(1, routes.size)
    }

    @Test
    fun `an index without routes gives nothing`() {
        // Порожній перелік репозиторій розбирає як невдачу оновлення, а не як «маршрутів немає».
        assertEquals(emptyList<RemoteRoute>(), source.parseIndex("""{"routes":[]}"""))
    }
}
