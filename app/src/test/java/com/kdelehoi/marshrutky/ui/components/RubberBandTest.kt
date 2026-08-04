package com.kdelehoi.marshrutky.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RubberBandTest {

    private val max = 64f
    private val threshold = 112f

    private fun band() = RubberBand(maxPx = max, thresholdPx = threshold)

    @Test
    fun `list gets only a part of the drag`() {
        val band = band()

        val consumed = band.pull(10f)

        // Забране гумкою плюс віддане списку — це вся дельта, інакше жест «губить» рух.
        assertEquals(10f, consumed + band.stretch, 0.01f)
    }

    @Test
    fun `the first pixels follow the finger`() {
        val band = band()

        band.pull(2f)

        // Гумка мусить бути прив'язана до пальця з першої ж точки, інакше жест читається як затримка.
        // Але й одразу туго — саме на початку вона й відчувається.
        val expected = 2f * RubberBand.START_FOLLOW
        assertEquals(expected, band.stretch, expected / 5f)
    }

    @Test
    fun `resistance grows the further you pull`() {
        val band = band()

        val near = band.pull(10f)
        repeat(20) { band.pull(10f) }
        val far = band.pull(10f)

        assertTrue("глибше тягнути має бути важче", far > near)
    }

    @Test
    fun `the stretch has a ceiling`() {
        val band = band()

        repeat(200) { band.pull(20f) }

        assertTrue("гумка не мусить рватися", band.stretch < max)
        assertTrue("але й майже сягати стелі", band.stretch > max * 0.9f)
    }

    @Test
    fun `a short pull stays under the threshold`() {
        val band = band()

        band.pull(threshold / 2)

        assertFalse(band.overThreshold)
        assertTrue("є що повертати пружиною", band.stretch > 0f)
    }

    @Test
    fun `a long pull reaches the threshold`() {
        val band = band()

        band.pull(threshold + 1f)

        assertTrue(band.overThreshold)
    }

    @Test
    fun `the lag is what the list has to catch up`() {
        val band = band()

        band.pull(threshold)

        // На порозі гумка рветься, і стрічка доганяє палець рівно на це відставання — саме воно й дає
        // той ривок на екрані, якого раніше бракувало.
        assertEquals(band.pulled - band.stretch, band.lag, 0.01f)
        assertTrue("відставання мусить бути помітним", band.lag > threshold / 2f)
    }

    @Test
    fun `finger jitter does not wipe the pull`() {
        val band = band()
        band.pull(40f)
        val pulled = band.pulled

        band.reverse(-1f, jitterPx = 5f)

        assertEquals(pulled, band.pulled, 0.01f)
    }

    @Test
    fun `a real move back drops the pull`() {
        val band = band()
        band.pull(40f)

        band.reverse(-20f, jitterPx = 5f)

        assertEquals(0f, band.pulled, 0.01f)
        assertEquals(0f, band.stretch, 0.01f)
    }
}
