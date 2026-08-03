package com.kdelehoi.marshrutky.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kdelehoi.marshrutky.R
import com.kdelehoi.marshrutky.domain.model.StopDeparture
import com.kdelehoi.marshrutky.ui.components.ScreenMessage
import com.kdelehoi.marshrutky.ui.components.countdownText
import com.kdelehoi.marshrutky.ui.components.formatted
import com.kdelehoi.marshrutky.viewmodel.ScheduleUiState

/**
 * Рейси однієї зупинки, зібрані по всіх маршрутах. Потрібна там, де через одне місце проходить
 * багато маршрутів: із Холодної Гори щодня відправляється більш ніж десяток, і перебирати їхні
 * картки по одній, щоб знайти найближчий, — марна робота.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearestScreen(
    state: ScheduleUiState,
    onSelectStop: (String) -> Unit,
    onOpenRoute: (String) -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_nearest)) }) }
    ) { innerPadding ->
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

            StopPicker(
                stopNames = stopNames,
                selected = selected,
                onSelectStop = onSelectStop,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StopPicker(
    stopNames: List<String>,
    selected: String?,
    onSelectStop: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected.orEmpty(),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(stringResource(R.string.nearest_stop_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            stopNames.forEach { name ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onSelectStop(name)
                        expanded = false
                    },
                    trailingIcon = if (name == selected) {
                        { Icon(Icons.Default.Check, contentDescription = null) }
                    } else {
                        null
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column {
                Text(
                    text = item.departure.time.formatted(),
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = countdownText(item.departure.secondsUntil),
                    style = MaterialTheme.typography.labelMedium,
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
