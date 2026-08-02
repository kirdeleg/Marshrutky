package com.kdelehoi.marshrutky.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ScheduleFile(
    val routes: List<Route> = emptyList()
)

@Serializable
data class Route(
    val id: String,
    val number: String,
    val title: String,
    val carrier: String? = null,
    val directions: List<Direction> = emptyList()
)

@Serializable
data class Direction(
    val origin: String,
    val destination: String,
    val boardingStop: String? = null,
    val travelTimeMinutes: Int? = null,
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
