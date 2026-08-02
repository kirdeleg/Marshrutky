package com.kdelehoi.marshrutky.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.kdelehoi.marshrutky.domain.model.ThemeMode
import com.kdelehoi.marshrutky.ui.screens.FavoritesScreen
import com.kdelehoi.marshrutky.ui.screens.RouteDetailScreen
import com.kdelehoi.marshrutky.ui.screens.RoutesScreen
import com.kdelehoi.marshrutky.ui.screens.SettingsScreen
import com.kdelehoi.marshrutky.viewmodel.ScheduleUiState
import com.kdelehoi.marshrutky.viewmodel.ScheduleViewModel
import com.kdelehoi.marshrutky.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@Composable
fun MarshrutkyNavGraph(
    scheduleViewModel: ScheduleViewModel,
    settingsViewModel: SettingsViewModel
) {
    val backStack = rememberNavBackStack(Screen.Main)
    val state by scheduleViewModel.state.collectAsState()
    val themeMode by settingsViewModel.themeMode.collectAsState()

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<Screen.Main> {
                MainTabs(
                    state = state,
                    themeMode = themeMode,
                    onOpenRoute = { routeId -> backStack.add(Screen.RouteDetail(routeId)) },
                    onToggleFavorite = scheduleViewModel::toggleFavorite,
                    onSelectThemeMode = settingsViewModel::selectThemeMode,
                    onRefresh = scheduleViewModel::refresh
                )
            }

            entry<Screen.RouteDetail> { destination ->
                RouteDetailScreen(
                    state = state,
                    routeId = destination.routeId,
                    onToggleFavorite = scheduleViewModel::toggleFavorite,
                    onBack = { backStack.removeLastOrNull() }
                )
            }
        }
    )
}

@Composable
private fun MainTabs(
    state: ScheduleUiState,
    themeMode: ThemeMode,
    onOpenRoute: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onRefresh: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { TopLevelDestination.entries.size })
    val scope = rememberCoroutineScope()

    Scaffold(
        // Верхні відступи кожен екран розбирає власним Scaffold, тут лишається тільки нижня панель.
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            NavigationBar {
                TopLevelDestination.entries.forEachIndexed { index, destination ->
                    NavigationBarItem(
                        selected = index == pagerState.currentPage,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        icon = { Icon(destination.icon, contentDescription = null) },
                        label = { Text(stringResource(destination.labelRes)) }
                    )
                }
            }
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
        ) { page ->
            when (TopLevelDestination.entries[page]) {
                TopLevelDestination.FAVORITES -> FavoritesScreen(
                    state = state,
                    onOpenRoute = onOpenRoute
                )

                TopLevelDestination.ROUTES -> RoutesScreen(
                    state = state,
                    onOpenRoute = onOpenRoute,
                    onToggleFavorite = onToggleFavorite
                )

                TopLevelDestination.SETTINGS -> SettingsScreen(
                    themeMode = themeMode,
                    syncStatus = state.syncStatus,
                    lastSyncedAt = state.lastSyncedAt,
                    onSelectThemeMode = onSelectThemeMode,
                    onRefresh = onRefresh
                )
            }
        }
    }
}
