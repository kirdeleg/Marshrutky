package com.kdelehoi.marshrutky.ui.components

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.kdelehoi.marshrutky.R
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
    return if (minutes < 1) {
        stringResource(R.string.departing_now)
    } else {
        spanText(minutes, R.plurals.in_minutes, R.plurals.in_hours, R.string.in_hours_minutes)
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
    return if (minutes < 1) {
        stringResource(R.string.departed_now)
    } else {
        spanText(minutes, R.plurals.minutes_ago, R.plurals.hours_ago, R.string.hours_minutes_ago)
    }
}

/**
 * Проміжок у хвилинах словами. Відлік уперед і час після відправлення різняться лише набором
 * рядків, тож розбивку на години з хвилинами тримаємо в одному місці.
 */
@Composable
private fun spanText(
    minutes: Int,
    @PluralsRes minutesPlural: Int,
    @PluralsRes hoursPlural: Int,
    @StringRes hoursWithMinutes: Int
): String {
    if (minutes < 60) return pluralStringResource(minutesPlural, minutes, minutes)

    val hours = minutes / 60
    val restMinutes = minutes % 60
    return if (restMinutes == 0) {
        pluralStringResource(hoursPlural, hours, hours)
    } else {
        stringResource(hoursWithMinutes, hours, restMinutes)
    }
}
