package com.kdelehoi.marshrutky.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

private const val NBSP = "\u00A0"

class FormatTest {

    @Test
    fun `spaces inside parentheses become unbreakable`() {
        assertEquals(
            "Харків (Холодна${NBSP}Гора)",
            "Харків (Холодна Гора)".wrapBeforeParentheses()
        )
        assertEquals(
            "Високий (Залізнична${NBSP}станція)",
            "Високий (Залізнична станція)".wrapBeforeParentheses()
        )
    }

    @Test
    fun `names without parentheses are left alone`() {
        assertEquals("Нова Водолага", "Нова Водолага".wrapBeforeParentheses())
        assertEquals("Санаторій «Роща»", "Санаторій «Роща»".wrapBeforeParentheses())
    }

    @Test
    fun `text after the closing parenthesis wraps as usual`() {
        assertEquals(
            "Мерефа (вул.${NBSP}Конституції) біля ринку",
            "Мерефа (вул. Конституції) біля ринку".wrapBeforeParentheses()
        )
    }

    @Test
    fun `stray closing parenthesis does not make the rest unbreakable`() {
        assertEquals("Мерефа) вул. Конституції", "Мерефа) вул. Конституції".wrapBeforeParentheses())
    }
}
