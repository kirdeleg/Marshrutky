package com.kdelehoi.marshrutky.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kdelehoi.marshrutky.domain.model.Route

@Composable
fun RouteSelector(
    routes: List<Route>,
    selectedRouteId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(routes, key = { it.id }) { route ->
            FilterChip(
                selected = route.id == selectedRouteId,
                onClick = { onSelect(route.id) },
                label = { Text(route.number) },
                shape = FilterChipDefaults.shape
            )
        }
    }
}
