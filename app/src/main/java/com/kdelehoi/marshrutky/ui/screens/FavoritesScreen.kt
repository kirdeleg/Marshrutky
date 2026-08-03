package com.kdelehoi.marshrutky.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kdelehoi.marshrutky.R
import com.kdelehoi.marshrutky.domain.DepartureCalculator
import com.kdelehoi.marshrutky.domain.model.Direction
import com.kdelehoi.marshrutky.domain.model.Route
import com.kdelehoi.marshrutky.ui.components.DirectionColumns
import com.kdelehoi.marshrutky.ui.components.ScreenMessage
import com.kdelehoi.marshrutky.ui.components.SearchableScaffold
import com.kdelehoi.marshrutky.ui.components.rememberReorderState
import com.kdelehoi.marshrutky.ui.components.reorderable
import com.kdelehoi.marshrutky.ui.components.countdownText
import com.kdelehoi.marshrutky.ui.components.formatted
import com.kdelehoi.marshrutky.viewmodel.ScheduleUiState
import java.time.LocalDateTime

private const val DEPARTURES_PER_DIRECTION = 3

/** Спільний бічний відступ картки: заголовок і напрямки мають починатися з однієї лінії. */
private val CARD_INSET = 20.dp

@Composable
fun FavoritesScreen(
    state: ScheduleUiState,
    onOpenRoute: (String) -> Unit,
    onReorder: (List<String>) -> Unit
) {
    SearchableScaffold { query ->
        val favorites = state.favoriteRoutes
        // Порядок правимо локально, поки картку тягнуть, і зберігаємо вже після відпускання —
        // інакше кожен обмін місцями їздив би в сховище й вертався звідти з затримкою.
        val ordered = remember { mutableStateListOf<Route>() }
        LaunchedEffect(favorites) {
            if (ordered != favorites) {
                ordered.clear()
                ordered.addAll(favorites)
            }
        }

        val visible = ordered.filter { it.matches(query) }
        // Під час пошуку видно не весь список, тож індекси перетягування не збігалися б із
        // реальним порядком. Міняти місцями дозволяємо тільки на повному списку.
        val canReorder = query.isBlank()

        val listState = rememberLazyListState()
        val reorder = rememberReorderState(
            listState = listState,
            onMove = { from, to -> ordered.add(to, ordered.removeAt(from)) },
            onSettled = { onReorder(ordered.map { it.id }) }
        )

        when {
            favorites.isEmpty() -> ScreenMessage(
                title = stringResource(R.string.favorites_empty_title),
                subtitle = stringResource(R.string.favorites_empty_subtitle)
            )

            visible.isEmpty() -> ScreenMessage(
                title = stringResource(R.string.nothing_found_title),
                subtitle = stringResource(R.string.nothing_found_subtitle)
            )

            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(visible, key = { _, route -> route.id }) { index, route ->
                    FavoriteRouteCard(
                        route = route,
                        now = state.now,
                        onClick = { onOpenRoute(route.id) },
                        modifier = reorder
                            .itemModifier(index, animate = Modifier.animateItem())
                            .then(
                                if (canReorder) {
                                    Modifier.reorderable(
                                        state = reorder,
                                        key = route.id,
                                        indexOf = { id -> ordered.indexOfFirst { it.id == id } }
                                    )
                                } else {
                                    Modifier
                                }
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoriteRouteCard(
    route: Route,
    now: LocalDateTime,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Text(
            text = route.title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(
                start = CARD_INSET,
                end = CARD_INSET,
                top = 18.dp,
                bottom = 14.dp
            )
        )

        DirectionColumns(
            directions = route.directions,
            modifier = Modifier.padding(start = CARD_INSET, end = CARD_INSET, bottom = 18.dp)
        ) { direction ->
            DirectionDepartures(direction = direction, now = now)
        }
    }
}

@Composable
private fun ColumnScope.DirectionDepartures(
    direction: Direction,
    now: LocalDateTime
) {
    val dayType = DepartureCalculator.dayTypeOf(now.toLocalDate())
    // Розбір рядків розкладу від часу не залежить, тож тік годинника його не чіпає.
    val times = remember(direction, dayType) { DepartureCalculator.timesOf(direction.origin, dayType) }
    val upcoming = DepartureCalculator.departures(times, now)
        .filterNot { it.hasLeft }
        .take(DEPARTURES_PER_DIRECTION)

    when {
        times.isEmpty() -> DirectionMessage(stringResource(R.string.direction_not_running_today))

        upcoming.isEmpty() -> DirectionMessage(stringResource(R.string.direction_no_more_today))

        else -> {
            val next = upcoming.first()
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(
                        text = next.time.formatted(),
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = countdownText(next.secondsUntil),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            upcoming.drop(1).forEach { departure ->
                Text(
                    text = departure.time.formatted(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun DirectionMessage(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
