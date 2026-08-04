package com.kdelehoi.marshrutky.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kdelehoi.marshrutky.R
import com.kdelehoi.marshrutky.domain.DepartureCalculator
import com.kdelehoi.marshrutky.domain.model.StopDeparture
import com.kdelehoi.marshrutky.ui.components.DropdownField
import com.kdelehoi.marshrutky.ui.components.ROUTE_BADGE_SIZE_SMALL
import com.kdelehoi.marshrutky.ui.components.RouteNumberBadge
import com.kdelehoi.marshrutky.ui.components.ScreenLoading
import com.kdelehoi.marshrutky.ui.components.ScreenMessage
import com.kdelehoi.marshrutky.ui.components.TabScreenInsets
import com.kdelehoi.marshrutky.ui.components.agoText
import com.kdelehoi.marshrutky.ui.components.countdownText
import com.kdelehoi.marshrutky.ui.components.formatted
import com.kdelehoi.marshrutky.ui.components.rememberPullResistance
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
            // Розбір розкладу не залежить від поточного моменту, тож тримаємо його в remember:
            // тік годинника й гортання вкладок мають коштувати лише перерахунку відліку.
            val stopNames = remember(state.routes) { DepartureCalculator.stopNames(state.routes) }
            // Зупинка могла зникнути з розкладів між запусками, тоді вибір скидається.
            val selected = state.selectedStop?.takeIf { it in stopNames }

            if (state.isLoading) {
                ScreenLoading()
                return@Column
            }

            if (stopNames.isEmpty()) {
                ScreenMessage(
                    title = stringResource(R.string.routes_empty_title),
                    subtitle = stringResource(R.string.routes_empty_subtitle)
                )
                return@Column
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
                return@Column
            }

            val today = DepartureCalculator.dayTypeOf(now.toLocalDate())
            val routeTimes = remember(state.routes, selected, today) {
                DepartureCalculator.routeTimesFrom(state.routes, selected, today)
            }

            if (routeTimes.isEmpty()) {
                ScreenMessage(
                    title = stringResource(R.string.nearest_none_title),
                    subtitle = stringResource(R.string.nearest_none_subtitle)
                )
                return@Column
            }

            DepartureList(
                departures = DepartureCalculator.departuresOf(routeTimes, now),
                stop = selected,
                onOpenRoute = onOpenRoute
            )
        }
    }
}

/**
 * Увесь сьогоднішній день однією стрічкою. Ті рейси, що вже поїхали, зі списку не викидаються:
 * прибігши на зупинку, найчастіше хочеш знати не котра година, а чи маршрутка вже була. Вони
 * лежать вище найближчого рейсу, і список одразу відкривається на ньому.
 */
@Composable
private fun DepartureList(
    departures: List<StopDeparture>,
    stop: String,
    onOpenRoute: (String) -> Unit
) {
    val past = departures.takeWhile { it.departure.hasLeft }
    val upcoming = departures.drop(past.size)

    // Позицію рахуємо один раз на зупинку, і навмисне звичайним remember, а не через збережений
    // стан: вкладку пейджер викидає, тож повернення до неї починається з найближчого рейсу, а не з
    // того місця, де людина колись копалася в ранкових. Протягом дня стрічку теж нікуди не веземо —
    // якщо о 15:15 маршрутка поїде, поки її роздивляються, список не має вискочити з-під пальця.
    val listState = remember(stop) { LazyListState(firstVisibleItemIndex = past.size) }
    val scope = rememberCoroutineScope()

    val haptics = LocalHapticFeedback.current
    // Стрічка стала на найближчий рейс — подія одна, хоч жестом, хоч кнопкою, тож і відгук один.
    val detent = { haptics.performHapticFeedback(HapticFeedbackType.LongPress) }
    val resistance = rememberPullResistance(
        listState = listState,
        threshold = PULL_THRESHOLD,
        maxStretch = PULL_MAX,
        // Межа рухається протягом дня, тож питаємо її щоразу, а не запам'ятовуємо при створенні.
        boundary = { past.size },
        onDetent = detent
    )

    val density = LocalDensity.current
    val spacingPx = with(density) { ROW_SPACING.roundToPx() }
    val slopPx = with(density) { PAST_SLOP.roundToPx() }
    val rowPx by remember { derivedStateOf { listState.measuredRow() } }

    // Кнопка повернення потрібна, лише поки найближчий рейс справді пішов з екрана. Кілька точок
    // залишку — це ще не минуле, інакше кнопка блимає під час витягування гумки.
    val isInPast by remember(past.size, slopPx) {
        derivedStateOf { listState.pastDepth(past.size) > slopPx }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().then(resistance.modifier)) {
        // Висоту екрана беремо звідси, а не з розкладки списку. Інакше перша розкладка ще не знає про
        // порожнє місце під днем, устигає впертися в кінець стрічки — і найближчий рейс лишається не
        // на своєму місці, бо сам список назад не поїде.
        // Смужку «Раніше сьогодні» в рахунок навмисне не беремо: вона нижча за рейс, тож її висота і
        // є наш запас. Помилятися тут можна лише в бік зайвого місця — його ніхто не побачить, поки не
        // доскролить день до кінця, а от нестача навіть на десяток точок одразу виштовхує поїханий
        // рейс на екран.
        val filler = when {
            past.isEmpty() -> 0
            else -> {
                val row = if (rowPx > 0) rowPx else with(density) { ROW_ESTIMATE.roundToPx() }
                val viewport = with(density) { maxHeight.roundToPx() }
                (viewport - upcoming.size * (row + spacingPx)).coerceAtLeast(0)
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(ROW_SPACING)
        ) {
            items(past, key = { it.key }) { item ->
                DepartureRow(
                    item = item,
                    style = DepartureRowStyle.PAST,
                    onClick = { onOpenRoute(item.route.id) },
                    modifier = Modifier.animateItem()
                )
            }

            // Межа між тим, що поїхало, і тим, що буде. Заразом єдина підказка, що вище щось є:
            // без неї верхній край виглядає як початок списку.
            if (past.isNotEmpty() && upcoming.isNotEmpty()) {
                item(key = EARLIER_KEY) {
                    EarlierHeader(
                        onClick = { scope.launch { listState.scrollUpOneScreen() } },
                        modifier = Modifier.animateItem()
                    )
                }
            }

            itemsIndexed(upcoming, key = { _, item -> item.key }) { index, item ->
                DepartureRow(
                    item = item,
                    style = if (index == 0) DepartureRowStyle.NEXT else DepartureRowStyle.UPCOMING,
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
                        listState.jumpToItem(past.size)
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

private enum class DepartureRowStyle {
    /** Найближчий рейс. */
    NEXT,

    /** Рейс попереду. */
    UPCOMING,

    /** Маршрутка вже поїхала. */
    PAST
}

@Composable
private fun DepartureRow(
    item: StopDeparture,
    style: DepartureRowStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = when (style) {
        DepartureRowStyle.NEXT -> MaterialTheme.colorScheme.surfaceContainerHighest
        DepartureRowStyle.UPCOMING -> MaterialTheme.colorScheme.surfaceContainerHigh
        DepartureRowStyle.PAST -> MaterialTheme.colorScheme.surfaceContainerLow
    }
    // Ті самі значення, що в чипів розкладу маршруту: минуле скрізь у застосунку гасне однаково.
    val contentColor = if (style == DepartureRowStyle.PAST) {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = PAST_ALPHA)
    } else {
        contentColorFor(containerColor)
    }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Маршрути без номера місце під значок усе одно займають, інакше кінцеві в сусідніх
            // рядках роз'їхалися б по різних вертикалях.
            val badgeModifier = if (style == DepartureRowStyle.PAST) {
                // Значок кольорів картки не успадковує, тож гасимо його окремо.
                Modifier.alpha(PAST_ALPHA)
            } else {
                Modifier
            }
            if (item.route.number != null) {
                RouteNumberBadge(
                    number = item.route.number,
                    modifier = badgeModifier,
                    size = ROUTE_BADGE_SIZE_SMALL,
                    style = MaterialTheme.typography.labelMedium
                )
            } else {
                Spacer(modifier = Modifier.size(ROUTE_BADGE_SIZE_SMALL))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.destination,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (style == DepartureRowStyle.PAST) {
                        agoText(item.departure.secondsUntil)
                    } else {
                        countdownText(item.departure.secondsUntil)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = when (style) {
                        DepartureRowStyle.NEXT -> MaterialTheme.colorScheme.primary
                        DepartureRowStyle.UPCOMING -> MaterialTheme.colorScheme.onSurfaceVariant
                        // Успадковує пригашений колір картки.
                        DepartureRowStyle.PAST -> Color.Unspecified
                    }
                )
            }

            // Час — головне в рядку, тож він найбільший. У найближчого рейсу він ще й залитий:
            // разом із фоном картки це позначка «оце наступний». Ширина стала в обох станах, щоб
            // права кромка часу лишалася рівною.
            Box(
                modifier = Modifier.size(width = TIME_WIDTH, height = TIME_HEIGHT),
                contentAlignment = Alignment.Center
            ) {
                if (style == DepartureRowStyle.NEXT) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            TimeText(item.departure.time.formatted())
                        }
                    }
                } else {
                    TimeText(item.departure.time.formatted())
                }
            }
        }
    }
}

@Composable
private fun TimeText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
    )
}

/**
 * Тап по «Раніше сьогодні» показує рейси, що були перед цим, а не початок дня: питання майже
 * завжди «а що поїхало щойно». Трохи екрана лишаємо на місці, щоб не втратити орієнтир.
 */
private suspend fun LazyListState.scrollUpOneScreen() {
    animateScrollBy(-layoutInfo.viewportSize.height * SCREEN_STEP)
}

/**
 * Повернення до найближчого рейсу. Анімувати сотню карток немає сенсу — це змазана стрічка, тож
 * здалеку стрибаємо миттєво і анімуємо лише останні кілька, як і при зміні вкладок.
 */
private suspend fun LazyListState.jumpToItem(index: Int) {
    if (index - firstVisibleItemIndex > NEAR_ITEMS) {
        scrollToItem(index - NEAR_ITEMS)
    }
    animateScrollToItem(index)
}

/** Виміряна висота рядка; нуль — розкладки ще не було. Усі рядки однакові, тож досить будь-якого. */
private fun LazyListState.measuredRow(): Int =
    layoutInfo.visibleItemsInfo
        .filterNot { it.key == FILLER_KEY }
        .maxOfOrNull { it.size }
        ?: 0

/** Наскільки стрічка заїхала в минуле; нуль — найближчий рейс стоїть першим рядком. */
private fun LazyListState.pastDepth(boundary: Int): Int {
    val info = layoutInfo
    val item = info.visibleItemsInfo.firstOrNull { it.index == boundary } ?: return Int.MAX_VALUE
    return (item.offset - info.viewportStartOffset).coerceAtLeast(0)
}

private const val EARLIER_KEY = "earlier-today"
private const val FILLER_KEY = "filler"
private const val NO_MORE_KEY = "no-more-today"
private const val PAST_ALPHA = 0.5f
private const val SCREEN_STEP = 0.85f
private const val NEAR_ITEMS = 3
private val ROW_SPACING = 8.dp

/** Поки рядок не виміряно, порожнє місце під днем рахуємо з оцінки — рядки в стрічці однакові. */
private val ROW_ESTIMATE = 68.dp

/** Ближче за це до межі — це ще найближчий рейс на своєму місці, а не заїзд у минуле. */
private val PAST_SLOP = 32.dp

/** Скільки треба витягнути пальцем, щоб список лишився в минулому, а не відскочив назад. */
private val PULL_THRESHOLD = 112.dp

/** Далі гумка не тягнеться: третина картки, хоч веди палець через увесь екран. */
private val PULL_MAX = 64.dp
private val TIME_WIDTH = 80.dp
private val TIME_HEIGHT = 44.dp
