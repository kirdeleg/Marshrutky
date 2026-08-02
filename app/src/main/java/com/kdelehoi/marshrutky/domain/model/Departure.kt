package com.kdelehoi.marshrutky.domain.model

import java.time.LocalDate
import java.time.LocalTime

data class Departure(
    val time: LocalTime,
    val date: LocalDate,
    val secondsUntil: Long,
    val isToday: Boolean
) {
    val minutesUntil: Long
        get() = secondsUntil / 60
}
