package com.kdelehoi.marshrutky.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kdelehoi.marshrutky.R
import com.kdelehoi.marshrutky.domain.DepartureCalculator
import com.kdelehoi.marshrutky.domain.model.StopDeparture
import com.kdelehoi.marshrutky.ui.components.DropdownField
import com.kdelehoi.marshrutky.ui.components.ROUTE_BADGE_SIZE_SMALL
import com.kdelehoi.marshrutky.ui.components.RouteNumberBadge
import com.kdelehoi.marshrutky.ui.components.ScreenLoading
import com.kdelehoi.marshrutky.ui.components.ScreenMessage
import com.kdelehoi.marshrutky.ui.components.TabScreenInsets
import com.kdelehoi.marshrutky.ui.components.countdownText
import com.kdelehoi.marshrutky.ui.components.formatted
import java.time.LocalDateTime
import com.kdelehoi.marshrutky.viewmodel.ScheduleUiState

/**
 * Рейси однієї зупинки, зібрані по всіх маршрутах. Потрібна там, де через одне місце проходить
 * багато маршрутів: від Смачних історій щодня відправляється більш ніж три десятки, і перебирати
 * їхні картки по одній, щоб знайти найближчий, — марна робота.
 */
@Composable
fun NearestScreen(
    state: ScheduleUiState,
    now: LocalDateTime,
    onSelectStop: (String) -> Unit,
    onOpenRoute: (String) -> Unit
) {
    Scaffold(contentWindowInsets = TabScreenInsets) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            // Розбір розкладу не залежить від поточного моменту, тож тримаємо його в remember:
            // тік годинника й гортання вкладок мають коштувати лише перерахунку відліку.
            val stopNames = remember(state.routes) { DepartureCalculator.stopNames(state.routes) }
            // Зупинка могла зникнути з розкладів між запусками, тоді вибір скидається.
            val selected = state.selectedStop?.takeIf { it in stopNames }

            if (state.isLoading) {
                ScreenLoading()
                return@Column
            }

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

            val today = DepartureCalculator.dayTypeOf(now.toLocalDate())
            val routeTimes = remember(state.routes, selected, today) {
                DepartureCalculator.routeTimesFrom(state.routes, selected, today)
            }
            val upcoming = DepartureCalculator.departuresOf(routeTimes, now)
                .filterNot { it.departure.hasLeft }

            when {
                routeTimes.isEmpty() -> ScreenMessage(
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
                    itemsIndexed(upcoming, key = { _, item -> item.key }) { index, item ->
                        DepartureRow(
                            item = item,
                            isNext = index == 0,
                            onClick = { onOpenRoute(item.route.id) },
                            // Коли маршрутка від'їжджає, її рядок зникає зі списку — без цього
                            // решта стрибала б угору ривком.
                            modifier = Modifier.animateItem()
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
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = if (isNext) {
                MaterialTheme.colorScheme.surfaceContainerHighest
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            }
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
                    text = countdownText(item.departure.secondsUntil),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isNext) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            // Час — головне в рядку, тож він найбільший. У найближчого рейсу він ще й залитий:
            // разом із фоном картки це позначка «оце наступний». Ширина стала в обох станах, щоб
            // права кромка часу лишалася рівною.
            Box(
                modifier = Modifier.size(width = TIME_WIDTH, height = TIME_HEIGHT),
                contentAlignment = Alignment.Center
            ) {
                if (isNext) {
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
