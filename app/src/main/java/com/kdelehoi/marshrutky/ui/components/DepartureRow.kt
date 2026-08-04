package com.kdelehoi.marshrutky.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kdelehoi.marshrutky.domain.model.StopDeparture

/**
 * Рейс однією карткою: номер маршруту, кінцева, відлік і час відправлення.
 *
 * Час — головне в рядку, тож він найбільший. У найближчого рейсу він ще й залитий: разом із фоном
 * картки це позначка «оце наступний».
 */
@Composable
fun DepartureRow(
    item: StopDeparture,
    state: TripState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = when (state) {
        TripState.NEXT -> MaterialTheme.colorScheme.surfaceContainerHighest
        TripState.UPCOMING -> MaterialTheme.colorScheme.surfaceContainerHigh
        TripState.PAST -> MaterialTheme.colorScheme.surfaceContainerLow
    }
    val contentColor = if (state == TripState.PAST) {
        pastTripContentColor()
    } else {
        contentColorFor(containerColor)
    }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Маршрути без номера місце під значок усе одно займають, інакше кінцеві в сусідніх
            // рядках роз'їхалися б по різних вертикалях.
            if (item.route.number != null) {
                RouteNumberBadge(
                    number = item.route.number,
                    // Значок кольорів картки не успадковує, тож гасимо його окремо.
                    modifier = if (state == TripState.PAST) Modifier.alpha(TRIP_PAST_ALPHA) else Modifier,
                    size = ROUTE_BADGE_SIZE_SMALL,
                    style = MaterialTheme.typography.labelMedium
                )
            } else {
                Spacer(modifier = Modifier.size(ROUTE_BADGE_SIZE_SMALL))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.destination,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (state == TripState.PAST) {
                        agoText(item.departure.secondsUntil)
                    } else {
                        countdownText(item.departure.secondsUntil)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = when (state) {
                        TripState.NEXT -> MaterialTheme.colorScheme.primary
                        TripState.UPCOMING -> MaterialTheme.colorScheme.onSurfaceVariant
                        // Успадковує пригашений колір картки.
                        TripState.PAST -> Color.Unspecified
                    }
                )
            }

            // Ширина стала в обох станах, щоб права кромка часу лишалася рівною.
            Box(
                modifier = Modifier.size(width = TIME_WIDTH, height = TIME_HEIGHT),
                contentAlignment = Alignment.Center
            ) {
                if (state == TripState.NEXT) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            TimeText(item.departure.time.formatted())
                        }
                    }
                } else {
                    TimeText(item.departure.time.formatted())
                }
            }
        }
    }
}

@Composable
private fun TimeText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
    )
}

private val TIME_WIDTH = 80.dp
private val TIME_HEIGHT = 44.dp
