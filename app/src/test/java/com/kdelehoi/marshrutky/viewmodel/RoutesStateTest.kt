package com.kdelehoi.marshrutky.viewmodel

import com.kdelehoi.marshrutky.domain.model.Route
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class RoutesStateTest {

    private fun route(id: String) = Route(id = id, number = null, name = id, directions = emptyList())

    @Test
    fun `an unread cache is still loading`() {
        val state = routesState(routes = null, isStartingUp = false, syncStatus = SyncStatus.IDLE)

        assertSame(RoutesState.Loading, state)
    }

    @Test
    fun `nothing on the first run is loading, not empty`() {
        val state = routesState(routes = emptyList(), isStartingUp = true, syncStatus = SyncStatus.IDLE)

        // Саме тут колись блимало «Маршрутів немає» — рівно тій людині, яка щойно поставила застосунок.
        assertSame(RoutesState.Loading, state)
    }

    @Test
    fun `nothing while the sync is running is loading too`() {
        val state = routesState(
            routes = emptyList(),
            isStartingUp = false,
            syncStatus = SyncStatus.IN_PROGRESS
        )

        assertSame(RoutesState.Loading, state)
    }

    @Test
    fun `nothing after a finished sync really means nothing`() {
        val state = routesState(
            routes = emptyList(),
            isStartingUp = false,
            syncStatus = SyncStatus.FAILED
        )

        assertSame(RoutesState.Empty, state)
    }

    @Test
    fun `routes are ready even while the next sync runs`() {
        val state = routesState(
            routes = listOf(route("199")),
            isStartingUp = true,
            syncStatus = SyncStatus.IN_PROGRESS
        )

        // Кеш уже показуємо: оновлення у фоні не причина ховати те, що є на пристрої.
        assertEquals(RoutesState.Ready(listOf(route("199"))), state)
    }

    @Test
    fun `favorites keep the order the user chose`() {
        val ready = RoutesState.Ready(listOf(route("a"), route("b"), route("c")))

        val favorites = ready.inOrder(listOf("c", "a"))

        assertEquals(listOf("c", "a"), favorites.map { it.id })
    }

    @Test
    fun `a favorite that no longer exists is skipped`() {
        // Маршрут могли прибрати з репозиторію, а в обраному він лишився.
        val ready = RoutesState.Ready(listOf(route("a")))

        assertEquals(listOf("a"), ready.inOrder(listOf("a", "zzz")).map { it.id })
        assertNull(ready.route("zzz"))
    }
}
