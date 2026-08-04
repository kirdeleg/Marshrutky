package com.kdelehoi.marshrutky.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PullResistanceTest {

    private val max = 64f
    private val threshold = 112f

    private fun state() = PullResistanceState(maxPx = max, thresholdPx = threshold)

    @Test
    fun `list gets only a part of the drag`() {
        val state = state()

        val consumed = state.drag(10f)

        // Забране гумкою плюс віддане списку — це вся дельта, інакше жест «губить» рух.
        assertEquals(10f, consumed + state.stretch, 0.01f)
    }

    @Test
    fun `the first pixels follow the finger`() {
        val state = state()

        state.drag(2f)

        // Гумка мусить бути прив'язана до пальця з першої ж точки, інакше жест читається як затримка.
        // Але й одразу туго — саме на початку вона й відчувається.
        val expected = 2f * PullResistanceState.START_FOLLOW
        assertEquals(expected, state.stretch, expected / 5f)
    }

    @Test
    fun `resistance grows the further you pull`() {
        val state = state()

        val near = state.drag(10f)
        repeat(20) { state.drag(10f) }
        val far = state.drag(10f)

        assertTrue("глибше тягнути має бути важче", far > near)
    }

    @Test
    fun `the stretch has a ceiling`() {
        val state = state()

        repeat(200) { state.drag(20f) }

        assertTrue("гумка не мусить рватися", state.stretch < max)
        assertTrue("але й майже сягати стелі", state.stretch > max * 0.9f)
    }

    @Test
    fun `a short pull stays under the threshold`() {
        val state = state()

        state.drag(threshold / 2)

        assertFalse(state.overThreshold)
        assertTrue("є що повертати пружиною", state.stretch > 0f)
    }

    @Test
    fun `a long pull reaches the threshold`() {
        val state = state()

        state.drag(threshold + 1f)

        assertTrue(state.overThreshold)
    }

    @Test
    fun `the lag is what the list has to catch up`() {
        val state = state()

        state.drag(threshold)

        // На порозі гумка рветься, і стрічка доганяє палець рівно на це відставання — саме воно й дає
        // той ривок на екрані, якого раніше бракувало.
        assertEquals(state.pulled - state.stretch, state.lag, 0.01f)
        assertTrue("відставання мусить бути помітним", state.lag > threshold / 2f)
    }

    @Test
    fun `finger jitter does not wipe the pull`() {
        val state = state()
        state.drag(40f)
        val pulled = state.pulled

        state.reverse(-1f, jitterPx = 5f)

        assertEquals(pulled, state.pulled, 0.01f)
    }

    @Test
    fun `a real move back drops the pull`() {
        val state = state()
        state.drag(40f)

        state.reverse(-20f, jitterPx = 5f)

        assertEquals(0f, state.pulled, 0.01f)
        assertEquals(0f, state.stretch, 0.01f)
    }
}
