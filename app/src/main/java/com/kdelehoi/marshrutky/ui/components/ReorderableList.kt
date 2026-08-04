package com.kdelehoi.marshrutky.ui.components

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.zIndex

/**
 * Перетягування елементів у [androidx.compose.foundation.lazy.LazyColumn]: готового рішення в
 * Compose немає, а логіка невелика. Тримаємо індекс картки, яка зараз у руці, і її зсув від
 * пальця; щойно середина картки заходить на сусідню — міняємо їх місцями. Решта списку
 * розсувається сама, бо на ній висить `animateItem`.
 *
 * Перетягування починається довгим натисканням, тож звичайний тап по картці лишається кліком.
 */
class ReorderState internal constructor(
    private val listState: LazyListState,
    private val haptics: HapticFeedback,
    private val indexOf: (key: Any) -> Int,
    private val onMove: (from: Int, to: Int) -> Unit,
    private val onSettled: () -> Unit
) {
    private var draggingIndex by mutableIntStateOf(NOTHING)
    private var initialOffset = 0
    private var draggedDelta by mutableFloatStateOf(0f)

    /** Де зараз має бути намальована верхня межа картки, яку тягнуть. */
    private val draggedTop: Float
        get() = initialOffset + draggedDelta

    private fun itemAt(index: Int): LazyListItemInfo? =
        listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }

    /**
     * Модифікатор для картки списку. Ту, що в руці, зміщуємо вручну й піднімаємо над сусідами;
     * решті лишаємо звичайну анімацію переміщення.
     */
    fun itemModifier(index: Int, animate: Modifier): Modifier =
        if (index == draggingIndex) {
            Modifier
                .zIndex(1f)
                .graphicsLayer { translationY = draggedTop - (itemAt(index)?.offset ?: 0) }
        } else {
            animate
        }

    internal fun start(key: Any) {
        val index = indexOf(key)
        // Індексу немає, коли картку встигли забрати зі списку між натисканням і початком жесту.
        val item = itemAt(index) ?: return
        draggingIndex = index
        initialOffset = item.offset
        draggedDelta = 0f
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    internal fun drag(delta: Float) {
        if (draggingIndex == NOTHING) return
        draggedDelta += delta

        val dragged = itemAt(draggingIndex) ?: return
        val middle = (draggedTop + dragged.size / 2f).toInt()
        val target = listState.layoutInfo.visibleItemsInfo.firstOrNull { candidate ->
            candidate.index != dragged.index && middle in candidate.offset..(candidate.offset + candidate.size)
        } ?: return

        onMove(dragged.index, target.index)
        // Палець не рухався, тож картка лишається там само — змінився лише її індекс у списку.
        draggingIndex = target.index
        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
    }

    internal fun stop() {
        if (draggingIndex == NOTHING) return
        draggingIndex = NOTHING
        draggedDelta = 0f
        haptics.performHapticFeedback(HapticFeedbackType.GestureEnd)
        onSettled()
    }

    private companion object {
        const val NOTHING = -1
    }
}

/**
 * Що саме означає обмін місцями: картку виймаємо і вставляємо на місце сусіда, решта зсувається сама.
 * Окремою функцією, бо на індексах тут легко схибити, а так це перевіряється без екрана.
 */
fun <T> List<T>.moved(from: Int, to: Int): List<T> =
    toMutableList().apply { add(to, removeAt(from)) }

/**
 * Усі лямбди проходять через [rememberUpdatedState], бо сам [ReorderState] запам'ятовується один раз:
 * без цього він до кінця життя екрана правив би той список, який побачив на початку.
 */
@Composable
fun rememberReorderState(
    listState: LazyListState,
    indexOf: (key: Any) -> Int,
    onMove: (from: Int, to: Int) -> Unit,
    onSettled: () -> Unit
): ReorderState {
    val haptics = LocalHapticFeedback.current
    val currentIndexOf by rememberUpdatedState(indexOf)
    val currentMove by rememberUpdatedState(onMove)
    val currentSettled by rememberUpdatedState(onSettled)

    return remember(listState, haptics) {
        ReorderState(
            listState = listState,
            haptics = haptics,
            indexOf = { key -> currentIndexOf(key) },
            onMove = { from, to -> currentMove(from, to) },
            onSettled = { currentSettled() }
        )
    }
}

/**
 * Жест перетягування для картки. Прив'язуємось до [key], а не до індексу: індекс змінюється просто
 * посеред жесту, і `pointerInput` перезапустився б, обірвавши перетягування на першому ж обміні.
 *
 * Через це ж тут немає жодної лямбди, крім самого ключа: блок `pointerInput` не перезапускається, тож
 * усе, що він захопив, лишилося б таким, як на початку. За свіжим порядком ходить [state].
 */
fun Modifier.reorderable(
    state: ReorderState,
    key: Any
): Modifier = pointerInput(key) {
    detectDragGesturesAfterLongPress(
        onDragStart = { state.start(key) },
        onDrag = { change, amount ->
            change.consume()
            state.drag(amount.y)
        },
        onDragEnd = { state.stop() },
        onDragCancel = { state.stop() }
    )
}
