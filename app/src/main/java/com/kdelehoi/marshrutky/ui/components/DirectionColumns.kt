package com.kdelehoi.marshrutky.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kdelehoi.marshrutky.domain.model.Direction

/**
 * Два напрямки поруч: лівий стовпчик — перший напрямок з файлу маршруту, правий — другий.
 * Проміжні зупинки транзитних маршрутів сюди не влізають, тож у заголовку стоїть початок
 * напрямку, а весь список лишається на екрані маршруту й на вкладці «Найближчі».
 *
 * Бічних відступів стовпчики не мають — їх задає той, хто викликає, щоб текст напрямків
 * стояв рівно під заголовком картки. Проміжок між колонками тримає роздільник.
 */
@Composable
fun DirectionColumns(
    directions: List<Direction>,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.(Direction) -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        directions.forEachIndexed { index, direction ->
            if (index > 0) {
                VerticalDivider(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = direction.origin.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    // Два рядки завжди: так час у сусідніх стовпчиках лишається на одній лінії.
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                content(direction)
            }
        }
    }
}
