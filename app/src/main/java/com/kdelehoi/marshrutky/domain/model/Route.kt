package com.kdelehoi.marshrutky.domain.model

import kotlinx.serialization.Serializable

/** Вміст одного файлу з теки `routes` репозиторію. Ідентифікатор маршруту — це ім'я файлу. */
@Serializable
data class RouteFile(
    val number: String? = null,
    val name: String,
    val directions: List<Direction> = emptyList()
)

data class Route(
    val id: String,
    val number: String?,
    val name: String,
    val directions: List<Direction>
) {
    /** Те, що показуємо в заголовку чіпа: «Комарівка — Харків (199)». Приміські рейси часто
     *  ходять без оприлюдненого номера — тоді лишається сама назва. */
    val title: String
        get() = if (number.isNullOrBlank()) name else "$name ($number)"

    fun matches(query: String): Boolean {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return true
        return number?.contains(trimmed, ignoreCase = true) == true ||
            name.contains(trimmed, ignoreCase = true)
    }
}

@Serializable
data class Direction(
    val label: String,
    /**
     * Кінцева зупинка напрямку. Її немає серед [stops], бо там лише місця посадки, а виводити її
     * з назви маршруту не виходить: «Зелений Гай — Харків» вирушає з «Високий (Зелений Гай)», тож
     * населений пункт у назві й у зупинці різні.
     */
    val destination: String? = null,
    val stops: List<BoardingStop> = emptyList(),
    // Ранній формат: одна точка посадки прямо в напрямку. Нові файли пишуть `stops`.
    private val boardingStop: String? = null,
    private val schedule: WeekSchedule = WeekSchedule()
) {
    /**
     * Точки посадки в порядку руху. Транзитний маршрут перелічує їх усі, тож у Мерефі видно час
     * машини, яка стартувала в Островерхівці. Файли старого формату дають список з однієї точки.
     */
    val boardingStops: List<BoardingStop>
        get() = stops.ifEmpty { listOf(BoardingStop(boardingStop ?: label, schedule)) }

    /** Початок напрямку — те, що показуємо там, де для всіх зупинок немає місця. */
    val origin: BoardingStop
        get() = boardingStops.first()
}

/** Зупинка з власним розкладом: та сама машина проходить її пізніше, ніж початковий пункт. */
@Serializable
data class BoardingStop(
    val name: String,
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

/** Мова інтерфейсу. [tag] — тег IETF для `Locale`; у системної його немає, бо ми її не задаємо. */
enum class AppLanguage(val tag: String?) {
    SYSTEM(null),
    UKRAINIAN("uk"),
    ENGLISH("en"),
    RUSSIAN("ru")
}
