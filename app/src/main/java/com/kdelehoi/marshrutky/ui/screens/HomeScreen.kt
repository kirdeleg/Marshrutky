package com.kdelehoi.marshrutky.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kdelehoi.marshrutky.R
import com.kdelehoi.marshrutky.ui.components.DepartureRow
import com.kdelehoi.marshrutky.ui.components.DirectionCard
import com.kdelehoi.marshrutky.ui.components.NextDepartureCard
import com.kdelehoi.marshrutky.ui.components.RouteSelector
import com.kdelehoi.marshrutky.viewmodel.ScheduleUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: ScheduleUiState,
    onSelectRoute: (String) -> Unit,
    onSwapDirection: () -> Unit,
    onOpenFullSchedule: () -> Unit
) {
    val route = state.selectedRoute
    val direction = state.selectedDirection
    val departures = state.upcoming

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(route?.title ?: stringResource(R.string.app_name)) },
                actions = {
                    if (direction != null) {
                        IconButton(onClick = onOpenFullSchedule) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = stringResource(R.string.full_schedule)
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            state.isLoading -> LoadingContent(Modifier.padding(innerPadding))

            state.loadFailed || route == null || direction == null -> MessageContent(
                title = stringResource(R.string.load_failed_title),
                subtitle = stringResource(R.string.load_failed_subtitle),
                modifier = Modifier.padding(innerPadding)
            )

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    RouteSelector(
                        routes = state.routes,
                        selectedRouteId = route.id,
                        onSelect = onSelectRoute
                    )
                }

                item {
                    DirectionCard(
                        direction = direction,
                        canSwap = route.directions.size > 1,
                        onSwap = onSwapDirection,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                val next = departures.firstOrNull()
                if (next == null) {
                    item {
                        MessageContent(
                            title = stringResource(R.string.no_departures_title),
                            subtitle = stringResource(R.string.no_departures_subtitle)
                        )
                    }
                } else {
                    item {
                        NextDepartureCard(
                            departure = next,
                            today = state.now.toLocalDate(),
                            travelTimeMinutes = direction.travelTimeMinutes,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    val rest = departures.drop(1)
                    if (rest.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.next_departures),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 20.dp, top = 8.dp)
                            )
                        }

                        itemsIndexed(rest) { index, departure ->
                            if (index > 0) {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
                            }
                            DepartureRow(
                                departure = departure,
                                today = state.now.toLocalDate()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun MessageContent(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
