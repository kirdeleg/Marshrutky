package com.kdelehoi.marshrutky.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Стан рейсу на екрані. Один енум на всі місця, де показані часи: і чипи розкладу маршруту, і рядки
 * «Найближчих» мусять гасити минуле однаково, а поки в кожного був власний набір станів, однаковість
 * трималася на комментарі.
 */
enum class TripState {

    /** Найближчий рейс — головний акцент. */
    NEXT,

    /** Рейс попереду. */
    UPCOMING,

    /** Маршрутка вже поїхала. */
    PAST
}

/** Наскільки гасне те, що поїхало. */
const val TRIP_PAST_ALPHA = 0.5f

/** Колір тексту поїханого рейсу. */
@Composable
fun pastTripContentColor(): Color =
    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = TRIP_PAST_ALPHA)
