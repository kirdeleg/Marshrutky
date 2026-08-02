package com.kdelehoi.marshrutky.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kdelehoi.marshrutky.R
import com.kdelehoi.marshrutky.domain.model.Departure
import java.time.LocalDate

@Composable
fun NextDepartureCard(
    departure: Departure,
    today: LocalDate,
    travelTimeMinutes: Int?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsBus,
                    contentDescription = null
                )
                Text(
                    text = stringResource(R.string.next_departure),
                    style = MaterialTheme.typography.labelLarge
                )
                dayLabel(departure, today)?.let { label ->
                    Text(
                        text = "· $label",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Text(
                text = departure.time.formatted(),
                style = MaterialTheme.typography.displayMedium
            )

            Text(
                text = countdownText(departure.secondsUntil),
                style = MaterialTheme.typography.titleMedium
            )

            travelTimeMinutes?.let { minutes ->
                Text(
                    text = stringResource(R.string.travel_time, minutes),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
