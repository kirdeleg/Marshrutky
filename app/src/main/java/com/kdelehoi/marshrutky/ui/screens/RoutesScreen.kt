package com.kdelehoi.marshrutky.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kdelehoi.marshrutky.R
import com.kdelehoi.marshrutky.domain.model.Route
import com.kdelehoi.marshrutky.ui.components.RouteNumberBadge
import com.kdelehoi.marshrutky.ui.components.ScreenLoading
import com.kdelehoi.marshrutky.ui.components.ScreenMessage
import com.kdelehoi.marshrutky.ui.components.SearchableScaffold
import com.kdelehoi.marshrutky.viewmodel.RoutesState
import com.kdelehoi.marshrutky.viewmodel.ScheduleUiState

@Composable
fun RoutesScreen(
    state: ScheduleUiState,
    onOpenRoute: (String) -> Unit,
    onToggleFavorite: (String) -> Unit
) {
    SearchableScaffold { query ->
        when (val routes = state.routes) {
            RoutesState.Loading -> ScreenLoading()

            RoutesState.Empty -> ScreenMessage(
                title = stringResource(R.string.routes_empty_title),
                subtitle = stringResource(R.string.routes_empty_subtitle)
            )

            is RoutesState.Ready -> {
                val visible = remember(routes, query) { routes.routes.filter { it.matches(query) } }

                if (visible.isEmpty()) {
                    ScreenMessage(
                        title = stringResource(R.string.nothing_found_title),
                        subtitle = stringResource(R.string.nothing_found_subtitle)
                    )
                    return@SearchableScaffold
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(visible, key = { it.id }) { route ->
                        RouteCard(
                            route = route,
                            isFavorite = route.id in state.favoriteRouteIds,
                            onClick = { exitSearchAnd { onOpenRoute(route.id) } },
                            onToggleFavorite = { onToggleFavorite(route.id) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RouteCard(
    route: Route,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            route.number?.let { RouteNumberBadge(it) }

            Text(
                text = route.name,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            FilledTonalIconToggleButton(
                checked = isFavorite,
                onCheckedChange = { onToggleFavorite() }
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                    contentDescription = stringResource(
                        if (isFavorite) R.string.favorite_remove else R.string.favorite_add
                    )
                )
            }
        }
    }
}
