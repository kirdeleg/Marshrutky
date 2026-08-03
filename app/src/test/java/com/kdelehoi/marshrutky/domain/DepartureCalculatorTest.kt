package com.kdelehoi.marshrutky.domain

import com.kdelehoi.marshrutky.domain.model.BoardingStop
import com.kdelehoi.marshrutky.domain.model.DayType
import com.kdelehoi.marshrutky.domain.model.Direction
import com.kdelehoi.marshrutky.domain.model.Route
import com.kdelehoi.marshrutky.domain.model.WeekSchedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class DepartureCalculatorTest {

    // Маршрут 199: у будні шість рейсів, на вихідних не курсує.
    private val stop = BoardingStop(
        name = "Комарівка",
        schedule = WeekSchedule(
            weekday = listOf("05:50", "07:30", "09:30", "13:30", "15:40", "17:30")
        )
    )

    @Test
    fun `day type is derived from day of week`() {
        assertEquals(DayType.WEEKDAY, DepartureCalculator.dayTypeOf(LocalDate.of(2026, 8, 3)))
        assertEquals(DayType.SATURDAY, DepartureCalculator.dayTypeOf(LocalDate.of(2026, 8, 1)))
        assertEquals(DayType.SUNDAY, DepartureCalculator.dayTypeOf(LocalDate.of(2026, 8, 2)))
    }

    @Test
    fun `departures of today keep the ones that already left`() {
        // Понеділок, 10:00 — перші три рейси вже поїхали.
        val departures = DepartureCalculator.departuresToday(stop, monday(10, 0))

        assertEquals(6, departures.size)
        assertEquals(listOf(true, true, true, false, false, false), departures.map { it.hasLeft })
    }

    @Test
    fun `countdown is measured to the departure time`() {
        val departures = DepartureCalculator.departuresToday(stop, monday(13, 0))

        val next = departures.first { !it.hasLeft }
        assertEquals(LocalTime.of(13, 30), next.time)
        assertEquals(30 * 60L, next.secondsUntil)
    }

    @Test
    fun `a departure happening right now has not left yet`() {
        val departures = DepartureCalculator.departuresToday(stop, monday(9, 30))

        val current = departures.first { it.time == LocalTime.of(9, 30) }
        assertEquals(0L, current.secondsUntil)
        assertTrue(!current.hasLeft)
    }

    @Test
    fun `no departures on a day the route does not run`() {
        // Субота — розклад порожній, тож і рейсів немає.
        val saturday = LocalDateTime.of(2026, 8, 1, 9, 0)

        assertTrue(DepartureCalculator.departuresToday(stop, saturday).isEmpty())
    }

    @Test
    fun `times are parsed leniently, sorted and deduplicated`() {
        val parsed = DepartureCalculator.parseTimes(
            listOf("10:00", " 07:05 ", "7.30", "10:00", "25:00", "не час", "08:75")
        )

        assertEquals(
            listOf(LocalTime.of(7, 5), LocalTime.of(7, 30), LocalTime.of(10, 0)),
            parsed
        )
    }

    @Test
    fun `departures of one stop are merged across routes and sorted`() {
        val routeTimes = DepartureCalculator.routeTimesFrom(
            routes = listOf(throughRoute("Островерхівка", "06:20"), throughRoute("Соколово", "06:05")),
            stopName = "Мерефа (Селекційна)",
            dayType = DayType.WEEKDAY
        )
        val departures = DepartureCalculator.departuresOf(routeTimes, monday(6, 0))

        assertEquals(listOf(LocalTime.of(6, 5), LocalTime.of(6, 20)), departures.map { it.departure.time })
        assertEquals(listOf("Соколово", "Островерхівка"), departures.map { it.route.name })
    }

    @Test
    fun `a stop the route does not serve gives nothing`() {
        val routeTimes = DepartureCalculator.routeTimesFrom(
            routes = listOf(throughRoute("Островерхівка", "06:20")),
            stopName = "Мерефа (вул. Конституції)",
            dayType = DayType.WEEKDAY
        )

        assertTrue(routeTimes.isEmpty())
    }

    @Test
    fun `stop names are collected from every direction without repeats`() {
        val names = DepartureCalculator.stopNames(
            listOf(throughRoute("Островерхівка", "06:20"), throughRoute("Соколово", "06:05"))
        )

        // Мерефа спільна для обох маршрутів, але в списку вибору має бути один раз.
        assertEquals(
            listOf("Мерефа (Селекційна)", "Островерхівка", "Соколово", "Харків (Холодна Гора)"),
            names
        )
    }

    /** Транзитний маршрут: стартує в селі, дорогою бере пасажирів у Мерефі. */
    private fun throughRoute(origin: String, merefaTime: String) = Route(
        id = origin,
        number = null,
        name = origin,
        directions = listOf(
            Direction(
                label = "На Харків",
                stops = listOf(
                    BoardingStop(origin, WeekSchedule(weekday = listOf("05:50"))),
                    BoardingStop("Мерефа (Селекційна)", WeekSchedule(weekday = listOf(merefaTime)))
                )
            ),
            Direction(
                label = "З Харкова",
                stops = listOf(
                    BoardingStop("Харків (Холодна Гора)", WeekSchedule(weekday = listOf("08:00")))
                )
            )
        )
    )

    private fun monday(hour: Int, minute: Int) = LocalDateTime.of(2026, 8, 3, hour, minute)
}
