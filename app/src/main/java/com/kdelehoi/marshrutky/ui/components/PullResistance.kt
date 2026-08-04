package com.kdelehoi.marshrutky.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
 * Сама гумка, без стрічки: перші точки вона йде рівно за пальцем, далі кожна наступна дається все
 * важче, а [maxPx] не перетне, хоч тягни через увесь екран.
 *
 * Опір мусить бути видимий, а не відчутний як затримка. Якщо стрічку тільки пригальмувати, жест
 * читається як «свайп, що підвис»: рух є, він просто пізніше. Гумка читається інакше, бо контент
 * прив'язаний до пальця, відстає від нього все більше й має стелю, у яку впирається.
 */
internal class RubberBand(private val maxPx: Float, private val thresholdPx: Float) {

    /** Шлях пальця за межу. */
    var pulled = 0f
        private set

    /** Наскільки розтягнута гумка — тобто скільки минулого вже видно. Це ж і вертаємо пружиною. */
    var stretch = 0f
        private set

    /** Дотягнули до порогу — гумці час рватися. */
    val overThreshold: Boolean
        get() = pulled >= thresholdPx

    /** Наскільки стрічка відстала від пальця: рівно це вона й доганяє, коли гумка лусне. */
    val lag: Float
        get() = pulled - stretch

    /**
     * Забирає свою частину дельти й повертає її. Решта дістається списку — рівно стільки, скільки
     * бракує гумці до нової довжини.
     */
    fun pull(delta: Float): Float {
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
        if (-delta > jitterPx) release()
    }

    fun release() {
        pulled = 0f
        stretch = 0f
    }

    internal companion object {
        /** Частка руху пальця, яку стрічка бере на самому початку. Далі — тільки менше. */
        const val START_FOLLOW = 0.45f
    }
}

/**
 * Що з межею сталося в межах одного жесту.
 *
 * Одне значення, а не набір прапорців: станів тут п'ять, і поки їх описували три незалежні
 * булеві змінні, більшість комбінацій не означала нічого, а одна з них глушила прокрутку
 * назовсім — досить було скасованої інерції, після якої `onPostFling` не приходить.
 */
private enum class Phase {

    /** Ще нічого не сталося: жест може і спинитися на домі, і потягнути гумку. */
    FREE,

    /**
     * Стрічка стала на дім, ідучи в минуле: далі в минуле цей жест уже не поїде.
     *
     * Засув тримає лише той бік, з якого до нього прийшли. Повести палець назад — це нова думка, а
     * не продовження попередньої, тож там стрічка знову вільна: інакше після клацання жест виглядав
     * би як заклякла прокрутка, поки не відпустиш палець.
     */
    HOME_GOING_PAST,

    /** Стрічка стала на дім, вертаючись із минулого: далі в майбутнє цей жест уже не поїде. */
    HOME_GOING_FUTURE,

    /** Гумка тягнеться. */
    STRETCH,

    /** Гумка лусну́ла — до кінця жесту не заважаємо. */
    TORN,

    /** Везе стрічку з минулого назад до дому. */
    RETURN;

    val isHome: Boolean
        get() = this == HOME_GOING_PAST || this == HOME_GOING_FUTURE
}

/**
 * Межа перед домом [AnchoredScroll.homePosition]: дім — це домівка списку, і потрапити за неї можна
 * лише гумкою.
 *
 * Прокрутка спиняється на домі з обох боків — і коли повертаєшся з минулого, і коли підходиш до нього
 * з майбутнього, — разом із інерцією, тож флік не пролітає домівку, а стає на неї.
 *
 * За межу пускає лише гумка, і на [thresholdPx] вона **рветься**: клац у пальці, стрічка тим же рухом
 * доганяє палець, і далі жест іде як звичайна прокрутка. Це найважливіше в усій механіці. Поки гумка
 * тільки «зараховувала» витягування, вібрація обіцяла подію, якої на екрані не було: рейси стояли
 * там, де їх застав палець, і тіло з очима казали різне. Відпустив, не дотягнувши, — стрічка
 * відскакує пружиною на дім.
 *
 * [onDetent] — той самий клац на обидва випадки: стрічка стала на дім або гумка лусну́ла.
 *
 * Інерцію в бік минулого гумкою не чіпаємо: пригальмований флік відчувається як заїдання, а не як
 * опір.
 */
internal class PullResistance(
    private val anchored: AnchoredScroll,
    thresholdPx: Float,
    maxPx: Float,
    private val jitterPx: Float,
    private val roundingPx: Float,
    private val homeSlopPx: Float,
    private val scope: CoroutineScope,
    private val onDetent: () -> Unit
) : NestedScrollConnection {

    private val band = RubberBand(maxPx = maxPx, thresholdPx = thresholdPx)

    private var phase = Phase.FREE
    private var catchUp: Job? = null

    /**
     * Разом із опором навішує скидання стану на дотик.
     *
     * Фаза живе рівно один жест, і спиратися на його кінець не можна: скасовану інерцію Compose
     * завершує без `onPostFling`, а скасоване перетягування — і без `onPreFling`. Через це фаза
     * переживала свій жест і глушила прокрутку в обидва боки. Дотик до екрана — єдиний сигнал
     * початку жесту, який не губиться ніколи.
     */
    val modifier: Modifier = Modifier
        .pointerInput(this) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                forgetGesture()
            }
        }
        .nestedScroll(this)

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        // Минулого сьогодні не було — тоді верхній край списку звичайний, і спротив на ньому дає
        // система своїм stretch-overscroll.
        if (!anchored.hasPast) return Offset.Zero

        val delta = available.y
        return if (delta > 0f) towardPast(delta, source) else towardFuture(delta)
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        val stretch = band.stretch
        band.release()

        // Гумка лусну́ла — інерцію пускаємо далі: різкий жест має влетіти в минуле, а не спинитися
        // там, де його застало розривання.
        if (phase == Phase.TORN) return Velocity.Zero

        // Не дотягнув: гумка стягується назад, і швидкість гасимо, щоб список не поїхав туди, куди
        // його не пустили.
        if (stretch > 0f) {
            anchored.settle(stretch)
            return available
        }

        // Жест міг лишити стрічку за кілька точок від дому — наприклад, коли посеред витягування
        // передумав і повів палець назад. Такий залишок треба прибирати, бо він тихо ламає одразу
        // все: гумка вважає, що ми вже в минулому, і не чіпляється, зверху визирає смужка поїханого
        // рейсу, а кнопка повернення світиться без причини.
        val leftover = (anchored.homePosition as? HomePosition.Visible)?.gap ?: return Velocity.Zero
        if (leftover > roundingPx && leftover <= homeSlopPx) {
            anchored.settle(leftover)
            return available
        }

        return Velocity.Zero
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        val landed = phase.isHome
        forgetGesture()

        // Дуже різкий флік бачить дім лише за кадр до нього й може його перескочити — тоді доводимо
        // стрічку на місце самі, це кілька десятків точок.
        if (landed && anchored.hasPast) {
            val home = anchored.homePosition
            if (home !is HomePosition.Visible || home.gap > roundingPx) anchored.snapToHome()
        }

        return Velocity.Zero
    }

    /** Рух у бік минулого: з майбутнього — до дому, з самого дому — гумкою. */
    private fun towardPast(delta: Float, source: NestedScrollSource): Offset {
        if (phase == Phase.TORN) return Offset.Zero
        // Напрямок змінився: повернення додому вже не при чому, а засув із того боку відпускає.
        if (phase == Phase.RETURN || phase == Phase.HOME_GOING_FUTURE) phase = Phase.FREE

        val home = anchored.homePosition
        // Ми знову за домом, тож наступний підхід до нього теж отримає свій клац.
        if (phase == Phase.HOME_GOING_PAST && home is HomePosition.Visible && home.gap < -roundingPx) {
            phase = Phase.FREE
        }
        // Цим жестом дім уже спійманий — глибше в минуле стрічка не поїде.
        if (phase == Phase.HOME_GOING_PAST) return Offset(0f, delta)

        if (phase != Phase.STRETCH) {
            when (home) {
                // Дім вище екрана: ми пішли далеко по дню, спиняти ще рано.
                HomePosition.AboveScreen -> return Offset.Zero
                // Глибше в минулому все вже показане — тягнути там нічого.
                HomePosition.BelowScreen -> return Offset.Zero
                is HomePosition.Visible -> when {
                    // Підходимо з майбутнього: віддаємо рівно стільки, щоб дім став врівень із краєм.
                    home.gap < -roundingPx -> {
                        val room = -home.gap
                        if (delta <= room) return Offset.Zero
                        arriveHome(Phase.HOME_GOING_PAST)
                        return Offset(0f, delta - room)
                    }
                    // Ми вже за домом, у минулому: гумка своє відпрацювала.
                    home.gap > roundingPx -> return Offset.Zero
                }
            }
        }

        if (source != NestedScrollSource.UserInput) return Offset.Zero

        phase = Phase.STRETCH
        val consumed = band.pull(delta)
        if (band.overThreshold) tear()

        return Offset(0f, consumed)
    }

    /** Рух назад до найближчого рейсу — до дому й ані точки далі. */
    private fun towardFuture(delta: Float): Offset {
        band.reverse(delta, jitterPx)
        // Гумка відпущена, тож жест почався заново — з тим самим правом спинитися на домі.
        if (phase == Phase.STRETCH && band.stretch == 0f) phase = Phase.FREE
        // Напрямок змінився, і засув із боку минулого відпускає.
        if (phase == Phase.HOME_GOING_PAST) phase = Phase.FREE
        // Цим жестом дім уже спійманий — далі по дню стрічка не поїде.
        if (phase == Phase.HOME_GOING_FUTURE) return Offset(0f, delta)

        val home = anchored.homePosition
        // Дому не видно: або ми глибоко в минулому, або далеко по дню — спиняти нічого.
        if (home !is HomePosition.Visible) return Offset.Zero
        if (home.gap > roundingPx) phase = Phase.RETURN
        // Стоїмо на домі й рушили далі по дню — це звичайна прокрутка.
        if (phase != Phase.RETURN) return Offset.Zero
        if (delta >= -home.gap) return Offset.Zero

        arriveHome(Phase.HOME_GOING_FUTURE)
        return Offset(0f, delta + home.gap)
    }

    /** Стрічка стала на дім: клац, і далі цим жестом у той самий бік за дім не пускаємо. */
    private fun arriveHome(phase: Phase) {
        this.phase = phase
        onDetent()
    }

    /**
     * Гумка лусну́ла: клац і стрічка доганяє палець.
     *
     * Догін веземо сирими дельтами, бо палець тримає сесію прокрутки з вищим приоритетом — звичайну
     * анімовану прокрутку в неї просто не пустять. Сирі дельти цю чергу обходять, тож ривок видно
     * навіть тоді, коли палець спинився рівно на порозі.
     */
    private fun tear() {
        phase = Phase.TORN
        onDetent()

        val distance = band.lag
        band.release()
        if (distance <= 0f) return

        catchUp?.cancel()
        catchUp = scope.launch {
            var applied = 0f
            animate(
                initialValue = 0f,
                targetValue = distance,
                animationSpec = spring(dampingRatio = CATCH_UP_DAMPING, stiffness = Spring.StiffnessMedium)
            ) { value, _ ->
                anchored.nudge(applied - value)
                applied = value
            }
        }
    }

    /** Забути все, що стосувалося попереднього жесту. */
    internal fun forgetGesture() {
        phase = Phase.FREE
        catchUp?.cancel()
        catchUp = null
        band.release()
    }

    private companion object {
        /** Догін пальця — з натяком на ривок, але без коливань. */
        const val CATCH_UP_DAMPING = 0.8f
    }
}

@Composable
internal fun rememberPullResistance(
    anchored: AnchoredListState,
    threshold: Dp,
    maxStretch: Dp,
    onDetent: () -> Unit
): PullResistance {
    val density = LocalDensity.current
    val thresholdPx = with(density) { threshold.toPx() }
    val maxPx = with(density) { maxStretch.toPx() }
    val jitterPx = with(density) { JITTER.toPx() }
    val roundingPx = with(density) { ROUNDING.toPx() }
    val homeSlopPx = with(density) { HOME_SLOP.toPx() }
    val scope = rememberCoroutineScope()
    val detent = rememberUpdatedState(onDetent)

    return remember(anchored, thresholdPx, maxPx) {
        PullResistance(
            anchored = anchored,
            thresholdPx = thresholdPx,
            maxPx = maxPx,
            jitterPx = jitterPx,
            roundingPx = roundingPx,
            homeSlopPx = homeSlopPx,
            scope = scope,
            onDetent = { detent.value() }
        )
    }
}

/** Менші ривки назад під час перетягування — це тремтіння пальця. */
private val JITTER = 2.dp

/** Допуск на округлення: після анімації можна стояти за півпікселя від дому. */
private val ROUNDING = 2.dp
