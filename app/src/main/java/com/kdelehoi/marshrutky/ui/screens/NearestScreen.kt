package com.kdelehoi.marshrutky.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kdelehoi.marshrutky.R
import com.kdelehoi.marshrutky.domain.DepartureCalculator
import com.kdelehoi.marshrutky.domain.model.Route
import com.kdelehoi.marshrutky.domain.model.StopDeparture
import com.kdelehoi.marshrutky.ui.components.DepartureRow
import com.kdelehoi.marshrutky.ui.components.DropdownField
import com.kdelehoi.marshrutky.ui.components.ScreenLoading
import com.kdelehoi.marshrutky.ui.components.ScreenMessage
import com.kdelehoi.marshrutky.ui.components.TabScreenInsets
import com.kdelehoi.marshrutky.ui.components.TripState
import com.kdelehoi.marshrutky.ui.components.fillerPx
import com.kdelehoi.marshrutky.ui.components.measuredRowPx
import com.kdelehoi.marshrutky.ui.components.rememberAnchoredListState
import com.kdelehoi.marshrutky.ui.components.rememberPullResistance
import com.kdelehoi.marshrutky.viewmodel.RoutesState
import com.kdelehoi.marshrutky.viewmodel.ScheduleUiState
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/**
 * Рейси однієї зупинки, зібрані по всіх маршрутах. Потрібна там, де через одне місце проходить
 * багато маршрутів: від Смачних історій щодня відправляється більш ніж три десятки, і перебирати
 * їхні картки по одній, щоб знайти найближчий, — марна робота.
 */
@Composable
fun NearestScreen(
    state: ScheduleUiState,
    now: LocalDateTime,
    onSelectStop: (String) -> Unit,
    onOpenRoute: (String) -> Unit
) {
    Scaffold(contentWindowInsets = TabScreenInsets) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            when (val routes = state.routes) {
                RoutesState.Loading -> ScreenLoading()

                RoutesState.Empty -> ScreenMessage(
                    title = stringResource(R.string.routes_empty_title),
                    subtitle = stringResource(R.string.routes_empty_subtitle)
                )

                is RoutesState.Ready -> StopDepartures(
                    routes = routes.routes,
                    selectedStop = state.selectedStop,
                    now = now,
                    onSelectStop = onSelectStop,
                    onOpenRoute = onOpenRoute
                )
            }
        }
    }
}

/** Вибір зупинки й день від неї. Зупинок може не бути взагалі — тоді нема з чого й вибирати. */
@Composable
private fun ColumnScope.StopDepartures(
    routes: List<Route>,
    selectedStop: String?,
    now: LocalDateTime,
    onSelectStop: (String) -> Unit,
    onOpenRoute: (String) -> Unit
) {
    // Розбір розкладу не залежить від поточного моменту, тож тримаємо його в remember: тік
    // годинника й гортання вкладок мають коштувати лише перерахунку відліку.
    val stopNames = remember(routes) { DepartureCalculator.stopNames(routes) }
    // Зупинка могла зникнути з розкладів між запусками, тоді вибір скидається.
    val selected = selectedStop?.takeIf { it in stopNames }

    if (stopNames.isEmpty()) {
        ScreenMessage(
            title = stringResource(R.string.routes_empty_title),
            subtitle = stringResource(R.string.routes_empty_subtitle)
        )
        return
    }

    DropdownField(
        label = stringResource(R.string.nearest_stop_label),
        selected = selected,
        options = stopNames,
        optionLabel = { it },
        onSelect = onSelectStop,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )

    if (selected == null) {
        ScreenMessage(
            title = stringResource(R.string.nearest_no_stop_title),
            subtitle = stringResource(R.string.nearest_no_stop_subtitle)
        )
        return
    }

    val today = DepartureCalculator.dayTypeOf(now.toLocalDate())
    val routeTimes = remember(routes, selected, today) {
        DepartureCalculator.routeTimesFrom(routes, selected, today)
    }

    if (routeTimes.isEmpty()) {
        ScreenMessage(
            title = stringResource(R.string.nearest_none_title),
            subtitle = stringResource(R.string.nearest_none_subtitle)
        )
        return
    }

    DepartureDayList(
        departures = DepartureCalculator.departuresOf(routeTimes, now),
        stop = selected,
        onOpenRoute = onOpenRoute
    )
}

/**
 * Увесь сьогоднішній день однією стрічкою. Ті рейси, що вже поїхали, зі списку не викидаються:
 * прибігши на зупинку, найчастіше хочеш знати не котра година, а чи маршрутка вже була. Вони
 * лежать вище найближчого рейсу, і список одразу відкривається на ньому.
 */
@Composable
private fun DepartureDayList(
    departures: List<StopDeparture>,
    stop: String,
    onOpenRoute: (String) -> Unit
) {
    val past = departures.takeWhile { it.departure.hasLeft }
    val upcoming = departures.drop(past.size)

    // Дім стрічки — роздільник перед найближчим рейсом, а коли попереду вже нічого, то картка про
    // це. Зі зміною зупинки стрічка починається з дому заново.
    val anchored = rememberAnchoredListState(resetKey = stop, home = past.size)
    val listState = anchored.listState
    val scope = rememberCoroutineScope()

    val haptics = LocalHapticFeedback.current
    // Стрічка стала на найближчий рейс — подія одна, хоч жестом, хоч кнопкою, тож і відгук один.
    val detent = { haptics.performHapticFeedback(HapticFeedbackType.LongPress) }
    val resistance = rememberPullResistance(
        anchored = anchored,
        threshold = PULL_THRESHOLD,
        maxStretch = PULL_MAX,
        onDetent = detent
    )

    val density = LocalDensity.current
    val spacingPx = with(density) { ROW_SPACING.roundToPx() }
    val rowPx by remember { derivedStateOf { listState.measuredRowPx(ROW_TYPE) } }
    // Кнопка повернення потрібна, лише поки найближчий рейс справді пішов з екрана.
    val isInPast by remember(anchored) { derivedStateOf { !anchored.isAtHome } }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().then(resistance.modifier)) {
        // Висоту екрана беремо звідси, а не з розкладки списку. Інакше перша розкладка ще не знає про
        // порожнє місце під днем, устигає впертися в кінець стрічки — і найближчий рейс лишається не
        // на своєму місці, бо сам список назад не поїде.
        val filler = when {
            past.isEmpty() -> 0
            else -> fillerPx(
                viewportPx = with(density) { maxHeight.roundToPx() },
                rowPx = if (rowPx > 0) rowPx else with(density) { ROW_ESTIMATE.roundToPx() },
                spacingPx = spacingPx,
                rowsBelowHome = upcoming.size
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(ROW_SPACING)
        ) {
            items(past, key = { it.key }, contentType = { ROW_TYPE }) { item ->
                DepartureRow(
                    item = item,
                    state = TripState.PAST,
                    onClick = { onOpenRoute(item.route.id) },
                    modifier = Modifier.animateItem()
                )
            }

            // Межа між тим, що поїхало, і тим, що буде. Заразом єдина підказка, що вище щось є:
            // без неї верхній край виглядає як початок списку.
            if (past.isNotEmpty() && upcoming.isNotEmpty()) {
                item(key = EARLIER_KEY) {
                    EarlierHeader(
                        onClick = { scope.launch { anchored.scrollUpOneScreen() } },
                        modifier = Modifier.animateItem()
                    )
                }
            }

            itemsIndexed(
                items = upcoming,
                key = { _, item -> item.key },
                contentType = { _, _ -> ROW_TYPE }
            ) { index, item ->
                DepartureRow(
                    item = item,
                    state = if (index == 0) TripState.NEXT else TripState.UPCOMING,
                    onClick = { onOpenRoute(item.route.id) },
                    // Коли маршрутка від'їжджає, вона переїжджає за роздільник — анімація робить
                    // цей обмін плавним замість ривка.
                    modifier = Modifier.animateItem()
                )
            }

            if (upcoming.isEmpty()) {
                item(key = NO_MORE_KEY) {
                    ScreenMessage(
                        title = stringResource(R.string.direction_no_more_today),
                        subtitle = stringResource(R.string.nearest_no_more_subtitle)
                    )
                }
            }

            if (filler > 0) {
                item(key = FILLER_KEY) {
                    Spacer(modifier = Modifier.height(with(density) { filler.toDp() }))
                }
            }
        }

        AnimatedVisibility(
            visible = isInPast,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        ) {
            FilledTonalButton(
                // Клац на приході, а не на натисканні: відгук означає «стрічка стала», як і в жесті.
                onClick = {
                    scope.launch {
                        anchored.goHome()
                        detent()
                    }
                },
                elevation = ButtonDefaults.filledTonalButtonElevation(defaultElevation = 3.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.nearest_jump_to_next))
            }
        }
    }
}

@Composable
private fun EarlierHeader(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 20.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.KeyboardArrowUp,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.nearest_earlier_today),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private const val EARLIER_KEY = "earlier-today"
private const val FILLER_KEY = "filler"
private const val NO_MORE_KEY = "no-more-today"

/** За цим типом знаходимо рядок рейсу серед видимих елементів, щоб виміряти його висоту. */
private const val ROW_TYPE = "departure-row"

private val ROW_SPACING = 8.dp

/** Поки рядок не виміряно, порожнє місце під днем рахуємо з оцінки — рядки в стрічці однакові. */
private val ROW_ESTIMATE = 68.dp

/** Скільки треба витягнути пальцем, щоб список лишився в минулому, а не відскочив назад. */
private val PULL_THRESHOLD = 112.dp

/** Далі гумка не тягнеться: третина картки, хоч веди палець через увесь екран. */
private val PULL_MAX = 64.dp
