package com.kdelehoi.marshrutky.domain.model

import kotlinx.serialization.Serializable

/** Вміст одного файлу з `assets/routes`. Ідентифікатор маршруту береться з імені файлу. */
@Serializable
data class RouteFile(
    val number: String,
    val name: String,
    val directions: List<Direction> = emptyList()
)

data class Route(
    val id: String,
    val number: String,
    val name: String,
    val directions: List<Direction>
) {
    /** Те, що показуємо в заголовку чіпа: «Комарівка — Харків (199)». */
    val title: String
        get() = "$name ($number)"

    fun matches(query: String): Boolean {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return true
        return number.contains(trimmed, ignoreCase = true) || name.contains(trimmed, ignoreCase = true)
    }
}

@Serializable
data class Direction(
    val label: String,
    val boardingStop: String? = null,
    val schedule: WeekSchedule = WeekSchedule()
)

@Serializable
data class WeekSchedule(
    val weekday: List<String> = emptyList(),
    val saturday: List<String> = emptyList(),
    val sunday: List<String> = emptyList()
) {
    fun timesFor(dayType: DayType): List<String> = when (dayType) {
        DayType.WEEKDAY -> weekday
        DayType.SATURDAY -> saturday
        DayType.SUNDAY -> sunday
    }
}

enum class DayType {
    WEEKDAY,
    SATURDAY,
    SUNDAY
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}
