package com.kdelehoi.marshrutky.data.format

import com.kdelehoi.marshrutky.domain.model.BoardingStop
import com.kdelehoi.marshrutky.domain.model.Direction
import com.kdelehoi.marshrutky.domain.model.Route
import com.kdelehoi.marshrutky.domain.model.WeekSchedule
import kotlinx.serialization.Serializable

/**
 * Формат файлу з теки `routes` — рівно те, що лежить у репозиторії з розкладами.
 *
 * Окремо від доменної моделі навмисне. Тут живуть усі поступки старим файлам: пропущені поля,
 * ранній вигляд напрямку, назва маршруту замість кінцевої. Сумісність — справа шару даних, і що
 * менше про неї знає решта застосунку, то дешевше буде додати друге джерело розкладів.
 */
@Serializable
data class RouteFile(
    val number: String? = null,
    val name: String,
    val directions: List<DirectionFile> = emptyList()
)

@Serializable
data class DirectionFile(
    /** Підпис напрямку зі старих файлів. Ніде не показуємо, лишився запасною назвою зупинки. */
    val label: String = "",
    val destination: String? = null,
    val stops: List<StopFile> = emptyList(),
    /** Ранній формат: одна точка посадки прямо в напрямку. Нові файли пишуть [stops]. */
    val boardingStop: String? = null,
    val schedule: ScheduleFile = ScheduleFile()
)

@Serializable
data class StopFile(
    val name: String,
    val schedule: ScheduleFile = ScheduleFile()
)

@Serializable
data class ScheduleFile(
    val weekday: List<String> = emptyList(),
    val saturday: List<String> = emptyList(),
    val sunday: List<String> = emptyList()
)

/** Файл у маршрут. Ідентифікатор маршруту — це [id], тобто ім'я файлу без суфікса. */
fun RouteFile.toRoute(id: String): Route = Route(
    id = id,
    number = number?.takeIf { it.isNotBlank() },
    name = name,
    directions = directions.mapNotNull { it.toDirection(routeName = name) }
)

/** Напрямок без жодної точки посадки показати нічим, тож такий просто не доїжджає до домену. */
private fun DirectionFile.toDirection(routeName: String): Direction? {
    val stops = boardingStops()
    if (stops.isEmpty()) return null

    return Direction(
        // Файли без кінцевої лишаються читабельними: замість неї показуємо назву маршруту, тобто
        // рівно те, що бачили до появи поля.
        destination = destination?.takeIf { it.isNotBlank() } ?: routeName,
        boardingStops = stops
    )
}

private fun DirectionFile.boardingStops(): List<BoardingStop> = when {
    stops.isNotEmpty() -> stops.map { BoardingStop(it.name, it.schedule.toWeekSchedule()) }
    else -> {
        val name = boardingStop ?: label
        if (name.isBlank()) emptyList() else listOf(BoardingStop(name, schedule.toWeekSchedule()))
    }
}

private fun ScheduleFile.toWeekSchedule(): WeekSchedule =
    WeekSchedule(weekday = weekday, saturday = saturday, sunday = sunday)
