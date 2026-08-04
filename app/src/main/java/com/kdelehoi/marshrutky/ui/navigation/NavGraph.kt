package com.kdelehoi.marshrutky.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.kdelehoi.marshrutky.ui.screens.RouteDetailScreen
import com.kdelehoi.marshrutky.viewmodel.ScheduleViewModel
import com.kdelehoi.marshrutky.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MarshrutkyNavGraph(
    scheduleViewModel: ScheduleViewModel,
    settingsViewModel: SettingsViewModel
) {
    val backStack = rememberNavBackStack(Screen.Main)
    val state by scheduleViewModel.state.collectAsStateWithLifecycle()
    val appearance by settingsViewModel.appearance.collectAsStateWithLifecycle()

    // Набір лишається тим самим об'єктом, поки живе екран: інакше вкладки отримували б нові лямбди
    // на кожну зміну стану й перемальовувалися б усі заразом.
    val actions = remember(backStack, scheduleViewModel, settingsViewModel) {
        MainTabsActions(
            openRoute = { routeId -> backStack.add(Screen.RouteDetail(routeId)) },
            toggleFavorite = scheduleViewModel::toggleFavorite,
            reorderFavorites = scheduleViewModel::saveFavoriteOrder,
            selectStop = scheduleViewModel::selectStop,
            refresh = scheduleViewModel::refresh,
            selectThemeMode = settingsViewModel::selectThemeMode,
            selectLanguage = settingsViewModel::selectLanguage
        )
    }

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
                    appearance = appearance,
                    actions = actions
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
