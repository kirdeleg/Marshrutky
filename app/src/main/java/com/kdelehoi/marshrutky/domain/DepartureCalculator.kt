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

    // Розклад повторюється щотижня, тож далі за 7 днів шукати нема сенсу.
    private const val MAX_DAYS_AHEAD = 8

    fun dayTypeOf(date: LocalDate): DayType = when (date.dayOfWeek) {
        DayOfWeek.SATURDAY -> DayType.SATURDAY
        DayOfWeek.SUNDAY -> DayType.SUNDAY
        else -> DayType.WEEKDAY
    }

    fun parseTimes(times: List<String>): List<LocalTime> =
        times.mapNotNull(::parseTime).distinct().sorted()

    fun upcoming(direction: Direction, now: LocalDateTime, limit: Int): List<Departure> {
        val result = mutableListOf<Departure>()
        val today = now.toLocalDate()

        var dayOffset = 0L
        while (result.size < limit && dayOffset < MAX_DAYS_AHEAD) {
            val date = today.plusDays(dayOffset)
            val times = parseTimes(direction.schedule.timesFor(dayTypeOf(date)))

            for (time in times) {
                val departureAt = LocalDateTime.of(date, time)
                if (departureAt.isBefore(now)) continue

                result += Departure(
                    time = time,
                    date = date,
                    secondsUntil = Duration.between(now, departureAt).seconds,
                    isToday = dayOffset == 0L
                )
                if (result.size == limit) break
            }
            dayOffset++
        }

        return result
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
