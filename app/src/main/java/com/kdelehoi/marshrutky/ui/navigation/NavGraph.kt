package com.kdelehoi.marshrutky.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.kdelehoi.marshrutky.ui.screens.FullScheduleScreen
import com.kdelehoi.marshrutky.ui.screens.HomeScreen
import com.kdelehoi.marshrutky.viewmodel.ScheduleViewModel

@Composable
fun MarshrutkyNavGraph(viewModel: ScheduleViewModel) {
    val backStack = rememberNavBackStack(Screen.Home)
    val state by viewModel.state.collectAsState()

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<Screen.Home> {
                HomeScreen(
                    state = state,
                    onSelectRoute = viewModel::selectRoute,
                    onSwapDirection = viewModel::toggleDirection,
                    onOpenFullSchedule = { backStack.add(Screen.FullSchedule) }
                )
            }

            entry<Screen.FullSchedule> {
                FullScheduleScreen(
                    state = state,
                    onBack = { backStack.removeLastOrNull() }
                )
            }
        }
    )
}
