package com.kdelehoi.marshrutky.domain

import com.kdelehoi.marshrutky.domain.model.RouteFile
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Розбір файлу маршруту. Зіпсований файл репозиторій ковтає мовчки, щоб не втратити решту
 * списку, тож помилка в схемі проявляється як маршрут, що просто зник з екрана. Ці тести
 * тримають обидва формати живими.
 */
class RouteFileTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `direction with several boarding stops keeps them in order`() {
        val route = json.decodeFromString<RouteFile>(
            """
            {
              "name": "Островерхівка — Харків",
              "directions": [
                {
                  "label": "На Харків",
                  "stops": [
                    { "name": "Островерхівка", "schedule": { "weekday": ["05:50"] } },
                    { "name": "Мерефа (Селекційна)", "schedule": { "weekday": ["06:20"] } }
                  ]
                }
              ]
            }
            """.trimIndent()
        )

        val stops = route.directions.single().boardingStops
        assertEquals(listOf("Островерхівка", "Мерефа (Селекційна)"), stops.map { it.name })
        assertEquals(listOf("06:20"), stops.last().schedule.weekday)
    }

    @Test
    fun `old file with a single boardingStop still reads`() {
        val route = json.decodeFromString<RouteFile>(
            """
            {
              "number": "1611",
              "name": "Мерефа — Харків",
              "directions": [
                {
                  "label": "На Харків",
                  "boardingStop": "Мерефа (вул. Конституції)",
                  "schedule": { "weekday": ["06:40", "07:20"] }
                }
              ]
            }
            """.trimIndent()
        )

        val direction = route.directions.single()
        assertEquals("Мерефа (вул. Конституції)", direction.origin.name)
        assertEquals(listOf("06:40", "07:20"), direction.origin.schedule.weekday)
    }

    @Test
    fun `route without a number is valid`() {
        val route = json.decodeFromString<RouteFile>(
            """{ "name": "Яковлівка — Харків", "directions": [] }"""
        )

        assertNull(route.number)
    }

    @Test
    fun `every published route file parses`() {
        val files = File("../routes").listFiles { file -> file.extension == "json" }.orEmpty()
        assertTrue("Теку routes не знайдено", files.isNotEmpty())

        files.forEach { file ->
            val route = json.decodeFromString<RouteFile>(file.readText())

            assertTrue("${file.name}: порожня назва", route.name.isNotBlank())
            route.directions.forEach { direction ->
                direction.boardingStops.forEach { stop ->
                    assertTrue("${file.name}: зупинка без назви", stop.name.isNotBlank())
                }
            }
        }
    }

    @Test
    fun `direction without any stop falls back to its label`() {
        val route = json.decodeFromString<RouteFile>(
            """{ "name": "Тест", "directions": [{ "label": "На Харків" }] }"""
        )

        assertEquals("На Харків", route.directions.single().origin.name)
    }
}
