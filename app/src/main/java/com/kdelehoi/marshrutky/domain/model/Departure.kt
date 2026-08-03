package com.kdelehoi.marshrutky.domain.model

import java.time.LocalTime

/** Рейс сьогоднішнього дня. Від'ємний [secondsUntil] означає, що маршрутка вже поїхала. */
data class Departure(
    val time: LocalTime,
    val secondsUntil: Long
) {
    val hasLeft: Boolean
        get() = secondsUntil < 0
}

/**
 * Час відправлення від зупинки разом з маршрутом. Не залежить від поточного моменту, тому
 * рахується один раз на добу, а не щосекунди разом із відліком.
 */
data class RouteTime(
    val route: Route,
    /** Куди їде саме цей рейс: назва маршруту від зупинки не каже, у який бік він рушить. */
    val destination: String,
    val time: LocalTime
)

/** Рейс на вкладці «Найближчі»: час відправлення плюс маршрут, яким він поїде. */
data class StopDeparture(
    val route: Route,
    val destination: String,
    val departure: Departure
) {
    /**
     * Час у добі унікальний разом з маршрутом і напрямком: об 19:10 від Холодної Гори їде і
     * Соколове, і Островерхівка, а транзитний маршрут може проходити ту саму зупинку в обидва боки.
     */
    val key: String
        get() = "${route.id}@$destination@${departure.time}"
}
