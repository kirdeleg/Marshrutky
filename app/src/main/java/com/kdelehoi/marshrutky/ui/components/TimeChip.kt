package com.kdelehoi.marshrutky.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalTime

@Composable
fun TimeChip(
    time: LocalTime,
    state: TripState,
    modifier: Modifier = Modifier,
    secondsUntil: Long = 0
) {
    val containerColor = when (state) {
        TripState.NEXT -> MaterialTheme.colorScheme.primary
        TripState.UPCOMING -> MaterialTheme.colorScheme.surfaceContainerHighest
        TripState.PAST -> MaterialTheme.colorScheme.surfaceContainerLow
    }
    val contentColor = when (state) {
        TripState.NEXT -> MaterialTheme.colorScheme.onPrimary
        TripState.UPCOMING -> MaterialTheme.colorScheme.onSurface
        TripState.PAST -> pastTripContentColor()
    }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = containerColor,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = time.formatted(),
                style = MaterialTheme.typography.titleLarge
            )

            if (state == TripState.NEXT) {
                Text(
                    text = countdownText(secondsUntil),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}
