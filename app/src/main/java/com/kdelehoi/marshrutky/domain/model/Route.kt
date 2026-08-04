package com.kdelehoi.marshrutky.domain.model

data class Route(
    /** Ім'я файлу з розкладом без суфікса: іншого стійкого ключа в маршрута немає. */
    val id: String,
    val number: String?,
    val name: String,
    val directions: List<Direction>
) {
    /** Те, що показуємо в заголовку: «Комарівка — Харків (199)». Приміські рейси часто ходять без
     *  оприлюдненого номера — тоді лишається сама назва. */
    val title: String
        get() = if (number == null) name else "$name ($number)"

    fun matches(query: String): Boolean {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return true
        return number?.contains(trimmed, ignoreCase = true) == true ||
            name.contains(trimmed, ignoreCase = true)
    }
}

data class Direction(
    /**
     * Кінцева зупинка напрямку. Її немає серед [boardingStops], бо там лише місця посадки, а виводити
     * її з назви маршруту не виходить: «Зелений Гай — Харків» вирушає з «Високий (Зелений Гай)», тож
     * населений пункт у назві й у зупинці різні.
     */
    val destination: String,
    /**
     * Точки посадки в порядку руху. Транзитний маршрут перелічує їх усі, тож у Мерефі видно час
     * машини, яка стартувала в Островерхівці. Порожнім цей список не буває — напрямок без зупинок
     * не проходить розбору файлу.
     */
    val boardingStops: List<BoardingStop>
) {
    /** Початок напрямку — те, що показуємо там, де для всіх зупинок немає місця. */
    val origin: BoardingStop
        get() = boardingStops.first()
}

/** Зупинка з власним розкладом: та сама машина проходить її пізніше, ніж початковий пункт. */
data class BoardingStop(
    val name: String,
    val schedule: WeekSchedule
)

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
