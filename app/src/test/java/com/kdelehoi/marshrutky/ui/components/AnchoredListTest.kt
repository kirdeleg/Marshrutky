package com.kdelehoi.marshrutky.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Порожнє місце під днем. Виглядає як дрібниця, а це найдорожча арифметика в застосунку: рівно тут
 * двічі бракувало кількох десятків точок, і щоразу через це зверху визирала картка поїханого рейсу.
 */
class AnchoredListTest {

    private val viewport = 1_800
    private val row = 200
    private val spacing = 24

    @Test
    fun `a full day needs no room at the bottom`() {
        val filler = fillerPx(viewport, row, spacing, rowsBelowHome = 40)

        assertEquals(0, filler)
    }

    @Test
    fun `the last trip of the day still reaches the top`() {
        val filler = fillerPx(viewport, row, spacing, rowsBelowHome = 0)

        assertEquals("під самотньою карткою мусить лишитися цілий екран", viewport, filler)
    }

    @Test
    fun `a couple of trips left still let home reach the top`() {
        for (left in 0..8) {
            val filler = fillerPx(viewport, row, spacing, rowsBelowHome = left)
            val below = left * (row + spacing) + filler

            // Саме ця нерівність і є весь сенс порожнього місця: нижче дому мусить бути щонайменше
            // екран, інакше список упреться в кінець стрічки й потягне дім із верхнього краю вниз.
            assertTrue("рейсів попереду: $left", below >= viewport)
        }
    }

    @Test
    fun `the divider is the reserve, not a row`() {
        val filler = fillerPx(viewport, row, spacing, rowsBelowHome = 2)

        // Роздільник нижчий за рядок, тож у рахунок він не входить — його висота і є запас. Порахувати
        // його повним рядком означало б занизити порожнє місце рівно на різницю, і день не доїхав би
        // до краю саме на ту смужку, яку видно на екрані.
        assertEquals(viewport - 2 * (row + spacing), filler)
    }
}

/**
 * Кнопка повернення обіцяє стрілкою напрямок, і збрехати нею гірше, ніж не показати нічого: саме це
 * й сталося, коли напрямок вважали завжди однаковим — «ми ж у минулому».
 */
class HomeDirectionTest {

    @Test
    fun `home out of sight upwards means the way home leads up`() {
        // Так виглядає гортання вперед по дню: найближчий рейс лишився вище екрана.
        assertTrue(HomePosition.AboveScreen.isAbove)
    }

    @Test
    fun `home out of sight downwards means the way home leads down`() {
        // А так — заїзд у минуле: найближчий рейс лишився нижче.
        assertFalse(HomePosition.BelowScreen.isAbove)
    }

    @Test
    fun `home caught by its top edge counts as already passed`() {
        assertTrue(HomePosition.Visible(gap = -1f).isAbove)
    }

    @Test
    fun `home below the top edge is still ahead`() {
        assertFalse(HomePosition.Visible(gap = 0f).isAbove)
        assertFalse(HomePosition.Visible(gap = 300f).isAbove)
    }
}
