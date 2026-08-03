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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kdelehoi.marshrutky.R
import com.kdelehoi.marshrutky.domain.model.AppLanguage
import com.kdelehoi.marshrutky.domain.model.ThemeMode
import com.kdelehoi.marshrutky.ui.components.DropdownField
import com.kdelehoi.marshrutky.viewmodel.AppearanceState
import com.kdelehoi.marshrutky.viewmodel.SyncStatus
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SettingsScreen(
    appearance: AppearanceState,
    syncStatus: SyncStatus,
    lastSyncedAt: Instant?,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
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
                selected = appearance.themeMode,
                options = ThemeMode.entries,
                optionLabel = { stringResource(themeModeLabelRes(it)) },
                onSelect = onSelectThemeMode
            )

            DropdownField(
                label = stringResource(R.string.settings_language),
                selected = appearance.language,
                options = AppLanguage.entries,
                optionLabel = { languageLabel(it) },
                onSelect = onSelectLanguage
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
    else -> stringResource(R.string.settings_data_synced, syncedAtFormat().format(lastSyncedAt))
}

/** Назва місяця має бути тією ж мовою, що й решта екрана, а не тією, що стоїть у телефоні. */
@Composable
private fun syncedAtFormat(): DateTimeFormatter {
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale) {
        DateTimeFormatter.ofPattern("d MMMM, HH:mm", locale).withZone(ZoneId.systemDefault())
    }
}

private fun themeModeLabelRes(mode: ThemeMode): Int = when (mode) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}

@Composable
private fun languageLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SYSTEM -> stringResource(R.string.language_system)
    // Назви мов не перекладаються: людина шукає в списку свою мову такою, як вона зветься.
    AppLanguage.UKRAINIAN -> "Українська"
    AppLanguage.ENGLISH -> "English"
    AppLanguage.RUSSIAN -> "Русский"
}
