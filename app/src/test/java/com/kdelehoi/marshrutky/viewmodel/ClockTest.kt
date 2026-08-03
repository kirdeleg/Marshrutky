package com.kdelehoi.marshrutky.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class ClockTest {

    @Test
    fun `wakes up exactly on the next minute`() {
        assertEquals(39_500L, millisUntilNextMinute(at(9, 35, 20, millis = 500)))
        assertEquals(1_000L, millisUntilNextMinute(at(9, 35, 59)))
    }

    @Test
    fun `a whole minute passes when we are already on the boundary`() {
        // Інакше тікер закрутився б на місці, прокидаючись без паузи.
        assertEquals(60_000L, millisUntilNextMinute(at(9, 35, 0)))
    }

    @Test
    fun `the last minute of the day leads into midnight`() {
        assertEquals(30_000L, millisUntilNextMinute(at(23, 59, 30)))
    }

    private fun at(hour: Int, minute: Int, second: Int, millis: Int = 0): LocalDateTime =
        LocalDateTime.of(2026, 8, 3, hour, minute, second, millis * 1_000_000)
}
