package com.kdelehoi.marshrutky.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Де зараз картка-дім відносно верхнього краю екрана.
 *
 * Окремий тип, а не число: «дому не видно» і «дім за півпікселя від краю» — різні речі, і поки вони
 * жили в одному Float зі сторожовими значеннями, кожен читач тлумачив їх по-своєму.
 */
sealed interface HomePosition {

    /** [gap] — відступ від верхнього краю: додатний, коли ми в минулому, від'ємний — коли за домом. */
    data class Visible(val gap: Float) : HomePosition

    /** Дім вище екрана: ми пішли далеко вперед по дню. */
    data object AboveScreen : HomePosition

    /** Дім нижче екрана: ми глибоко в минулому. */
    data object BelowScreen : HomePosition
}

/**
 * Чи лежить дім вище екрана. Питання не риторичне: стрічку можна залишити в обидва боки — вгору в
 * минуле й уперед по дню, — і дорога додому щоразу веде в інший бік.
 */
internal val HomePosition.isAbove: Boolean
    get() = when (this) {
        HomePosition.AboveScreen -> true
        HomePosition.BelowScreen -> false
        // Дім ще видно, але його верх уже за краєм екрана — отже, ми його проїхали.
        is HomePosition.Visible -> gap < 0
    }

/** Те, що гумці треба знати про стрічку: де дім і як стрічку посунути. */
internal interface AnchoredScroll {

    /** Чи є взагалі минуле. Якщо ні, верхній край звичайний, і жодної межі там немає. */
    val hasPast: Boolean

    val homePosition: HomePosition

    /** Посунути стрічку просто зараз, обійшовши чергу прокрутки. */
    fun nudge(delta: Float)

    /** Довезти стрічку на [distance] пружиною — це видимий відскок, а не переміщення. */
    suspend fun settle(distance: Float)

    /** Поставити дім першим рядком, хай там де стрічка зараз. */
    suspend fun snapToHome()
}

/**
 * Стрічка, у якої є дім — картка [homeIndex]. Усе, що вище, показане, але дійти туди можна лише
 * свідомо: прокрутка спиняється на домі з обох боків.
 *
 * Тримає це один об'єкт навмисне. Раніше «як далеко ми від дому» рахували незалежно екран для
 * кнопки повернення, гумка для клацання і розрахунок порожнього місця — і кожен наш баг на цій
 * вкладці був розбіжністю між цими лічильниками на десяток точок.
 */
class AnchoredListState internal constructor(
    val listState: LazyListState,
    private val homeIndex: () -> Int,
    private val homeSlopPx: Float
) : AnchoredScroll {

    /** Питаємо щоразу, а не запам'ятовуємо: рейси від'їжджають, і дім протягом дня переїжджає. */
    val home: Int
        get() = homeIndex()

    override val hasPast: Boolean
        get() = home > 0

    override val homePosition: HomePosition
        get() {
            val info = listState.layoutInfo
            val index = home
            val item = info.visibleItemsInfo.firstOrNull { it.index == index }
            return when {
                item != null -> HomePosition.Visible((item.offset - info.viewportStartOffset).toFloat())
                listState.firstVisibleItemIndex > index -> HomePosition.AboveScreen
                else -> HomePosition.BelowScreen
            }
        }

    /** Дім стоїть першим рядком. Кілька точок залишку — це ще дім, а не заїзд у минуле. */
    val isAtHome: Boolean
        get() = (homePosition as? HomePosition.Visible)?.gap?.let { it <= homeSlopPx } ?: false

    /** В який бік поїде стрічка, якщо попросити її додому. */
    val isHomeAbove: Boolean
        get() = homePosition.isAbove

    override fun nudge(delta: Float) {
        listState.dispatchRawDelta(delta)
    }

    override suspend fun settle(distance: Float) {
        listState.animateScrollBy(distance, snapBack)
    }

    override suspend fun snapToHome() {
        listState.animateScrollToItem(home)
    }

    /**
     * Повернення додому здалеку. Анімувати сотню карток немає сенсу — це змазана стрічка, тож
     * стрибаємо миттєво і анімуємо лише останні кілька, як і при зміні вкладок.
     */
    suspend fun goHome() {
        val target = home
        if (target - listState.firstVisibleItemIndex > NEAR_ITEMS) {
            listState.scrollToItem(target - NEAR_ITEMS)
        }
        listState.animateScrollToItem(target)
    }

    /**
     * Показати те, що було перед цим, а не початок дня: питання майже завжди «а що поїхало щойно».
     * Трохи екрана лишаємо на місці, щоб не втратити орієнтир.
     */
    suspend fun scrollUpOneScreen() {
        listState.animateScrollBy(-listState.layoutInfo.viewportSize.height * SCREEN_STEP)
    }

    private companion object {
        /** Відскок мусить бути видно, тож пружину лишаємо недогашеною. */
        val snapBack = spring<Float>(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium)

        const val NEAR_ITEMS = 3
        const val SCREEN_STEP = 0.85f
    }
}

/**
 * Стрічка з домом на картці [home]. Позицію рахуємо один раз на [resetKey], і навмисне звичайним
 * remember, а не збереженим станом: вкладку пейджер викидає, тож повернення до неї починається з
 * дому, а не з того місця, де людина колись копалася в ранкових рейсах.
 */
@Composable
fun rememberAnchoredListState(
    resetKey: Any?,
    home: Int,
    homeSlop: Dp = HOME_SLOP
): AnchoredListState {
    val homeSlopPx = with(LocalDensity.current) { homeSlop.toPx() }
    val currentHome = rememberUpdatedState(home)

    return remember(resetKey, homeSlopPx) {
        AnchoredListState(
            listState = LazyListState(firstVisibleItemIndex = currentHome.value),
            homeIndex = { currentHome.value },
            homeSlopPx = homeSlopPx
        )
    }
}

/**
 * Порожнє місце під днем, щоб дім міг стати першим рядком навіть тоді, коли попереду лишилося два
 * рейси. Без нього список упирається в кінець стрічки й тримає дім десь посередині екрана, а зверху
 * при цьому визирає те, що вже поїхало.
 *
 * Роздільник у [rowsBelowHome] не рахуємо: він нижчий за рядок, тож його висота і є наш запас.
 * Помилятися тут можна лише в бік зайвого місця — його ніхто не побачить, поки не доскролить день
 * до кінця, а нестача навіть на десяток точок одразу виштовхує поїханий рейс на екран.
 */
internal fun fillerPx(viewportPx: Int, rowPx: Int, spacingPx: Int, rowsBelowHome: Int): Int =
    (viewportPx - rowsBelowHome * (rowPx + spacingPx)).coerceAtLeast(0)

/**
 * Виміряна висота рядка; нуль — розкладки ще не було. Міряємо саме рядки за [contentType], бо серед
 * видимих елементів є і роздільник, і саме порожнє місце, і вони дали б іншу висоту.
 */
fun LazyListState.measuredRowPx(contentType: Any): Int =
    layoutInfo.visibleItemsInfo
        .filter { it.contentType == contentType }
        .maxOfOrNull { it.size }
        ?: 0

/** Ближче за це до дому — це ще дім на своєму місці, а не заїзд у минуле. */
val HOME_SLOP = 32.dp
