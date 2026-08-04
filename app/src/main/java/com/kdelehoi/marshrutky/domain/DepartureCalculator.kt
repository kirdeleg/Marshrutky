package com.kdelehoi.marshrutky.domain

import com.kdelehoi.marshrutky.domain.model.BoardingStop
import com.kdelehoi.marshrutky.domain.model.DayType
import com.kdelehoi.marshrutky.domain.model.Departure
import com.kdelehoi.marshrutky.domain.model.Route
import com.kdelehoi.marshrutky.domain.model.RouteTime
import com.kdelehoi.marshrutky.domain.model.StopDeparture
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

    fun timesOf(stop: BoardingStop, dayType: DayType): List<LocalTime> =
        parseTimes(stop.schedule.timesFor(dayType))

    fun parseTimes(times: List<String>): List<LocalTime> =
        times.mapNotNull(::parseTime).distinct().sorted()

    /**
     * Відлік до кожного з переданих рейсів. Розбір рядків і сортування сюди не входять, тому
     * викликати щосекунди дешево — саме цю частину й перераховує годинник.
     */
    fun departures(times: List<LocalTime>, now: LocalDateTime): List<Departure> =
        times.map { time ->
            Departure(
                time = time,
                secondsUntil = Duration.between(now.toLocalTime(), time).seconds
            )
        }

    /**
     * Усі сьогоднішні рейси від зупинки. Ті, що вже поїхали, лишаються в списку з від'ємним
     * відліком — на вкладці «Сьогодні» вони показані приглушено.
     */
    fun departuresToday(stop: BoardingStop, now: LocalDateTime): List<Departure> =
        departures(timesOf(stop, dayTypeOf(now.toLocalDate())), now)

    /**
     * Час відправлення від зупинки, зібраний по всіх маршрутах. Одна зупинка з однаковою назвою
     * в різних файлах — це одне й те саме місце, тому списки зливаються в один.
     */
    fun routeTimesFrom(routes: List<Route>, stopName: String, dayType: DayType): List<RouteTime> =
        routes.flatMap { route ->
            route.directions.flatMap { direction ->
                direction.boardingStops
                    .filter { it.name == stopName }
                    .flatMap { stop ->
                        timesOf(stop, dayType).map { RouteTime(route, direction.destination, it) }
                    }
            }
        }.sortedBy { it.time }

    fun departuresOf(routeTimes: List<RouteTime>, now: LocalDateTime): List<StopDeparture> =
        routeTimes.map { (route, destination, time) ->
            StopDeparture(
                route = route,
                destination = destination,
                departure = Departure(
                    time = time,
                    secondsUntil = Duration.between(now.toLocalTime(), time).seconds
                )
            )
        }

    /** Назви всіх зупинок, від яких хоч колись хтось відправляється. */
    fun stopNames(routes: List<Route>): List<String> =
        routes.asSequence()
            .flatMap { it.directions }
            .flatMap { it.boardingStops }
            .map { it.name }
            .distinct()
            .sorted()
            .toList()

    private fun parseTime(raw: String): LocalTime? {
        val parts = raw.trim().split(":", ".")
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return LocalTime.of(hour, minute)
    }
}
