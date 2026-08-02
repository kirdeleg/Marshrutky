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

enum class TimeChipStyle {
    /** Рейс попереду — звичайний чип. */
    UPCOMING,

    /** Найближчий рейс — залитий, з відліком. */
    NEXT,

    /** Маршрутка вже поїхала. */
    PAST
}

@Composable
fun TimeChip(
    time: LocalTime,
    style: TimeChipStyle,
    modifier: Modifier = Modifier,
    secondsUntil: Long = 0
) {
    val containerColor = when (style) {
        TimeChipStyle.NEXT -> MaterialTheme.colorScheme.primary
        TimeChipStyle.UPCOMING -> MaterialTheme.colorScheme.surfaceContainerHighest
        TimeChipStyle.PAST -> MaterialTheme.colorScheme.surfaceContainerLow
    }
    val contentColor = when (style) {
        TimeChipStyle.NEXT -> MaterialTheme.colorScheme.onPrimary
        TimeChipStyle.UPCOMING -> MaterialTheme.colorScheme.onSurface
        TimeChipStyle.PAST -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
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

            if (style == TimeChipStyle.NEXT) {
                Text(
                    text = countdownText(secondsUntil),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}
