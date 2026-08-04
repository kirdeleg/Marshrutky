package com.kdelehoi.marshrutky.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Обмін місцями в обраному: помилка на одиницю тут виглядає як картка, що стрибнула не туди, і
 * ловиться лише пальцем. Тому арифметика перевіряється окремо від жесту.
 */
class MovedTest {

    private val list = listOf("a", "b", "c", "d")

    @Test
    fun `a card moved down takes the place of its neighbour`() {
        assertEquals(listOf("b", "a", "c", "d"), list.moved(from = 0, to = 1))
    }

    @Test
    fun `a card moved up takes the place of its neighbour`() {
        assertEquals(listOf("a", "c", "b", "d"), list.moved(from = 2, to = 1))
    }

    @Test
    fun `a card dragged to the very end lands last`() {
        assertEquals(listOf("b", "c", "d", "a"), list.moved(from = 0, to = 3))
    }

    @Test
    fun `a card dragged to the very top lands first`() {
        assertEquals(listOf("d", "a", "b", "c"), list.moved(from = 3, to = 0))
    }

    @Test
    fun `a move onto itself changes nothing`() {
        // Так виглядає жест, у якому картку потримали й відпустили на місці.
        assertEquals(list, list.moved(from = 2, to = 2))
    }

    @Test
    fun `the order stays a permutation of the same routes`() {
        assertEquals(list.toSet(), list.moved(from = 1, to = 3).toSet())
        assertEquals(list.size, list.moved(from = 1, to = 3).size)
    }
}
