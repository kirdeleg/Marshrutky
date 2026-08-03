package com.kdelehoi.marshrutky.ui.components

import androidx.compose.foundation.pager.PagerState
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
