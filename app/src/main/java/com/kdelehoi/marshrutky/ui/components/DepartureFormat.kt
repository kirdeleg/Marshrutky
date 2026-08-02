package com.kdelehoi.marshrutky.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.kdelehoi.marshrutky.R
import com.kdelehoi.marshrutky.domain.model.DayType
import com.kdelehoi.marshrutky.domain.model.Departure
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")

// Інтерфейс україномовний, тож назви днів беремо з української локалі, а не з локалі системи.
private val UK_LOCALE = Locale.forLanguageTag("uk")

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
fun dayLabel(departure: Departure, today: LocalDate): String? = when {
    departure.isToday -> null
    departure.date == today.plusDays(1) -> stringResource(R.string.tomorrow)
    else -> departure.date.dayOfWeek.getDisplayName(TextStyle.FULL_STANDALONE, UK_LOCALE)
        .replaceFirstChar { it.titlecase(UK_LOCALE) }
}

@Composable
fun dayTypeLabel(dayType: DayType): String = stringResource(
    when (dayType) {
        DayType.WEEKDAY -> R.string.day_weekday
        DayType.SATURDAY -> R.string.day_saturday
        DayType.SUNDAY -> R.string.day_sunday
    }
)
