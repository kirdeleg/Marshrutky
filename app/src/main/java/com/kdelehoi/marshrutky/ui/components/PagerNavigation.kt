package com.kdelehoi.marshrutky.ui.components

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.flow.drop
import kotlin.math.abs

/**
 * Перехід на вкладку через тап по її назві. Звичайний animateScrollToPage прогортає всі сторінки
 * між поточною і потрібною: з «Обраного» в «Параметри» встигають скомпонуватися й змигнути ще два
 * екрани. Тому далекий стрибок робимо миттєво до сусідньої сторінки, а анімуємо лише останній крок.
 */
suspend fun PagerState.goToPage(page: Int) {
    if (abs(page - currentPage) > 1) {
        scrollToPage(if (page > currentPage) page - 1 else page + 1)
    }
    animateScrollToPage(page)
}

/**
 * Короткий відгук на зміну вкладки. Слухаємо саме сторінку пейджера, а не натискання по назві,
 * щоб гортання пальцем відчувалося так само, як тап, і щоб на тапі не вібрувати двічі. Перше
 * значення пропускаємо — це просто відкриття екрана.
 */
@Composable
fun TabChangeHaptics(pagerState: PagerState) {
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(pagerState, haptics) {
        snapshotFlow { pagerState.currentPage }
            .drop(1)
            .collect { haptics.performHapticFeedback(HapticFeedbackType.SegmentTick) }
    }
}
