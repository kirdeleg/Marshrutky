package com.kdelehoi.marshrutky.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.kdelehoi.marshrutky.R
import com.kdelehoi.marshrutky.domain.model.DayType
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")

fun LocalTime.formatted(): String = format(TIME_FORMATTER)

@Composable
fun countdownText(secondsUntil: Long): String {
    val minutes = (secondsUntil / 60).toInt()
    return when {
        minutes < 1 -> stringResource(R.string.departing_now)
        minutes < 60 -> pluralStringResource(R.plurals.in_minutes, minutes, minutes)
        else -> {
            val hours = minutes / 60
            val restMinutes = minutes % 60
            if (restMinutes == 0) {
                pluralStringResource(R.plurals.in_hours, hours, hours)
            } else {
                stringResource(R.string.in_hours_minutes, hours, restMinutes)
            }
        }
    }
}

@Composable
fun dayTypeLabel(dayType: DayType): String = stringResource(
    when (dayType) {
        DayType.WEEKDAY -> R.string.day_weekday
        DayType.SATURDAY -> R.string.day_saturday
        DayType.SUNDAY -> R.string.day_sunday
    }
)
