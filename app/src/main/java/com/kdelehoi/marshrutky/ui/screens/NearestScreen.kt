package com.kdelehoi.marshrutky.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kdelehoi.marshrutky.R
import com.kdelehoi.marshrutky.domain.model.StopDeparture
import com.kdelehoi.marshrutky.ui.components.DropdownField
import com.kdelehoi.marshrutky.ui.components.ScreenMessage
import com.kdelehoi.marshrutky.ui.components.countdownText
import com.kdelehoi.marshrutky.ui.components.formatted
import com.kdelehoi.marshrutky.viewmodel.ScheduleUiState

/**
 * Рейси однієї зупинки, зібрані по всіх маршрутах. Потрібна там, де через одне місце проходить
 * багато маршрутів: від Смачних історій щодня відправляється більш ніж три десятки, і перебирати
 * їхні картки по одній, щоб знайти найближчий, — марна робота.
 */
@Composable
fun NearestScreen(
    state: ScheduleUiState,
    onSelectStop: (String) -> Unit,
    onOpenRoute: (String) -> Unit
) {
    Scaffold { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            val stopNames = state.stopNames
            val selected = state.knownSelectedStop

            if (stopNames.isEmpty()) {
                ScreenMessage(
                    title = stringResource(R.string.routes_empty_title),
                    subtitle = stringResource(R.string.routes_empty_subtitle)
                )
                return@Column
            }

            DropdownField(
                label = stringResource(R.string.nearest_stop_label),
                selected = selected,
                options = stopNames,
                optionLabel = { it },
                onSelect = onSelectStop,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            if (selected == null) {
                ScreenMessage(
                    title = stringResource(R.string.nearest_no_stop_title),
                    subtitle = stringResource(R.string.nearest_no_stop_subtitle)
                )
                return@Column
            }

            val today = state.departuresFromSelectedStop()
            val upcoming = today.filterNot { it.departure.hasLeft }

            when {
                today.isEmpty() -> ScreenMessage(
                    title = stringResource(R.string.nearest_none_title),
                    subtitle = stringResource(R.string.nearest_none_subtitle)
                )

                upcoming.isEmpty() -> ScreenMessage(
                    title = stringResource(R.string.direction_no_more_today),
                    subtitle = stringResource(R.string.nearest_no_more_subtitle)
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(upcoming) { index, item ->
                        DepartureRow(
                            item = item,
                            isNext = index == 0,
                            onClick = { onOpenRoute(item.route.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DepartureRow(
    item: StopDeparture,
    isNext: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (isNext) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ширина фіксована: «через 23 хвилини» і «через 1 год 23 хв» різної довжини,
            // і без цього назви маршрутів стрибали б від рядка до рядка.
            Column(modifier = Modifier.width(TIME_COLUMN_WIDTH)) {
                Text(
                    text = item.departure.time.formatted(),
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = countdownText(item.departure.secondsUntil),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isNext) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            Text(
                text = item.route.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private val TIME_COLUMN_WIDTH = 116.dp
