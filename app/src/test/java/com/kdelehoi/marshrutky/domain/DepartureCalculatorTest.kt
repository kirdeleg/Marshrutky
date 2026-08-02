package com.kdelehoi.marshrutky.domain

import com.kdelehoi.marshrutky.domain.model.DayType
import com.kdelehoi.marshrutky.domain.model.Direction
import com.kdelehoi.marshrutky.domain.model.WeekSchedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class DepartureCalculatorTest {

    private val direction = Direction(
        origin = "Київ",
        destination = "Бровари",
        schedule = WeekSchedule(
            weekday = listOf("08:00", "09:00", "10:00"),
            saturday = listOf("09:30"),
            sunday = emptyList()
        )
    )

    @Test
    fun `day type is derived from day of week`() {
        assertEquals(DayType.WEEKDAY, DepartureCalculator.dayTypeOf(LocalDate.of(2026, 8, 3)))
        assertEquals(DayType.SATURDAY, DepartureCalculator.dayTypeOf(LocalDate.of(2026, 8, 1)))
        assertEquals(DayType.SUNDAY, DepartureCalculator.dayTypeOf(LocalDate.of(2026, 8, 2)))
    }

    @Test
    fun `upcoming skips departures that already left`() {
        // Понеділок, 08:30 — рейс о 08:00 уже пішов.
        val now = LocalDateTime.of(2026, 8, 3, 8, 30)

        val upcoming = DepartureCalculator.upcoming(direction, now, limit = 2)

        assertEquals(listOf(LocalTime.of(9, 0), LocalTime.of(10, 0)), upcoming.map { it.time })
        assertTrue(upcoming.all { it.isToday })
        assertEquals(30 * 60L, upcoming.first().secondsUntil)
    }

    @Test
    fun `upcoming includes a departure happening right now`() {
        val now = LocalDateTime.of(2026, 8, 3, 9, 0)

        val upcoming = DepartureCalculator.upcoming(direction, now, limit = 1)

        assertEquals(LocalTime.of(9, 0), upcoming.single().time)
        assertEquals(0L, upcoming.single().secondsUntil)
    }

    @Test
    fun `upcoming rolls over to the next day with its own schedule`() {
        // П'ятниця ввечері — далі субота з одним рейсом, потім понеділок.
        val now = LocalDateTime.of(2026, 7, 31, 23, 0)

        val upcoming = DepartureCalculator.upcoming(direction, now, limit = 2)

        assertEquals(LocalDate.of(2026, 8, 1), upcoming[0].date)
        assertEquals(LocalTime.of(9, 30), upcoming[0].time)
        assertFalse(upcoming[0].isToday)

        // Неділя порожня, тож наступний рейс аж у понеділок.
        assertEquals(LocalDate.of(2026, 8, 3), upcoming[1].date)
        assertEquals(LocalTime.of(8, 0), upcoming[1].time)
    }

    @Test
    fun `empty schedule produces no departures`() {
        val empty = direction.copy(schedule = WeekSchedule())

        val upcoming = DepartureCalculator.upcoming(empty, LocalDateTime.of(2026, 8, 3, 8, 0), limit = 5)

        assertTrue(upcoming.isEmpty())
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
}
