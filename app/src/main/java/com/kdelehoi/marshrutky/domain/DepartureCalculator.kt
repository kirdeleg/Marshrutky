package com.kdelehoi.marshrutky.domain

import com.kdelehoi.marshrutky.domain.model.DayType
import com.kdelehoi.marshrutky.domain.model.Departure
import com.kdelehoi.marshrutky.domain.model.Direction
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

object DepartureCalculator {

    fun dayTypeOf(date: LocalDate): DayType = when (date.dayOfWeek) {
        DayOfWeek.SATURDAY -> DayType.SATURDAY
        DayOfWeek.SUNDAY -> DayType.SUNDAY
        else -> DayType.WEEKDAY
    }

    fun timesOf(direction: Direction, dayType: DayType): List<LocalTime> =
        parseTimes(direction.schedule.timesFor(dayType))

    fun parseTimes(times: List<String>): List<LocalTime> =
        times.mapNotNull(::parseTime).distinct().sorted()

    /**
     * Усі сьогоднішні рейси напрямку. Ті, що вже поїхали, лишаються в списку з від'ємним
     * відліком — на вкладці «Сьогодні» вони показані приглушено.
     */
    fun departuresToday(direction: Direction, now: LocalDateTime): List<Departure> {
        val times = timesOf(direction, dayTypeOf(now.toLocalDate()))
        return times.map { time ->
            Departure(
                time = time,
                secondsUntil = Duration.between(now.toLocalTime(), time).seconds
            )
        }
    }

    private fun parseTime(raw: String): LocalTime? {
        val parts = raw.trim().split(":", ".")
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return LocalTime.of(hour, minute)
    }
}
