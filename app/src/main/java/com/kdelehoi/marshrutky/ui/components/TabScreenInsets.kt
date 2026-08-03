package com.kdelehoi.marshrutky.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable

/**
 * Системні відступи для екрана, який живе вкладкою всередині пейджера. Низ там тримає господар
 * з панеллю вкладок, тож екранові лишається сам тільки верх.
 *
 * Сказано явно, а не через `consumeWindowInsets` у господаря: типовий `Scaffold` бере відступи
 * знизу сам, і достатньо клавіатурі один раз змінити їх, щоб домовленість про «низ уже спожитий»
 * зламалася — над панеллю вкладок зависає порожня смуга, у яку список не доїжджає.
 */
val TabScreenInsets: WindowInsets
    @Composable get() = WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
