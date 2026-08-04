package com.kdelehoi.marshrutky.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.kdelehoi.marshrutky.R
import com.kdelehoi.marshrutky.domain.model.DayType
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.ceil

private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")

/** Нерозривний пробіл: місце, де рядок ламатися не має. */
private const val NBSP = '\u00A0'

fun LocalTime.formatted(): String = format(TIME_FORMATTER)

/**
 * Уточнення в дужках лишається цілим: «Харків (Холодна Гора)» переноситься перед дужкою, а не
 * всередині неї, як робив би звичайний перенос по пробілах.
 */
fun String.wrapBeforeParentheses(): String = buildString {
    var depth = 0
    for (char in this@wrapBeforeParentheses) {
        when (char) {
            '(' -> depth++
            ')' -> if (depth > 0) depth--
        }
        append(if (char == ' ' && depth > 0) NBSP else char)
    }
}

@Composable
fun countdownText(secondsUntil: Long): String {
    // Округлення вгору, бо перемальовуємо раз на хвилину: о 09:35 рейс о 09:40 має лишатися
    // «через 5 хвилин» усю хвилину. З округленням униз він став би «через 4» вже о 09:35:01.
    val minutes = ceil(secondsUntil / 60.0).toInt()
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

/**
 * Скільки минуло після відправлення — дзеркало [countdownText] для рейсів, які вже поїхали.
 * Приймає той самий від'ємний `secondsUntil`, щоб місце виклику не мусило міняти знак.
 */
@Composable
fun agoText(secondsUntil: Long): String {
    // Тут, на відміну від countdownText, округлення вниз: рейс о 15:00 лишається «щойно
    // відправився» всю хвилину і стає «1 хвилину тому» рівно тоді, коли годинник покаже 15:01.
    val minutes = (-secondsUntil / 60).toInt()
    return when {
        minutes < 1 -> stringResource(R.string.departed_now)
        minutes < 60 -> pluralStringResource(R.plurals.minutes_ago, minutes, minutes)
        else -> {
            val hours = minutes / 60
            val restMinutes = minutes % 60
            if (restMinutes == 0) {
                pluralStringResource(R.plurals.hours_ago, hours, hours)
            } else {
                stringResource(R.string.hours_minutes_ago, hours, restMinutes)
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
