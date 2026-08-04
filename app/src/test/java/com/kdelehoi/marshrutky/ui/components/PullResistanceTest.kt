package com.kdelehoi.marshrutky.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Межа очима жесту. Стрічку тут підмінено, бо цікаво не те, як їде список, а які рішення ухвалює
 * гумка: саме в них жили обидва баги — і клац без ривка, і намертво заклякла прокрутка.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PullResistanceTest {

    private val threshold = 112f
    private val max = 64f
    private val homeSlop = 32f

    /** Стрічка, яка просто відповідає, де дім, і запам'ятовує, куди її просили поїхати. */
    private class FakeScroll(
        override var homePosition: HomePosition,
        override var hasPast: Boolean = true
    ) : AnchoredScroll {
        var nudged = 0f
        var settled = 0f
        var snappedHome = false

        override fun nudge(delta: Float) {
            nudged += delta
        }

        override suspend fun settle(distance: Float) {
            settled += distance
        }

        override suspend fun snapToHome() {
            snappedHome = true
        }
    }

    private var detents = 0

    private fun resistance(scroll: FakeScroll) = PullResistance(
        anchored = scroll,
        thresholdPx = threshold,
        maxPx = max,
        jitterPx = 2f,
        roundingPx = 2f,
        homeSlopPx = homeSlop,
        // Ривок доганяння тут не потрібен: анімація вимагає годинника кадрів, якого в тесті немає,
        // тож черга лишається нерозібраною.
        scope = TestScope(),
        onDetent = { detents++ }
    )

    private fun PullResistance.scroll(delta: Float): Float =
        onPreScroll(Offset(0f, delta), NestedScrollSource.UserInput).y

    @Test
    fun `without a past there is no boundary at all`() {
        val scroll = FakeScroll(HomePosition.Visible(0f), hasPast = false)

        val consumed = resistance(scroll).scroll(100f)

        assertEquals(0f, consumed, 0.01f)
        assertEquals(0, detents)
    }

    @Test
    fun `coming back up the day the list stops on home`() {
        // Дім вище краю на 50 точок: ми пішли вперед по дню й вертаємося.
        val scroll = FakeScroll(HomePosition.Visible(-50f))

        val consumed = resistance(scroll).scroll(100f)

        // Гумці віддали рівно шлях до дому, решта лишилася стрічці.
        assertEquals(50f, consumed, 0.01f)
        assertEquals("клац на приході, і лише один", 1, detents)
    }

    @Test
    fun `after landing the rest of the gesture stays put`() {
        val scroll = FakeScroll(HomePosition.Visible(-50f))
        val resistance = resistance(scroll)
        resistance.scroll(100f)
        scroll.homePosition = HomePosition.Visible(0f)

        val consumed = resistance.scroll(100f)

        // За дім — лише наступним жестом, тож цей рух гумка забирає весь.
        assertEquals(100f, consumed, 0.01f)
        assertEquals("другого клацу бути не має", 1, detents)
    }

    @Test
    fun `changing direction after landing frees the list`() {
        val scroll = FakeScroll(HomePosition.Visible(-50f))
        val resistance = resistance(scroll)
        resistance.scroll(100f)
        scroll.homePosition = HomePosition.Visible(0f)

        val consumed = resistance.scroll(-100f)

        // Повести палець назад — це нова думка, а не продовження попередньої: інакше стрічка стоїть
        // намертво, поки палець на екрані, і це читається як зламана прокрутка.
        assertEquals(0f, consumed, 0.01f)
    }

    @Test
    fun `a new touch releases a landed gesture`() {
        val scroll = FakeScroll(HomePosition.Visible(-50f))
        val resistance = resistance(scroll)
        resistance.scroll(100f)
        scroll.homePosition = HomePosition.Visible(0f)

        resistance.forgetGesture()
        val consumed = resistance.scroll(10f)

        // Саме тут ламалася прокрутка: фаза жила довше за свій жест, і стрічка не рухалася зовсім.
        assertTrue("новий жест мусить тягнути гумку, а не стояти", consumed < 10f)
    }

    @Test
    fun `at home the band takes only a part of the drag`() {
        val scroll = FakeScroll(HomePosition.Visible(0f))

        val consumed = resistance(scroll).scroll(20f)

        assertTrue("щось мусить дістатися стрічці", consumed < 20f)
        assertTrue("але не все", consumed > 0f)
    }

    @Test
    fun `crossing the threshold tears the band and lets the gesture go`() {
        val scroll = FakeScroll(HomePosition.Visible(0f))
        val resistance = resistance(scroll)

        repeat(20) { resistance.scroll(20f) }

        assertEquals("клац на розриві", 1, detents)
        // Далі жест іде як звичайна прокрутка: гумка більше не забирає нічого.
        assertEquals(0f, resistance.scroll(20f), 0.01f)
    }

    @Test
    fun `deep in the past the pull is free`() {
        val scroll = FakeScroll(HomePosition.BelowScreen)

        assertEquals(0f, resistance(scroll).scroll(100f), 0.01f)
        assertEquals(0, detents)
    }

    @Test
    fun `far ahead in the day the pull is free`() {
        val scroll = FakeScroll(HomePosition.AboveScreen)

        assertEquals(0f, resistance(scroll).scroll(100f), 0.01f)
        assertEquals(0, detents)
    }

    @Test
    fun `returning from the past stops on home too`() {
        // Дім на 40 точок нижче краю: ми в минулому й рушили назад до найближчого рейсу.
        val scroll = FakeScroll(HomePosition.Visible(40f))

        val consumed = resistance(scroll).scroll(-100f)

        assertEquals(-60f, consumed, 0.01f)
        assertEquals(1, detents)
    }

    @Test
    fun `standing at home the day scrolls on`() {
        val scroll = FakeScroll(HomePosition.Visible(0f))

        val consumed = resistance(scroll).scroll(-100f)

        // Уперед по дню межі немає: дім спиняє тільки зворотний рух.
        assertEquals(0f, consumed, 0.01f)
        assertEquals(0, detents)
    }

    @Test
    fun `an unfinished pull springs back`() = runTest {
        val scroll = FakeScroll(HomePosition.Visible(0f))
        val resistance = resistance(scroll)
        resistance.scroll(20f)

        val left = resistance.onPreFling(Velocity(0f, 400f))

        assertTrue("пружина мусить стягнути гумку назад", scroll.settled > 0f)
        assertTrue("а швидкість — не поїхати туди, куди не пустили", left.y > 0f)
    }

    @Test
    fun `a leftover a few points off home is snapped away`() = runTest {
        // Жест міг передумати на півдорозі й лишити стрічку майже на домі. Майже — це той самий баг:
        // гумка вже не чіпляється, зверху визирає смужка поїханого рейсу, кнопка світиться нізащо.
        val scroll = FakeScroll(HomePosition.Visible(homeSlop / 2))

        resistance(scroll).onPreFling(Velocity.Zero)

        assertEquals(homeSlop / 2, scroll.settled, 0.01f)
    }

    @Test
    fun `a leftover deep in the past is left alone`() = runTest {
        val scroll = FakeScroll(HomePosition.Visible(homeSlop * 10))

        resistance(scroll).onPreFling(Velocity.Zero)

        assertEquals("це вже свідомий заїзд у минуле", 0f, scroll.settled, 0.01f)
    }

    @Test
    fun `a fling that overshoots home is brought back`() = runTest {
        val scroll = FakeScroll(HomePosition.Visible(-50f))
        val resistance = resistance(scroll)
        resistance.scroll(100f)
        // Різкий флік бачить дім лише за кадр до нього й може його перескочити.
        scroll.homePosition = HomePosition.AboveScreen

        resistance.onPostFling(Velocity(0f, 800f), Velocity.Zero)

        assertTrue("стрічку доводимо на місце самі", scroll.snappedHome)
    }
}
