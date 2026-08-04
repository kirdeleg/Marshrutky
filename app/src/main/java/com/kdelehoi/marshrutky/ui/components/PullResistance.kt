package com.kdelehoi.marshrutky.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Гумка на межі списку: перші точки стрічка йде рівно за пальцем, далі кожна наступна дається все
 * важче, а [maxPx] вона не перетне, хоч тягни через увесь екран.
 *
 * Опір мусить бути видимий, а не відчутний як затримка. Якщо стрічку тільки пригальмувати, жест
 * читається як «свайп, що підвис»: рух є, він просто пізніше. Гумка читається інакше, бо контент
 * прив'язаний до пальця, відстає від нього все більше й має стелю, у яку впирається.
 */
internal class PullResistanceState(private val maxPx: Float, private val thresholdPx: Float) {

    /** Шлях пальця за межу. */
    var pulled = 0f
        private set

    /** Наскільки розтягнута гумка — тобто скільки минулого вже видно. Це ж і вертаємо пружиною. */
    var stretch = 0f
        private set

    /** Дотягнули до порогу — гумці час рватися. */
    val overThreshold: Boolean
        get() = pulled >= thresholdPx

    /** Наскільно стрічка відстала від пальця: рівно це вона й доганяє, коли гумка лусне. */
    val lag: Float
        get() = pulled - stretch

    /**
     * Забирає свою частину дельти й повертає її. Решта дістається списку — рівно стільки, скільки
     * бракує гумці до нової довжини.
     */
    fun drag(delta: Float): Float {
        pulled += delta

        // Крива з асимптотою maxPx, але з власною початковою жорсткістю: [START_FOLLOW] задає, як
        // туго йде стрічка з перших же точок, а maxPx — де вона стане остаточно. Без цього множника
        // одне число керувало б і тим, і тим: висока стеля сама собою робить гумку слабкою.
        val eased = pulled * START_FOLLOW
        val target = maxPx * eased / (eased + maxPx)
        val toList = target - stretch
        stretch = target

        return delta - toList
    }

    /**
     * Рух у зворотний бік. Дрібні ривки — це тремтіння пальця, а не зміна напрямку, тож облік вони
     * не зачіпають; на справжньому русі назад ми просто перестаємо заважати.
     */
    fun reverse(delta: Float, jitterPx: Float) {
        if (-delta > jitterPx) reset()
    }

    fun reset() {
        pulled = 0f
        stretch = 0f
    }

    internal companion object {
        /** Частка руху пальця, яку стрічка бере на самому початку. Далі — тільно менше. */
        const val START_FOLLOW = 0.45f
    }
}

/**
 * Межа перед карткою [boundary]: картка стає домівкою списку, і потрапити за неї можна лише гумкою.
 *
 * Прокрутка спиняється на цій картці з обох боків — і коли повертаєшся з-за межі, і коли підходиш до
 * неї знизу, — разом із інерцією, тож флік не пролітає домівку, а стає на неї.
 *
 * За межу пускає лише гумка, і на [thresholdPx] вона **рветься**: клац у пальці, стрічка тим же рухом
 * доганяє палець, і далі жест іде як звичайна прокрутка. Це найважливіше в усій механіці. Поки гумка
 * тільно «зараховувала» витягування, вібрація обіцяла подію, якої на екрані не було: рейси стояли
 * там, де їх застав палець, і тіло з очима казали різне. Відпустив, не дотягнувши, — стрічка
 * відскакує пружиною на межу.
 *
 * [onDetent] — той самий клац на обидва випадки: стрічка стала на межу або гумка лусну́ла.
 *
 * Інерцію в бік минулого гумкою не чіпаємо: пригальмований флік відчувається як заїдання, а не як
 * опір.
 */
internal class PullResistance(
    private val listState: LazyListState,
    thresholdPx: Float,
    maxPx: Float,
    private val jitterPx: Float,
    private val slopPx: Float,
    private val snapPx: Float,
    private val scope: CoroutineScope,
    private val boundary: () -> Int,
    private val onDetent: () -> Unit
) : NestedScrollConnection {

    private val state = PullResistanceState(maxPx = maxPx, thresholdPx = thresholdPx)

    /** Пружина повернення на межу: відскок мусить бути видно, тож лишаємо її недогашеною. */
    private val snapBack = spring<Float>(dampingRatio = BOUNCE, stiffness = Spring.StiffnessMedium)

    /** Жест уже везе стрічку назад до межі, тож далі за неї ми його не пустимо. */
    private var returning = false
    private var landed = false

    /** Гумка цього жесту вже лусну́ла — далі ми не заважаємо. */
    private var torn = false
    private var catchUp: Job? = null

    /**
     * Разом із опором навішує скидання стану на дотик.
     *
     * Стан межі живе рівно один жест, і спиратися на його кінець не можна: скасовану інерцію Compose
     * завершує без `onPostFling`, а скасоване перетягування — і без `onPreFling`. Через це прапорці
     * переживали свій жест і глушили прокрутку в обидва боки. Дотик до екрана — єдиний сигнал початку
     * жесту, який не губиться ніколи.
     */
    val modifier: Modifier = Modifier
        .pointerInput(this) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                forget()
            }
        }
        .nestedScroll(this)

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        val index = boundary()
        // Минулого сьогодні не було — тоді верхній край списку звичайний, і спротив на ньому дає
        // система своїм stretch-overscroll.
        if (index <= 0) return Offset.Zero

        val delta = available.y
        val gap = listState.gapTo(index)

        return if (delta > 0f) pull(delta, index, source, gap) else giveBack(delta, gap)
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        val stretch = state.stretch
        state.reset()

        // Гумка лусну́ла — інерцію пускаємо далі: різкий жест має влетіти в минуле, а не спинитися
        // там, де його застало розривання.
        if (torn) return Velocity.Zero

        // Не дотягнув: гумка стягується назад, і швидкість гасимо, щоб список не поїхав туди, куди
        // його не пустили.
        if (stretch > 0f) {
            listState.animateScrollBy(stretch, snapBack)
            return available
        }

        // Жест міг лишити стрічку за кілька точок від межі — наприклад, коли посеред витягування
        // передумав і повів палець назад. Такий залишок треба прибирати, бо він тихо ламає одразу
        // все: гумка вважає, що ми вже в минулому, і не чіпляється, зверху визирає смужка поїханого
        // рейсу, а кнопка повернення світиться без причини.
        val leftover = listState.gapTo(boundary()) ?: return Velocity.Zero
        if (leftover > slopPx && leftover <= snapPx) {
            listState.animateScrollBy(leftover, snapBack)
            return available
        }

        return Velocity.Zero
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        val stopped = landed
        forget()

        // Дуже різкий флік бачить межу лише за кадр до неї й може її перескочити — тоді доводимо
        // стрічку на місце самі, це кілька десятків точок.
        val index = boundary()
        if (stopped && index > 0) {
            val gap = listState.gapTo(index)
            if (gap == null || gap > slopPx) listState.animateScrollToItem(index)
        }

        return Velocity.Zero
    }

    /** Рух у бік минулого: знизу — до межі, з самої межі — гумкою. */
    private fun pull(delta: Float, index: Int, source: NestedScrollSource, gap: Float?): Offset {
        returning = false
        if (torn) return Offset.Zero
        // Ми знову нижче межі, тож наступний підхід до неї теж отримає свій клац.
        if (gap != null && gap < -slopPx) landed = false

        if (state.pulled == 0f) {
            val room = roomToBoundary(index, gap)
            when {
                // До межі ще далеко — не заважаємо.
                room == null -> Unit
                room == FAR -> return Offset.Zero
                // Підходимо знизу: віддаємо рівно стільки, щоб картка стала врівень із краєм.
                delta > room -> {
                    click()
                    return Offset(0f, delta - room)
                }

                else -> return Offset.Zero
            }

            // Цим жестом ми вже стали на межу: за неї — лише наступним рухом, свідомо.
            if (landed) return Offset(0f, delta)
            // Глибше в минулому все вже показане, тягнути там нічого.
            if (gap == null || gap > slopPx) return Offset.Zero
        }

        if (source != NestedScrollSource.UserInput) return Offset.Zero

        val consumed = state.drag(delta)
        if (state.overThreshold) tear()

        return Offset(0f, consumed)
    }

    /**
     * Гумка лусну́ла: клац і стрічка доганяє палець.
     *
     * Догін веземо власною анімацією через `dispatchRawDelta`, бо палець тримає сесію прокрутки з
     * вищим приоритетом — звичайний `animateScrollBy` в неї просто не пустять. Сирі дельти цю чергу
     * обходять, тож ривок видно навіть тоді, коли палець спинився рівно на порозі.
     */
    private fun tear() {
        torn = true
        onDetent()

        val distance = state.lag
        state.reset()
        if (distance <= 0f) return

        catchUp?.cancel()
        catchUp = scope.launch {
            var applied = 0f
            animate(
                initialValue = 0f,
                targetValue = distance,
                animationSpec = spring(dampingRatio = CATCH_UP_DAMPING, stiffness = Spring.StiffnessMedium)
            ) { value, _ ->
                listState.dispatchRawDelta(applied - value)
                applied = value
            }
        }
    }

    /** Рух назад до найближчого рейсу — до межі й ані точки далі. */
    private fun giveBack(delta: Float, gap: Float?): Offset {
        state.reverse(delta, jitterPx)

        // Межі ще не видно: ми глибоко в минулому, спиняти нічого.
        if (gap == null) return Offset.Zero
        if (gap > slopPx) returning = true
        // Стоїмо на межі й рушили в майбутнє — це звичайна прокрутка дня.
        if (!returning) return Offset.Zero
        if (delta >= -gap) return Offset.Zero

        click()
        return Offset(0f, delta + gap)
    }

    /**
     * Скільки ще можна проїхати вгору, поки межа не стане врівень із краєм: [FAR] — вона десь далеко
     * внизу, null — ми вже не нижче за неї.
     */
    private fun roomToBoundary(index: Int, gap: Float?): Float? = when {
        gap != null -> if (gap < -slopPx) -gap else null
        // Картку не видно: або вона попереду за екраном, або ми вже в минулому.
        listState.firstVisibleItemIndex > index -> FAR
        else -> null
    }

    private fun click() {
        if (!landed) {
            landed = true
            onDetent()
        }
    }

    /** Забути все, що стосувалося попереднього жесту. */
    private fun forget() {
        returning = false
        landed = false
        torn = false
        catchUp?.cancel()
        catchUp = null
        state.reset()
    }
}

@Composable
internal fun rememberPullResistance(
    listState: LazyListState,
    threshold: Dp,
    maxStretch: Dp,
    boundary: () -> Int,
    onDetent: () -> Unit
): PullResistance {
    val density = LocalDensity.current
    val thresholdPx = with(density) { threshold.toPx() }
    val maxPx = with(density) { maxStretch.toPx() }
    val jitterPx = with(density) { JITTER.toPx() }
    val slopPx = with(density) { SLOP.toPx() }
    val snapPx = with(density) { SNAP.toPx() }
    val scope = rememberCoroutineScope()
    val detent = rememberUpdatedState(onDetent)
    val boundaryIndex = rememberUpdatedState(boundary)

    return remember(listState, thresholdPx, maxPx) {
        PullResistance(
            listState = listState,
            thresholdPx = thresholdPx,
            maxPx = maxPx,
            jitterPx = jitterPx,
            slopPx = slopPx,
            snapPx = snapPx,
            scope = scope,
            boundary = { boundaryIndex.value() },
            onDetent = { detent.value() }
        )
    }
}

/** Відступ картки [index] від верхнього краю; null — її не видно. */
private fun LazyListState.gapTo(index: Int): Float? {
    val item = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index } ?: return null
    return (item.offset - layoutInfo.viewportStartOffset).toFloat()
}

/** Межа десь за екраном: скільки саме до неї — невідомо, та й спиняти ще рано. */
private const val FAR = Float.MAX_VALUE

/** Менші ривки назад під час перетягування — це тремтіння пальця. */
private val JITTER = 2.dp

/** Допуск на округлення: після анімації можна стояти за півпікселя від межі. */
private val SLOP = 2.dp

/** Залишок до цієї відстані вважаємо недоведеним жестом і прибираємо самі. */
private val SNAP = 32.dp

/** Відскок мусить бути видно, тож пружину лишаємо недогашеною. */
private const val BOUNCE = 0.55f

/** Догін пальця — з натяком на ривок, але без коливань. */
private const val CATCH_UP_DAMPING = 0.8f
