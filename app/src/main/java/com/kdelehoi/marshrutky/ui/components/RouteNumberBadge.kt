package com.kdelehoi.marshrutky.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Розмір значка на картках; у щільніших рядках «Найближчих» він менший. */
val ROUTE_BADGE_SIZE = 56.dp
val ROUTE_BADGE_SIZE_SMALL = 44.dp

/**
 * Номер маршруту в дев'ятикутнику — єдиний спосіб показати номер у застосунку. Приміські рейси
 * часто ходять без оприлюдненого номера, і тоді значка немає взагалі: місце під нього тримає той,
 * хто вирівнює по ньому сусідні рядки.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RouteNumberBadge(
    number: String,
    modifier: Modifier = Modifier,
    size: Dp = ROUTE_BADGE_SIZE,
    style: TextStyle = MaterialTheme.typography.labelLarge
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(MaterialShapes.Cookie9Sided.toShape())
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number,
            style = style,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
