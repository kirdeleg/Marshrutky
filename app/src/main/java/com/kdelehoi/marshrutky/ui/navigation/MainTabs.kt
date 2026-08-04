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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kdelehoi.marshrutky.domain.model.AppLanguage
import com.kdelehoi.marshrutky.domain.model.ThemeMode
import com.kdelehoi.marshrutky.ui.components.TabChangeHaptics
import com.kdelehoi.marshrutky.ui.components.goToPage
import com.kdelehoi.marshrutky.ui.screens.FavoritesScreen
import com.kdelehoi.marshrutky.ui.screens.NearestScreen
import com.kdelehoi.marshrutky.ui.screens.RoutesScreen
import com.kdelehoi.marshrutky.ui.screens.SettingsScreen
import com.kdelehoi.marshrutky.viewmodel.AppearanceState
import com.kdelehoi.marshrutky.viewmodel.ScheduleUiState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/**
 * Усе, що вкладки можуть попросити зробити. Одним набором, бо в підписі [MainTabs] кожна дія — це
 * ще один параметр, який доводиться нести крізь каркас, хоча потрібен він одній вкладці.
 *
 * [Immutable] — обіцянка, що набір не змінюється; разом із `remember` на місці створення це дозволяє
 * Compose не перемальовувати вкладки, коли змінилося щось, що їх не стосується.
 */
@Immutable
class MainTabsActions(
    val openRoute: (String) -> Unit,
    val toggleFavorite: (String) -> Unit,
    val reorderFavorites: (List<String>) -> Unit,
    val selectStop: (String) -> Unit,
    val refresh: () -> Unit,
    val selectThemeMode: (ThemeMode) -> Unit,
    val selectLanguage: (AppLanguage) -> Unit
)

@Composable
fun MainTabs(
    state: ScheduleUiState,
    /**
     * Годинник заходить сюди потоком, а не готовим значенням: якби час читався тут, щохвилинне
     * оновлення перемальовувало б увесь каркас із панеллю вкладок. Кожна вкладка підписується
     * сама, тож на «Маршрутах» і «Параметрах» годинник взагалі не працює.
     */
    clock: StateFlow<LocalDateTime>,
    appearance: AppearanceState,
    actions: MainTabsActions
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
            // Місце під панель вкладок віднімаємо тут, а самі вкладки нижніх системних відступів
            // не беруть — див. TabScreenInsets.
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
        ) { page ->
            when (TopLevelDestination.entries[page]) {
                TopLevelDestination.FAVORITES -> {
                    val now by clock.collectAsStateWithLifecycle()

                    FavoritesScreen(
                        state = state,
                        now = now,
                        onOpenRoute = actions.openRoute,
                        onReorder = actions.reorderFavorites
                    )
                }

                TopLevelDestination.NEAREST -> {
                    val now by clock.collectAsStateWithLifecycle()

                    NearestScreen(
                        state = state,
                        now = now,
                        onSelectStop = actions.selectStop,
                        onOpenRoute = actions.openRoute
                    )
                }

                TopLevelDestination.ROUTES -> RoutesScreen(
                    state = state,
                    onOpenRoute = actions.openRoute,
                    onToggleFavorite = actions.toggleFavorite
                )

                TopLevelDestination.SETTINGS -> SettingsScreen(
                    appearance = appearance,
                    syncStatus = state.syncStatus,
                    lastSyncedAt = state.lastSyncedAt,
                    onSelectThemeMode = actions.selectThemeMode,
                    onSelectLanguage = actions.selectLanguage,
                    onRefresh = actions.refresh
                )
            }
        }
    }
}
