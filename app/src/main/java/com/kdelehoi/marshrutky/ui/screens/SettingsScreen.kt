package com.kdelehoi.marshrutky.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kdelehoi.marshrutky.R
import com.kdelehoi.marshrutky.domain.model.ThemeMode
import com.kdelehoi.marshrutky.ui.components.DropdownField
import com.kdelehoi.marshrutky.viewmodel.SyncStatus
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    syncStatus: SyncStatus,
    lastSyncedAt: Instant?,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onRefresh: () -> Unit
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            DropdownField(
                label = stringResource(R.string.settings_theme),
                selected = themeMode,
                options = ThemeMode.entries,
                optionLabel = { stringResource(themeModeLabelRes(it)) },
                onSelect = onSelectThemeMode
            )

            DataSection(
                syncStatus = syncStatus,
                lastSyncedAt = lastSyncedAt,
                onRefresh = onRefresh
            )
        }
    }
}

@Composable
private fun DataSection(
    syncStatus: SyncStatus,
    lastSyncedAt: Instant?,
    onRefresh: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_data_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = syncSummary(syncStatus, lastSyncedAt),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FilledTonalButton(
                onClick = onRefresh,
                enabled = syncStatus != SyncStatus.IN_PROGRESS,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(stringResource(R.string.settings_refresh))
            }
        }
    }
}

@Composable
private fun syncSummary(syncStatus: SyncStatus, lastSyncedAt: Instant?): String = when {
    syncStatus == SyncStatus.IN_PROGRESS -> stringResource(R.string.settings_data_syncing)
    syncStatus == SyncStatus.FAILED -> stringResource(R.string.settings_data_failed)
    lastSyncedAt == null -> stringResource(R.string.settings_data_never)
    else -> stringResource(R.string.settings_data_synced, SYNCED_AT_FORMAT.format(lastSyncedAt))
}

private val SYNCED_AT_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMMM, HH:mm", Locale.forLanguageTag("uk"))
        .withZone(ZoneId.systemDefault())

private fun themeModeLabelRes(mode: ThemeMode): Int = when (mode) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}
