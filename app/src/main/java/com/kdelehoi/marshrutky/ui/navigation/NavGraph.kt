package com.kdelehoi.marshrutky.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.kdelehoi.marshrutky.domain.model.ThemeMode
import com.kdelehoi.marshrutky.ui.components.TabChangeHaptics
import com.kdelehoi.marshrutky.ui.components.goToPage
import com.kdelehoi.marshrutky.ui.screens.FavoritesScreen
import com.kdelehoi.marshrutky.ui.screens.NearestScreen
import com.kdelehoi.marshrutky.ui.screens.RouteDetailScreen
import com.kdelehoi.marshrutky.ui.screens.RoutesScreen
import com.kdelehoi.marshrutky.ui.screens.SettingsScreen
import com.kdelehoi.marshrutky.viewmodel.ScheduleUiState
import com.kdelehoi.marshrutky.viewmodel.ScheduleViewModel
import com.kdelehoi.marshrutky.viewmodel.SettingsViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MarshrutkyNavGraph(
    scheduleViewModel: ScheduleViewModel,
    settingsViewModel: SettingsViewModel
) {
    val backStack = rememberNavBackStack(Screen.Main)
    val state by scheduleViewModel.state.collectAsStateWithLifecycle()
    val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()

    // Типовий перехід NavDisplay — згасання, однакове в обидва боки, тож по анімації не видно,
    // заходиш ти вглиб чи повертаєшся. Пружина з motionScheme дає той самий рух, що й решта теми.
    val slide = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
    val fade = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    val enterDeeper = slideInHorizontally(slide) { width -> width / 4 } + fadeIn(fade)
    val backToPrevious = slideOutHorizontally(slide) { width -> width / 4 } + fadeOut(fade)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        transitionSpec = { enterDeeper togetherWith fadeOut(fade) },
        popTransitionSpec = { fadeIn(fade) togetherWith backToPrevious },
        predictivePopTransitionSpec = { fadeIn(fade) togetherWith backToPrevious },
        entryProvider = entryProvider {
            entry<Screen.Main> {
                MainTabs(
                    state = state,
                    clock = scheduleViewModel.now,
                    themeMode = themeMode,
                    onOpenRoute = { routeId -> backStack.add(Screen.RouteDetail(routeId)) },
                    onToggleFavorite = scheduleViewModel::toggleFavorite,
                    onReorderFavorites = scheduleViewModel::saveFavoriteOrder,
                    onSelectThemeMode = settingsViewModel::selectThemeMode,
                    onSelectStop = scheduleViewModel::selectStop,
                    onRefresh = scheduleViewModel::refresh
                )
            }

            entry<Screen.RouteDetail> { destination ->
                val now by scheduleViewModel.now.collectAsStateWithLifecycle()

                RouteDetailScreen(
                    state = state,
                    now = now,
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
    /**
     * Годинник заходить сюди потоком, а не готовим значенням: якби час читався тут, щохвилинне
     * оновлення перемальовувало б увесь каркас із панеллю вкладок. Кожна вкладка підписується
     * сама, тож на «Маршрутах» і «Параметрах» годинник взагалі не працює.
     */
    clock: StateFlow<LocalDateTime>,
    themeMode: ThemeMode,
    onOpenRoute: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onReorderFavorites: (List<String>) -> Unit,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onSelectStop: (String) -> Unit,
    onRefresh: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { TopLevelDestination.entries.size })
    val scope = rememberCoroutineScope()

    TabChangeHaptics(pagerState)

    Scaffold(
        // Верхні відступи кожен екран розбирає власним Scaffold, тут лишається тільки нижня панель.
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            NavigationBar {
                TopLevelDestination.entries.forEachIndexed { index, destination ->
                    NavigationBarItem(
                        selected = index == pagerState.currentPage,
                        onClick = { scope.launch { pagerState.goToPage(index) } },
                        icon = { Icon(destination.icon, contentDescription = null) },
                        label = {
                            // На чотирьох вкладках задовга мітка переноситься на другий рядок
                            // і вилазить за межі панелі, тож краще обрізати.
                            Text(
                                text = stringResource(destination.labelRes),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
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
                TopLevelDestination.FAVORITES -> {
                    val now by clock.collectAsStateWithLifecycle()

                    FavoritesScreen(
                        state = state,
                        now = now,
                        onOpenRoute = onOpenRoute,
                        onReorder = onReorderFavorites
                    )
                }

                TopLevelDestination.NEAREST -> {
                    val now by clock.collectAsStateWithLifecycle()

                    NearestScreen(
                        state = state,
                        now = now,
                        onSelectStop = onSelectStop,
                        onOpenRoute = onOpenRoute
                    )
                }

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
