package com.kdelehoi.marshrutky.data.format

import com.kdelehoi.marshrutky.data.remote.INDEX_FILE_NAME
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Розбір файлу маршруту й перетворення його в домен. Зіпсований файл репозиторій ковтає мовчки, щоб
 * не втратити решту списку, тож помилка в схемі проявляється як маршрут, що просто зник з екрана.
 * Ці тести тримають живими обидва формати.
 */
class RouteFileTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun parse(raw: String, id: String = "test") = json.decodeFromString<RouteFile>(raw).toRoute(id)

    @Test
    fun `direction with several boarding stops keeps them in order`() {
        val route = parse(
            """
            {
              "name": "Островерхівка — Харків",
              "directions": [
                {
                  "label": "На Харків",
                  "destination": "Харків",
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
        val route = parse(
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
        val route = parse("""{ "name": "Яковлівка — Харків", "directions": [] }""")

        assertNull(route.number)
        assertEquals("Яковлівка — Харків", route.title)
    }

    @Test
    fun `an empty number is the same as no number`() {
        // Інакше в заголовку з'явилися б порожні дужки, а значок номера — з порожнім кружечком.
        val route = parse("""{ "number": "  ", "name": "Тест", "directions": [] }""")

        assertNull(route.number)
    }

    @Test
    fun `the file name becomes the route id`() {
        val route = parse("""{ "name": "Тест" }""", id = "199-komarivka-kharkiv")

        assertEquals("199-komarivka-kharkiv", route.id)
    }

    @Test
    fun `direction without a destination falls back to the route name`() {
        // Так виглядали файли до появи поля, і читатися вони мусять так само, як раніше.
        val route = parse(
            """
            {
              "name": "Комарівка — Харків",
              "directions": [
                { "label": "На Харків", "stops": [{ "name": "Комарівка" }] }
              ]
            }
            """.trimIndent()
        )

        assertEquals("Комарівка — Харків", route.directions.single().destination)
    }

    @Test
    fun `direction without any stop falls back to its label`() {
        val route = parse("""{ "name": "Тест", "directions": [{ "label": "На Харків" }] }""")

        assertEquals("На Харків", route.directions.single().origin.name)
    }

    @Test
    fun `direction with nowhere to board is dropped`() {
        // Домен обіцяє, що зупинка в напрямку є завжди, тож порожній напрямок туди не доїжджає.
        val route = parse("""{ "name": "Тест", "directions": [{ "destination": "Харків" }] }""")

        assertTrue(route.directions.isEmpty())
    }

    @Test
    fun `every published route file becomes a usable route`() {
        // Поруч із розкладами лежить їхній перелік — маршрутом він не є.
        val files = File("../routes")
            .listFiles { file -> file.extension == "json" && file.name != INDEX_FILE_NAME }
            .orEmpty()
        assertTrue("Теку routes не знайдено", files.isNotEmpty())

        files.forEach { file ->
            val route = parse(file.readText(), id = file.nameWithoutExtension)

            assertTrue("${file.name}: порожня назва", route.name.isNotBlank())
            assertTrue("${file.name}: маршрут без напрямків", route.directions.isNotEmpty())
            route.directions.forEach { direction ->
                assertTrue("${file.name}: напрямок без кінцевої", direction.destination.isNotBlank())
                direction.boardingStops.forEach { stop ->
                    assertTrue("${file.name}: зупинка без назви", stop.name.isNotBlank())
                }
            }
        }
    }
}
