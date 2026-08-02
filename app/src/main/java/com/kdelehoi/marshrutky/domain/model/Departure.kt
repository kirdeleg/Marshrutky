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
