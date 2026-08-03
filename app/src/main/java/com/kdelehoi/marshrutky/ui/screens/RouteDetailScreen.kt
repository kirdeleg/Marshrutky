package com.kdelehoi.marshrutky.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kdelehoi.marshrutky.R
import com.kdelehoi.marshrutky.domain.DepartureCalculator
import com.kdelehoi.marshrutky.domain.model.BoardingStop
import com.kdelehoi.marshrutky.domain.model.DayType
import com.kdelehoi.marshrutky.ui.components.ScreenMessage
import com.kdelehoi.marshrutky.ui.components.TabChangeHaptics
import com.kdelehoi.marshrutky.ui.components.goToPage
import com.kdelehoi.marshrutky.ui.components.TimeChip
import com.kdelehoi.marshrutky.ui.components.TimeChipStyle
import com.kdelehoi.marshrutky.viewmodel.ScheduleUiState
import kotlinx.coroutines.launch
import java.time.LocalDateTime

private enum class ScheduleTab(val dayType: DayType?) {
    TODAY(null),
    WEEKDAY(DayType.WEEKDAY),
    SATURDAY(DayType.SATURDAY),
    SUNDAY(DayType.SUNDAY)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteDetailScreen(
    state: ScheduleUiState,
    routeId: String,
    onToggleFavorite: (String) -> Unit,
    onBack: () -> Unit
) {
    val route = state.routeById(routeId)
    val isFavorite = routeId in state.favoriteRouteIds
    val pagerState = rememberPagerState(pageCount = { ScheduleTab.entries.size })
    val scope = rememberCoroutineScope()

    TabChangeHaptics(pagerState)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = route?.title ?: stringResource(R.string.app_name),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onToggleFavorite(routeId) }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                            contentDescription = stringResource(
                                if (isFavorite) R.string.favorite_remove else R.string.favorite_add
                            ),
                            tint = if (isFavorite) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (route == null) {
            ScreenMessage(
                title = stringResource(R.string.route_missing_title),
                subtitle = stringResource(R.string.route_missing_subtitle),
                modifier = Modifier.padding(innerPadding)
            )
            return@Scaffold
        }

        Column(modifier = Modifier.padding(innerPadding)) {
            PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
                ScheduleTab.entries.forEachIndexed { index, tab ->
                    Tab(
                        selected = index == pagerState.currentPage,
                        onClick = { scope.launch { pagerState.goToPage(index) } },
                        text = { Text(stringResource(tabLabelRes(tab))) }
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val tab = ScheduleTab.entries[page]

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    route.directions.forEachIndexed { index, direction ->
                        // Транзитний маршрут дає кілька секцій поспіль на один напрямок,
                        // тож без риски між напрямками вони зливаються в суцільний стовпчик.
                        if (index > 0) {
                            HorizontalDivider()
                        }

                        direction.boardingStops.forEach { stop ->
                            StopSection(
                                stop = stop,
                                dayType = tab.dayType,
                                now = state.now
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StopSection(
    stop: BoardingStop,
    dayType: DayType?,
    now: LocalDateTime
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = stop.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        if (dayType == null) {
            TodayChips(stop = stop, now = now)
        } else {
            DayChips(stop = stop, dayType = dayType)
        }
    }
}

@Composable
private fun TodayChips(
    stop: BoardingStop,
    now: LocalDateTime
) {
    val dayType = DepartureCalculator.dayTypeOf(now.toLocalDate())
    val times = remember(stop, dayType) { DepartureCalculator.timesOf(stop, dayType) }
    val departures = DepartureCalculator.departures(times, now)
    if (departures.isEmpty()) {
        SectionMessage(stringResource(R.string.direction_not_running_today))
        return
    }

    val next = departures.firstOrNull { !it.hasLeft }
    if (next == null) {
        SectionMessage(stringResource(R.string.direction_no_more_today))
    }

    ChipGrid {
        departures.forEach { departure ->
            TimeChip(
                time = departure.time,
                style = when {
                    departure == next -> TimeChipStyle.NEXT
                    departure.hasLeft -> TimeChipStyle.PAST
                    else -> TimeChipStyle.UPCOMING
                },
                secondsUntil = departure.secondsUntil
            )
        }
    }
}

@Composable
private fun DayChips(
    stop: BoardingStop,
    dayType: DayType
) {
    val times = remember(stop, dayType) { DepartureCalculator.timesOf(stop, dayType) }
    if (times.isEmpty()) {
        SectionMessage(stringResource(R.string.direction_not_running))
        return
    }

    ChipGrid {
        times.forEach { time ->
            TimeChip(time = time, style = TimeChipStyle.UPCOMING)
        }
    }
}

@Composable
private fun ChipGrid(content: @Composable FlowRowScope.() -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}

@Composable
private fun SectionMessage(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

private fun tabLabelRes(tab: ScheduleTab): Int = when (tab) {
    ScheduleTab.TODAY -> R.string.tab_today
    ScheduleTab.WEEKDAY -> R.string.day_weekday
    ScheduleTab.SATURDAY -> R.string.day_saturday
    ScheduleTab.SUNDAY -> R.string.day_sunday
}
